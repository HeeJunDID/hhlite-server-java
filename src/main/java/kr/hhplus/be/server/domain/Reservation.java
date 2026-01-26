package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @Column(name = "reservation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "payment_id")
    private Long paymentId;

    private String status;

    @Column(name = "expired_at")
    private Date expiredAt;

    private long amount;

    @Column(name = "seat_num")
    private int seatNum;
}
