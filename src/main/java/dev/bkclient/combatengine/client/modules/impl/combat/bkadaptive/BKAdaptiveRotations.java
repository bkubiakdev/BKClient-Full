package dev.bkclient.combatengine.client.modules.impl.combat.bkadaptive;

import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import net.minecraft.util.math.MathHelper;
import java.util.concurrent.ThreadLocalRandom;

/** BKAdaptive 4.1.3 rotation equations, adapted to BK's rotation transport.
 * Bezier state persists across ticks; zero angular error never divides by zero. */
public final class BKAdaptiveRotations {
    private Rotation start, end;
    private float progress = 1, controlYaw, controlPitch;
    private String previousMode = "";
    public void reset() { start = end = null; progress = 1; previousMode = ""; }
    private static float random(float lo, float hi) { return ThreadLocalRandom.current().nextFloat(lo, hi); }

    public Rotation step(String mode, Rotation current, Rotation desired, boolean ready, boolean readyNext,
                         boolean intersects, int age, int hits, long sinceAttack, double distance) {
        if (!mode.equals(previousMode)) { reset(); previousMode = mode; }
        float yaw = MathHelper.wrapDegrees(desired.getYaw() - current.getYaw());
        float pitch = desired.getPitch() - current.getPitch();
        float magnitude = (float)Math.hypot(yaw, pitch);
        if (magnitude < 1.0e-5f) return current;
        float y = MathHelper.clamp(yaw, -Math.abs(yaw / magnitude) * 180, Math.abs(yaw / magnitude) * 180);
        float p = MathHelper.clamp(pitch, -Math.abs(pitch / magnitude) * 180, Math.abs(pitch / magnitude) * 180);
        long now = System.currentTimeMillis();
        float a = (now % 2000) / 75f, b = (now % 1000) / 75f;
        float waveY = (float)(switch (hits % 4) { case 0 -> Math.cos(a); case 1, 2 -> Math.sin(a); default -> -Math.cos(a); });
        float waveP = (float)(switch (hits % 4) { case 0, 3 -> Math.sin(b); case 1 -> Math.cos(b); default -> -Math.cos(b); });
        switch (mode) {
            case "Funtime" -> {
                if (!ready) {
                    float base = sinceAttack < 500 ? -.2f : (ThreadLocalRandom.current().nextBoolean() ? .4f : .2f);
                    y = y * MathHelper.clamp(random(base, base + .2f), 0, 1) + (sinceAttack < 1000 ? random(6, 12) * waveY : 0);
                    p = p * MathHelper.clamp(random(base, base + .2f), 0, 1) + (sinceAttack < 1000 ? random(2, 4) * waveP : 0);
                }
            }
            case "Smooth" -> {
                y = (MathHelper.clamp(yaw, -Math.abs(yaw / magnitude) * 60, Math.abs(yaw / magnitude) * 60)
                    + (ready ? 0 : (float)Math.ceil(random(2, 5) * Math.sin(now / 40d)))) * random(.6f, 1.2f);
                p = MathHelper.clamp(pitch, -Math.abs(pitch / magnitude) * 40, Math.abs(pitch / magnitude) * 40) * random(.5f, 1);
            }
            case "Grim" -> {
                y *= readyNext ? 1 : sinceAttack >= 50 ? random(.4f, .7f) : -.2f;
                p *= readyNext ? 1 : sinceAttack >= 50 ? random(.4f, .7f) : -.2f;
            }
            case "SlothAC" -> {
                float factor = random(.75f, .95f);
                y = y * factor + (ready ? 0 : 8 * waveY);
                p = p * factor + (ready ? 0 : 8 * waveP);
            }
            case "HolyWorld" -> {
                if ((age & 1) == 1) return current;
                y = MathHelper.clamp(yaw, -80, 80) + (float)Math.sin(now / 40d) * random(3, 4);
                p = MathHelper.clamp(pitch, readyNext && !intersects ? -Math.abs(pitch) : -4,
                    readyNext && !intersects ? Math.abs(pitch) : 4) + (float)Math.cos(now / 40d) * random(1, 2);
            }
            case "Bezier" -> { return bezier(current, desired, distance); }
            case "NoRotate" -> { return current; }
            default -> throw new IllegalArgumentException(mode);
        }
        return new Rotation(current.getYaw() + y, MathHelper.clamp(current.getPitch() + p, -89, 89));
    }

    private Rotation bezier(Rotation current, Rotation desired, double distance) {
        if (end == null || progress >= 1) {
            start = current;
            end = new Rotation(current.getYaw() + MathHelper.wrapDegrees(desired.getYaw() - current.getYaw()), desired.getPitch());
            controlYaw = random(-1, 1); controlPitch = random(-.5f, .5f); progress = 0;
        }
        // Tick time, not render time. Source clamp had its lower and upper bounds reversed.
        progress = Math.min(1, progress + .05f / MathHelper.clamp((float)distance / 10, .1f, .2f));
        float t = progress, u = 1 - t;
        float cy = (start.getYaw() + end.getYaw()) * .5f + controlYaw;
        float cp = (start.getPitch() + end.getPitch()) * .5f + controlPitch;
        return new Rotation(u*u*start.getYaw() + 2*u*t*cy + t*t*end.getYaw(),
            MathHelper.clamp(u*u*start.getPitch() + 2*u*t*cp + t*t*end.getPitch(), -89, 89));
    }
}
