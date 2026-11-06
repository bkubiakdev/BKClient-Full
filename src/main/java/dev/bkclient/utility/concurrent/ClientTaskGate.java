package dev.bkclient.utility.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/** Allows one queued/running client callback, even after its caller times out and cancels. */
public final class ClientTaskGate {
    private CompletableFuture<?> pending;

    public synchronized <T> CompletableFuture<T> submit(Executor executor, Supplier<T> work) {
        if (pending != null) return CompletableFuture.failedFuture(new RejectedExecutionException("Client callback still pending"));
        CompletableFuture<T> result = new CompletableFuture<>();
        pending = result;
        try {
            executor.execute(() -> {
                try {
                    if (!result.isDone()) result.complete(work.get());
                } catch (Throwable error) {
                    result.completeExceptionally(error);
                } finally {
                    synchronized (ClientTaskGate.this) { if (pending == result) pending = null; }
                }
            });
        } catch (RuntimeException error) {
            pending = null;
            result.completeExceptionally(error);
        }
        return result;
    }

    public synchronized void cancel() {
        if (pending != null) pending.cancel(false);
    }
}
