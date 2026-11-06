package dev.bkclient.utility.concurrent;

import dev.bkclient.utility.network.LimitedStringBodySubscriber;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Flow;

public final class PerformanceResourceTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        cache(); gate(); body(); executor();
        System.out.println("Performance resource regressions passed");
    }
    private static void cache() {
        List<Object> closed = new ArrayList<>();
        ResourceCache<String, Object> cache = new ResourceCache<>(2, 10, closed::add);
        Object a = new Object(), b = new Object(), c = new Object();
        cache.put("a", a, 4); cache.put("b", b, 4); cache.get("a");
        cache.put("c", c, 4);
        check(closed.equals(List.of(b)) && cache.get("b") == null, "LRU eviction");
        cache.put("a", a, 7);
        check(closed.equals(List.of(b, c)) && cache.bytes() == 7, "byte budget / same resource replacement");
        try { cache.put("large", new Object(), 11); throw new AssertionError("accepted oversized resource"); }
        catch (IllegalArgumentException expected) { }
        cache.clear(); cache.clear();
        check(closed.equals(List.of(b, c, a)) && cache.size() == 0 && cache.bytes() == 0, "release once");
    }
    private static void gate() {
        ClientTaskGate gate = new ClientTaskGate();
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<Integer> first = gate.submit(queue::add, calls::incrementAndGet);
        gate.cancel();
        check(first.isCancelled(), "cancel future");
        for (int i = 0; i < 100; i++) check(gate.submit(queue::add, calls::incrementAndGet).isCompletedExceptionally(), "reject backlog");
        check(queue.size() == 1, "bounded client callbacks");
        queue.remove().run();
        check(calls.get() == 0, "cancelled work skipped");
        check(gate.submit(Runnable::run, () -> 42).join() == 42, "reuse after drain");
        check(gate.submit(task -> { throw new RejectedExecutionException(); }, () -> 1).isCompletedExceptionally(), "executor rejection");
        check(gate.submit(Runnable::run, () -> { throw new IllegalStateException(); }).isCompletedExceptionally(), "supplier failure");
        check(gate.submit(Runnable::run, () -> 9).join() == 9, "reuse after failures");
    }
    private static final class Subscription implements Flow.Subscription {
        boolean cancelled;
        public void request(long n) { }
        public void cancel() { cancelled = true; }
    }
    private static void body() {
        byte[] bytes = "zażółć".getBytes(StandardCharsets.UTF_8);
        LimitedStringBodySubscriber good = new LimitedStringBodySubscriber(bytes.length);
        Subscription subscription = new Subscription(); good.onSubscribe(subscription);
        for (byte b : bytes) good.onNext(List.of(ByteBuffer.wrap(new byte[]{b})));
        good.onComplete();
        check(good.getBody().toCompletableFuture().join().equals("zażółć") && !subscription.cancelled, "split UTF8 exact budget");
        LimitedStringBodySubscriber bad = new LimitedStringBodySubscriber(3);
        Subscription rejected = new Subscription(); bad.onSubscribe(rejected);
        bad.onNext(List.of(ByteBuffer.wrap(new byte[2])));
        bad.onNext(List.of(ByteBuffer.wrap(new byte[2]))); bad.onComplete();
        check(rejected.cancelled && bad.getBody().toCompletableFuture().isCompletedExceptionally(), "stream budget before collection");
    }
    private static void executor() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 80; i++) futures.add(ClientAsyncExecutor.supply(() -> {
                try { if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("worker stuck"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new CompletionException(e); }
                return 1;
            }));
            check(futures.stream().anyMatch(CompletableFuture::isCompletedExceptionally), "saturation rejected explicitly");
            release.countDown();
            CompletableFuture.allOf(futures.stream().map(f -> f.handle((v,e) -> null)).toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
            check(futures.stream().allMatch(CompletableFuture::isDone), "no abandoned futures");
        } finally { release.countDown(); ClientAsyncExecutor.shutdown(); }
        check(ClientAsyncExecutor.supply(() -> 1).isCompletedExceptionally(), "shutdown rejection terminal");
    }
}
