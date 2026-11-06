package dev.bkclient.mixin.minecraft.entity;

import dev.bkclient.BKClientInit;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.bkclient.systems.event.impl.game.EntityDeathEvent;
import dev.bkclient.systems.event.impl.game.EntityJumpEvent;
import dev.bkclient.systems.modules.modules.player.NoDelay;
import dev.bkclient.systems.modules.modules.player.NoPush;
import dev.bkclient.systems.modules.modules.visuals.SwingAnimation;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.RotationHandler;
import dev.bkclient.utility.rotations.RotationTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

   @Shadow
   private int field_6228;

   @Shadow
   public abstract void method_5650(RemovalReason var1);

   @Shadow
   public abstract ItemStack method_6047();

   @ModifyReturnValue(method = "getHandSwingDuration()I", at = @At("RETURN"))
   public int replaceSwingSpeed(int original) {
      SwingAnimation swingAnimationModule = BKClientInit.getInstance().getModuleManager().getModule(SwingAnimation.class);
      return swingAnimationModule.isEnabled() && swingAnimationModule.shouldApplyAnimation(this.method_6047())
         ? (int)(original * BKClientInit.getInstance().getSwingManager().getSpeed().getCurrentValue())
         : original;
   }

   @Inject(method = "jump()V", at = @At("HEAD"), cancellable = true)
   public void triggerJumpEvent(CallbackInfo ci) {
      LivingEntity livingEntity = (LivingEntity)(Object)this;
      EntityJumpEvent event = new EntityJumpEvent(livingEntity);
      BKClientInit.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(method = "jump()V", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
   public Vec3d movementCorrection(Vec3d original) {
      if ((Object)this == MinecraftClient.getInstance().player) {
         if (dev.bkclient.combatengine.client.modules.impl.combat.Aura.INSTANCE.isEnable()) return original;
         Float reference=dev.bkclient.combatengine.bridge.AuraRuntime.movementReferenceYaw();
         if(reference!=null) {
            float angle=reference*(float)(Math.PI/180);
            return new Vec3d(-MathHelper.sin(angle)*.2,0,MathHelper.cos(angle)*.2);
         }
      }

      RotationHandler rotationHandler = BKClientInit.INSTANCE.getRotationHandler();
      RotationTask currentTask = rotationHandler.getCurrentTask();
      if ((Object)this != MinecraftClient.getInstance().player) {
         return original;
      } else if (currentTask != null && currentTask.getMoveCorrection() != MoveCorrection.NONE) {
         float yaw = rotationHandler.getCurrentRotation().getYaw() * (float) (Math.PI / 180.0);
         return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
      } else {
         return original;
      }
   }

   @Inject(method = "tickMovement()V", at = @At("HEAD"))
   public void removeJumpDelay(CallbackInfo ci) {
      NoDelay noDelay = BKClientInit.getInstance().getModuleManager().getModule(NoDelay.class);
      if (noDelay.isEnabled() && noDelay.getJump().isEnabled()) {
         this.field_6228 = 0;
      }
   }

   @Inject(method = "isPushable()Z", at = @At("HEAD"), cancellable = true)
   private void removePushFromEntity(CallbackInfoReturnable<Boolean> cir) {
      if (!((Object)this instanceof ClientPlayerEntity)) return;
      NoPush noPush = BKClientInit.getInstance().getModuleManager().getModule(NoPush.class);
      if (noPush.isEnabled() && noPush.getEntities().isSelected()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at = @At("TAIL"))
   public void triggerEntityDeathEvent(DamageSource damageSource, CallbackInfo ci) {
      LivingEntity entity = (LivingEntity)(Object)this;
      if (!entity.getWorld().isClient()) {
         return;
      }
      BKClientInit.getInstance().getEventManager().triggerEvent(new EntityDeathEvent(entity, damageSource));
   }

   @Redirect(
      method = "calcGlidingVelocity(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F")
   )
   private float redirectGetPitch(LivingEntity instance) {
      RotationHandler rotationHandler = BKClientInit.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getPitch() : rotationHandler.getCurrentRotation().getPitch();
   }

   @Redirect(
      method = "calcGlidingVelocity(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;")
   )
   private Vec3d redirectGetRotationVector(LivingEntity instance) {
      RotationHandler rotationHandler = BKClientInit.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getRotationVector() : rotationHandler.getCurrentRotation().getRotationVector();
   }
}
