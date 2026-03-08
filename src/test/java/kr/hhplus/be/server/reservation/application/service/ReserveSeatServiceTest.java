package kr.hhplus.be.server.reservation.application.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.reservation.application.port.in.ReservationResult;
import kr.hhplus.be.server.reservation.application.port.in.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.out.LoadSeatPort;
import kr.hhplus.be.server.reservation.application.port.out.SaveReservationPort;
import kr.hhplus.be.server.reservation.application.port.out.SeatInfo;
import kr.hhplus.be.server.reservation.application.port.out.UpdateSeatStatusPort;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReserveSeatServiceTest {

    @Mock
    private LoadSeatPort loadSeatPort;

    @Mock
    private UpdateSeatStatusPort updateSeatStatusPort;

    @Mock
    private SaveReservationPort saveReservationPort;

    @InjectMocks
    private ReserveSeatService reserveSeatService;

    @Test
    @DisplayName("Green: 좌석 예약이 정상적으로 처리된다")
    void execute_success() {
        // given
        Long userId = 1L;
        Long scheduleId = 1L;
        Long seatId = 1L;
        int seatNum = 10;
        long price = 50000L;

        ReserveSeatCommand command = new ReserveSeatCommand(userId, scheduleId, seatId);

        SeatInfo seatInfo = new SeatInfo(seatId, scheduleId, seatNum, price, "AVAILABLE", "VIP");

        Reservation savedReservation = Reservation.builder()
                .reservationId(1L)
                .userId(userId)
                .seatId(seatId)
                .seatNum(seatNum)
                .amount(price)
                .status(ReservationStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        given(loadSeatPort.loadById(seatId)).willReturn(Optional.of(seatInfo));
        given(saveReservationPort.save(any(Reservation.class))).willReturn(savedReservation);

        // when
        ReservationResult result = reserveSeatService.execute(command);

        // then
        assertThat(result.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(result.seatId()).isEqualTo(seatId);
        assertThat(result.amount()).isEqualTo(price);
        assertThat(result.expiredAt()).isAfter(LocalDateTime.now());

        verify(updateSeatStatusPort).markAsReserved(seatId);
        verify(saveReservationPort).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Red: 존재하지 않는 좌석 예약 시 예외가 발생한다")
    void execute_seatNotFound() {
        // given
        ReserveSeatCommand command = new ReserveSeatCommand(1L, 1L, 999L);

        given(loadSeatPort.loadById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reserveSeatService.execute(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SEAT_NOT_FOUND);
                });

        verify(updateSeatStatusPort, never()).markAsReserved(any(Long.class));
        verify(saveReservationPort, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Red: 이미 예약된 좌석 예약 시 예외가 발생한다")
    void execute_seatAlreadyReserved() {
        // given
        Long seatId = 1L;
        ReserveSeatCommand command = new ReserveSeatCommand(1L, 1L, seatId);

        SeatInfo reservedSeat = new SeatInfo(seatId, 1L, 10, 50000L, "RESERVED", "VIP");

        given(loadSeatPort.loadById(seatId)).willReturn(Optional.of(reservedSeat));

        // when & then
        assertThatThrownBy(() -> reserveSeatService.execute(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SEAT_ALREADY_RESERVED);
                });

        verify(updateSeatStatusPort, never()).markAsReserved(any(Long.class));
        verify(saveReservationPort, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Red: 이미 판매된 좌석 예약 시 예외가 발생한다")
    void execute_seatAlreadySold() {
        // given
        Long seatId = 1L;
        ReserveSeatCommand command = new ReserveSeatCommand(1L, 1L, seatId);

        SeatInfo soldSeat = new SeatInfo(seatId, 1L, 10, 50000L, "SOLD", "VIP");

        given(loadSeatPort.loadById(seatId)).willReturn(Optional.of(soldSeat));

        // when & then
        assertThatThrownBy(() -> reserveSeatService.execute(command))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SEAT_ALREADY_RESERVED);
                });
    }

    @Test
    @DisplayName("Green: 예약 시 5분 후 만료 시간이 설정된다")
    void execute_setsExpirationTime() {
        // given
        Long userId = 1L;
        Long scheduleId = 1L;
        Long seatId = 1L;

        ReserveSeatCommand command = new ReserveSeatCommand(userId, scheduleId, seatId);

        SeatInfo seatInfo = new SeatInfo(seatId, scheduleId, 10, 50000L, "AVAILABLE", "VIP");

        LocalDateTime beforeExecution = LocalDateTime.now();

        Reservation savedReservation = Reservation.builder()
                .reservationId(1L)
                .userId(userId)
                .seatId(seatId)
                .seatNum(10)
                .amount(50000L)
                .status(ReservationStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        given(loadSeatPort.loadById(seatId)).willReturn(Optional.of(seatInfo));
        given(saveReservationPort.save(any(Reservation.class))).willReturn(savedReservation);

        // when
        ReservationResult result = reserveSeatService.execute(command);

        // then
        assertThat(result.expiredAt()).isAfter(beforeExecution.plusMinutes(4));
        assertThat(result.expiredAt()).isBefore(beforeExecution.plusMinutes(6));
    }
}
