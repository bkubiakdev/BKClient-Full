package dev.bkclient.utility.rotations.modes;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.Rotation;
import dev.bkclient.utility.rotations.RotationHandler;
import dev.bkclient.utility.rotations.RotationPriority;
import dev.bkclient.utility.time.Timer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class FunTimeRotationMode implements AuraRotationMode {
   private final Animation yawAnimation = new Animation(750L, 1.0F, Easing.LINEAR);
   private final Animation pitchAnimation = new Animation(1350L, 1.0F, Easing.LINEAR);
   private final Timer idleTimer = new Timer();
   private float savedYaw;
   private float savedPitch;
   private long idleDelayMs;
   private Aura aura;
   private RotationHandler rotationHandler;
   private final EventListener<ClientPlayerTickEvent> tickListener = event -> {
      if (this.isActive() && this.rotationHandler != null) {
         if (BKClientInit.getInstance().getTargetManager().getCurrentTarget() == null && !this.idleTimer.finished(this.idleDelayMs)) {
            Rotation rotation = new Rotation(this.rotationHandler.getCurrentRotation().getYaw(), this.rotationHandler.getCurrentRotation().getPitch());
            float pitchOffset = this.nextWave(this.pitchAnimation, 130L, 20.0F);
            float yawOffset = this.nextWave(this.yawAnimation, 170L, 20.0F);
            rotation.modify(yawOffset, pitchOffset);
            float yawSpeed = AuraRotationSupport.random(60.0F, 80.0F);
            float pitchSpeed = AuraRotationSupport.random(40.0F, 80.0F);
            this.rotationHandler.rotate(rotation, MoveCorrection.SILENT, yawSpeed, pitchSpeed, 180.0F, RotationPriority.TO_TARGET);
         }
      }
   };

   public FunTimeRotationMode() {
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   @Override
   public void rotate(Aura aura, RotationHandler handler, LivingEntity target, MoveCorrection moveCorrection) {
      this.aura = aura;
      this.rotationHandler = handler;
      if (mc.player != null && mc.world != null && target != null) {
         Rotation currentRotation = handler.getCurrentRotation();
         this.savedYaw = currentRotation.getYaw();
         this.savedPitch = currentRotation.getPitch();
         Box box = target.getBoundingBox();
         Vec3d aimPoint = AuraRotationSupport.aimPoint(aura, target);
         double targetY = MathHelper.clamp(
            MathUtility.interpolate(mc.player.getY(), aimPoint.y, 0.5), box.minY, box.maxY
         );
         Vec3d playerPos = mc.player
            .getPos()
            .add(mc.player.getVelocity().multiply(1.0).withAxis(Axis.Y, 0.0));
         double deltaX = target.getX() - playerPos.getX();
         double deltaY = targetY - (playerPos.getY() + mc.player.getEyeHeight(mc.player.getPose()));
         double deltaZ = target.getZ() - playerPos.getZ();
         double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
         float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
         float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontal)));
         float pitchOffset = this.nextWave(this.pitchAnimation, 300L, 40.0F);
         float yawOffset = this.nextWave(this.yawAnimation, 470L, 40.0F);
         float yawSpeed;
         float pitchSpeed;
         if (aura.isRotationTargetValid(target) && aura.isReadyToAttackNow()) {
            yaw += yawOffset / 4.0F;
            pitch += pitchOffset / 4.0F;
            yawSpeed = AuraRotationSupport.random(30.0F, 69.0F);
            pitchSpeed = AuraRotationSupport.random(8.0F, 15.0F);
         } else {
            yaw = this.savedYaw + yawOffset;
            pitch = this.savedPitch + pitchOffset;
            yawSpeed = AuraRotationSupport.random(20.0F, 35.0F);
            pitchSpeed = AuraRotationSupport.random(8.0F, 15.0F);
         }

         Rotation rotation = new Rotation(yaw, MathHelper.clamp(pitch, -90.0F, 90.0F));
         handler.rotate(rotation, moveCorrection, yawSpeed, pitchSpeed, 180.0F, RotationPriority.TO_TARGET);
         this.savedYaw = rotation.getYaw();
         this.savedPitch = rotation.getPitch();
         this.idleTimer.reset();
         this.idleDelayMs = (long)AuraRotationSupport.random(700.0F, 1000.0F);
      }
   }

   private float nextWave(Animation animation, long duration, float width) {
      animation.setDuration(duration);
      animation.nonono();
      return -20.0F + width * animation.getValue();
   }

   private boolean isActive() {
      return this.aura != null && this.aura.isEnabled() && this.aura.isFunTimeRotationSelected();
   }

   @Override
   public void reset() {
      this.yawAnimation.reset(1.0F);
      this.pitchAnimation.reset(1.0F);
      this.savedYaw = 0.0F;
      this.savedPitch = 0.0F;
   }
}
