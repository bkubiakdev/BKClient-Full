package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.utility.render.DynamicLightUtility;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.player.NoInteract;
import dev.bkclient.systems.modules.modules.visuals.PlayerVisibility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private <E extends Entity> void bkClient$hideFilteredEntity(E entity, double x, double y, double z,
       float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      NoInteract noInteract = BKClientInit.getInstance().getModuleManager().getModule(NoInteract.class);
      if (noInteract != null && noInteract.shouldHideEntity(entity)) {
         ci.cancel();
         return;
      }
      if (!(entity instanceof PlayerEntity player)) {
         return;
      }
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && (player == mc.player || player.getUuid().equals(mc.player.getUuid()))) {
         return;
      }
      PlayerVisibility module = BKClientInit.getInstance().getModuleManager().getModule(PlayerVisibility.class);
      if (module != null && module.isEnabled() && !PlayerVisibility.isNpc(player)) {
         ci.cancel();
      }
   }

   // Per entity per frame: no CallbackInfoReturnable/boxing, and no camera-position math while dynamic light is off.
   @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getLight(Lnet/minecraft/entity/Entity;F)I", at = @At("RETURN"))
   private <E extends Entity> int bkclient$applyDynamicFullbright(int original, E entity, float tickDelta) {
      if (!DynamicLightUtility.isActive()) return original;
      return DynamicLightUtility.apply(BlockPos.ofFloored(entity.getClientCameraPosVec(tickDelta)), original);
   }
}
