package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "user_id")
    private Long userId;

    private String status;

    @Column(name = "paid_at")
    private Date paidAt;

    private long paymentAmt;

    public static Payment createCompleted(Long reservationId, Long userId, long amount) {
        return Payment.builder()
                .reservationId(reservationId)
                .userId(userId)
                .paymentAmt(amount)
                .status(STATUS_COMPLETED)
                .paidAt(new Date())
                .build();
    }
}
