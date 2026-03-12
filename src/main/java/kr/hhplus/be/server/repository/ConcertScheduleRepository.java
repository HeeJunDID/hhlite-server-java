package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.ConcertSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConcertScheduleRepository extends JpaRepository<ConcertSchedule, Long> {

    @Query(value = "SELECT cs.concert_id FROM concert_schedule cs JOIN seat s ON s.schedule_id = cs.schedule_id WHERE s.seat_id = :seatId", nativeQuery = true)
    Optional<Long> findConcertIdBySeatId(@Param("seatId") Long seatId);
}
