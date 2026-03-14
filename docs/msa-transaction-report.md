# MSA 도메인 분리 및 트랜잭션 처리 설계 보고서

## 1. 개요

현재 콘서트 예약 서비스는 단일 모놀리식 애플리케이션으로 구성되어 있다.
서비스 규모 확장 시 MSA 형태로 전환할 경우, 각 도메인의 독립 배포 단위와 분산 트랜잭션 처리 전략을 사전에 설계해야 한다.

---

## 2. 도메인 배포 단위 설계

### 2.1 서비스 분리 기준

| 기준 | 내용 |
|------|------|
| 변경 빈도 | 변경이 잦은 도메인은 독립 배포 단위로 분리 |
| 팀 소유권 | 각 팀이 독립적으로 개발·배포할 수 있어야 함 |
| 트래픽 패턴 | 트래픽이 집중되는 도메인은 별도 스케일아웃 필요 |
| 데이터 응집도 | 밀접하게 연관된 데이터는 같은 서비스에 유지 |

### 2.2 제안하는 MSA 서비스 분리

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Queue Service │  │ Concert Service │  │   User Service  │
│                 │  │                 │  │                 │
│ - 대기열 토큰   │  │ - 콘서트 정보   │  │ - 회원 정보     │
│ - 순위 관리     │  │ - 일정 관리     │  │ - 잔액 관리     │
│ - 입장 제어     │  │ - 좌석 관리     │  │                 │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              │
              ┌───────────────┴────────────────┐
              │                                │
   ┌──────────┴──────────┐      ┌─────────────┴──────────┐
   │ Reservation Service │      │    Payment Service      │
   │                     │      │                         │
   │ - 예약 생성/관리    │      │ - 결제 처리             │
   │ - 예약 만료 처리    │      │ - 결제 내역 관리        │
   └─────────────────────┘      └─────────────────────────┘
                              │
              ┌───────────────┴────────────────┐
              │        Ranking Service         │
              │                                │
              │ - 콘서트 판매 랭킹 집계        │
              │ - 랭킹 조회 API                │
              └────────────────────────────────┘
```

### 2.3 각 서비스 책임

| 서비스 | 주요 책임 | DB |
|--------|----------|----|
| Queue Service | 대기열 관리, 토큰 발급·검증 | Redis |
| Concert Service | 콘서트·일정·좌석 정보 관리 | MySQL + Redis(캐시) |
| User Service | 회원 정보, 잔액 충전·조회 | MySQL |
| Reservation Service | 예약 생성, 만료 처리 | MySQL |
| Payment Service | 결제 처리, 결제 내역 | MySQL |
| Ranking Service | 판매 랭킹 집계·조회 | Redis |

---

## 3. 분리에 따른 트랜잭션 처리 한계

현재 모놀리식 구조에서 결제 흐름은 **단일 DB 트랜잭션**으로 원자적으로 처리된다.

```
[현재 단일 트랜잭션]
BEGIN TX
  1. 예약 조회 및 검증
  2. 사용자 잔액 차감        ← User DB
  3. 결제 정보 저장          ← Payment DB
  4. 결제 내역 저장          ← Payment DB
  5. 예약 확정               ← Reservation DB
  6. 좌석 판매 처리          ← Concert DB
COMMIT TX
```

MSA로 분리되면 각 서비스가 독립 DB를 가지므로 **분산 트랜잭션** 문제가 발생한다.

### 3.1 주요 장애 시나리오

| 시나리오 | 결과 |
|----------|------|
| 잔액 차감 성공 → 결제 저장 실패 | 돈은 빠졌지만 결제 기록 없음 |
| 결제 확정 성공 → 예약 확정 실패 | 결제됐지만 예약이 PENDING 상태 유지 |
| 예약 확정 성공 → 좌석 SOLD 처리 실패 | 예약은 됐지만 좌석이 AVAILABLE 상태 → 중복 예약 가능 |

---

## 4. 해결 방안

### 4.1 SAGA 패턴 (권장)

분산 트랜잭션을 여러 로컬 트랜잭션의 연속으로 처리하고, 각 단계 실패 시 **보상 트랜잭션**으로 롤백한다.

#### Choreography-based SAGA (이벤트 기반)

```
Payment Service                  Reservation Service           User Service
      │                                  │                          │
      │── PaymentInitiated ─────────────>│                          │
      │                                  │── ReservationConfirming ->│
      │                                  │                          │ (잔액 차감)
      │                                  │<─ BalanceDeducted ───────│
      │                                  │ (예약 확정)
      │<── ReservationConfirmed ─────────│
      │ (결제 완료 처리)
      │── PaymentCompleted ──────────────────────────────────────>Concert Service
                                                                  (좌석 SOLD 처리)
