package dev.bkclient.mixin.minecraft.client.render;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Removals;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
   @Inject(method = "addBlockBreakParticles(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at = @At("HEAD"), cancellable = true)
   private void onAddBlockBreakParticles(BlockPos blockPos, BlockState state, CallbackInfo info) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getBreakParticles().isSelected()) {
         info.cancel();
      }
   }

   @Inject(method = "addBlockBreakingParticles(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)V", at = @At("HEAD"), cancellable = true)
   private void onAddBlockBreakingParticles(BlockPos blockPos, Direction direction, CallbackInfo info) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getBreakParticles().isSelected()) {
         info.cancel();
      }
   }

   @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
   private void onAddParticle(
      ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir
   ) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getWeather().isSelected() && parameters.getType() == ParticleTypes.RAIN) {
         cir.cancel();
      }
   }
}
