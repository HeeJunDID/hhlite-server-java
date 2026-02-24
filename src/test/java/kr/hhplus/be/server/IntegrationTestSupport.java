package kr.hhplus.be.server;

import org.mockito.Answers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public abstract class IntegrationTestSupport {

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    protected StringRedisTemplate stringRedisTemplate;
}
