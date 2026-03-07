package kr.hhplus.be.server.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 분산락 어노테이션.
 *
 * <p>주의: DB 트랜잭션(@Transactional)과 혼용 시, 반드시 락이 트랜잭션 외부를 감싸야 한다.
 * 트랜잭션 커밋 이전에 락이 해제되면, 커밋 전 데이터를 다른 스레드가 읽는 race condition이 발생한다.
 * 이 어노테이션을 처리하는 {@link DistributedLockAspect}는 @Order(1)로 설정되어
 * @Transactional(Integer.MAX_VALUE)보다 먼저 실행(외부 래핑)된다.
 *
 * <p>key는 SpEL 표현식을 지원한다. 예: "'lock:seat:' + #command.seatId()"
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /** SpEL 표현식으로 락 키를 지정 */
    String key();

    /** 락 획득 대기 시간 */
    long waitTime() default 3;

    /** 락 보유 시간 (자동 만료 TTL) */
    long leaseTime() default 5;

    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
