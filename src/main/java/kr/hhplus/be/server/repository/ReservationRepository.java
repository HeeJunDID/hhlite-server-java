package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
