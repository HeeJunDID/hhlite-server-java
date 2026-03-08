package kr.hhplus.be.server.reservation.application.scheduler;

import kr.hhplus.be.server.domain.Seat;
import kr.hhplus.be.server.repository.SeatRepository;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.infrastructure.persistence.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private final ReservationJpaRepository reservationJpaRepository;
    private final SeatRepository seatRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireReservations() {
        List<Reservation> expiredReservations = reservationJpaRepository
                .findByStatusAndExpiredAtBefore(ReservationStatus.PENDING, LocalDateTime.now());

        for (Reservation reservation : expiredReservations) {
            reservation.expire();

            seatRepository.findById(reservation.getSeatId()).ifPresent(Seat::release);
        }
    }
}
