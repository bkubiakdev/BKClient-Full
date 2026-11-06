package dev.bkclient.utility.animation;

/** Original first-person item choreography. Time is independent of ticks, camera and movement. */
public final class InspectAnimation {
    public enum Style {
        EXAMINE, SPIN, FLIP, ROLL, ORBIT, BALANCE,
        FIGURE_EIGHT, HELIX, PRESENT, REVERSE_SPIN, DOUBLE_FLIP, PULSE
    }

    public record Pose(float x, float y, float z, float pitch, float yaw, float roll) {
        public static final Pose IDENTITY = new Pose(0, 0, 0, 0, 0, 0);

        public Pose mirrored() { return new Pose(-x, y, z, pitch, -yaw, -roll); }
        public Pose scaled(float amount) {
            return new Pose(x * amount, y * amount, z * amount, pitch * amount, yaw * amount, roll * amount);
        }
        public Pose shortestRotation() {
            return new Pose(x, y, z, wrap(pitch), wrap(yaw), wrap(roll));
        }
        private static float wrap(float angle) { return angle - 360 * (float)Math.floor((angle + 180) / 360); }
    }

    private boolean active, returning;
    private long lastNanos;
    private double elapsed, duration;
    private float strength;
    private Style style;
    private Pose returnFrom = Pose.IDENTITY;

    public void start(long now, Style style, double seconds, float strength) {
        this.style = style;
        this.duration = Double.isFinite(seconds) ? Math.clamp(seconds, .6, 8) : 2.8;
        this.strength = Float.isFinite(strength) ? Math.clamp(strength, .25f, 1.5f) : 1;
        elapsed = 0;
        lastNanos = now;
        returning = false;
        active = true;
    }

    public void advance(long now, boolean paused) {
        if (!active) return;
        double delta = Math.max(0, (now - lastNanos) / 1_000_000_000.0);
        lastNanos = now;
        if (!paused) elapsed += delta;
        if (elapsed + 1e-9 >= duration) reset();
    }

    /** A second press returns by the shortest rotation, without snapping or doing another full turn. */
    public void finish(long now) {
        advance(now, false);
        if (!active || returning) return;
        returnFrom = pose(false).shortestRotation();
        returning = true;
        elapsed = 0;
        duration = .22;
    }

    public Pose pose(boolean leftHand) {
        if (!active) return Pose.IDENTITY;
        Pose value = returning ? returnFrom.scaled(1 - smooth(elapsed / duration))
            : sample(style, elapsed / duration, strength);
        return leftHand ? value.mirrored() : value;
    }

    public boolean isActive() { return active; }
    public void reset() { active = false; returning = false; elapsed = 0; returnFrom = Pose.IDENTITY; }

    public static Pose sample(Style style, double progress, float strength) {
        if (!Double.isFinite(progress) || progress <= 0 || progress >= 1) return Pose.IDENTITY;
        float amount = Float.isFinite(strength) ? Math.clamp(strength, .25f, 1.5f) : 1;
        float envelope = smooth(progress / .20) * (1 - smooth((progress - .78) / .22));
        float turn = smooth((progress - .22) / .52);
        float sway = (float)Math.sin(turn * Math.PI * 2);
        float lift = (float)Math.sin(turn * Math.PI);
        float circle = (float)(1 - Math.cos(turn * Math.PI * 2));
        float doubleSway = (float)Math.sin(turn * Math.PI * 4);
        float rocking = doubleSway * lift;
        float x = -.22f * envelope * amount;
        float y = .20f * envelope * amount;
        float z = -.10f * envelope * amount;
        return switch (style) {
            case EXAMINE -> new Pose(x, y, z, -18 * envelope * amount,
                (28 * envelope + 42 * sway) * amount, (-22 * envelope + 12 * sway) * amount);
            case SPIN -> new Pose(x, y + .045f * lift * amount, z,
                -12 * envelope * amount, 360 * turn, -15 * envelope * amount);
            case FLIP -> new Pose(x, y + .17f * lift * amount, z - .08f * lift * amount,
                -360 * turn, 14 * envelope * amount, -12 * envelope * amount);
            case ROLL -> new Pose(x, y + .07f * lift * amount, z,
                -16 * envelope * amount, 14 * envelope * amount, -360 * turn);
            case ORBIT -> new Pose(x + .15f * sway * amount, y + .10f * circle * amount,
                z - .04f * lift * amount, (-12 * envelope + 10 * sway) * amount,
                (24 * envelope + 12 * sway) * amount, (-12 * envelope + 18 * sway) * amount);
            case BALANCE -> new Pose(x - .05f * rocking * amount, y + .06f * rocking * rocking * amount, z,
                (-14 * envelope + 12 * sway) * amount, 12 * envelope * amount, 50 * rocking * amount);
            case FIGURE_EIGHT -> new Pose(x + .16f * sway * amount, y + .09f * doubleSway * amount, z,
                -12 * envelope * amount, (18 * envelope + 18 * sway) * amount, 28 * doubleSway * amount);
            case HELIX -> new Pose(x + .10f * doubleSway * lift * amount, y + .20f * lift * amount,
                z - .10f * lift * amount, -18 * envelope * amount, 360 * turn, 18 * sway * amount);
            case PRESENT -> new Pose(-.32f * envelope * amount, .25f * envelope * amount, -.18f * envelope * amount,
                -24 * envelope * amount, (18 * envelope + 112 * lift * lift) * amount, -28 * envelope * amount);
            case REVERSE_SPIN -> new Pose(x, y + .045f * lift * amount, z,
                -12 * envelope * amount, -360 * turn, 15 * envelope * amount);
            case DOUBLE_FLIP -> new Pose(x, y + .19f * sway * sway * amount, z - .10f * lift * amount,
                -720 * turn, 12 * envelope * amount, -10 * envelope * amount);
            case PULSE -> {
                float pulse = (float)Math.sin(turn * Math.PI * 6) * lift * lift;
                yield new Pose(x, y + .045f * pulse * amount, z + .12f * pulse * amount,
                    (-12 * envelope + 14 * pulse) * amount, 18 * envelope * amount, -10 * envelope * amount);
            }
        };
    }

    // Quintic easing keeps velocity and acceleration continuous at holds and reversals.
    private static float smooth(double t) {
        t = Math.clamp(t, 0, 1);
        return (float)(t * t * t * (t * (t * 6 - 15) + 10));
    }
}
