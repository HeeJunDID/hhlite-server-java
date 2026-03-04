package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.IntegrationTestSupport;
import kr.hhplus.be.server.domain.Seat;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentCommand;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentUseCase;
import kr.hhplus.be.server.repository.SeatRepository;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.infrastructure.persistence.ReservationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceConcurrencyIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        reservationJpaRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();

        // 잔액 50,000원을 가진 유저 1명 생성
        testUser = userRepository.save(User.builder()
                .password("concurrency_test")
                .balance(50_000L)
                .build());
    }

    @Test
    @DisplayName("잔액 50,000원 유저에게 5건 동시 결제 시도 시 1건만 성공하고 잔액이 0원이 된다")
    void 동시_결제_시도시_한건만_성공하고_잔액이_0원() throws InterruptedException {
        // given: 5개 좌석(각 50,000원)과 해당 유저의 PENDING 예약 5건 생성
        int concurrentCount = 5;
        List<Long> reservationIds = new ArrayList<>();

        for (int i = 0; i < concurrentCount; i++) {
            Seat seat = seatRepository.save(Seat.builder()
                    .scheduleId(1L)
                    .seatNum(i + 1)
                    .price(50_000L)
                    .status(Seat.STATUS_RESERVED)
                    .grade("VIP")
                    .build());

            Reservation reservation = reservationJpaRepository.save(
                    Reservation.create(testUser.getUserId(), seat.getSeatId(), seat.getSeatNum(), 50_000L)
            );
            reservationIds.add(reservation.getReservationId());
        }

        ExecutorService executorService = Executors.newFixedThreadPool(concurrentCount);
        CountDownLatch latch = new CountDownLatch(concurrentCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 5개 스레드가 동시에 각각 다른 예약으로 결제 시도
        for (Long reservationId : reservationIds) {
            executorService.submit(() -> {
                try {
                    processPaymentUseCase.execute(
                            new ProcessPaymentCommand(reservationId, testUser.getUserId())
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 정확히 1건만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(concurrentCount - 1);

        // 잔액이 0원이어야 하고 절대 음수가 되어서는 안 됨
        User updatedUser = userRepository.findById(testUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getBalance()).isGreaterThanOrEqualTo(0L);
        assertThat(updatedUser.getBalance()).isEqualTo(0L);
    }
}
