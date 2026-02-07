package kr.hhplus.be.server.payment.infrastructure.persistence;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.payment.application.port.out.ConfirmReservationPort;
import kr.hhplus.be.server.payment.application.port.out.LoadReservationPort;
import kr.hhplus.be.server.payment.application.port.out.ReservationInfo;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.infrastructure.persistence.ReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReservationPortAdapter implements LoadReservationPort, ConfirmReservationPort {

    private final ReservationJpaRepository reservationJpaRepository;

    @Override
    public Optional<ReservationInfo> loadById(Long reservationId) {
        return reservationJpaRepository.findById(reservationId)
                .map(this::toReservationInfo);
    }

    @Override
    public void confirm(Long reservationId, Long paymentId) {
        Reservation reservation = reservationJpaRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        reservation.confirm(paymentId);
    }

    private ReservationInfo toReservationInfo(Reservation reservation) {
        return new ReservationInfo(
                reservation.getReservationId(),
                reservation.getUserId(),
                reservation.getSeatId(),
                reservation.getAmount(),
                reservation.getStatus().name(),
                reservation.getExpiredAt()
        );
    }
}
