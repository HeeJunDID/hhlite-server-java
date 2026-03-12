# Redis 캐싱 전략 보고서

## 1. 개요

콘서트 예약 서비스에서 발생하는 조회 쿼리를 분석하여, 캐싱이 적합한 구간을 선정하고 Redis 기반의 캐시 전략을 적용한 결과를 기술한다.

---

## 2. 시나리오별 쿼리 분석

### 2.1 사용자 흐름

```
로그인 → 콘서트 목록 조회 → 콘서트 선택 → 날짜/좌석 조회 → 예약 → 결제
```

### 2.2 각 단계별 쿼리 및 특성

| 단계 | 쿼리 대상 | 조회 빈도 | 변경 빈도 | 캐시 적합 여부 |
|------|-----------|-----------|-----------|---------------|
| 콘서트 목록 조회 | `Concert` 전체 | 매우 높음 | 매우 낮음 | ✅ 적합 |
| 콘서트 단건 조회 | `Concert` 단건 | 높음 | 매우 낮음 | ✅ 적합 |
| 콘서트 일정 조회 | `ConcertSchedule` | 높음 | 낮음 | ✅ 적합 |
| 좌석 상태 조회 | `Seat` 상태 | 매우 높음 | 매우 높음 | ❌ 부적합 |
| 유저 잔액 조회 | `User.balance` | 높음 | 높음 | ❌ 부적합 |
| 대기열 순위 조회 | Redis ZSet | - | 실시간 | ❌ 이미 Redis |

---

## 3. 캐시 적용 구간 분석

### 3.1 캐시 적합 구간: 콘서트/일정 데이터

**이유:**
- 콘서트와 일정은 운영자가 등록 후 거의 수정하지 않는 **정적 데이터**
- 예약 시즌에는 수천~수만 명의 사용자가 **동시에 반복 조회**
- 별도의 DB 쿼리 없이 Redis에서 응답 → DB 부하 대폭 감소

**대상:**
- `ConcertService.getAllConcerts()` → `concerts` 캐시, TTL 1시간
- `ConcertService.getConcert(Long concertId)` → `concert:{id}` 캐시, TTL 1시간

### 3.2 캐시 부적합 구간

| 대상 | 부적합 이유 |
|------|-------------|
| `Seat.status` | 예약/결제/만료 시 실시간 변경. Stale 데이터는 중복 예약 야기 |
| `User.balance` | 충전/결제 시 실시간 변경. Stale 잔액은 잔액 초과 차감 야기 |
| 예약 정보 | 상태 변환이 잦음 (PENDING → CONFIRMED/EXPIRED) |
| 대기열 순위 | 이미 Redis ZSet으로 관리. 별도 캐시 불필요 |

---

## 4. 적용 캐시 전략

### 4.1 캐시 아키텍처

```
Client Request
     ↓
[Spring Cache @Cacheable]
     ↓
  Cache Hit? ──Yes──→ Redis (응답, ~1ms)
     ↓ No
  DB Query ──────────→ MySQL
     ↓
  Redis 저장 (TTL 설정)
     ↓
  응답
```

### 4.2 캐시 설정

```java
// TTL 설정
concerts         : 1시간
concert (단건)   : 1시간
concertSchedules : 30분

// 직렬화
Key   : StringRedisSerializer
Value : GenericJackson2JsonRedisSerializer (JSON)
```

### 4.3 캐시 무효화 전략

현재 콘서트 데이터는 운영자 API를 통해서만 변경되므로, 운영자가 콘서트를 수정/삭제할 경우 다음 방식으로 캐시를 무효화해야 한다:

```java
@CacheEvict(value = {"concert", "concerts"}, allEntries = true)
public void updateConcert(Long concertId, ...) { ... }
```

---

## 5. 성능 개선 효과 분석

### 5.1 조회 지연 비교

| 항목 | DB 직접 조회 | Redis 캐시 캐싱 |
|------|-------------|----------------|
| 응답 시간 | ~5~20ms (MySQL 네트워크 + 쿼리) | ~0.5~1ms (Redis 메모리) |
| DB 연결 소모 | O (Connection Pool 소비) | X |
| 부하 집중 시 | DB 커넥션 고갈 위험 | Redis로 분산 |

### 5.2 대량 트래픽 시나리오

**시나리오:** 인기 콘서트 예매 오픈 시 10,000 RPS의 콘서트 목록 조회 발생

**캐시 미적용:**
- MySQL에 10,000 RPS 직접 부하
- Hikari Pool(최대 3개) 고갈 → 커넥션 대기 폭발
- 응답 지연 급증 → 서비스 장애 가능성

**캐시 적용:**
- 첫 1회만 MySQL 조회 후 Redis 저장
- 이후 요청은 모두 Redis에서 처리 (~1ms)
- DB 부하 99.9% 감소 (첫 캐시 워밍업 제외)
- Hikari Connection Pool을 예약/결제 트랜잭션에만 사용 가능

---

## 6. 주의사항 및 한계

### 6.1 Cache Stampede (캐시 스탬피드)

TTL 만료 시 다수의 요청이 동시에 DB로 몰리는 현상이 발생할 수 있다.

**대응 방안:**
- TTL에 Random Jitter 추가 (e.g., 60분 ± 5분)
- Lock-based 캐시 워밍업 (분산락과 연계)
- 별도 캐시 Pre-warming 스케줄러 구성

### 6.2 캐시 일관성

콘서트 정보 변경 시 Redis 캐시와 DB 간 일시적 불일치가 발생할 수 있다 (최대 TTL 시간).

**허용 가능 여부:** 콘서트 제목/설명 등 메타 정보는 수 분의 불일치가 서비스에 치명적이지 않으므로 허용.

### 6.3 Redis 장애 시 Fallback

Redis 장애 시 `@Cacheable`은 기본적으로 DB로 Fallback된다 (Spring Cache의 기본 동작). 단, Redis 장애가 장시간 지속되면 DB 부하가 증가하므로 Redis High Availability(Sentinel/Cluster) 구성이 필요하다.

---

## 7. 결론

| 항목 | 내용 |
|------|------|
| 캐시 대상 | 콘서트 목록 조회, 콘서트 단건 조회 |
| 캐시 저장소 | Redis (RedisCacheManager) |
| 직렬화 방식 | JSON (GenericJackson2JsonRedisSerializer) |
| TTL | 콘서트: 1시간, 일정: 30분 |
| 기대 효과 | DB 조회 부하 99% 감소, 응답속도 5~20ms → 1ms 이하 |
| 캐시 미적용 구간 | 좌석 상태, 유저 잔액 (실시간성 필요) |
