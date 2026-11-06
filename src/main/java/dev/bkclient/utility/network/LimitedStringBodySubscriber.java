package dev.bkclient.utility.network;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Enforces a byte budget before an HTTP response is collected into a String. */
public final class LimitedStringBodySubscriber implements HttpResponse.BodySubscriber<String> {
    private final HttpResponse.BodySubscriber<String> delegate = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
    private final long maxBytes;
    private long received;
    private Flow.Subscription subscription;
    private boolean done;

    public LimitedStringBodySubscriber(long maxBytes) {
        if (maxBytes < 0) throw new IllegalArgumentException("Negative response budget");
        this.maxBytes = maxBytes;
    }
    @Override public CompletionStage<String> getBody() { return delegate.getBody(); }
    @Override public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        delegate.onSubscribe(subscription);
    }
    @Override public void onNext(List<ByteBuffer> buffers) {
        if (done) return;
        for (ByteBuffer buffer : buffers) {
            if (buffer.remaining() > maxBytes - received) {
                subscription.cancel();
                onError(new IOException("HTTP response exceeds byte budget"));
                return;
            }
            received += buffer.remaining();
        }
        delegate.onNext(buffers);
    }
    @Override public void onError(Throwable error) { if (!done) { done = true; delegate.onError(error); } }
    @Override public void onComplete() { if (!done) { done = true; delegate.onComplete(); } }
}
