package dev.bkclient.utility.render;

import org.joml.Quaternionf;

/** Orient the text's +Z face toward the viewer; no position or camera-look dependency. */
public final class LyricsBillboard {
    private LyricsBillboard() { }
    public static Quaternionf facing(double dx, double dy, double dz) {
        double horizontal = Math.hypot(dx, dz);
        if (horizontal < 1e-9 && Math.abs(dy) < 1e-9) return new Quaternionf();
        return new Quaternionf().rotationY((float)Math.atan2(dx, dz))
            .rotateX((float)-Math.atan2(dy, horizontal));
    }
}
