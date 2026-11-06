package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.systems.cosmetics.render.CharacterCosmeticRenderer;
import dev.bkclient.systems.emotes.EmoteManager;
import dev.bkclient.systems.emotes.EmotePoseApplier;
import dev.bkclient.systems.emotes.EmoteStateHolder;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Armor and other humanoid models that are posed from a player's render state follow the emote too. */
@Mixin(BipedEntityModel.class)
public abstract class BipedEmoteModelMixin {
   @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At("TAIL"))
   private void bkclient$emotePose(BipedEntityRenderState state, CallbackInfo ci) {
      // The player model itself is posed in PlayerEmoteModelMixin, after its own layers are set up.
      if ((Object)this instanceof PlayerEntityModel || !(state instanceof PlayerEntityRenderState)) return;
      if (CharacterCosmeticRenderer.selected(state) != null) return;
      EmoteManager.Active active = EmoteManager.activeFor(((EmoteStateHolder)state).bkclient$getUuid());
      if (active != null) EmotePoseApplier.applyModel((BipedEntityModel<?>)(Object)this, active);
   }
}
