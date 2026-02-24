package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.IntegrationTestSupport;
import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.Seat;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.payment.application.port.in.PaymentResult;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentCommand;
import kr.hhplus.be.server.payment.application.port.in.ProcessPaymentUseCase;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.repository.SeatRepository;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.reservation.application.port.in.ReservationResult;
import kr.hhplus.be.server.reservation.application.port.in.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.ReserveSeatUseCase;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.infrastructure.persistence.ReservationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    private User poorUser;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        reservationJpaRepository.deleteAll();
        seatRepository.deleteAll();
        userRepository.deleteAll();

        // 좌석 가격(50,000)보다 적은 잔액을 가진 유저
        poorUser = userRepository.save(User.builder()
                .password("poor123")
                .balance(30_000L)
                .build());

        testSeat = seatRepository.save(Seat.builder()
                .scheduleId(1L)
                .seatNum(1)
                .price(50_000L)
                .status(Seat.STATUS_AVAILABLE)
                .grade("VIP")
                .build());
    }

    @Test
    @DisplayName("잔액 부족 시 결제가 실패하고 잔액은 그대로 유지된다")
    void 잔액_부족_결제_실패() {
        // given: 예약 생성
        ReservationResult reservationResult = reserveSeatUseCase.execute(
                new ReserveSeatCommand(poorUser.getUserId(), testSeat.getScheduleId(), testSeat.getSeatId())
        );

        // when & then: 잔액 부족으로 결제 실패
        assertThatThrownBy(() -> processPaymentUseCase.execute(
                new ProcessPaymentCommand(reservationResult.reservationId(), poorUser.getUserId())
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);

        // 잔액이 차감되지 않아야 함
        User updatedUser = userRepository.findById(poorUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getBalance()).isEqualTo(30_000L);
    }

    @Test
    @DisplayName("이미 결제된 예약을 재결제 시도하면 RESERVATION_ALREADY_PAID 예외가 발생한다")
    void 이미결제된_예약_재결제_실패() {
        // given: 충분한 잔액을 가진 유저와 새 좌석 준비
        User richUser = userRepository.save(User.builder()
                .password("rich123")
                .balance(200_000L)
                .build());

        Seat newSeat = seatRepository.save(Seat.builder()
                .scheduleId(1L)
                .seatNum(2)
                .price(50_000L)
                .status(Seat.STATUS_AVAILABLE)
                .grade("R")
                .build());

        ReservationResult reservationResult = reserveSeatUseCase.execute(
                new ReserveSeatCommand(richUser.getUserId(), newSeat.getScheduleId(), newSeat.getSeatId())
        );

        // 첫 번째 결제 성공
        PaymentResult firstPayment = processPaymentUseCase.execute(
                new ProcessPaymentCommand(reservationResult.reservationId(), richUser.getUserId())
        );
        assertThat(firstPayment.status()).isEqualTo(PaymentStatus.COMPLETED);

        // when & then: 동일 예약 재결제 시도
        assertThatThrownBy(() -> processPaymentUseCase.execute(
                new ProcessPaymentCommand(reservationResult.reservationId(), richUser.getUserId())
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_ALREADY_PAID);
    }

    @Test
    @DisplayName("만료된 예약을 결제 시도하면 RESERVATION_EXPIRED 예외가 발생한다")
    void 만료된_예약_결제_실패() {
        // given: 충분한 잔액을 가진 유저와 새 좌석 준비
        User richUser = userRepository.save(User.builder()
                .password("rich456")
                .balance(200_000L)
                .build());

        Seat newSeat = seatRepository.save(Seat.builder()
                .scheduleId(1L)
                .seatNum(3)
                .price(50_000L)
                .status(Seat.STATUS_AVAILABLE)
                .grade("S")
                .build());

        ReservationResult reservationResult = reserveSeatUseCase.execute(
                new ReserveSeatCommand(richUser.getUserId(), newSeat.getScheduleId(), newSeat.getSeatId())
        );

        // 예약 만료 시간을 과거로 조작
        Reservation reservation = reservationJpaRepository.findById(reservationResult.reservationId()).orElseThrow();
        forceExpire(reservation);
        reservationJpaRepository.save(reservation);

        // when & then: 만료된 예약 결제 시도
        assertThatThrownBy(() -> processPaymentUseCase.execute(
                new ProcessPaymentCommand(reservationResult.reservationId(), richUser.getUserId())
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_EXPIRED);
    }

    @Test
    @DisplayName("결제 성공 시 유저 잔액이 차감되고 좌석 상태가 SOLD로 변경된다")
    void 결제_성공_잔액차감_좌석상태변경() {
        // given: 충분한 잔액을 가진 유저
        User richUser = userRepository.save(User.builder()
                .password("rich789")
                .balance(100_000L)
                .build());

        Seat newSeat = seatRepository.save(Seat.builder()
                .scheduleId(1L)
                .seatNum(4)
                .price(50_000L)
                .status(Seat.STATUS_AVAILABLE)
                .grade("A")
                .build());

        ReservationResult reservationResult = reserveSeatUseCase.execute(
                new ReserveSeatCommand(richUser.getUserId(), newSeat.getScheduleId(), newSeat.getSeatId())
        );

        // when: 결제
        PaymentResult paymentResult = processPaymentUseCase.execute(
                new ProcessPaymentCommand(reservationResult.reservationId(), richUser.getUserId())
        );

        // then
        assertThat(paymentResult.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(paymentResult.paymentAmount()).isEqualTo(50_000L);

        User updatedUser = userRepository.findById(richUser.getUserId()).orElseThrow();
        assertThat(updatedUser.getBalance()).isEqualTo(50_000L);

        Seat updatedSeat = seatRepository.findById(newSeat.getSeatId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(Seat.STATUS_SOLD);
    }

    private void forceExpire(Reservation reservation) {
        try {
            var field = Reservation.class.getDeclaredField("expiredAt");
            field.setAccessible(true);
            field.set(reservation, LocalDateTime.now().minusMinutes(10));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
