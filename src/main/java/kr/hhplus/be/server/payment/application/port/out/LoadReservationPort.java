package kr.hhplus.be.server.payment.application.port.out;

import java.util.Optional;

public interface LoadReservationPort {
    Optional<ReservationInfo> loadById(Long reservationId);
}
