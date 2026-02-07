package kr.hhplus.be.server.payment.application.port.in;

import java.util.Objects;

public record ProcessPaymentCommand(
        Long reservationId,
        Long userId
) {
    public ProcessPaymentCommand {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
    }
}
