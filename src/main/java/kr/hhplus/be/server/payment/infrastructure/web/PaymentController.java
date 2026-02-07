package kr.hhplus.be.server.payment.infrastructure.web;

import kr.hhplus.be.server.payment.application.port.in.PaymentResult;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentCommand;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentUseCase;
import kr.hhplus.be.server.payment.infrastructure.web.dto.PaymentRequest;
import kr.hhplus.be.server.payment.infrastructure.web.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                request.getReservationId(),
                request.getUserId()
        );

        PaymentResult result = processPaymentUseCase.execute(command);

        return ResponseEntity.ok(PaymentResponse.from(result));
    }
}
