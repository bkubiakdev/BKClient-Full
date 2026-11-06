package dev.bkclient.utility.render;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/** Frame-rate independent, bounded angular inertia. A turn can never leave lyrics behind the viewer. */
public final class LyricsCameraFollower {
    private final Quaternionf rotation = new Quaternionf();
    private long lastNanos;
    public void reset() { lastNanos = 0; }
    public Quaternionf update(Quaternionfc target, long now, float maxLagRadians) {
        float angle = angle(rotation, target);
        double dt = (now - lastNanos) / 1e9;
        if (lastNanos == 0 || dt > .5 || angle > Math.PI / 2) rotation.set(target);
        else {
            rotation.slerp(target, (float)(1 - Math.exp(-Math.max(0, dt) / .09))).normalize();
            angle = angle(rotation, target);
            if (angle > maxLagRadians) rotation.slerp(target, 1 - maxLagRadians / angle).normalize();
        }
        lastNanos = now;
        return rotation;
    }
    private static float angle(Quaternionfc a, Quaternionfc b) {
        float dot = a.x()*b.x() + a.y()*b.y() + a.z()*b.z() + a.w()*b.w();
        return (float)(2 * Math.acos(Math.clamp(Math.abs(dot), 0, 1)));
    }
}
