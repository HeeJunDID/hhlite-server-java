package kr.hhplus.be.server.reservation.application.port.in;

import kr.hhplus.be.server.reservation.domain.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResult(
        Long reservationId,
        Long userId,
        Long seatId,
        int seatNum,
        long amount,
        ReservationStatus status,
        LocalDateTime expiredAt
) {
}
