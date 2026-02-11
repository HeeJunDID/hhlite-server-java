package kr.hhplus.be.server.payment.application.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.payment.application.port.in.PaymentResult;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentCommand;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentUseCase;
import kr.hhplus.be.server.payment.application.port.out.*;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentHistory;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final LoadReservationPort loadReservationPort;
    private final LoadUserPort loadUserPort;
    private final DeductUserBalancePort deductUserBalancePort;
    private final SavePaymentPort savePaymentPort;
    private final SavePaymentHistoryPort savePaymentHistoryPort;
    private final ConfirmReservationPort confirmReservationPort;
    private final MarkSeatAsSoldPort markSeatAsSoldPort;

    @Override
    public PaymentResult execute(ProcessPaymentCommand command) {
        // 1. 예약 조회 및 검증
        ReservationInfo reservation = loadReservationPort.loadById(command.reservationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        validateReservation(reservation);

        // 2. 사용자 조회 및 잔액 검증
        UserInfo user = loadUserPort.loadById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateUserBalance(user, reservation.amount());

        // 3. 잔액 차감
        deductUserBalancePort.deductBalance(command.userId(), reservation.amount());

        // 4. 결제 정보 생성 및 저장
        Payment payment = Payment.createCompleted(
                command.reservationId(),
                command.userId(),
                reservation.amount()
        );
        Payment savedPayment = savePaymentPort.save(payment);

        // 5. 결제 내역 생성
        PaymentHistory history = PaymentHistory.create(
                savedPayment.getPaymentId(),
                command.userId(),
                reservation.amount(),
                PaymentStatus.COMPLETED
        );
        savePaymentHistoryPort.save(history);

        // 6. 예약 확정
        confirmReservationPort.confirm(command.reservationId(), savedPayment.getPaymentId());

        // 7. 좌석 판매 완료 처리
        if (reservation.seatId() != null) {
            markSeatAsSoldPort.markAsSold(reservation.seatId());
        }

        return toResult(savedPayment);
    }

    private void validateReservation(ReservationInfo reservation) {
        if (reservation.isExpired()) {
            throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
        }
        if (reservation.isPaid()) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_PAID);
        }
    }

    private void validateUserBalance(UserInfo user, long amount) {
        if (user.balance() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    private PaymentResult toResult(Payment payment) {
        return new PaymentResult(
                payment.getPaymentId(),
                payment.getReservationId(),
                payment.getUserId(),
                payment.getPaymentAmt(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
