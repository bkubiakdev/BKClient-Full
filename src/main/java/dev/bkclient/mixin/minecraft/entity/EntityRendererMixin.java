package dev.bkclient.mixin.minecraft.entity;

import dev.bkclient.systems.modules.modules.visuals.ESP;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
   @Inject(method = "getDisplayName(Lnet/minecraft/entity/Entity;)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
   private void onRenderLabel(T entity, CallbackInfoReturnable<Text> cir) {
      if (entity instanceof PlayerEntity player && ESP.hidesVanillaName(player)) {
         cir.setReturnValue(null);
      }
   }
}
