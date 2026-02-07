package kr.hhplus.be.server.reservation.application.service;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.reservation.application.port.in.ReservationResult;
import kr.hhplus.be.server.reservation.application.port.in.ReserveSeatCommand;
import kr.hhplus.be.server.reservation.application.port.in.ReserveSeatUseCase;
import kr.hhplus.be.server.reservation.application.port.out.LoadSeatPort;
import kr.hhplus.be.server.reservation.application.port.out.SaveReservationPort;
import kr.hhplus.be.server.reservation.application.port.out.SeatInfo;
import kr.hhplus.be.server.reservation.application.port.out.UpdateSeatStatusPort;
import kr.hhplus.be.server.reservation.domain.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReserveSeatService implements ReserveSeatUseCase {

    private final LoadSeatPort loadSeatPort;
    private final UpdateSeatStatusPort updateSeatStatusPort;
    private final SaveReservationPort saveReservationPort;

    @Override
    public ReservationResult execute(ReserveSeatCommand command) {
        // 1. 좌석 조회
        SeatInfo seat = loadSeatPort.loadById(command.seatId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        // 2. 좌석 예약 가능 여부 확인
        if (!seat.isAvailable()) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED);
        }

        // 3. 좌석 상태를 예약됨으로 변경
        updateSeatStatusPort.markAsReserved(command.seatId());

        // 4. 예약 생성 (5분 임시 배정)
        Reservation reservation = Reservation.create(
                command.userId(),
                command.seatId(),
                seat.seatNum(),
                seat.price()
        );

        Reservation savedReservation = saveReservationPort.save(reservation);

        return toResult(savedReservation);
    }

    private ReservationResult toResult(Reservation reservation) {
        return new ReservationResult(
                reservation.getReservationId(),
                reservation.getUserId(),
                reservation.getSeatId(),
                reservation.getSeatNum(),
                reservation.getAmount(),
                reservation.getStatus(),
                reservation.getExpiredAt()
        );
    }
}
