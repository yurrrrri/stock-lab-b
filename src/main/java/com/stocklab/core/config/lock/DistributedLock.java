package com.stocklab.core.config.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * Lock key name
     */
    String key();

    /**
     * Time to wait for the lock
     */
    long waitTime() default 5L;

    /**
     * Time to hold the lock before automatic release.
     * Use -1 to enable Redisson watchdog renewal until the method completes.
     */
    long leaseTime() default -1L;

    /**
     * Time unit
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
