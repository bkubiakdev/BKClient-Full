package dev.bkclient.utility.render.ambience;

import dev.bkclient.utility.render.shader.Shader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Water rings on wet ground for the local player: one at jump take-off, a stronger one at landing,
 * and small soft rings under each footstep while walking. The rings live inside the wet_world pass
 * as a normal perturbation, so they cost no extra draw.
 */
public final class WetRipples {
    public static final int MAX = 16;
    /** Horizontal distance between footsteps, in blocks (sprinting covers it faster, so steps come quicker). */
    private static final double STEP_DISTANCE = 0.85;
    /** Sideways offset of each foot from the body centre. */
    private static final double FOOT_OFFSET = 0.17;
    private static final float STEP_AMP = 0.32f;
    private static final float STEP_SCALE = 0.32f;
    private static final float STEP_LIFE = 0.6f;

    private static final double[] X = new double[MAX], Y = new double[MAX], Z = new double[MAX];
    private static final long[] START = new long[MAX];
    private static final float[] AMP = new float[MAX];
    private static final float[] SCALE = new float[MAX];
    private static final float[] LIFE = new float[MAX];
    private static int count;
    private static boolean wasAirborne;
    private static boolean hasLast;
    private static double lastX, lastZ, stepDistance;
    private static boolean leftFoot;
    private static final float[] DATA = new float[MAX * 4];
    private static final float[] AMPS = new float[MAX];
    private static final float[] SCALES = new float[MAX];

    private WetRipples() {}

    public static void clear() { count = 0; wasAirborne = false; hasLast = false; stepDistance = 0; }

    private static boolean active(AmbienceEffects effects) {
        return effects.wetWorld.isEnabled() && (effects.wetWorldRipples.isEnabled() || effects.wetWorldStepRipples.isEnabled());
    }

    /** Call once per client tick. */
    public static void tick(AmbienceEffects effects) {
        var mc = MinecraftClient.getInstance();
        var player = mc.player;
        if (player == null || !active(effects)) { clear(); return; }
        Vec3d pos = player.getPos();
        boolean inWater = player.isTouchingWater();
        boolean airborne = !player.isOnGround() && !player.isClimbing() && !inWater && !player.getAbilities().flying;
        if (effects.wetWorldRipples.isEnabled() && airborne != wasAirborne) {
            // Landing: the ring is spawned where the feet are; take-off: a softer ring.
            spawn(pos.x, pos.y + 0.02, pos.z, airborne ? 0.55f : 1.0f, 1.0f, 1.0f);
        }
        wasAirborne = airborne;

        if (effects.wetWorldStepRipples.isEnabled() && hasLast && player.isOnGround() && !inWater
                && !player.getAbilities().flying && !player.hasVehicle()) {
            double dx = pos.x - lastX, dz = pos.z - lastZ;
            double moved = Math.sqrt(dx * dx + dz * dz);
            // Teleports and very large jumps in position are not steps.
            if (moved < 2.0) stepDistance += moved; else stepDistance = 0;
            if (stepDistance >= STEP_DISTANCE && moved > 1e-4) {
                stepDistance -= STEP_DISTANCE;
                if (stepDistance > STEP_DISTANCE) stepDistance = 0;
                // Perpendicular to the motion: alternate left and right foot.
                double side = leftFoot ? FOOT_OFFSET : -FOOT_OFFSET;
                double px = -dz / moved * side, pz = dx / moved * side;
                float amp = STEP_AMP * (player.isSneaking() ? 0.6f : player.isSprinting() ? 1.2f : 1.0f);
                spawn(pos.x + px, pos.y + 0.02, pos.z + pz, amp, STEP_SCALE, STEP_LIFE);
                leftFoot = !leftFoot;
            }
        } else {
            stepDistance = 0;
        }
        lastX = pos.x; lastZ = pos.z; hasLast = true;

        long now = System.nanoTime();
        float life = life(effects);
        for (int i = 0; i < count; ) {
            if ((now - START[i]) / 1_000_000_000f > life * LIFE[i]) remove(i); else i++;
        }
    }

    private static float life(AmbienceEffects effects) {
        return 0.9f + effects.wetWorldRippleSize.getCurrentValue() * 0.25f;
    }

    private static void spawn(double x, double y, double z, float amp, float scale, float life) {
        if (count == MAX) removeOldest();
        X[count] = x; Y[count] = y; Z[count] = z; START[count] = System.nanoTime();
        AMP[count] = amp; SCALE[count] = scale; LIFE[count] = life; count++;
    }

    private static void removeOldest() {
        int oldest = 0;
        for (int i = 1; i < count; i++) if (START[i] < START[oldest]) oldest = i;
        remove(oldest);
    }

    private static void remove(int i) {
        int last = count - 1;
        X[i] = X[last]; Y[i] = Y[last]; Z[i] = Z[last]; START[i] = START[last];
        AMP[i] = AMP[last]; SCALE[i] = SCALE[last]; LIFE[i] = LIFE[last];
        count = last;
    }

    /** Uploads the rings relative to the camera; {@code RippleCount} is 0 when there is nothing to draw. */
    public static void upload(Shader shader, AmbienceEffects effects, Vec3d camera) {
        if (count == 0 || !active(effects)) { shader.setUniform1i("RippleCount", 0); return; }
        long now = System.nanoTime();
        float life = life(effects);
        for (int i = 0; i < count; i++) {
            DATA[i * 4] = (float) (X[i] - camera.x);
            DATA[i * 4 + 1] = (float) (Y[i] - camera.y);
            DATA[i * 4 + 2] = (float) (Z[i] - camera.z);
            DATA[i * 4 + 3] = Math.min(1f, (now - START[i]) / 1_000_000_000f / (life * LIFE[i]));
            AMPS[i] = AMP[i];
            SCALES[i] = SCALE[i];
        }
        shader.setUniform1i("RippleCount", count);
        shader.setUniform4fv("Ripples", DATA);
        shader.setUniform1fv("RippleAmp", AMPS);
        shader.setUniform1fv("RippleScale", SCALES);
        shader.setUniform1f("RippleSize", effects.wetWorldRippleSize.getCurrentValue());
        shader.setUniform1f("RippleStrength", effects.wetWorldRippleStrength.getCurrentValue() / 100f);
    }
}
