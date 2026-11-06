package dev.bkclient.mixin.minecraft.client.render.entity;

import dev.bkclient.mixin.accessors.LivingEntityRendererAccessor;
import dev.bkclient.utility.render.ChinaHatFeatureRenderer;
import dev.bkclient.utility.render.wavey.WaveyCapeFeatureRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void bkclient$replaceVanillaName(net.minecraft.client.network.AbstractClientPlayerEntity player,
            net.minecraft.client.render.entity.state.PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (dev.bkclient.systems.modules.modules.visuals.ESP.hidesVanillaName(player)) {
            state.displayName = null;
            state.playerName = null;
        }
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    private void addFeatures(CallbackInfo ci) {
        LivingEntityRendererAccessor accessor = (LivingEntityRendererAccessor)(Object)this;
        accessor.bkclient$addFeature(new ChinaHatFeatureRenderer((PlayerEntityRenderer)(Object)this));
        accessor.bkclient$addFeature(new WaveyCapeFeatureRenderer((PlayerEntityRenderer)(Object)this));
    }
}
