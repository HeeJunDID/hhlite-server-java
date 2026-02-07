package kr.hhplus.be.server.controller.payment;

import kr.hhplus.be.server.controller.payment.dto.PaymentRequest;
import kr.hhplus.be.server.controller.payment.dto.PaymentResponse;
import kr.hhplus.be.server.domain.Payment;
import kr.hhplus.be.server.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(
                request.getReservationId(),
                request.getUserId()
        );
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }
}
