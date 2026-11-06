package dev.bkclient.mixin.minecraft.entity;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.game.AttackEvent;
import dev.bkclient.systems.event.impl.game.PostAttackEvent;
import dev.bkclient.systems.modules.modules.player.NoPush;
import dev.bkclient.utility.rotations.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
   @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void attackAHook2(Entity target, CallbackInfo ci) {
      PlayerEntity player = (PlayerEntity)(Object)this;
      if (!player.getWorld().isClient()) {
         return;
      }
      AttackEvent event = new AttackEvent(target);
      BKClientInit.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("RETURN"), cancellable = true)
   private void attackAHook(Entity target, CallbackInfo ci) {
      PlayerEntity player = (PlayerEntity)(Object)this;
      if (!player.getWorld().isClient()) {
         return;
      }
      PostAttackEvent event = new PostAttackEvent(target);
      BKClientInit.getInstance().getEventManager().triggerEvent(event);
   }

   @Inject(method = "resetLastAttackedTicks", at = @At("HEAD"), cancellable = true)
   private void bkclient$noMissDelay(CallbackInfo ci) {
      PlayerEntity player = (PlayerEntity)(Object)this;
      if (!player.getWorld().isClient() || player != net.minecraft.client.MinecraftClient.getInstance().player) return;
      var module = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.combat.NoMissDelay.class);
      if (module != null && module.shouldSkipReset()) ci.cancel();
   }

   @Inject(method = "isPushedByFluids()Z", at = @At("HEAD"), cancellable = true)
   private void removePushFromFluids(CallbackInfoReturnable<Boolean> cir) {
      NoPush noPush = BKClientInit.getInstance().getModuleManager().getModule(NoPush.class);
      PlayerEntity player = (PlayerEntity)(Object)this;
      if (player == MinecraftClient.getInstance().player && noPush.isEnabled() && noPush.getFluids().isSelected()) {
         cir.setReturnValue(false);
      }
   }

   @Redirect(
      method = "travel(Lnet/minecraft/util/math/Vec3d;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;")
   )
   private Vec3d redirectGetRotationVectorInTravel(PlayerEntity instance) {
      RotationHandler rotationHandler = BKClientInit.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getRotationVector() : rotationHandler.getCurrentRotation().getRotationVector();
   }
}
