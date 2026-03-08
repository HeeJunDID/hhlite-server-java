package kr.hhplus.be.server;

import kr.hhplus.be.server.common.lock.RedisLockManager;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

@SpringBootTest
public abstract class IntegrationTestSupport {

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    protected StringRedisTemplate stringRedisTemplate;

    @MockBean
    protected RedisLockManager redisLockManager;

    @BeforeEach
    void setUpDistributedLock() {
        // 기존 통합 테스트에서는 분산락을 항상 획득 성공으로 처리
        lenient().when(redisLockManager.tryLock(any(), anyLong(), anyLong())).thenReturn(true);
    }
}
