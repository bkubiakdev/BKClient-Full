package dev.bkclient.mixin.accessors;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    @Invoker("setupTransforms")
    void bkclient$setupTransforms(net.minecraft.client.render.entity.state.LivingEntityRenderState state,
        net.minecraft.client.util.math.MatrixStack matrices, float bodyYaw, float baseScale);

    @Invoker("scale")
    void bkclient$scale(net.minecraft.client.render.entity.state.LivingEntityRenderState state,
        net.minecraft.client.util.math.MatrixStack matrices);

    @Invoker("addFeature")
    boolean bkclient$addFeature(FeatureRenderer<?, ?> feature);
}
