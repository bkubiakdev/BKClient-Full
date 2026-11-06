package dev.bkclient.utility.render.blizzard;

/** Monotonic simulation clock, independent of render passes and wall-clock changes. */
public final class BlizzardState {
    private long lastNanos;
    private float strength, exposure, cover, delta;
    private double seconds;

    public void update(long now, boolean paused, float target, boolean outdoors, boolean snowCover, float coverSeconds) {
        delta = lastNanos == 0 || paused ? 0 : (float)Math.max(0, Math.min(.1, (now-lastNanos)/1e9));
        lastNanos = now;
        seconds += delta;
        target = Math.max(0, Math.min(1, target));
        strength = approach(strength, target, (target > strength ? .45f : .6f)*delta);
        exposure = approach(exposure, outdoors ? 1 : 0, 1.4f*delta);
        if (!snowCover) cover = 0;
        else if (strength > .01f) cover = Math.min(1, cover + strength/Math.max(1, coverSeconds)*delta);
        else cover = Math.max(0, cover-delta/90);
    }
    private static float approach(float from, float to, float step) {
        return from < to ? Math.min(to, from+step) : Math.max(to, from-step);
    }
    public void reset() { lastNanos=0; strength=exposure=cover=delta=0; seconds=0; }
    public float strength() { return strength; }
    public float exposure() { return exposure; }
    public float cover() { return cover; }
    public float delta() { return delta; }
    public double seconds() { return seconds; }
}
