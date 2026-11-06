package dev.bkclient.utility.math;

public final class KnockbackDirection {
    private KnockbackDirection() {}
    public static float wrap(float angle) {
        float value = angle % 360F;
        return value >= 180F ? value - 360F : value < -180F ? value + 360F : value;
    }
    /** Equivalent yaw nearest the current unbounded Minecraft yaw. */
    public static float nearest(float reference, float desired) {
        return reference + wrap(desired - reference);
    }
    public static float towardAttacker(double dx, double dz, float fallback) {
        return dx * dx + dz * dz < 1.0E-8 ? fallback : wrap((float)Math.toDegrees(Math.atan2(-dx, dz)));
    }
}
