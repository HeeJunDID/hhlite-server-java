package kr.hhplus.be.server.reservation.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation {

    private static final int EXPIRATION_MINUTES = 5;

    @Id
    @Column(name = "reservation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private long amount;

    @Column(name = "seat_num", nullable = false)
    private int seatNum;

    public static Reservation create(Long userId, Long seatId, int seatNum, long amount) {
        return Reservation.builder()
                .userId(userId)
                .seatId(seatId)
                .seatNum(seatNum)
                .amount(amount)
                .status(ReservationStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .build();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED;
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
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }
}
