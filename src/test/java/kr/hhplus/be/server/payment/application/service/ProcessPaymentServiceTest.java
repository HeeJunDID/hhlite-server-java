package kr.hhplus.be.server.payment.application.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.payment.application.port.in.PaymentResult;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentCommand;
import kr.hhplus.be.server.payment.application.port.out.*;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentHistory;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock
    private LoadReservationPort loadReservationPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private DeductUserBalancePort deductUserBalancePort;

    @Mock
    private SavePaymentPort savePaymentPort;

    @Mock
    private SavePaymentHistoryPort savePaymentHistoryPort;

    @Mock
    private ConfirmReservationPort confirmReservationPort;

    @Mock
    private MarkSeatAsSoldPort markSeatAsSoldPort;

    @InjectMocks
    private ProcessPaymentService processPaymentService;

    @Test
    @DisplayName("Green: 결제가 정상적으로 처리된다")
    void execute_success() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;
        Long seatId = 1L;
        long amount = 10000L;

        ProcessPaymentCommand command = new ProcessPaymentCommand(reservationId, userId);

        ReservationInfo reservationInfo = new ReservationInfo(
                reservationId,
                userId,
                seatId,
                amount,
                "PENDING",
                LocalDateTime.now().plusMinutes(5)
        );

        UserInfo userInfo = new UserInfo(userId, 50000L);

        Payment savedPayment = Payment.builder()
                .paymentId(1L)
                .reservationId(reservationId)
                .userId(userId)
                .paymentAmt(amount)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build();

        given(loadReservationPort.loadById(reservationId)).willReturn(Optional.of(reservationInfo));
        given(loadUserPort.loadById(userId)).willReturn(Optional.of(userInfo));
        given(savePaymentPort.save(any(Payment.class))).willReturn(savedPayment);

        // when
        PaymentResult result = processPaymentService.execute(command);

        // then
        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.paymentAmount()).isEqualTo(amount);

        verify(deductUserBalancePort).deductBalance(userId, amount);
        verify(savePaymentPort).save(any(Payment.class));
        verify(savePaymentHistoryPort).save(any(PaymentHistory.class));
        verify(confirmReservationPort).confirm(reservationId, savedPayment.getPaymentId());
        verify(markSeatAsSoldPort).markAsSold(seatId);
    }

    @Test
    @DisplayName("Red: 존재하지 않는 예약으로 결제 시 예외가 발생한다")
    void execute_reservationNotFound() {
        // given
        ProcessPaymentCommand command = new ProcessPaymentCommand(999L, 1L);

        given(loadReservationPort.loadById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> processPaymentService.execute(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
                });

        verify(deductUserBalancePort, never()).deductBalance(any(Long.class), any(Long.class));
    }

    @Test
    @DisplayName("Red: 만료된 예약으로 결제 시 예외가 발생한다")
    void execute_reservationExpired() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;

        ProcessPaymentCommand command = new ProcessPaymentCommand(reservationId, userId);

        ReservationInfo expiredReservation = new ReservationInfo(
                reservationId,
                userId,
                1L,
                10000L,
                "PENDING",
                LocalDateTime.now().minusMinutes(5) // 이미 만료됨
        );

        given(loadReservationPort.loadById(reservationId)).willReturn(Optional.of(expiredReservation));

        // when & then
        assertThatThrownBy(() -> processPaymentService.execute(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_EXPIRED);
                });
    }

    @Test
    @DisplayName("Red: 이미 결제된 예약으로 결제 시 예외가 발생한다")
    void execute_alreadyPaid() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;

        ProcessPaymentCommand command = new ProcessPaymentCommand(reservationId, userId);

        ReservationInfo paidReservation = new ReservationInfo(
                reservationId,
                userId,
                1L,
                10000L,
                "CONFIRMED", // 이미 결제됨
                LocalDateTime.now().plusMinutes(5)
        );

        given(loadReservationPort.loadById(reservationId)).willReturn(Optional.of(paidReservation));

        // when & then
        assertThatThrownBy(() -> processPaymentService.execute(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_ALREADY_PAID);
                });
    }

    @Test
    @DisplayName("Red: 잔액이 부족하면 예외가 발생한다")
    void execute_insufficientBalance() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;
        long amount = 10000L;

        ProcessPaymentCommand command = new ProcessPaymentCommand(reservationId, userId);

        ReservationInfo reservationInfo = new ReservationInfo(
                reservationId,
                userId,
                1L,
                amount,
                "PENDING",
                LocalDateTime.now().plusMinutes(5)
        );

        UserInfo userInfo = new UserInfo(userId, 5000L); // 잔액 부족

        given(loadReservationPort.loadById(reservationId)).willReturn(Optional.of(reservationInfo));
        given(loadUserPort.loadById(userId)).willReturn(Optional.of(userInfo));

        // when & then
        assertThatThrownBy(() -> processPaymentService.execute(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                });

        verify(deductUserBalancePort, never()).deductBalance(any(Long.class), any(Long.class));
    }

    @Test
    @DisplayName("Red: 존재하지 않는 유저로 결제 시 예외가 발생한다")
    void execute_userNotFound() {
        // given
        Long reservationId = 1L;
        Long userId = 999L;

        ProcessPaymentCommand command = new ProcessPaymentCommand(reservationId, userId);

        ReservationInfo reservationInfo = new ReservationInfo(
                reservationId,
                userId,
                1L,
                10000L,
                "PENDING",
                LocalDateTime.now().plusMinutes(5)
        );

        given(loadReservationPort.loadById(reservationId)).willReturn(Optional.of(reservationInfo));
        given(loadUserPort.loadById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> processPaymentService.execute(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Green: 좌석 ID가 없어도 결제가 정상 처리된다")
    void execute_withoutSeatId() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;
        long amount = 10000L;

        ProcessPaymentCommand command = new ProcessPaymentCommand(reservationId, userId);

        ReservationInfo reservationInfo = new ReservationInfo(
                reservationId,
                userId,
                null, // seatId 없음
                amount,
                "PENDING",
                LocalDateTime.now().plusMinutes(5)
        );

        UserInfo userInfo = new UserInfo(userId, 50000L);

        Payment savedPayment = Payment.builder()
                .paymentId(1L)
                .reservationId(reservationId)
                .userId(userId)
                .paymentAmt(amount)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now())
                .build();

        given(loadReservationPort.loadById(reservationId)).willReturn(Optional.of(reservationInfo));
        given(loadUserPort.loadById(userId)).willReturn(Optional.of(userInfo));
        given(savePaymentPort.save(any(Payment.class))).willReturn(savedPayment);

        // when
        PaymentResult result = processPaymentService.execute(command);

        // then
        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);

        verify(markSeatAsSoldPort, never()).markAsSold(any());
    }
}
