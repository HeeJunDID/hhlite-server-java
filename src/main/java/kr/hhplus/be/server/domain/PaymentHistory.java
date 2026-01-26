package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentHistory {

    @Id
    @Column(name = "payment_history_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentHistoryId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "payment_amt")
    private Long paymentAmt;

    private String status;
}
