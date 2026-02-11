package kr.hhplus.be.server.payment.application.port.in;

import kr.hhplus.be.server.payment.domain.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResult(
        Long paymentId,
        Long reservationId,
        Long userId,
        long paymentAmount,
        PaymentStatus status,
        LocalDateTime paidAt
) {
}
