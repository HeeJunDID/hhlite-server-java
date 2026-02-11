package kr.hhplus.be.server.payment.application.port.in;

public interface ProcessPaymentUseCase {
    PaymentResult execute(ProcessPaymentCommand command);
}
