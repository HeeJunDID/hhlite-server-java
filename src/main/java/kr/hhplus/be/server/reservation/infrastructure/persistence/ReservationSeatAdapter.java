package kr.hhplus.be.server.reservation.infrastructure.persistence;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.Seat;
import kr.hhplus.be.server.repository.SeatRepository;
import kr.hhplus.be.server.reservation.application.port.out.LoadSeatPort;
import kr.hhplus.be.server.reservation.application.port.out.SeatInfo;
import kr.hhplus.be.server.reservation.application.port.out.UpdateSeatStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReservationSeatAdapter implements LoadSeatPort, UpdateSeatStatusPort {

    private final SeatRepository seatRepository;

    @Override
    public Optional<SeatInfo> loadById(Long seatId) {
        return seatRepository.findById(seatId)
                .map(this::toSeatInfo);
    }

    @Override
    public void markAsReserved(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
        seat.markAsReserved();
    }

    @Override
    public void release(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
        seat.release();
    }

    private SeatInfo toSeatInfo(Seat seat) {
        return new SeatInfo(
                seat.getSeatId(),
                seat.getScheduleId(),
                seat.getSeatNum(),
                seat.getPrice(),
                seat.getStatus(),
                seat.getGrade()
        );
    }
}
