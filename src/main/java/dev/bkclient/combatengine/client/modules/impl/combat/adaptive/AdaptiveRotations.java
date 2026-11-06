package dev.bkclient.combatengine.client.modules.impl.combat.adaptive;

import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import net.minecraft.util.math.MathHelper;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Combat adaptive rotation engine with single default profile.
 */
public final class AdaptiveRotations {
    private float currentSpeedYaw = 34.0F;
    private float currentSpeedPitch = 12.0F;

    public void reset() {
        currentSpeedYaw = 34.0F;
        currentSpeedPitch = 12.0F;
    }

    private static float random(float lo, float hi) {
        return ThreadLocalRandom.current().nextFloat(lo, hi);
    }

    public Rotation step(String mode, Rotation current, Rotation desired, boolean ready, boolean readyNext,
                         boolean intersects, int age, int hits, long sinceAttack, double distance) {
        float yaw = MathHelper.wrapDegrees(desired.getYaw() - current.getYaw());
        float pitch = desired.getPitch() - current.getPitch();
        float magnitude = (float)Math.hypot(yaw, pitch);
        if (magnitude < 1.0e-5f) return current;

        // Default rotation: smooth adaptive speed with raytrace-based slow-down
        float targetSpeedYaw = random(50.0F, 80.0F);
        float targetSpeedPitch = random(7.0F, 12.0F);

        if (intersects) {
            targetSpeedYaw = random(3.0F, 4.0F);
            targetSpeedPitch = random(1.0F, 2.0F);
        }

        float smoothFactor = 0.3F;
        currentSpeedYaw += (targetSpeedYaw - currentSpeedYaw) * smoothFactor;
        currentSpeedPitch += (targetSpeedPitch - currentSpeedPitch) * smoothFactor;

        float y = MathHelper.clamp(yaw, -Math.min(Math.abs(yaw), currentSpeedYaw), Math.min(Math.abs(yaw), currentSpeedYaw));
        float p = MathHelper.clamp(pitch, -Math.min(Math.abs(pitch), currentSpeedPitch), Math.min(Math.abs(pitch), currentSpeedPitch));

        return new Rotation(current.getYaw() + y, MathHelper.clamp(current.getPitch() + p, -89, 89));
    }
}