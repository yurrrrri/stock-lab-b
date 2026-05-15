package com.stocklab.core.domain.matching.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class StockMatchLockExecutor {

    private static final String LOCK_PREFIX = "LOCK:MATCH:";

    private final RedissonClient redissonClient;

    public void execute(String stockCode, Runnable action) {
        runWithLock(stockCode, () -> {
            action.run();
            return null;
        });
    }

    public <T> T runWithLock(String stockCode, Supplier<T> action) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + stockCode);
        lock.lock();
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
