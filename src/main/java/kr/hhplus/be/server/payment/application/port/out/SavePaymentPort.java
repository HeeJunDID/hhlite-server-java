package kr.hhplus.be.server.payment.application.port.out;

import kr.hhplus.be.server.payment.domain.Payment;

public interface SavePaymentPort {
    Payment save(Payment payment);
}
