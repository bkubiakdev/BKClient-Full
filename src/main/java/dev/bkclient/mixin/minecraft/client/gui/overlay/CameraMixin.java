package dev.bkclient.mixin.minecraft.client.gui.overlay;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Removals;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
   @org.spongepowered.asm.mixin.Unique
   private dev.bkclient.combatengine.api.events.implement.EventRotation bkengine$rotation;
   @org.spongepowered.asm.mixin.Unique private float bkengine$originalYaw;
   @org.spongepowered.asm.mixin.Unique private float bkengine$originalPitch;

   @Inject(method = "update", at = @At("HEAD"))
   private void bkengine$prepareCamera(net.minecraft.world.BlockView area, net.minecraft.entity.Entity focusedEntity,
         boolean thirdPerson, boolean inverseView, float tickDelta,
         org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
      bkengine$rotation = null;
      if (dev.bkclient.systems.modules.modules.visuals.FreeLook.active()) return;
      if (dev.bkclient.systems.modules.modules.player.FreecamUtil.isActive()) return;
      if (dev.bkclient.compat.PerspektiveCompatibility.isFreeLookActive()) return;
      if (!dev.bkclient.combatengine.bridge.AuraRuntime.bkengineOwnsRotation() || focusedEntity == null) return;
      bkengine$originalYaw = focusedEntity.getYaw(tickDelta);
      bkengine$originalPitch = focusedEntity.getPitch(tickDelta);
      bkengine$rotation = new dev.bkclient.combatengine.api.events.implement.EventRotation(
         bkengine$originalYaw, bkengine$originalPitch, tickDelta);
      dev.bkclient.combatengine.bridge.AuraRuntime.emit(bkengine$rotation);
   }

   @org.spongepowered.asm.mixin.injection.ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
   private void aura$camera(org.spongepowered.asm.mixin.injection.invoke.arg.Args args,
         net.minecraft.world.BlockView area, net.minecraft.entity.Entity focusedEntity,
         boolean thirdPerson, boolean inverseView, float tickDelta) {
      if (dev.bkclient.systems.modules.modules.player.FreecamUtil.isActive()) return;
      if (dev.bkclient.compat.PerspektiveCompatibility.isFreeLookActive()) return;
      if (dev.bkclient.systems.modules.modules.visuals.FreeLook.active()) {
         float yaw=dev.bkclient.systems.modules.modules.visuals.FreeLook.yaw();
         float pitch=dev.bkclient.systems.modules.modules.visuals.FreeLook.pitch();
         args.set(0,thirdPerson && inverseView ? yaw+180 : yaw);
         args.set(1,thirdPerson && inverseView ? -pitch : pitch);return;
      }
      if (!dev.bkclient.combatengine.bridge.AuraRuntime.active()) return;
      if (bkengine$rotation != null) {
         if (bkengine$rotation.getYaw() != bkengine$originalYaw || bkengine$rotation.getPitch() != bkengine$originalPitch) {
            args.set(0, thirdPerson && inverseView ? bkengine$rotation.getYaw() + 180.0F : bkengine$rotation.getYaw());
            args.set(1, thirdPerson && inverseView ? -bkengine$rotation.getPitch() : bkengine$rotation.getPitch());
         }
         return;
      }
      var event=new dev.bkclient.combatengine.api.events.implement.EventRotation(args.get(0),args.get(1),tickDelta);
      dev.bkclient.combatengine.bridge.AuraRuntime.emit(event);
      args.set(0,thirdPerson && inverseView ? event.getYaw()+180.0F : event.getYaw());
      args.set(1,thirdPerson && inverseView ? -event.getPitch() : event.getPitch());
   }

   @org.spongepowered.asm.mixin.injection.ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"))
   private void bkclient$smoothCamera(org.spongepowered.asm.mixin.injection.invoke.arg.Args args,
         net.minecraft.world.BlockView area, net.minecraft.entity.Entity focusedEntity,
         boolean thirdPerson, boolean inverseView, float tickDelta) {
      dev.bkclient.systems.modules.modules.visuals.SmoothCamera smooth = dev.bkclient.systems.modules.modules.visuals.SmoothCamera.getInstance();
      if (smooth == null || !smooth.isEnabled()) return;
      if (dev.bkclient.systems.modules.modules.player.FreecamUtil.isActive()) return;
      double[] position = smooth.apply(args.<Double>get(0), args.<Double>get(1), args.<Double>get(2), thirdPerson,
         net.minecraft.client.MinecraftClient.getInstance().world);
      args.set(0, position[0]);
      args.set(1, position[1]);
      args.set(2, position[2]);
   }

   @Inject(method = "getSubmersionType()Lnet/minecraft/block/enums/CameraSubmersionType;", at = @At("HEAD"), cancellable = true)
   private void getSubmergedFluidState(CallbackInfoReturnable<CameraSubmersionType> ci) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getWater().isSelected()) {
         ci.setReturnValue(CameraSubmersionType.NONE);
      }
   }

   @Inject(method = "clipToSpace(F)F", at = @At("HEAD"), cancellable = true)
   private void onClipToSpace(float desiredCameraDistance, CallbackInfoReturnable<Float> info) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.getClip().isSelected() && removals.isEnabled()) {
         info.setReturnValue(desiredCameraDistance);
      }
   }
}
