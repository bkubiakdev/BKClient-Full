package dev.bkclient.utility.rotations.modes;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.Rotation;
import dev.bkclient.utility.rotations.RotationHandler;
import dev.bkclient.utility.rotations.RotationPointUtil;
import dev.bkclient.utility.rotations.RotationPriority;
import dev.bkclient.utility.time.Timer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class SpookyTimeRotationMode implements AuraRotationMode {
   private final Timer idleTimer = new Timer();
   private final Rotation lastRotation = Rotation.ZERO;
   private long idleDelayMs;
   private Aura aura;
   private RotationHandler rotationHandler;
   private final EventListener<ClientPlayerTickEvent> tickListener = event -> {
      if (this.isActive() && this.rotationHandler != null) {
         if (BKClientInit.getInstance().getTargetManager().getCurrentTarget() == null && !this.idleTimer.finished(this.idleDelayMs)) {
            Rotation rotation = new Rotation(this.rotationHandler.getPrevRotation().getYaw(), this.rotationHandler.getPrevRotation().getPitch());
            rotation.modify(AuraRotationSupport.random(-5.0F, 5.0F), AuraRotationSupport.random(-5.0F, 5.0F));
            float yawSpeed = AuraRotationSupport.random(60.0F, 80.0F);
            float pitchSpeed = AuraRotationSupport.random(40.0F, 80.0F);
            this.rotationHandler.rotate(rotation, MoveCorrection.SILENT, yawSpeed, pitchSpeed, 180.0F, RotationPriority.TO_TARGET);
         }
      }
   };

   public SpookyTimeRotationMode() {
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   @Override
   public void rotate(Aura aura, RotationHandler handler, LivingEntity target, MoveCorrection moveCorrection) {
      this.aura = aura;
      this.rotationHandler = handler;
      if (mc.player != null && mc.world != null && target != null) {
         Box box = target.getBoundingBox();
         Vec3d hitPoint = aura.isHitVectorModeEnabled() ? RotationPointUtil.nearestPoint(box, mc.player.getEyePos()) : box.getCenter();
         double targetY = aura.isHitVectorModeEnabled() ? hitPoint.y : target.getY() + target.getHeight() * 0.5F;
         double aimY = MathHelper.clamp(MathUtility.interpolate(mc.player.getY(), targetY, 0.5), box.minY, box.maxY);
         Vec3d playerPos = mc.player
            .getPos()
            .add(mc.player.getVelocity().multiply(2.0).withAxis(Axis.Y, 0.0));
         double deltaX = target.getX() - playerPos.getX();
         double deltaY = aimY - (playerPos.getY() + mc.player.getEyeHeight(mc.player.getPose()));
         double deltaZ = target.getZ() - playerPos.getZ();
         double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
         float yaw = (float)Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
         float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, horizontal)));
         Rotation rotation = new Rotation(yaw, pitch);
         rotation.modify(AuraRotationSupport.random(-5.0F, 5.0F), AuraRotationSupport.random(-5.0F, 5.0F));
         float yawSpeed = AuraRotationSupport.random(60.0F, 80.0F);
         float pitchSpeed = AuraRotationSupport.random(40.0F, 80.0F);
         if (!target.isOnGround() && target.fallDistance > 0.0F) {
            pitchSpeed /= 12.0F;
         }

         if (!aura.isRotationTargetValid(target)) {
            yawSpeed /= AuraRotationSupport.random(5.0F, 10.0F);
            pitchSpeed /= AuraRotationSupport.random(5.0F, 10.0F);
         }

         handler.rotate(rotation, moveCorrection, yawSpeed, pitchSpeed, 180.0F, RotationPriority.TO_TARGET);
         this.lastRotation.setYaw(rotation.getYaw());
         this.lastRotation.setPitch(rotation.getPitch());
         this.idleTimer.reset();
         this.idleDelayMs = (long)AuraRotationSupport.random(700.0F, 1000.0F);
      }
   }

   private boolean isActive() {
      return this.aura != null && this.aura.isEnabled() && this.aura.isSpookyTimeRotationSelected();
   }

   @Override
   public void reset() {
      this.lastRotation.setYaw(0.0F);
      this.lastRotation.setPitch(0.0F);
      this.idleDelayMs = 0L;
      this.idleTimer.reset();
   }
}