```

**보상 트랜잭션 예시:**
- 결제 실패 시 → `BalanceRefundRequested` 이벤트 발행 → User Service 잔액 복구
- 예약 확정 실패 시 → `ReservationCancelRequested` 이벤트 발행 → Reservation Service 취소 처리

#### Orchestration-based SAGA

별도 **Payment Orchestrator**가 각 서비스 호출 순서와 보상 트랜잭션을 중앙에서 제어.

```
Payment Orchestrator
  1. User Service.deductBalance()
     ├─ 성공 → 2단계 진행
     └─ 실패 → 종료 (보상 불필요)
  2. Reservation Service.confirm()
     ├─ 성공 → 3단계 진행
     └─ 실패 → User Service.refundBalance() (보상)
  3. Concert Service.markSeatAsSold()
     ├─ 성공 → 완료
     └─ 실패 → Reservation Service.cancel() + User Service.refundBalance() (보상)
```

### 4.2 Outbox Pattern (이벤트 유실 방지)

이벤트 발행 시 네트워크 장애로 인한 **이벤트 유실**을 방지한다.

```
[Outbox Pattern 흐름]
BEGIN TX
  1. 비즈니스 로직 처리 (잔액 차감, 결제 저장 등)
  2. outbox 테이블에 이벤트 INSERT
COMMIT TX

[별도 스케줄러/CDC]
  outbox 테이블 폴링 → Kafka/RabbitMQ로 이벤트 발행 → published 처리
```

**장점:** DB 커밋과 이벤트 발행을 같은 로컬 트랜잭션으로 묶어 유실 제거

### 4.3 현재 적용된 해결책 (Application Event)

모놀리식 구조에서도 **관심사 분리**를 위해 Spring Application Event를 적용했다.

```
ProcessPaymentService
  [TX 내부]
  1~7. 결제 핵심 로직 처리
  8. ApplicationEventPublisher.publishEvent(ReservationConfirmedEvent)

  [TX 커밋 후]
  ReservationEventListener (@TransactionalEventListener AFTER_COMMIT)
  → @Async 비동기로 DataPlatformClient.send() 호출
  → 실패해도 결제 응답에 영향 없음
```

---

## 5. 도메인 간 경계 설계 원칙

| 원칙 | 내용 |
|------|------|
| **데이터 소유권** | 각 서비스는 자신의 데이터만 직접 수정. 다른 서비스 DB 직접 접근 금지 |
| **API 계약** | 서비스 간 통신은 REST API 또는 이벤트(Kafka)로만 |
| **이벤트 소싱** | 상태 변경은 이벤트로 표현하여 다른 서비스가 구독 가능하게 |
| **최종 일관성** | 분산 환경에서는 즉시 일관성 대신 최종 일관성(Eventual Consistency) 허용 |

---

## 6. 결론

| 항목 | 내용 |
|------|------|
| 배포 단위 | Queue / Concert / User / Reservation / Payment / Ranking 6개 서비스 |
| 트랜잭션 전략 | Choreography-based SAGA + Outbox Pattern |
| 이벤트 브로커 | Kafka (고가용성 이벤트 스트리밍) |
| 현재 적용 | Spring Application Event (@TransactionalEventListener AFTER_COMMIT) |
| 핵심 가치 | 트랜잭션 원자성 → SAGA 보상 트랜잭션으로 대체, 관심사 분리 유지 |
