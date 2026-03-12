package kr.hhplus.be.server.payment.infrastructure.persistence;

import kr.hhplus.be.server.payment.application.port.out.LoadConcertIdBySeatPort;
import kr.hhplus.be.server.repository.ConcertScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SeatScheduleAdapter implements LoadConcertIdBySeatPort {

    private final ConcertScheduleRepository concertScheduleRepository;

    @Override
    public Optional<Long> loadConcertIdBySeatId(Long seatId) {
        return concertScheduleRepository.findConcertIdBySeatId(seatId);
    }
}
