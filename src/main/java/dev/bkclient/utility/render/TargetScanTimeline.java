package dev.bkclient.utility.render;

/** Time-based scan movement; changing the camera or frame rate cannot advance it. */
public final class TargetScanTimeline {
    private long lastNanos = Long.MIN_VALUE;
    private double phase;
    private float alpha;

    public void reset() { lastNanos = Long.MIN_VALUE; phase = 0; alpha = 0; }

    public Sample update(long now, boolean acquired, float passSeconds) {
        double dt = lastNanos == Long.MIN_VALUE ? 0 : Math.max(0, (now - lastNanos) / 1_000_000_000.0);
        lastNanos = now;
        phase = (phase + dt / (2 * Math.max(.5f, passSeconds))) % 1;
        alpha = (float)Math.clamp(alpha + (acquired ? dt : -dt) / .18, 0, 1);
        double angle = phase * Math.PI * 2;
        // Signed velocity crosses zero continuously: the trail must not flip at a turn.
        return new Sample((float)(.5 - .5 * Math.cos(angle)), (float)Math.sin(angle), alpha);
    }

    public record Sample(float height, float direction, float alpha) { }
}
