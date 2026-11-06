package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.systems.cosmetics.render.CharacterCosmeticRenderer;
import dev.bkclient.systems.emotes.EmoteManager;
import dev.bkclient.systems.emotes.EmotePoseApplier;
import dev.bkclient.systems.emotes.EmoteStateHolder;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerEmoteModelMixin {
   @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
   private void bkclient$emotePose(PlayerEntityRenderState state, CallbackInfo ci) {
      if (CharacterCosmeticRenderer.selected(state) != null) return;
      EmoteManager.Active active = EmoteManager.activeFor(((EmoteStateHolder)state).bkclient$getUuid());
      if (active != null) EmotePoseApplier.applyModel((PlayerEntityModel)(Object)this, active);
   }
}
