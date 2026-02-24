package kr.hhplus.be.server.queue.infrastructure.persistence;

import kr.hhplus.be.server.queue.domain.QueueTokenEntity;
import kr.hhplus.be.server.queue.domain.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueTokenJpaRepository extends JpaRepository<QueueTokenEntity, Long> {

    Optional<QueueTokenEntity> findByToken(String token);

    Optional<QueueTokenEntity> findByUserIdAndConcertIdAndStatusIn(Long userId, Long concertId, List<TokenStatus> statuses);

    @Query("SELECT COUNT(q) FROM QueueTokenEntity q WHERE q.status = :status AND q.createdAt < :createdAt")
    Long countByStatusAndCreatedAtBefore(@Param("status") TokenStatus status, @Param("createdAt") LocalDateTime createdAt);

    List<QueueTokenEntity> findByStatusAndExpiredAtBefore(TokenStatus status, LocalDateTime now);
}
