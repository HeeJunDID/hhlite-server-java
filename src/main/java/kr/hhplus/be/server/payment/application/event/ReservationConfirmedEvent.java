package kr.hhplus.be.server.payment.application.event;

import java.time.LocalDateTime;

public record ReservationConfirmedEvent(
        Long paymentId,
        Long reservationId,
        Long userId,
        Long concertId,
        long amount,
        LocalDateTime paidAt
) {
}
