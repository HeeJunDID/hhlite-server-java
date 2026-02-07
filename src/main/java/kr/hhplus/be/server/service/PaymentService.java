package kr.hhplus.be.server.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    @Transactional
    public Payment processPayment(Long reservationId, Long userId) {
        // 1. 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        // 2. 예약 유효성 검증 (만료 여부, 이미 결제 여부)
        reservation.validateForPayment();

        // 3. 사용자 조회 및 잔액 차감
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.usePoint(reservation.getAmount());

        // 4. 결제 정보 생성
        Payment payment = Payment.createCompleted(reservationId, userId, reservation.getAmount());
        paymentRepository.save(payment);

        // 5. 결제 내역 생성
        PaymentHistory paymentHistory = PaymentHistory.builder()
                .paymentId(payment.getPaymentId())
                .userId(userId)
                .paymentAmt(reservation.getAmount())
                .status(Payment.STATUS_COMPLETED)
                .build();
        paymentHistoryRepository.save(paymentHistory);

        // 6. 예약 상태 확정
        reservation.confirm(payment.getPaymentId());

        // 7. 좌석 상태 변경 (SOLD)
        if (reservation.getSeatId() != null) {
            Seat seat = seatRepository.findById(reservation.getSeatId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
            seat.markAsSold();
        }

        return payment;
    }
}
