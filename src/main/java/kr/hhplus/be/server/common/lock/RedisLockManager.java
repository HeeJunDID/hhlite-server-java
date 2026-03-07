package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis SET NX PX 명령을 이용한 스핀락 방식의 분산락 구현.
 *
 * <p>Redisson 없이 Lettuce만으로 구현한다.
 * SET key value NX PX milliseconds → 키가 없을 때만 설정(NX), TTL(PX) 자동 만료.
 * 락 획득 실패 시 50ms 간격으로 재시도(스핀락).
 */
@Component
@RequiredArgsConstructor
public class RedisLockManager {

    private static final long SPIN_INTERVAL_MS = 50;

    private final StringRedisTemplate redisTemplate;

    /**
     * 분산락 획득 시도.
     *
     * @param key      락 키
     * @param leaseMs  락 보유 시간(ms) - Redis TTL
     * @param waitMs   최대 대기 시간(ms)
     * @return 락 획득 성공 여부
     */
    public boolean tryLock(String key, long leaseMs, long waitMs) {
        long deadline = System.currentTimeMillis() + waitMs;

        while (System.currentTimeMillis() < deadline) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofMillis(leaseMs));

            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }

            try {
                Thread.sleep(SPIN_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * 분산락 해제.
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
