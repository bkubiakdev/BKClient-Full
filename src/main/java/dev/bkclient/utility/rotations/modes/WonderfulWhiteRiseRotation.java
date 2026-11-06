package dev.bkclient.utility.rotations.modes;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.Rotation;
import dev.bkclient.utility.rotations.RotationMath;
import dev.bkclient.utility.rotations.RotationPointUtil;
import dev.bkclient.utility.rotations.RotationPriority;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Exact WhiteRise/Sloth rotation state machine used by Wonderful Aura. */
public final class WonderfulWhiteRiseRotation implements IMinecraft {
   private LivingEntity trackedTarget;
   private float lastYaw;
   private float lastPitch;
   private float speedAcceleration;
   private boolean back;
   private boolean initialized;
   private float jitterOffset;
   private int tickCounter;

   public void reset() {
      this.trackedTarget = null;
      this.speedAcceleration = 0.0F;
      this.back = false;
      this.jitterOffset = 0.0F;
      this.tickCounter = 0;
      this.initialized = mc.player != null;
      if (mc.player != null) {
         this.lastYaw = mc.player.getYaw();
         this.lastPitch = mc.player.getPitch();
      } else {
         this.lastYaw = 0.0F;
         this.lastPitch = 0.0F;
      }
   }

   public void onAttack() {
   }

   public void rotate(Aura aura, LivingEntity target, MoveCorrection moveCorrection) {
      if (mc.player == null || target == null) return;
      if (mc.player.isBlocking()) {
         this.lastYaw = mc.player.getYaw();
         this.lastPitch = mc.player.getPitch();
         return;
      }
      if (!this.initialized) {
         this.lastYaw = mc.player.getYaw();
         this.lastPitch = mc.player.getPitch();
         this.initialized = true;
      }
      if (this.trackedTarget != target) {
         this.trackedTarget = target;
         this.speedAcceleration = 0.0F;
         this.back = false;
         this.tickCounter = 0;
      }

      this.tickCounter++;
      this.jitterOffset = (float)(((Math.sin(this.tickCounter * 0.17) * 0.12) + (Math.random() * 0.08 - 0.04)) * 0.7F);
      Vec3d point = RotationPointUtil.nearestPoint(target.getBoundingBox(), mc.player.getEyePos());
      Rotation angle = RotationMath.getRotationTo(point);
      float targetYaw = angle.getYaw();
      float targetPitch = angle.getPitch();
      float yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - this.lastYaw));
      boolean readyToAttack = mc.player.getAttackCooldownProgress(1.0F) > 0.9F && aura.getWhiteRiseTicksToAttack() <= 1;

      if (!this.back) {
         float gain = 0.0055F;
         if (yawDiff > 60.0F) gain += 0.016F * 1.8F;
         else if (yawDiff > 30.0F) gain += 0.008F * 1.8F;
         else gain += 0.004F * 1.8F;
         if (readyToAttack) gain += 0.018F / 1.4F;
         this.speedAcceleration += gain * (1.6F + this.jitterOffset);
         if (this.speedAcceleration >= 0.22F) this.back = true;
      } else {
         float loss = readyToAttack ? 0.045F : 0.008F;
         this.speedAcceleration -= loss * (2.1F + this.jitterOffset);
         if (this.speedAcceleration <= -0.04F) this.back = false;
      }

      float smooth = MathHelper.clamp(this.speedAcceleration, 0.0F, mc.player.isGliding() ? 0.38F : 0.26F);
      if (readyToAttack) smooth = Math.min(smooth + 0.1F, mc.player.isGliding() ? 0.46F : 0.34F);
      smooth += this.jitterOffset * 0.5F;
      if (this.tickCounter % 7 == 0) smooth += 0.03F;

      float deltaYaw = MathHelper.wrapDegrees(targetYaw - this.lastYaw);
      float deltaPitch = targetPitch - this.lastPitch;
      float yawLimit = mc.player.isGliding() ? 42.0F : (readyToAttack ? 28.0F : 20.0F);
      float pitchLimit = mc.player.isGliding() ? 12.0F : (readyToAttack ? 4.5F : 2.8F);
      deltaYaw = MathHelper.clamp(deltaYaw, -yawLimit, yawLimit);
      deltaPitch = MathHelper.clamp(deltaPitch, -pitchLimit, pitchLimit);
      float pitchSpeed = smooth * 0.28F;
      float yawSpeed = smooth * (0.85F + this.jitterOffset * 0.4F);
      float newYaw = this.lastYaw + deltaYaw * yawSpeed;
      float newPitch = this.lastPitch + deltaPitch * pitchSpeed;

      float gcd = (float)RotationMath.getGcd();
      if (gcd > 0.0F) {
         newYaw = this.lastYaw + Math.round((newYaw - this.lastYaw) / gcd) * gcd;
         newPitch = this.lastPitch + Math.round((newPitch - this.lastPitch) / gcd) * gcd;
      }
      newPitch = MathHelper.clamp(newPitch, -89.0F, 89.0F);
      Rotation finalRotation = new Rotation(newYaw, newPitch);
      float rotationSpeed = mc.player.isGliding() && target.isGliding() ? 360.0F : 45.0F;
      BKClientInit.getInstance().getRotationHandler().rotate(
         finalRotation,
         moveCorrection,
         rotationSpeed,
         rotationSpeed,
         rotationSpeed,
         RotationPriority.TO_TARGET
      );
      if (aura.isClientLookEnabled()) {
         mc.player.setYaw(finalRotation.getYaw());
         mc.player.setPitch(finalRotation.getPitch());
      }
      this.lastYaw = finalRotation.getYaw();
      this.lastPitch = finalRotation.getPitch();
   }
}
