package com.mip.common.timelimit;

import com.mip.exception.InternalServerErrorException;
import com.mip.exception.OperationTimeoutException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs an expensive operation with a hard time budget. On timeout the worker is
 * interrupted and the caller gets a 503, so one runaway request cannot occupy a
 * request thread indefinitely.
 */
@Component
@Slf4j
public class TimeLimitedExecutor {

    private final ExecutorService executor;

    public TimeLimitedExecutor() {
        AtomicInteger counter = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "time-limited-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public <T> T call(Callable<T> task, Duration timeout, String operationName) {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            log.error("Operation '{}' exceeded its {}s budget and was aborted",
                    operationName, timeout.toSeconds());
            throw new OperationTimeoutException(
                    "The " + operationName + " took longer than " + timeout.toSeconds()
                            + " seconds and was aborted");
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new InternalServerErrorException(operationName + " failed: "
                    + ex.getCause().getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new InternalServerErrorException(operationName + " was interrupted");
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
