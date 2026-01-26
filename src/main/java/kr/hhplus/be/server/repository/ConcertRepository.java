package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

}
