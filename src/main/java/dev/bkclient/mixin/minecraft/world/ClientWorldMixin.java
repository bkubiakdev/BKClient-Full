package dev.bkclient.mixin.minecraft.world;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.XRay;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
   @Inject(method = "handleBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)V", at = @At("HEAD"))
   private void onHandleBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
      XRay xrayModule = BKClientInit.getInstance().getModuleManager().getModule(XRay.class);
      if (xrayModule != null && xrayModule.isEnabled()) {
         Block block = state.getBlock();
         BlockPos immutablePos = pos.toImmutable();
         if (xrayModule.isBlockEnabled(block)) {
            xrayModule.getCachedBlocks().add(immutablePos);
         } else {
            xrayModule.getCachedBlocks().remove(immutablePos);
         }
      }
   }

   @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
   private void bk$skyColor(net.minecraft.util.math.Vec3d position, float tickDelta,
         org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> ci) {
      var manager = BKClientInit.getInstance().getModuleManager();
      var sky = manager == null ? null : manager.getModule(dev.bkclient.systems.modules.modules.visuals.Sky.class);
      if (sky != null && sky.shouldTintSky()) ci.setReturnValue(sky.getResolvedSkyColor().getRGB());
   }

   @Inject(method = "getCloudsColor", at = @At("HEAD"), cancellable = true)
   private void bk$cloudColor(float tickDelta,
         org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> ci) {
      var manager = BKClientInit.getInstance().getModuleManager();
      var sky = manager == null ? null : manager.getModule(dev.bkclient.systems.modules.modules.visuals.Sky.class);
      if (sky != null && sky.shouldTintClouds()) ci.setReturnValue(sky.getResolvedCloudColor().getRGB());
   }
}
