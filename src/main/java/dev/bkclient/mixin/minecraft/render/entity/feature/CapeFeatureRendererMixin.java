package dev.bkclient.mixin.minecraft.render.entity.feature;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Capes;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaCape(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   int light, PlayerEntityRenderState state,
                                   float limbAngle, float limbDistance, CallbackInfo ci) {
        Capes capes = BKClientInit.getInstance().getModuleManager().getModule(Capes.class);
        if (capes != null && capes.waveyAnimation.isEnabled()) {
            ci.cancel();
        }
    }
}
