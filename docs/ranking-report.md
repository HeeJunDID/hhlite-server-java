# Redis 기반 콘서트 랭킹 설계 보고서

## 1. 개요

콘서트 예약 서비스에서 **빠른 매진 랭킹**을 Redis Sorted Set(ZSet)을 활용하여 구현한다.
인기 콘서트(판매 완료 좌석 수 기준 상위 N개)를 실시간으로 집계하고 조회할 수 있도록 설계한다.

---

## 2. 랭킹 기준

| 항목 | 내용 |
|------|------|
| 랭킹 지표 | 콘서트별 판매 완료(SOLD) 좌석 수 |
| 갱신 시점 | 결제 완료 시 즉시 갱신 |
| 저장소 | Redis Sorted Set |
| 키 | `ranking:concert:sold` |

**판매 수가 많을수록** 상위 순위 → ZSet의 score가 높을수록 앞에 위치 (ZREVRANGE 사용)

---

## 3. 아키텍처 설계

### 3.1 헥사고날 아키텍처 적용

```
[결제 완료 이벤트]
        ↓
ProcessPaymentService
        ↓
LoadConcertIdBySeatPort  ←→  SeatScheduleAdapter (JPA: seat→schedule→concert)
        ↓
ConcertRankingPort       ←→  RedisRankingAdapter (Redis ZINCRBY)
```

```
[랭킹 조회 요청]
GET /api/concerts/rankings?top=10
        ↓
RankingController
        ↓
GetConcertRankingUseCase
        ↓
ConcertRankingService
        ↓
ConcertRankingPort       ←→  RedisRankingAdapter (Redis ZREVRANGE)
```

### 3.2 모듈 구조

```
ranking/
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── GetConcertRankingUseCase.java
│   │   │   └── RankingEntry.java
│   │   └── out/
│   │       └── ConcertRankingPort.java
│   └── service/
│       └── ConcertRankingService.java
└── infrastructure/
    ├── persistence/
    │   └── RedisRankingAdapter.java
    └── web/
        ├── RankingController.java
        └── dto/
            └── RankingResponse.java
```

---

## 4. Redis 데이터 구조

### 4.1 Sorted Set 활용

```
Key: ranking:concert:sold

Members (concertId → score):
  "1001"  →  342   (1위: 342석 판매)
  "1005"  →  287   (2위: 287석 판매)
  "1002"  →  195   (3위: 195석 판매)
  ...
```

### 4.2 주요 Redis 명령어

| 작업 | 명령어 | 사용 시점 |
|------|--------|-----------|
| 판매 수 증가 | `ZINCRBY ranking:concert:sold 1 {concertId}` | 결제 완료 시 |
| 상위 N 조회 | `ZREVRANGE ranking:concert:sold 0 N-1 WITHSCORES` | 랭킹 조회 API |

---

## 5. 데이터 흐름

```
1. 사용자 결제 완료
2. ProcessPaymentService.execute()
   → markSeatAsSoldPort.markAsSold(seatId)           // DB: 좌석 SOLD 처리
   → loadConcertIdBySeatPort.loadConcertIdBySeatId(seatId)  // DB: seatId → concertId
   → concertRankingPort.incrementSoldCount(concertId)  // Redis: ZINCRBY +1
3. 랭킹 실시간 반영
```

---

## 6. concertId 조회 전략

결제 시점에는 `seatId`만 알고 있으므로, 아래 쿼리로 `concertId`를 조회한다.

```sql
SELECT cs.concert_id
FROM concert_schedule cs
JOIN seat s ON s.schedule_id = cs.schedule_id
WHERE s.seat_id = :seatId
```

- `Seat.scheduleId` → `ConcertSchedule.scheduleId` → `ConcertSchedule.concertId`
- 조회 실패 시 랭킹 갱신을 건너뛰고 경고 로그만 출력 (결제 실패 방지)

---

## 7. API 명세

### GET /api/concerts/rankings

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| top | int | 10 | 조회할 상위 순위 수 (최대 100) |

**응답 예시:**
```json
[
  { "rank": 1, "concertId": 1001, "soldCount": 342 },
  { "rank": 2, "concertId": 1005, "soldCount": 287 },
  { "rank": 3, "concertId": 1002, "soldCount": 195 }
]
```

---

## 8. 성능 특성

| 항목 | 내용 |
|------|------|
| 갱신 시간복잡도 | O(log N) — ZSet ZINCRBY |
| 조회 시간복잡도 | O(log N + K) — ZREVRANGE (K = 조회 수) |
| DB 부하 | 결제 완료 시 1회 조인 쿼리 추가 (기존 좌석 처리와 동일 트랜잭션) |
| Redis 부하 | 매우 낮음 — 단일 키 ZSet 연산 |

---

## 9. 주의사항 및 한계

### 9.1 Redis 장애 시

랭킹 갱신 실패가 결제 자체를 실패시키지 않도록 설계했다.
`loadConcertIdBySeatPort`가 비어 있으면 경고 로그만 출력 후 정상 응답.

단, Redis가 완전히 다운된 경우 `ZINCRBY` 호출 자체에서 예외가 발생하므로,
추후 try-catch로 랭킹 갱신을 Non-critical 처리하거나 이벤트 기반 비동기 처리를 고려할 수 있다.

### 9.2 TTL 미설정

랭킹 데이터는 누적 집계가 목적이므로 TTL을 설정하지 않는다.
기간별 랭킹이 필요하다면 키에 날짜를 포함(`ranking:concert:sold:2026-03`)하고 주기적으로 초기화하는 방식을 고려할 수 있다.

### 9.3 서버 재시작 시 데이터 유지

Redis AOF/RDB 영속성 설정을 통해 서버 재시작 후에도 랭킹 데이터가 보존되어야 한다.

---

## 10. 결론

| 항목 | 내용 |
|------|------|
| 랭킹 기준 | 콘서트별 판매 완료 좌석 수 |
| 저장소 | Redis Sorted Set (`ranking:concert:sold`) |
| 갱신 시점 | 결제 완료 시 동기 갱신 |
| 조회 API | `GET /api/concerts/rankings?top=N` |
| 응답 속도 | ~1ms (Redis 메모리 연산) |
