package dev.bkclient.utility.rotations;

/** Keep equivalent headings on the same continuous turn as the previous packet. */
public final class ContinuousYaw {
    private ContinuousYaw() {}

    public static float next(float previous, float requested) {
        double delta = ((double) requested - previous) % 360.0;
        if (delta >= 180.0) delta -= 360.0;
        if (delta < -180.0) delta += 360.0;
        return (float) (previous + delta);
    }
}
