package kr.hhplus.be.server.common.lock;

import kr.hhplus.be.server.common.exception.BusinessException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * 분산락 AOP Aspect.
 *
 * <p>@Order(1)로 설정하여 @Transactional(Integer.MAX_VALUE)보다 외부에서 실행된다.
 * 이를 통해 "락 획득 → TX 시작 → 비즈니스 로직 → TX 커밋 → 락 해제" 순서가 보장된다.
 *
 * <p>DB 트랜잭션과 분산락 혼용 시 주의사항:
 * 락 해제가 TX 커밋보다 먼저 발생하면, 다른 인스턴스가 커밋 전 데이터를 읽을 수 있어
 * race condition이 발생한다. 이 Aspect는 트랜잭션 외부를 감싸므로 이 문제를 방지한다.
 */
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final RedisLockManager lockManager;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        String lockKey = resolveKey(pjp, distributedLock.key());
        long waitMs = distributedLock.timeUnit().toMillis(distributedLock.waitTime());
        long leaseMs = distributedLock.timeUnit().toMillis(distributedLock.leaseTime());

        if (!lockManager.tryLock(lockKey, leaseMs, waitMs)) {
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }

        try {
            return pjp.proceed();
        } finally {
            lockManager.unlock(lockKey);
        }
    }

    private String resolveKey(ProceedingJoinPoint pjp, String keyExpression) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        Expression expression = PARSER.parseExpression(keyExpression);
        return expression.getValue(context, String.class);
    }
}
