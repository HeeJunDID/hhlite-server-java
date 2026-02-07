package kr.hhplus.be.server.controller.payment.dto;

import kr.hhplus.be.server.domain.Payment;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Getter
@Builder
public class PaymentResponse {

    private Long paymentId;
    private Long reservationId;
    private Long userId;
    private long paymentAmt;
    private String status;
    private Date paidAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .reservationId(payment.getReservationId())
                .userId(payment.getUserId())
                .paymentAmt(payment.getPaymentAmt())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
