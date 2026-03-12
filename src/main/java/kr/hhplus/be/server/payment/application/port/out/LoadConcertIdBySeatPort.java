package kr.hhplus.be.server.payment.application.port.out;

import java.util.Optional;

public interface LoadConcertIdBySeatPort {
    Optional<Long> loadConcertIdBySeatId(Long seatId);
}
