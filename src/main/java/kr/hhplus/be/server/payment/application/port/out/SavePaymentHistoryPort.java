package kr.hhplus.be.server.payment.application.port.out;

import kr.hhplus.be.server.payment.domain.PaymentHistory;

public interface SavePaymentHistoryPort {
    PaymentHistory save(PaymentHistory paymentHistory);
}
