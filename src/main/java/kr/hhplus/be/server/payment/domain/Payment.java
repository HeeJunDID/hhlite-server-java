package kr.hhplus.be.server.payment.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_amt", nullable = false)
    private long paymentAmt;

    public static Payment createCompleted(Long reservationId, Long userId, long amount) {
        return Payment.builder()
                .reservationId(reservationId)
                .userId(userId)
                .paymentAmt(amount)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build();
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }
}
