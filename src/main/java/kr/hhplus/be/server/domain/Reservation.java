package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @Column(name = "reservation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "seat_id")
    private Long seatId;

    private String status;

    @Column(name = "expired_at")
    private Date expiredAt;

    private long amount;

    @Column(name = "seat_num")
    private int seatNum;

    public boolean isExpired() {
        return expiredAt != null && new Date().after(expiredAt);
    }

    public boolean isPaid() {
        return STATUS_CONFIRMED.equals(status);
    }

    public void validateForPayment() {
        if (isExpired()) {
            throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
        }
        if (isPaid()) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_PAID);
        }
    }

    public void confirm(Long paymentId) {
        this.paymentId = paymentId;
        this.status = STATUS_CONFIRMED;
    }
}
