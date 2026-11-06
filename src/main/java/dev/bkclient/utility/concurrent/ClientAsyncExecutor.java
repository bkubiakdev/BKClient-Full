package dev.bkclient.utility.concurrent;

import dev.bkclient.BKClientInit;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Bounded daemon worker pool for short client I/O and CPU tasks.
 */
public final class ClientAsyncExecutor {
    private static final AtomicInteger THREAD_IDS = new AtomicInteger();
    private static final ThreadFactory THREAD_FACTORY = task -> {
        Thread thread = new Thread(task, "BK-Client Worker-" + THREAD_IDS.incrementAndGet());
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((ignored, error) ->
            BKClientInit.LOGGER.error("Uncaught asynchronous client task error", error));
        return thread;
    };
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
        1,
        2,
        30L,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(64),
        THREAD_FACTORY,
        new ThreadPoolExecutor.AbortPolicy()
    );
    private static final ScheduledThreadPoolExecutor SCHEDULER = new ScheduledThreadPoolExecutor(1, THREAD_FACTORY);
    private static final java.util.Set<CompletableFuture<?>> PENDING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    static {
        EXECUTOR.allowCoreThreadTimeOut(true);
        SCHEDULER.setRemoveOnCancelPolicy(true);
        SCHEDULER.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        SCHEDULER.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    private ClientAsyncExecutor() {
    }

    public static void execute(Runnable task) {
        if (task == null || EXECUTOR.isShutdown()) return;
        try {
            EXECUTOR.execute(task);
        } catch (RejectedExecutionException ignored) {
            BKClientInit.LOGGER.warn("Asynchronous client task rejected: queue full or executor shut down");
        }
    }

    public static <T> CompletableFuture<T> supply(Supplier<T> task) {
        if (task == null || EXECUTOR.isShutdown()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client executor is shut down"));
        }
        CompletableFuture<T> result = new CompletableFuture<>();
        PENDING.add(result);
        result.whenComplete((value, error) -> PENDING.remove(result));
        try {
            EXECUTOR.execute(() -> {
                if (result.isDone()) return;
                try { result.complete(task.get()); }
                catch (Throwable error) { result.completeExceptionally(error); }
            });
        } catch (RejectedExecutionException error) {
            result.completeExceptionally(error);
        }
        return result;
    }

    public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (task == null || unit == null || SCHEDULER.isShutdown()) {
            return null;
        }
        try {
            return SCHEDULER.schedule(task, Math.max(0L, delay), unit);
        } catch (RejectedExecutionException ignored) {
            BKClientInit.LOGGER.warn("Delayed client task rejected during shutdown");
            return null;
        }
    }

    public static void shutdown() {
        SCHEDULER.shutdownNow();
        EXECUTOR.shutdownNow();
        PENDING.forEach(future -> future.cancel(false));
        try {
            if (!EXECUTOR.awaitTermination(2L, TimeUnit.SECONDS)) {
                BKClientInit.LOGGER.warn("Asynchronous client workers did not stop within timeout");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
