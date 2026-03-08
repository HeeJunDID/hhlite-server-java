package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.IntegrationTestSupport;
import kr.hhplus.be.server.domain.Seat;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.repository.SeatRepository;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.reservation.application.scheduler.ReservationExpiryScheduler;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.ReservationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationExpirySchedulerTest extends IntegrationTestSupport {

    @Autowired
    private ReservationExpiryScheduler scheduler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @BeforeEach
    void setUp() {
        reservationJpaRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("만료된 PENDING 예약이 EXPIRED로 변경되고 좌석이 AVAILABLE로 해제된다")
    void 만료된_예약_EXPIRED_좌석_AVAILABLE_해제() {
        // given: 유저, 좌석, 만료된 예약 세팅
        User user = userRepository.save(User.builder()
                .password("scheduler_test")
                .balance(100_000L)
                .build());

        Seat seat = seatRepository.save(Seat.builder()
                .scheduleId(1L)
                .seatNum(1)
                .price(50_000L)
                .status(Seat.STATUS_RESERVED)
                .grade("VIP")
                .build());

        // 만료 시간을 과거로 설정한 PENDING 예약 직접 생성
        Reservation expiredReservation = reservationJpaRepository.save(
                buildExpiredReservation(user.getUserId(), seat.getSeatId(), seat.getSeatNum(), 50_000L)
        );

        // when: 스케줄러 직접 호출
        scheduler.expireReservations();

        // then: 예약 상태가 EXPIRED로 변경됐는지 검증
        Reservation updatedReservation = reservationJpaRepository.findById(expiredReservation.getReservationId()).orElseThrow();
        assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        // then: 좌석 상태가 AVAILABLE로 해제됐는지 검증
        Seat updatedSeat = seatRepository.findById(seat.getSeatId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(Seat.STATUS_AVAILABLE);
    }

    @Test
    @DisplayName("만료되지 않은 PENDING 예약은 스케줄러 실행 후 상태가 변경되지 않는다")
    void 만료되지_않은_예약은_상태_변경_없음() {
        // given: 유저, 좌석, 아직 만료되지 않은 예약 세팅
        User user = userRepository.save(User.builder()
                .password("scheduler_test2")
                .balance(100_000L)
                .build());

        Seat seat = seatRepository.save(Seat.builder()
                .scheduleId(1L)
                .seatNum(2)
                .price(50_000L)
                .status(Seat.STATUS_RESERVED)
                .grade("R")
                .build());

        Reservation activeReservation = reservationJpaRepository.save(
                Reservation.create(user.getUserId(), seat.getSeatId(), seat.getSeatNum(), 50_000L)
        );

        // when: 스케줄러 직접 호출
        scheduler.expireReservations();

        // then: 예약 상태가 PENDING으로 유지됐는지 검증
        Reservation updatedReservation = reservationJpaRepository.findById(activeReservation.getReservationId()).orElseThrow();
        assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.PENDING);

        // then: 좌석 상태가 RESERVED로 유지됐는지 검증
        Seat updatedSeat = seatRepository.findById(seat.getSeatId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(Seat.STATUS_RESERVED);
    }

    private Reservation buildExpiredReservation(Long userId, Long seatId, int seatNum, long amount) {
        Reservation reservation = Reservation.create(userId, seatId, seatNum, amount);
        try {
            var field = Reservation.class.getDeclaredField("expiredAt");
            field.setAccessible(true);
            field.set(reservation, LocalDateTime.now().minusMinutes(10));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return reservation;
    }
}
