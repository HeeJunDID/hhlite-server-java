package kr.hhplus.be.server.payment.application.port.out;

import java.time.LocalDateTime;

public record ReservationInfo(
        Long reservationId,
        Long userId,
        Long seatId,
        long amount,
        String status,
        LocalDateTime expiredAt
) {
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    public boolean isExpired() {
        return expiredAt != null && LocalDateTime.now().isAfter(expiredAt);
    }

    public boolean isPaid() {
        return STATUS_CONFIRMED.equals(status);
    }
}
