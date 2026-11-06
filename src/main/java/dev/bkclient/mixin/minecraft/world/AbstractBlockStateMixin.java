package dev.bkclient.mixin.minecraft.world;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.player.FreeCam;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.Block;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {

   @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
   private void bkClient$hideSelectedBlock(CallbackInfoReturnable<net.minecraft.block.BlockRenderType> cir) {
      Block block = ((AbstractBlockState)(Object)this).getBlock();
      if (dev.bkclient.systems.modules.modules.player.NoInteract.shouldHideBlock(block)) {
         cir.setReturnValue(net.minecraft.block.BlockRenderType.INVISIBLE);
      }
   }

   @Inject(
      method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onGetCollisionShape(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
      FreeCam freeCam = bkclient$freeCam;
      if (freeCam == null) {
         // Modules are registered once and never removed, so the reference can be kept after the first lookup.
         if (BKClientInit.getInstance().getModuleManager() == null) return;
         freeCam = bkclient$freeCam = BKClientInit.getInstance().getModuleManager().getModule(FreeCam.class);
      }
      if (freeCam.isEnabled()) {
         cir.setReturnValue(VoxelShapes.empty());
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static FreeCam bkclient$freeCam;
}
