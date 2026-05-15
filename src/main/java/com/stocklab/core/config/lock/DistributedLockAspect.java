package com.stocklab.core.config.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;

    @Around("@annotation(com.stocklab.core.config.lock.DistributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String lockKey = LOCK_PREFIX + CustomSpringELParser.getDynamicValue(
                signature.getParameterNames(),
                joinPoint.getArgs(),
                distributedLock.key()
        );
        RLock rLock = redissonClient.getLock(lockKey);

        boolean lockAcquired = false;
        try {
            lockAcquired = rLock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );
            if (!lockAcquired) {
                throw new DistributedLockAcquisitionException(lockKey);
            }
            return joinPoint.proceed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockAcquisitionException(lockKey);
        } finally {
            if (lockAcquired && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }
}
