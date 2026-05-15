package com.stocklab.core.config.lock;

public class DistributedLockAcquisitionException extends RuntimeException {

    public DistributedLockAcquisitionException(String lockKey) {
        super("Failed to acquire distributed lock: " + lockKey);
    }
}
