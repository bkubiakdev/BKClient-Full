package dev.bkclient.utility.render.wavey;

import java.util.ArrayList;
import java.util.List;

/**
 * Physics simulation for wavy cape effect.
 * Port of StickSimulation from WaveyCapes by tr7zw (MIT License).
 * https://github.com/tr7zw/WaveyCapes
 */
public class WaveySimulation {

    private static final int NUM_ITERATIONS = 3;
    private static final float MAX_BEND = 5f;
    private static final float DELTA_TIME = 50f / 1000f;

    public final List<Point> points = new ArrayList<>();
    public final List<Stick> sticks = new ArrayList<>();

    public float gravX = 0f;
    public float gravY = -1f;
    public float gravity = 0f;
    public boolean sneaking = false;

    /** Returns true if re-initialized (dirty). */
    public boolean init(int partCount) {
        if (points.size() == partCount) return false;
        points.clear();
        sticks.clear();
        for (int i = 0; i < partCount; i++) {
            Point p = new Point();
            p.y = -i;
            p.prevY = -i;
            p.locked = (i == 0);
            points.add(p);
            if (i > 0) {
                sticks.add(new Stick(points.get(i - 1), p, 1f));
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return sticks.isEmpty();
    }

    public void applyMovement(float dx, float dy, float dz) {
        Point base = points.get(0);
        base.prevX = base.x;
        base.prevY = base.y;
        base.prevZ = base.z;
        base.x += dx;
        base.y += dy;
        base.z += dz;
    }

    public void simulate() {
        float downX = gravX * gravity * DELTA_TIME;
        float downY = gravY * gravity * DELTA_TIME;

        for (Point p : points) {
            if (!p.locked) {
                float px = p.x, py = p.y, pz = p.z;
                p.x += downX;
                p.y += downY;
                p.prevX = px;
                p.prevY = py;
                p.prevZ = pz;
            }
        }

        // prevent clipping into player (cape can't go in front of player)
        Point base = points.get(0);
        for (Point p : points) {
            if (p != base && p.x - base.x > 0) {
                p.x = base.x;
            }
        }

        // prevent hard bends
        for (int i = points.size() - 2; i >= 1; i--) {
            double angle = getAngle(points.get(i), points.get(i - 1), points.get(i + 1));
            angle *= 57.2958;
            if (angle > 360) angle -= 360;
            if (angle < -360) angle += 360;
            double abs = Math.abs(angle);
            if (abs < 180 - MAX_BEND) {
                float[] r = getReplacement(points.get(i), points.get(i - 1), angle, 180 - MAX_BEND + 1);
                points.get(i + 1).x = r[0];
                points.get(i + 1).y = r[1];
            }
            if (abs > 180 + MAX_BEND) {
                float[] r = getReplacement(points.get(i), points.get(i - 1), angle, 180 + MAX_BEND - 1);
                points.get(i + 1).x = r[0];
                points.get(i + 1).y = r[1];
            }
        }

        // constraint solving
        for (int iter = 0; iter < NUM_ITERATIONS; iter++) {
            for (int i = sticks.size() - 1; i >= 0; i--) {
                Stick s = sticks.get(i);
                float cx = (s.a.x + s.b.x) / 2f;
                float cy = (s.a.y + s.b.y) / 2f;
                float cz = (s.a.z + s.b.z) / 2f;
                float dx = s.a.x - s.b.x;
                float dy = s.a.y - s.b.y;
                float dz = s.a.z - s.b.z;
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 1e-4f) continue;
                float nx = dx / len;
                float ny = dy / len;
                float nz = dz / len;
                if (!s.a.locked) {
                    s.a.x = cx + nx * s.length / 2f;
                    s.a.y = cy + ny * s.length / 2f;
                    s.a.z = cz + nz * s.length / 2f;
                }
                if (!s.b.locked) {
                    s.b.x = cx - nx * s.length / 2f;
                    s.b.y = cy - ny * s.length / 2f;
                    s.b.z = cz - nz * s.length / 2f;
                }
            }
        }

        // fix length (prevent spring effect)
        for (Stick s : sticks) {
            float dx = s.a.x - s.b.x;
            float dy = s.a.y - s.b.y;
            float dz = s.a.z - s.b.z;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-4f) continue;
            float nx = dx / len;
            float ny = dy / len;
            float nz = dz / len;
            if (!s.b.locked) {
                s.b.x = s.a.x - nx * s.length;
                s.b.y = s.a.y - ny * s.length;
                s.b.z = s.a.z - nz * s.length;
            }
        }
    }

    private double getAngle(Point mid, Point prev, Point next) {
        return Math.atan2(next.y - mid.y, next.x - mid.x)
             - Math.atan2(prev.y - mid.y, prev.x - mid.x);
    }

    private float[] getReplacement(Point mid, Point prev, double angle, double target) {
        double theta = target / 57.2958;
        float x = prev.x - mid.x;
        float y = prev.y - mid.y;
        if (angle < 0) theta *= -1;
        double cs = Math.cos(theta);
        double sn = Math.sin(theta);
        return new float[]{
            (float) ((x * cs) - (y * sn) + mid.x),
            (float) ((x * sn) + (y * cs) + mid.y)
        };
    }

    public static class Point {
        public float x, y, z;
        public float prevX, prevY, prevZ;
        public boolean locked;

        public float lerpX(float delta) { return prevX + (x - prevX) * delta; }
        public float lerpY(float delta) { return prevY + (y - prevY) * delta; }
        public float lerpZ(float delta) { return prevZ + (z - prevZ) * delta; }
    }

    public static class Stick {
        public final Point a, b;
        public final float length;
        public Stick(Point a, Point b, float length) { this.a = a; this.b = b; this.length = length; }
    }
}
