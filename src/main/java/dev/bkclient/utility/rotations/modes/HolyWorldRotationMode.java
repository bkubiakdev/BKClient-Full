package dev.bkclient.utility.rotations.modes;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.Rotation;
import dev.bkclient.utility.rotations.RotationHandler;
import dev.bkclient.utility.rotations.RotationMath;
import dev.bkclient.utility.rotations.RotationState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class HolyWorldRotationMode implements AuraRotationMode {
   private static final float MOUSE_STEP_SCALE = 2.0F;
   private static final float MAX_CORRECTION_SPEED = 30.0F;
   private static final float MIN_GAIN = 1.0F;
   private static final float PITCH_FORCE_SCALE = 0.23F;
   private static final float NOISE_FORCE_SCALE = 0.13F;
   private static final float EPSILON = 1.0E-4F;
   private static final float PITCH_OFFSET = -1.75F;
   private Aura aura;
   private RotationHandler rotationHandler;
   private LivingEntity target;
   private long lastFrameNanos;
   private float yawVelocity;
   private float pitchVelocity;
   private float yawNoiseVelocity;
   private float pitchNoiseVelocity;
   private float gcdYawRemainder;
   private float gcdPitchRemainder;
   private float lastPlayerYaw = Float.NaN;
   private float lastPlayerPitch = Float.NaN;
   private float yawCompensation;
   private float pitchCompensation;
   private float lastAppliedYawDelta;
   private float lastAppliedPitchDelta;
   private double smoothedTargetX;
   private double smoothedTargetY;
   private double smoothedTargetZ;
   private boolean hasSmoothedTarget;
   private double eyeX;
   private double eyeY;
   private double eyeZ;
   private final float[] gaussianNoise = new float[2];
   private final float[] noiseDelta = new float[2];
   private final float[] appliedDelta = new float[2];
   private final float[] targetDelta = new float[2];
   private float noiseGain;
   private float noiseTimer;
   private float noisePeriod;
   private float noiseRamp;
   private boolean noiseBurst;
   private float burstShortMin;
   private float burstShortMax;
   private float burstLongMin;
   private float burstLongMax;
   private float burstHighGain;
   private float burstLowGain;
   private float noiseRampSpeed;
   private float noiseRampMax;
   private float nearTargetDistance;
   private float settleDistance;
   private float settleScale;
   private final EventListener<Render3DEvent> renderListener = event -> {
      if (!this.isActive()) {
         if (this.target != null || this.hasSmoothedTarget) {
            this.resetState();
         }
      } else if (mc.player != null && mc.world != null && !mc.player.isDead()) {
         long now = System.nanoTime();
         float frameSeconds = this.frameSeconds(now);
         this.lastFrameNanos = now;
         if (frameSeconds < 1.0E-4F || frameSeconds > 0.1F) {
            frameSeconds = 0.016666668F;
         }

         this.updateCompensation();
         this.lastAppliedPitchDelta = 0.0F;
         this.lastAppliedYawDelta = 0.0F;
         this.saveCurrentRotation();
         if (this.target != null && !(frameSeconds < 1.0E-4F)) {
            Vec3d eyes = mc.player.getEyePos();
            this.eyeX = eyes.x;
            this.eyeY = eyes.y;
            this.eyeZ = eyes.z;
            this.updateTargetDelta(this.target, event.getTickDelta(), frameSeconds);
            float yawDelta = this.targetDelta[0];
            float pitchDelta = this.targetDelta[1];
            if (Float.isFinite(yawDelta) && Float.isFinite(pitchDelta)) {
               float distance = (float)Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
               if (distance < 0.1F) {
                  this.decayMotion(frameSeconds);
                  this.syncRotationHandler();
               } else {
                  pitchDelta += -1.75F;
                  float adjustedDistance = (float)Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
                  float inverseDistance = adjustedDistance > 1.0E-4F ? 1.0F / adjustedDistance : 0.0F;
                  float yawDirection = yawDelta * inverseDistance;
                  float pitchDirection = pitchDelta * inverseDistance;
                  float forceScale = Math.min(adjustedDistance / 10.0F, 1.0F);
                  float yawForce = yawDirection * 600.0F * forceScale;
                  float pitchForce = pitchDirection * 600.0F * forceScale * 0.23F;
                  float noiseForce = 0.13F * this.adaptiveNoiseGain(adjustedDistance, frameSeconds);
                  if (noiseForce > 1.0E-4F) {
                     this.applyNoise(yawForce, pitchForce, noiseForce, frameSeconds);
                     yawForce += this.noiseDelta[0];
                     pitchForce += this.noiseDelta[1] * 2.0F;
                  }

                  yawForce = this.dampenOppositeMotion(yawForce, this.yawCompensation, frameSeconds);
                  pitchForce = this.dampenOppositeMotion(pitchForce, this.pitchCompensation, frameSeconds);
                  this.updateVelocity(yawForce, pitchForce, frameSeconds);
                  this.applyFrame(frameSeconds);
                  this.saveCurrentRotation();
                  this.syncRotationHandler();
               }
            } else {
               this.decayMotion(frameSeconds);
               this.syncRotationHandler();
            }
         } else {
            this.decayMotion(frameSeconds);
            this.saveCurrentRotation();
            this.syncRotationHandler();
         }
      }
   };

   public HolyWorldRotationMode() {
      this.resetNoiseProfile();
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   @Override
   public void rotate(Aura aura, RotationHandler handler, LivingEntity target, MoveCorrection moveCorrection) {
      this.aura = aura;
      this.rotationHandler = handler;
      this.target = target;
   }

   private boolean isActive() {
      return this.aura != null && this.aura.isEnabled() && this.aura.isHolyWorldRotationSelected();
   }

   private void resetNoiseProfile() {
      this.burstShortMin = 0.1F + AuraRotationSupport.random(0.0F, 0.5F);
      this.burstShortMax = 0.5F + AuraRotationSupport.random(0.0F, 0.75F);
      this.burstLongMin = 0.5F + AuraRotationSupport.random(0.0F, 0.5F);
      this.burstLongMax = 3.0F + AuraRotationSupport.random(0.0F, 0.25F);
      this.burstHighGain = 0.5F + AuraRotationSupport.random(0.0F, 0.7F);
      this.burstLowGain = 0.08F + AuraRotationSupport.random(0.0F, 0.12F);
      this.noiseRampSpeed = 0.018F + AuraRotationSupport.random(0.0F, 0.005F);
      this.noiseRampMax = 0.25F + AuraRotationSupport.random(0.0F, 0.05F);
      this.nearTargetDistance = 2.0F + AuraRotationSupport.random(0.0F, 0.5F);
      this.settleDistance = 6.0F + AuraRotationSupport.random(0.0F, 3.0F);
      this.settleScale = 0.15F + AuraRotationSupport.random(0.0F, 0.05F);
      this.noisePeriod = AuraRotationSupport.random(this.burstLongMin, this.burstLongMax);
   }

   private void updateTargetDelta(LivingEntity target, float tickDelta, float frameSeconds) {
      if (target != null && mc.player != null) {
         double x = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX());
         double y = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY());
         double z = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ());
         double targetX = x;
         double targetY = y + target.getHeight() * 0.5F;
         double targetZ = z;
         if (!this.hasSmoothedTarget) {
            this.smoothedTargetX = targetX;
            this.smoothedTargetY = targetY;
            this.smoothedTargetZ = targetZ;
            this.hasSmoothedTarget = true;
         } else {
            double blend = 1.0 - Math.exp(-25.0 * frameSeconds);
            blend = MathHelper.clamp(blend, 0.05, 0.95);
            this.smoothedTargetX = this.smoothedTargetX + (targetX - this.smoothedTargetX) * blend;
            this.smoothedTargetY = this.smoothedTargetY + (targetY - this.smoothedTargetY) * blend;
            this.smoothedTargetZ = this.smoothedTargetZ + (targetZ - this.smoothedTargetZ) * blend;
         }

         double deltaX = this.smoothedTargetX - this.eyeX;
         double deltaY = this.smoothedTargetY - this.eyeY;
         double deltaZ = this.smoothedTargetZ - this.eyeZ;
         double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
         if (horizontal < 1.0E-4F && Math.abs(deltaY) < 1.0E-4F) {
            this.targetDelta[0] = 0.0F;
            this.targetDelta[1] = 0.0F;
         } else {
            float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
            float pitch = MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(deltaY, horizontal))), -90.0F, 90.0F);
            this.targetDelta[0] = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
            this.targetDelta[1] = pitch - mc.player.getPitch();
         }
      } else {
         this.targetDelta[0] = 0.0F;
         this.targetDelta[1] = 0.0F;
      }
   }

   private void updateVelocity(float yawForce, float pitchForce, float frameSeconds) {
      float blend = MathHelper.clamp(1.0F - (float)Math.exp(-20.0F * frameSeconds), 0.05F, 0.95F);
      this.yawVelocity = this.lerp(this.yawVelocity, yawForce, blend);
      this.pitchVelocity = this.lerp(this.pitchVelocity, pitchForce, blend * 0.75F);
      float maxYaw = Math.max(Math.abs(this.targetDelta[0]) / frameSeconds * 1.5F, 1.0F);
      float maxPitch = Math.max(Math.abs(this.targetDelta[1]) / frameSeconds * 1.5F, 1.0F);
      this.yawVelocity = MathHelper.clamp(this.yawVelocity, -maxYaw, maxYaw);
      this.pitchVelocity = MathHelper.clamp(this.pitchVelocity, -maxPitch, maxPitch);
   }

   private float lerp(float from, float to, float delta) {
      return from + (to - from) * delta;
   }

   private void applyFrame(float frameSeconds) {
      if (mc.player != null) {
         float yawDelta = this.yawVelocity * frameSeconds;
         float pitchDelta = this.pitchVelocity * frameSeconds;
         this.applyMouseStep(yawDelta, pitchDelta);
         float appliedYaw = this.appliedDelta[0];
         float appliedPitch = this.appliedDelta[1];
         if (!Float.isFinite(appliedYaw)) {
            appliedYaw = 0.0F;
         }

         if (!Float.isFinite(appliedPitch)) {
            appliedPitch = 0.0F;
         }

         float yaw = mc.player.getYaw() + appliedYaw;
         float pitch = MathHelper.clamp(mc.player.getPitch() + appliedPitch, -90.0F, 90.0F);
         mc.player.setYaw(yaw);
         mc.player.setPitch(pitch);
         mc.player.headYaw = yaw;
         mc.player.bodyYaw = yaw;
         this.lastAppliedYawDelta = appliedYaw;
         this.lastAppliedPitchDelta = appliedPitch;
         this.lastPlayerYaw = yaw;
         this.lastPlayerPitch = pitch;
      }
   }

   private float adaptiveNoiseGain(float distance, float frameSeconds) {
      this.noiseTimer += frameSeconds;
      if (this.noiseTimer >= this.noisePeriod) {
         this.noiseBurst = !this.noiseBurst;
         this.noiseTimer = 0.0F;
         this.noisePeriod = this.noiseBurst
            ? AuraRotationSupport.random(this.burstShortMin, this.burstShortMax)
            : AuraRotationSupport.random(this.burstLongMin, this.burstLongMax);
      }

      float speed = this.noiseBurst ? 7.0F : 2.5F;
      float targetGain = this.noiseBurst ? this.burstHighGain : this.burstLowGain;
      this.noiseGain = this.noiseGain + (targetGain - this.noiseGain) * (1.0F - (float)Math.exp(-speed * frameSeconds));
      float gain = this.noiseGain;
      float rampSpeed = this.noiseRampSpeed * 0.23333333F;
      this.noiseRamp = MathHelper.clamp(this.noiseRamp + frameSeconds * rampSpeed, 0.0F, this.noiseRampMax);
      gain += this.noiseRamp;
      if (distance < this.nearTargetDistance) {
         gain *= this.settleScale;
      } else if (distance < this.settleDistance) {
         float factor = (distance - this.nearTargetDistance) / (this.settleDistance - this.nearTargetDistance);
         gain *= this.settleScale + (1.0F - this.settleScale) * factor;
      }

      return Math.max(gain, 0.0F);
   }

   private void applyNoise(float yawForce, float pitchForce, float noiseForce, float frameSeconds) {
      float decay = (float)Math.exp(-frameSeconds / 0.04F);
      this.randomizeGaussianNoise();
      this.yawNoiseVelocity = this.yawNoiseVelocity * decay + this.gaussianNoise[0] * noiseForce * Math.abs(yawForce) * (1.0F - decay);
      this.pitchNoiseVelocity = this.pitchNoiseVelocity * decay + this.gaussianNoise[1] * noiseForce * Math.abs(pitchForce) * (1.0F - decay);
      this.noiseDelta[0] = this.yawNoiseVelocity;
      this.noiseDelta[1] = this.pitchNoiseVelocity;
   }

   private void randomizeGaussianNoise() {
      double first = Math.max(1.0E-10, Math.random());
      double second = Math.random();
      double radius = Math.sqrt(-2.0 * Math.log(first));
      this.gaussianNoise[0] = (float)(radius * Math.cos((Math.PI * 2) * second));
      this.gaussianNoise[1] = (float)(radius * Math.sin((Math.PI * 2) * second));
   }

   private void updateCompensation() {
      if (!Float.isNaN(this.lastPlayerYaw) && mc.player != null) {
         this.yawCompensation = MathHelper.wrapDegrees(mc.player.getYaw() - this.lastPlayerYaw) - this.lastAppliedYawDelta;
         this.pitchCompensation = mc.player.getPitch() - this.lastPlayerPitch - this.lastAppliedPitchDelta;
      } else {
         this.pitchCompensation = 0.0F;
         this.yawCompensation = 0.0F;
      }
   }

   private void saveCurrentRotation() {
      if (mc.player != null) {
         this.lastPlayerYaw = mc.player.getYaw();
         this.lastPlayerPitch = mc.player.getPitch();
      }
   }

   private float dampenOppositeMotion(float force, float compensation, float frameSeconds) {
      if (Math.abs(force) < 1.0E-4F) {
         return force;
      }

      float compensationSpeed = frameSeconds > 1.0E-4F ? compensation / frameSeconds : 0.0F;
      if (Math.abs(compensationSpeed) < 0.1F) {
         return force;
      }

      if (force > 0.0F == compensationSpeed > 0.0F) {
         return force * 0.95F;
      }

      float scale = 1.0F - MathHelper.clamp(Math.abs(compensationSpeed) / Math.abs(force), 0.0F, 0.8F);
      return force * scale;
   }

   private void applyMouseStep(float yawDelta, float pitchDelta) {
      float step = this.mouseStep();
      if (step < 1.0E-4F) {
         this.appliedDelta[0] = yawDelta;
         this.appliedDelta[1] = pitchDelta;
      } else {
         this.gcdYawRemainder += yawDelta;
         this.gcdPitchRemainder += pitchDelta;
         float snappedYaw = Math.round(this.gcdYawRemainder / step) * step;
         float snappedPitch = Math.round(this.gcdPitchRemainder / step) * step;
         this.gcdYawRemainder = MathHelper.clamp(this.gcdYawRemainder - snappedYaw, -step * 2.0F, step * 2.0F);
         this.gcdPitchRemainder = MathHelper.clamp(this.gcdPitchRemainder - snappedPitch, -step * 2.0F, step * 2.0F);
         this.appliedDelta[0] = snappedYaw;
         this.appliedDelta[1] = snappedPitch;
      }
   }

   private float mouseStep() {
      return (float)RotationMath.getGcd();
   }

   private float frameSeconds(long now) {
      if (this.lastFrameNanos == 0L) {
         return 0.016666668F;
      }

      long delta = now - this.lastFrameNanos;
      return delta <= 0L ? 0.016666668F : Math.min((float)delta / 1.0E9F, 0.1F);
   }

   private void decayMotion(float frameSeconds) {
      float decay = (float)Math.exp(-20.0F * frameSeconds);
      this.yawVelocity *= decay;
      this.pitchVelocity *= decay;
      this.gcdYawRemainder *= decay;
      this.gcdPitchRemainder *= decay;
      this.yawNoiseVelocity *= decay;
      this.pitchNoiseVelocity *= decay;
   }

   private void syncRotationHandler() {
      if (this.rotationHandler != null && mc.player != null) {
         Rotation rotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         this.rotationHandler.setCurrentTask(null);
         this.rotationHandler.setState(RotationState.IDLE);
         this.rotationHandler.setCurrentRotation(rotation);
         this.rotationHandler.setRenderRotation(rotation);
         this.rotationHandler.getServerRotation().setYaw(rotation.getYaw());
         this.rotationHandler.getServerRotation().setPitch(rotation.getPitch());
      }
   }

   private void resetState() {
      this.target = null;
      this.hasSmoothedTarget = false;
      this.gcdPitchRemainder = 0.0F;
      this.gcdYawRemainder = 0.0F;
      this.pitchNoiseVelocity = 0.0F;
      this.yawNoiseVelocity = 0.0F;
      this.lastPlayerPitch = Float.NaN;
      this.lastPlayerYaw = Float.NaN;
      this.pitchCompensation = 0.0F;
      this.yawCompensation = 0.0F;
      this.lastAppliedPitchDelta = 0.0F;
      this.lastAppliedYawDelta = 0.0F;
      this.noiseRamp = 0.0F;
      this.pitchVelocity = 0.0F;
      this.yawVelocity = 0.0F;
      this.lastFrameNanos = 0L;
      this.smoothedTargetZ = 0.0;
      this.smoothedTargetY = 0.0;
      this.smoothedTargetX = 0.0;
   }

   @Override
   public void reset() {
      this.resetState();
      this.resetNoiseProfile();
   }
}
