package kr.hhplus.be.server.queue.infrastructure.persistence;

import kr.hhplus.be.server.queue.application.port.out.QueueStore;
import kr.hhplus.be.server.queue.domain.QueueToken;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisQueueStore implements QueueStore {

    private final StringRedisTemplate redis;

    public RedisQueueStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String queueKey(Long concertId) {
        return "queue:" + concertId;
    }

    private String gateKey(Long concertId) {
        return "gate:" + concertId;
    }

    private String queueTokenKey(QueueToken token) {
        return "qtoken:" + token.asString();
    }

    private String accessTokenKey(String token) {
        return "atoken:" + token;
    }

    // 게이트에 몇명까지 출입가능하게 할지 설정하는 메서드
    public void setGate(Long concertId, long gate) {
        redis.opsForValue().set(gateKey(concertId), String.valueOf(gate));
    }

    @Override
    public void enqueue(Long concertId, Long userId, double score) {
        redis.opsForZSet()
                .add(queueKey(concertId), userId.toString(), score);
    }

    @Override
    public Optional<Long> findRank(Long concertId, Long userId) {
        Long rank = redis.opsForZSet()
                .rank(queueKey(concertId), userId.toString());

        return Optional.ofNullable(rank);
    }

    @Override
    public long getGate(Long concertId) {
        String gateValue = redis.opsForValue()
                .get(gateKey(concertId));
        return gateValue == null ? -1 : Long.parseLong(gateValue);
    }

    @Override
    public void saveQueueToken(QueueToken queueToken, Long concertId, Long userId, long ttlSeconds) {
        String value = concertId + ":" + userId;

        redis.opsForValue().set(
                queueTokenKey(queueToken),
                value,
                Duration.ofSeconds(ttlSeconds)
        );
    }

    @Override
    public Optional<TokenMapping> findByQueueToken(QueueToken queueToken) {
        String value = redis.opsForValue().get(queueToken);
        if(value == null) return Optional.empty();

        String[] parts = value.split(":");

        return Optional.of(
                new TokenMapping(
                        Long.parseLong(parts[0]),
                        Long.parseLong(parts[1])
                )
        );
    }

    @Override
    public void saveAccessToken(String accessToken, Long concertId, Long userId, long ttlSeconds) {
        String value = concertId + ":" + userId;

        redis.opsForValue().set(
                accessTokenKey(accessToken),
                value,
                Duration.ofSeconds(ttlSeconds)
        );
    }

    @Override
    public Optional<TokenMapping> findByAccessToken(String accessToken) {
        String value = redis.opsForValue().get(accessTokenKey(accessToken));
        if (value == null) return Optional.empty();

        String[] parts = value.split(":");
        return Optional.of(new TokenMapping(
                        Long.parseLong(parts[0]),
                        Long.parseLong(parts[1])
                )
        );
    }
}
