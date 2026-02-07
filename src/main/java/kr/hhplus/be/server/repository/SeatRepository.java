package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}
