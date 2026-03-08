package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.IntegrationTestSupport;
import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.domain.Seat;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.payment.application.port.in.PaymentResult;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentCommand;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentUseCase;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.queue.application.port.in.IssueTokenCommand;
import kr.hhplus.be.server.queue.application.port.in.IssueTokenUseCase;
import kr.hhplus.be.server.queue.application.port.in.TokenResult;
import kr.hhplus.be.server.queue.domain.TokenStatus;
import kr.hhplus.be.server.repository.SeatRepository;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.reservation.application.port.in.ReservationResult;
import kr.hhplus.be.server.reservation.application.port.in.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.ReserveSeatUseCase;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.ReservationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcertReservationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueTokenUseCase issueTokenUseCase;

    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    private User testUser;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        reservationJpaRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 유저 생성 (잔액 100,000원)
        testUser = userRepository.save(User.builder()
                .password("test123")
                .balance(100_000L)
                .build());

        // 테스트 좌석 생성 (가격 50,000원)
        testSeat = seatRepository.save(Seat.builder()
                .scheduleId(1L)
                .seatNum(1)
                .price(50_000L)
                .status(Seat.STATUS_AVAILABLE)
                .grade("VIP")
                .build());
    }

    @Test
    @DisplayName("토큰발급 -> 좌석예약 -> 결제 완료 전체 흐름 성공")
    void 토큰발급_좌석예약_결제_전체흐름_성공() {
        // given
        Long concertId = 1L;

        // when: 1. 토큰 발급
        TokenResult tokenResult = issueTokenUseCase.issue(
                new IssueTokenCommand(testUser.getUserId(), concertId)
        );

        // then: 토큰 발급 확인
        assertThat(tokenResult).isNotNull();
        assertThat(tokenResult.token()).isNotNull();
        assertThat(tokenResult.status()).isEqualTo(TokenStatus.WAITING);
        assertThat(tokenResult.userId()).isEqualTo(testUser.getUserId());

        // when: 2. 좌석 예약
        ReservationResult reservationResult = reserveSeatUseCase.execute(
                new ReserveSeatCommand(testUser.getUserId(), testSeat.getScheduleId(), testSeat.getSeatId())
        );

        // then: 예약 확인
        assertThat(reservationResult).isNotNull();
        assertThat(reservationResult.reservationId()).isNotNull();
        assertThat(reservationResult.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservationResult.amount()).isEqualTo(50_000L);

        // when: 3. 결제
        PaymentResult paymentResult = processPaymentUseCase.execute(
                new ProcessPaymentCommand(reservationResult.reservationId(), testUser.getUserId())
        );

        // then: 결제 완료 확인
        assertThat(paymentResult).isNotNull();
        assertThat(paymentResult.paymentId()).isNotNull();
        assertThat(paymentResult.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(paymentResult.paymentAmount()).isEqualTo(50_000L);

        // 유저 잔액 차감 확인
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getBalance()).isEqualTo(50_000L);

        // 좌석 상태 확인 (SOLD)
        Seat updatedSeat = seatRepository.findById(testSeat.getSeatId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(Seat.STATUS_SOLD);
    }

    @Test
    @DisplayName("예약 만료 후 좌석 재예약 가능")
    @Transactional
    void 예약만료후_좌석_재예약가능() {
        // given: 첫 번째 유저가 좌석 예약
        User firstUser = testUser;
        ReservationResult firstReservation = reserveSeatUseCase.execute(
                new ReserveSeatCommand(firstUser.getUserId(), testSeat.getScheduleId(), testSeat.getSeatId())
        );

        assertThat(firstReservation.status()).isEqualTo(ReservationStatus.PENDING);

        // 예약을 만료 상태로 변경 (직접 DB 업데이트)
        Reservation reservation = reservationJpaRepository.findById(firstReservation.reservationId()).orElseThrow();
        // Reflection을 사용하여 expiredAt을 과거로 설정
        setExpiredAt(reservation, LocalDateTime.now().minusMinutes(10));
        reservation.expire();
        reservationJpaRepository.save(reservation);

        // 좌석을 다시 AVAILABLE로 변경
        Seat seat = seatRepository.findById(testSeat.getSeatId()).orElseThrow();
        seat.release();
        seatRepository.save(seat);

        // given: 두 번째 유저 생성
        User secondUser = userRepository.save(User.builder()
                .password("test456")
                .balance(100_000L)
                .build());

        // when: 두 번째 유저가 같은 좌석 예약
        ReservationResult secondReservation = reserveSeatUseCase.execute(
                new ReserveSeatCommand(secondUser.getUserId(), testSeat.getScheduleId(), testSeat.getSeatId())
        );

        // then: 두 번째 예약 성공
        assertThat(secondReservation).isNotNull();
        assertThat(secondReservation.reservationId()).isNotNull();
        assertThat(secondReservation.userId()).isEqualTo(secondUser.getUserId());
        assertThat(secondReservation.status()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("동시 좌석 요청 시 한 명만 예약 성공")
    void 동시요청시_한명만_예약성공() throws InterruptedException {
        // given: 10명의 유저 생성
        int userCount = 10;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            users.add(userRepository.save(User.builder()
                    .password("test" + i)
                    .balance(100_000L)
                    .build()));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(userCount);
        CountDownLatch latch = new CountDownLatch(userCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 10명이 동시에 같은 좌석 예약 시도
        for (User user : users) {
            executorService.submit(() -> {
                try {
                    reserveSeatUseCase.execute(
                            new ReserveSeatCommand(user.getUserId(), testSeat.getScheduleId(), testSeat.getSeatId())
                    );
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 1명만 성공, 9명 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(userCount - 1);

        // 좌석 상태 확인 (RESERVED)
        Seat updatedSeat = seatRepository.findById(testSeat.getSeatId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(Seat.STATUS_RESERVED);
    }

    private void setExpiredAt(Reservation reservation, LocalDateTime expiredAt) {
        try {
            var field = Reservation.class.getDeclaredField("expiredAt");
            field.setAccessible(true);
            field.set(reservation, expiredAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
