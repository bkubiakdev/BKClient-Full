package dev.bkclient.utility.render.lyrics;

import java.util.Arrays;

/**
 * Comet flights for Lyrics 3D. Each comet repeats on its own period; a new flight starts from the camera's position and
 * view direction at that moment and then stays put in the world.
 */
public final class LyricsCometTimeline {
    public static final int LIMIT = 20;
    private final long[] cycles = new long[LIMIT];
    private final double[] origins = new double[LIMIT * 4];
    /** Per comet: head xyz (camera relative) + opacity; travel direction xyz + radius; tail end xyz. */
    public final float[] heads = new float[LIMIT * 4], travel = new float[LIMIT * 4], tails = new float[LIMIT * 3];

    public LyricsCometTimeline() { reset(); }
    public void reset() { Arrays.fill(cycles, Long.MIN_VALUE); }

    private static double random(double seed) {
        double x = Math.sin(seed * 127.1 + 311.7) * 43758.5453;
        return x - Math.floor(x);
    }

    /** {@code yaw} is the camera's horizontal view angle, {@code atan2(forward.x, forward.z)}. */
    public void sample(double seconds, int count, float size, double cx, double cy, double cz, double yaw) {
        count = Math.max(0, Math.min(LIMIT, count));
        for (int i = 0; i < LIMIT; i++) {
            int p = i * 4;
            heads[p + 3] = 0;
            if (i >= count) { cycles[i] = Long.MIN_VALUE; continue; }
            double period = 2.5 + random(i * 17 + 1) * 4.5;
            double clock = seconds / period + random(i * 13 + 2), phase = clock - Math.floor(clock);
            long cycle = (long) Math.floor(clock);
            if (cycles[i] != cycle) {
                cycles[i] = cycle;
                origins[p] = cx; origins[p + 1] = cy; origins[p + 2] = cz; origins[p + 3] = yaw;
            }
            double seed = i * 231.7 + cycle * 31.9;
            double window = (.55 + random(seed + 9) * .35) / period;
            if (phase > window) continue;
            double progress = phase / window;
            // Start somewhere in the upper part of the view (up to 100° to each side), then fall sideways.
            double az = origins[p + 3] + (random(seed + 1) - .5) * Math.toRadians(200);
            double el = Math.toRadians(12 + random(seed + 2) * 45), distance = 45 + random(seed + 3) * 25;
            double sx = Math.sin(az) * Math.cos(el) * distance, sy = Math.sin(el) * distance, sz = Math.cos(az) * Math.cos(el) * distance;
            double sign = random(seed + 4) < .5 ? -1 : 1, across = .7 + random(seed + 5) * .6, fall = .35 + random(seed + 6) * .5;
            double tx = Math.cos(az) * sign * across, ty = -fall, tz = -Math.sin(az) * sign * across;
            double length = Math.sqrt(tx * tx + ty * ty + tz * tz);
            tx /= length; ty /= length; tz /= length;
            double flight = (20 + random(seed + 7) * 16) * progress;
            double hx = origins[p] + sx + tx * flight - cx, hy = origins[p + 1] + sy + ty * flight - cy, hz = origins[p + 2] + sz + tz * flight - cz;
            double tail = (7 + random(seed + 8) * 6) * (.35 + .65 * progress) * size;
            heads[p] = (float) hx; heads[p + 1] = (float) hy; heads[p + 2] = (float) hz;
            heads[p + 3] = (float) Math.pow(Math.sin(Math.PI * progress), .65);
            travel[p] = (float) tx; travel[p + 1] = (float) ty; travel[p + 2] = (float) tz;
            travel[p + 3] = (float) ((.18 + random(seed + 10) * .1) * size);
            int t = i * 3;
            tails[t] = (float) (hx - tx * tail); tails[t + 1] = (float) (hy - ty * tail); tails[t + 2] = (float) (hz - tz * tail);
        }
    }
}
