# 동시성 이슈 해결 전략 보고서

## 1. 문제 상황

콘서트 예약 서비스에서 발생할 수 있는 3가지 동시성 이슈를 분석하였다.

### 1-1. 좌석 임시 배정 중복 예약

**시나리오:** 동일 좌석에 여러 유저가 동시에 예약 요청
**증상:** 하나의 좌석에 여러 예약이 생성되어 좌석 중복 점유
**원인:** `SELECT → 상태 검증 → UPDATE` 사이에 다른 트랜잭션이 끼어드는 TOCTOU(Time-Of-Check-Time-Of-Use) 문제

### 1-2. 잔액 차감 Race Condition

**시나리오:** 잔액 50,000원인 유저에게 동시에 5건의 결제(각 50,000원) 요청
**증상:** 여러 트랜잭션이 동시에 잔액을 읽어 모두 "잔액 충분"으로 판단, 중복 차감으로 음수 잔액 발생 가능
**원인:** `loadById(userId)` → 잔액 검증 → `deductBalance(userId)` 사이에 락이 없어 다수의 트랜잭션이 동일 잔액을 동시에 읽는 문제

### 1-3. 만료된 예약/좌석 자동 해제 부재

**시나리오:** 유저가 좌석을 예약(PENDING) 후 5분 내 결제하지 않는 경우
**증상:** 예약은 만료(isExpired=true)되었지만 좌석이 RESERVED 상태로 유지되어 다른 유저가 예약 불가
**원인:** 만료 예약을 주기적으로 정리하는 스케줄러가 없어 좌석이 영구 점유 상태에 빠짐

---

## 2. 해결 전략

### 2-1. 좌석 임시 배정: 비관적 락 (기존 구현)

**파일:** `SeatRepository.java`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Seat s WHERE s.seatId = :seatId")
Optional<Seat> findByIdWithLock(@Param("seatId") Long seatId);
```

**전략:** 좌석 조회 시 `SELECT ... FOR UPDATE`를 사용하여 행 단위 배타 락을 획득.
첫 번째 트랜잭션이 락을 보유하는 동안 다른 트랜잭션은 대기하므로, 락 해제 후 AVAILABLE 상태가 아님을 확인하여 중복 예약을 방지한다.

**선택 이유:**
- 좌석은 충돌 가능성이 높은 인기 자원 → 낙관적 락보다 비관적 락이 적합
- 롤백 비용이 크므로 선제적으로 충돌을 방지하는 것이 효율적

### 2-2. 잔액 차감: 비관적 락 추가 적용

**파일:** `UserRepository.java`, `UserPortAdapter.java`

```java
// UserRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM User u WHERE u.userId = :userId")
Optional<User> findByIdWithLock(@Param("userId") Long userId);

// UserPortAdapter
@Override
public Optional<UserInfo> loadById(Long userId) {
    return userRepository.findByIdWithLock(userId)  // 잔액 검증도 락 하에 수행
            .map(user -> new UserInfo(user.getUserId(), user.getBalance()));
}

@Override
public void deductBalance(Long userId, long amount) {
    User user = userRepository.findByIdWithLock(userId)  // 차감도 락 하에 수행
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.usePoint(amount);
}
```

**전략:** `ProcessPaymentService`에서 잔액 조회(`loadUserPort.loadById`) → 잔액 검증 → 잔액 차감(`deductUserBalancePort.deductBalance`) 전 과정을 단일 트랜잭션의 배타 락 하에 직렬화한다.
`loadById`(검증용)와 `deductBalance`(차감용) **모두** 락이 걸린 조회를 사용해야 한다. `loadById`만 락을 적용하면 두 번째 `findById` 호출 시 락이 빠져 같은 Race Condition이 재발할 수 있다.

**선택 이유:**
- 한 유저의 잔액에 대한 동시 수정은 직렬화가 필요
- 동일 트랜잭션 내에서 두 번 조회하므로 첫 조회에서만 락을 잡으면 일관성 보장 가능하지만, 두 포트가 별도 메서드로 분리되어 있으므로 각각 락을 보장하는 것이 안전

### 2-3. 예약 만료 해제: 스케줄러

**파일:** `ReservationExpiryScheduler.java`

```java
@Scheduled(fixedDelay = 60_000)
@Transactional
public void expireReservations() {
    List<Reservation> expiredReservations = reservationJpaRepository
            .findByStatusAndExpiredAtBefore(ReservationStatus.PENDING, LocalDateTime.now());

    for (Reservation reservation : expiredReservations) {
        reservation.expire();                                           // PENDING → EXPIRED
        seatRepository.findById(reservation.getSeatId())
                .ifPresent(Seat::release);                             // RESERVED → AVAILABLE
    }
}
```

**전략:** 1분마다 `expiredAt < now` 이고 `PENDING` 상태인 예약을 일괄 조회하여 `EXPIRED`로 변경하고, 해당 좌석을 `AVAILABLE`로 해제한다.

**선택 이유:**
- 별도 락 없이 주기적 배치로 처리 가능 → 단순하고 효과적
- 결제 흐름(`isExpired()` 체크)과 별도로 백그라운드에서 정리하므로 결제 경로에 부하 없음
- fixedDelay로 이전 실행 완료 후 1분 대기 → 중첩 실행 방지

---

## 3. 테스트 결과

### 3-1. 잔액 동시성 테스트

**테스트:** `BalanceConcurrencyIntegrationTest`

| 항목 | 결과 |
|------|------|
| 동시 결제 시도 | 5건 |
| 성공 건수 | 1건 ✅ |
| 실패 건수 | 4건 ✅ |
| 최종 잔액 | 0원 (음수 없음) ✅ |

비관적 락 적용 후 동시 5건 결제 시도 시 정확히 1건만 성공하고 잔액이 0원임을 확인.

### 3-2. 스케줄러 테스트

**테스트:** `ReservationExpirySchedulerTest`

| 시나리오 | 결과 |
|----------|------|
| 만료된 PENDING 예약 → EXPIRED 변경 | ✅ |
| 만료된 예약의 RESERVED 좌석 → AVAILABLE 해제 | ✅ |
| 미만료 PENDING 예약 상태 유지 | ✅ |
| 미만료 예약의 RESERVED 좌석 상태 유지 | ✅ |

### 3-3. 전체 통합 테스트 결과

```
BalanceConcurrencyIntegrationTest  : tests=1, failures=0, errors=0 ✅
ReservationExpirySchedulerTest     : tests=2, failures=0, errors=0 ✅
ConcertReservationIntegrationTest  : tests=3, failures=0, errors=0 ✅
PaymentIntegrationTest             : tests=4, failures=0, errors=0 ✅
QueueTokenIntegrationTest          : tests=6, failures=0, errors=0 ✅
UserIntegrationTest                : tests=5, failures=0, errors=0 ✅
```

모든 통합 테스트 통과 (21건, failures=0, errors=0).

---

## 4. 정리

| 이슈 | 해결 방식 | 구현 파일 |
|------|-----------|-----------|
| 좌석 중복 예약 | 비관적 락 (PESSIMISTIC_WRITE) | `SeatRepository.findByIdWithLock` |
| 잔액 음수 차감 | 비관적 락 (PESSIMISTIC_WRITE) | `UserRepository.findByIdWithLock`, `UserPortAdapter` |
| 만료 예약 미해제 | 주기적 스케줄러 (1분) | `ReservationExpiryScheduler` |
