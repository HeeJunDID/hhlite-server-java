package kr.hhplus.be.server.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentHistory {

    @Id
    @Column(name = "payment_history_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentHistoryId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "payment_amt", nullable = false)
    private long paymentAmt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static PaymentHistory create(Long paymentId, Long userId, long amount, PaymentStatus status) {
        return PaymentHistory.builder()
                .paymentId(paymentId)
                .userId(userId)
                .paymentAmt(amount)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
