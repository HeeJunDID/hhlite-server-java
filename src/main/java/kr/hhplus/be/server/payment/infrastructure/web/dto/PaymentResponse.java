package kr.hhplus.be.server.payment.infrastructure.web.dto;

import kr.hhplus.be.server.payment.application.port.in.PaymentResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {

    private Long paymentId;
    private Long reservationId;
    private Long userId;
    private long paymentAmt;
    private String status;
    private LocalDateTime paidAt;

    public static PaymentResponse from(PaymentResult result) {
        return PaymentResponse.builder()
                .paymentId(result.paymentId())
                .reservationId(result.reservationId())
                .userId(result.userId())
                .paymentAmt(result.paymentAmount())
                .status(result.status().name())
                .paidAt(result.paidAt())
                .build();
    }
}
