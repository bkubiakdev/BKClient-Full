package dev.bkclient.utility.render;

import java.util.Arrays;

/** Bounded, continuous slot weights driven by elapsed render time, not game ticks. */
public final class HotbarSelectionAnimation {
    private final float[] weights = new float[9];
    private long lastFrame;
    private boolean initialized;

    public void reset() {
        initialized = false;
        Arrays.fill(weights, 0);
    }

    public void update(int selected, long now) {
        if (!initialized) {
            Arrays.fill(weights, 0);
            weights[selected] = 1;
            initialized = true;
        } else {
            double seconds = Math.max(0, (now - lastFrame) * 1.0e-9);
            // Approximately 95% of the way to the target in 170 ms at any FPS.
            float blend = (float) -Math.expm1(-18.0 * seconds);
            for (int slot = 0; slot < weights.length; slot++) {
                weights[slot] += ((slot == selected ? 1 : 0) - weights[slot]) * blend;
            }
        }
        lastFrame = now;
    }

    public float weight(int slot) {
        return weights[slot];
    }

    public float neighborWeight(int slot) {
        return (slot > 0 ? weights[slot - 1] : 0) + (slot < 8 ? weights[slot + 1] : 0);
    }
}
