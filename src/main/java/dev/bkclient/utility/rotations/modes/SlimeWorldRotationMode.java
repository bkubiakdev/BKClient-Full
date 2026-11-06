package dev.bkclient.utility.rotations.modes;

import java.security.SecureRandom;
import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.utility.game.CombatUtility;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.Rotation;
import dev.bkclient.utility.rotations.RotationHandler;
import dev.bkclient.utility.rotations.RotationPriority;
import dev.bkclient.utility.rotations.RotationState;
import dev.bkclient.utility.time.Timer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class SlimeWorldRotationMode implements AuraRotationMode {
   private static final SecureRandom RANDOM = new SecureRandom();
   private final Timer fallTimer = new Timer();
   private float yawOffset;
   private float pitchOffset;

   @Override
   public void rotate(Aura aura, RotationHandler handler, LivingEntity target, MoveCorrection moveCorrection) {
      if (mc.player != null && target != null) {
         Rotation base = handler.getState() == RotationState.IDLE ? handler.getPrevRotation() : handler.getCurrentRotation();
         boolean validTarget = aura.isRotationTargetValid(target);
         double distance = mc.player.getPos().distanceTo(target.getBoundingBox().getCenter());
         Vec3d delta = AuraRotationSupport.targetDelta(target, 0.5);
         Rotation deltaRotation = AuraRotationSupport.rotationDelta(base, delta);
         float yawToTarget = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
         float yawDelta = (yawToTarget - base.getYaw() + 540.0F) % 360.0F - 180.0F;
         float desiredYawOffset = yawDelta + this.yawOffset;
         float desiredPitchOffset = deltaRotation.getPitch() + this.pitchOffset;
         boolean blockedEye = !AuraRotationSupport.canSeePoint(target.getEyePos());
         float waveYaw = (float)Math.ceil(AuraRotationSupport.random(10.0F, 15.0F) * Math.cos(System.currentTimeMillis() / 65.0));
         float wavePitch = (float)Math.ceil(AuraRotationSupport.random(5.0F, 15.0F) * Math.sin(System.currentTimeMillis() / 45.0));
         float nextYawOffset = validTarget && blockedEye ? RANDOM.nextFloat() : waveYaw;
         float nextPitchOffset = validTarget && blockedEye ? RANDOM.nextFloat() : wavePitch;
         if (target.isOnGround()) {
            this.fallTimer.reset();
         }

         float yawLerp = validTarget && blockedEye ? AuraRotationSupport.random(1.95F, 2.85F) : AuraRotationSupport.random(0.7F, 1.55F);
         float pitchLerp = validTarget
            ? (blockedEye ? AuraRotationSupport.random(0.45F, 0.76F) : AuraRotationSupport.random(0.35F, 0.55F))
            : AuraRotationSupport.random(0.25F, 0.35F);
         if (this.fallTimer.finished(500L) && target.isGliding()) {
            nextPitchOffset = (float)(
               52.0
                  + (mc.player.getY() - target.getY() + 1.0)
                     * AuraRotationSupport.random(-1.0F, 5.0F + (float)Math.sin(mc.player.age % 69 / 5.0F * 1488.0F) * 35.0F)
                  - 5.0
                  + 10.0
            );
         }

         float yawBlend = (float)Math.min(AuraRotationSupport.random(0.14F, 0.88F), Math.abs(nextYawOffset - this.yawOffset) * distance);
         float pitchBlend = (float)Math.min(AuraRotationSupport.random(0.14F, 0.88F), Math.abs(nextPitchOffset - this.pitchOffset) * distance);
         this.yawOffset = MathHelper.lerp(validTarget ? yawBlend : AuraRotationSupport.random(0.52F, 0.69F), this.yawOffset, nextYawOffset);
         this.pitchOffset = MathHelper.lerp(validTarget ? pitchBlend : AuraRotationSupport.random(0.1F, 0.5F), this.pitchOffset, nextPitchOffset);
         Rotation noisyTarget = new Rotation(base.getYaw() + desiredYawOffset, base.getPitch() + desiredPitchOffset);
         float yaw = AuraRotationSupport.lerp(yawLerp, base.getYaw(), MathHelper.wrapDegrees(noisyTarget.getYaw() - base.getYaw()) + base.getYaw());
         float pitch = MathHelper.clamp(
            AuraRotationSupport.lerp(
               pitchLerp, base.getPitch(), noisyTarget.getPitch() + RANDOM.nextFloat() * 20.0F + Math.clamp(mc.player.getPitch(), -84.0F, 84.0F)
            ),
            -85.0F,
            85.0F
         );
         if (CombatUtility.stalin(target)) {
            pitch = Math.min(pitch + 4.0F, 85.0F);
         }

         handler.rotate(new Rotation(yaw, pitch), moveCorrection, 160.0F, 160.0F, AuraRotationSupport.random(5.0F, 50.0F), RotationPriority.TO_TARGET);
      }
   }

   @Override
   public void reset() {
      this.yawOffset = 0.0F;
      this.pitchOffset = 0.0F;
      this.fallTimer.reset();
   }
}
