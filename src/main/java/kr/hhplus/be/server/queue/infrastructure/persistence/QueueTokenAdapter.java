package kr.hhplus.be.server.queue.infrastructure.persistence;

import kr.hhplus.be.server.queue.application.port.out.QueueTokenPort;
import kr.hhplus.be.server.queue.domain.QueueTokenEntity;
import kr.hhplus.be.server.queue.domain.TokenStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QueueTokenAdapter implements QueueTokenPort {

    private final QueueTokenJpaRepository queueTokenJpaRepository;

    @Override
    public QueueTokenEntity save(QueueTokenEntity token) {
        return queueTokenJpaRepository.save(token);
    }

    @Override
    public Optional<QueueTokenEntity> findByToken(String token) {
        return queueTokenJpaRepository.findByToken(token);
    }

    @Override
    public Optional<QueueTokenEntity> findByUserIdAndConcertIdAndStatusIn(Long userId, Long concertId, List<TokenStatus> statuses) {
        return queueTokenJpaRepository.findByUserIdAndConcertIdAndStatusIn(userId, concertId, statuses);
    }

    @Override
    public Long countByStatusAndCreatedAtBefore(TokenStatus status, LocalDateTime createdAt) {
        return queueTokenJpaRepository.countByStatusAndCreatedAtBefore(status, createdAt);
    }

    @Override
    public List<QueueTokenEntity> findByStatusAndExpiredAtBefore(TokenStatus status, LocalDateTime now) {
        return queueTokenJpaRepository.findByStatusAndExpiredAtBefore(status, now);
    }

    @Override
    public List<QueueTokenEntity> saveAll(List<QueueTokenEntity> tokens) {
        return queueTokenJpaRepository.saveAll(tokens);
    }
}
