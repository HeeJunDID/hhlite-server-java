package kr.hhplus.be.server.reservation.application.port.out;

import java.util.Optional;

public interface LoadSeatPort {
    Optional<SeatInfo> loadById(Long seatId);
}
