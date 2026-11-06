package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.systems.emotes.EmoteManager;
import dev.bkclient.systems.emotes.EmoteStateHolder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs before CosmeticHeldItemFeatureMixin (lower priority number = earlier): that one draws the melee cosmetic and then cancels,
 * so a later handler would never run.
 *
 * Emote poses are made for empty hands: a held item (or melee cosmetic) would stick out of them, so it is not drawn meanwhile. */
@Mixin(value = HeldItemFeatureRenderer.class, priority = 500)
public abstract class HeldItemEmoteMixin {
   @Inject(method = "renderItem(Lnet/minecraft/client/render/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At("HEAD"), cancellable = true)
   private void bkclient$noItemInEmote(ArmedEntityRenderState state, ItemRenderState item, Arm arm, MatrixStack matrices,
         VertexConsumerProvider consumers, int light, CallbackInfo ci) {
      if (state instanceof PlayerEntityRenderState && EmoteManager.activeFor(((EmoteStateHolder)state).bkclient$getUuid()) != null) ci.cancel();
   }
}
