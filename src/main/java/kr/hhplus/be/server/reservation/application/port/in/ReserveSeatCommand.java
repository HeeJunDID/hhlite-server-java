package kr.hhplus.be.server.reservation.application.port.in;

import java.util.Objects;

public record ReserveSeatCommand(
        Long userId,
        Long scheduleId,
        Long seatId
) {
    public ReserveSeatCommand {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        Objects.requireNonNull(seatId, "seatId must not be null");
    }
}
