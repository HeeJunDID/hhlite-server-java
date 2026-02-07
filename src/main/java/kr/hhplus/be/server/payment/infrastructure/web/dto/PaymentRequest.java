package kr.hhplus.be.server.payment.infrastructure.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentRequest {

    private Long reservationId;
    private Long userId;
}
