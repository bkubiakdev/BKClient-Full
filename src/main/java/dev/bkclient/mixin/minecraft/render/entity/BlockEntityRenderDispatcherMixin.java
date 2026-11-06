package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.systems.modules.modules.player.NoInteract;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void bkClient$hideSelectedBlockEntity(BlockEntity entity, float tickDelta,
       MatrixStack matrices, VertexConsumerProvider consumers, CallbackInfo ci) {
      if (NoInteract.shouldHideBlock(entity.getCachedState().getBlock())) ci.cancel();
   }
}
