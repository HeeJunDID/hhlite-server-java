package kr.hhplus.be.server.payment.infrastructure.persistence;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.Seat;
import kr.hhplus.be.server.payment.application.port.out.MarkSeatAsSoldPort;
import kr.hhplus.be.server.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentSeatAdapter implements MarkSeatAsSoldPort {

    private final SeatRepository seatRepository;

    @Override
    public void markAsSold(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
        seat.markAsSold();
    }
}
