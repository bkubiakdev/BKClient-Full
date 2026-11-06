package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.systems.cosmetics.render.CharacterCosmeticRenderer;
import dev.bkclient.systems.emotes.EmoteManager;
import dev.bkclient.systems.emotes.EmotePoseApplier;
import dev.bkclient.systems.emotes.EmoteStateHolder;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEmoteRendererMixin {
   @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
   private void bkclient$rememberPlayer(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
      ((EmoteStateHolder)state).bkclient$setUuid(player.getUuid());
   }

   @Inject(method = "setupTransforms(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V", at = @At("TAIL"))
   private void bkclient$emoteBody(PlayerEntityRenderState state, MatrixStack matrices, float bodyYaw, float baseHeight, CallbackInfo ci) {
      // A Character cosmetic has its own skeleton, so an emote would only move its body and leave the limbs idle.
      if (CharacterCosmeticRenderer.selected(state) != null) return;
      EmoteManager.Active active = EmoteManager.activeFor(((EmoteStateHolder)state).bkclient$getUuid());
      if (active != null) EmotePoseApplier.applyBody(active, matrices);
   }
}
