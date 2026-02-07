package kr.hhplus.be.server.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("Green: 결제가 정상적으로 처리된다")
    void processPayment_success() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;
        Long seatId = 1L;
        long amount = 10000L;

        Date futureDate = new Date(System.currentTimeMillis() + 300000); // 5분 후

        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .userId(userId)
                .seatId(seatId)
                .amount(amount)
                .status(Reservation.STATUS_PENDING)
                .expiredAt(futureDate)
                .build();

        User user = User.builder()
                .userId(userId)
                .balance(50000L)
                .build();

        Seat seat = Seat.builder()
                .seatId(seatId)
                .status(Seat.STATUS_RESERVED)
                .build();

        Payment savedPayment = Payment.builder()
                .paymentId(1L)
                .reservationId(reservationId)
                .userId(userId)
                .paymentAmt(amount)
                .status(Payment.STATUS_COMPLETED)
                .paidAt(new Date())
                .build();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        // when
        Payment result = paymentService.processPayment(reservationId, userId);

        // then
        assertThat(result.getStatus()).isEqualTo(Payment.STATUS_COMPLETED);
        assertThat(result.getPaymentAmt()).isEqualTo(amount);
        assertThat(user.getBalance()).isEqualTo(40000L); // 50000 - 10000
        assertThat(reservation.getStatus()).isEqualTo(Reservation.STATUS_CONFIRMED);
        assertThat(seat.getStatus()).isEqualTo(Seat.STATUS_SOLD);

        verify(paymentRepository).save(any(Payment.class));
        verify(paymentHistoryRepository).save(any(PaymentHistory.class));
    }

    @Test
    @DisplayName("Red: 존재하지 않는 예약으로 결제 시 예외가 발생한다")
    void processPayment_reservationNotFound() {
        // given
        Long reservationId = 999L;
        Long userId = 1L;

        given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(reservationId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Red: 만료된 예약으로 결제 시 예외가 발생한다")
    void processPayment_reservationExpired() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;

        Date pastDate = new Date(System.currentTimeMillis() - 300000); // 5분 전

        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .userId(userId)
                .amount(10000L)
                .status(Reservation.STATUS_PENDING)
                .expiredAt(pastDate)
                .build();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(reservationId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_EXPIRED);
                });
    }

    @Test
    @DisplayName("Red: 이미 결제된 예약으로 결제 시 예외가 발생한다")
    void processPayment_alreadyPaid() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;

        Date futureDate = new Date(System.currentTimeMillis() + 300000);

        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .userId(userId)
                .amount(10000L)
                .status(Reservation.STATUS_CONFIRMED)
                .expiredAt(futureDate)
                .build();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(reservationId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_ALREADY_PAID);
                });
    }

    @Test
    @DisplayName("Red: 잔액이 부족하면 예외가 발생한다")
    void processPayment_insufficientBalance() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;
        long amount = 10000L;

        Date futureDate = new Date(System.currentTimeMillis() + 300000);

        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .userId(userId)
                .amount(amount)
                .status(Reservation.STATUS_PENDING)
                .expiredAt(futureDate)
                .build();

        User user = User.builder()
                .userId(userId)
                .balance(5000L) // 잔액 부족
                .build();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(reservationId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                });
    }

    @Test
    @DisplayName("Red: 존재하지 않는 유저로 결제 시 예외가 발생한다")
    void processPayment_userNotFound() {
        // given
        Long reservationId = 1L;
        Long userId = 999L;

        Date futureDate = new Date(System.currentTimeMillis() + 300000);

        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .userId(userId)
                .amount(10000L)
                .status(Reservation.STATUS_PENDING)
                .expiredAt(futureDate)
                .build();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.processPayment(reservationId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
    }
}
