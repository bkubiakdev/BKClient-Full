package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.mixin.accessors.LivingEntityRendererAccessor;
import dev.bkclient.systems.cosmetics.render.CosmeticFeatureRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class CosmeticPlayerRendererMixin {
    @Inject(method="<init>",at=@At("TAIL"))
    private void bkclient$cosmetics(CallbackInfo ci) {
        ((LivingEntityRendererAccessor)(Object)this).bkclient$addFeature(new CosmeticFeatureRenderer((PlayerEntityRenderer)(Object)this));
    }
}
