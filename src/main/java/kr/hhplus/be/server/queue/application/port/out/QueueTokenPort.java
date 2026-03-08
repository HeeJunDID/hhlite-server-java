package kr.hhplus.be.server.queue.application.port.out;

import kr.hhplus.be.server.queue.domain.QueueTokenEntity;
import kr.hhplus.be.server.queue.domain.TokenStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueTokenPort {

    QueueTokenEntity save(QueueTokenEntity token);

    Optional<QueueTokenEntity> findByToken(String token);

    Optional<QueueTokenEntity> findByUserIdAndConcertIdAndStatusIn(Long userId, Long concertId, List<TokenStatus> statuses);

    Long countByStatusAndCreatedAtBefore(TokenStatus status, LocalDateTime createdAt);

    List<QueueTokenEntity> findByStatusAndExpiredAtBefore(TokenStatus status, LocalDateTime now);

    List<QueueTokenEntity> saveAll(List<QueueTokenEntity> tokens);
}
