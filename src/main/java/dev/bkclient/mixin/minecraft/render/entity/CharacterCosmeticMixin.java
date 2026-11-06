package dev.bkclient.mixin.minecraft.render.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.bkclient.systems.cosmetics.render.CharacterCosmeticRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class CharacterCosmeticMixin {
    @WrapOperation(method="render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void bkclient$character(EntityModel<?> model, MatrixStack matrices, VertexConsumer vertices,
            int light, int overlay, int color, Operation<Void> original, LivingEntityRenderState state,
            MatrixStack enclosingMatrices, VertexConsumerProvider consumers, int enclosingLight) {
        var entry = CharacterCosmeticRenderer.selected(state);
        if (entry == null) original.call(model, matrices, vertices, light, overlay, color);
        else CharacterCosmeticRenderer.render(entry, (PlayerEntityRenderState)state, matrices, consumers, light, overlay, color);
    }

    @WrapWithCondition(method="render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V"))
    private boolean bkclient$characterFeatures(FeatureRenderer<?,?> feature, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, EntityRenderState state, float yaw, float pitch) {
        // Human armor, capes, hands and attachments do not share these characters' skeletons.
        // Selections stay saved and return as soon as the player chooses their regular skin.
        return CharacterCosmeticRenderer.selected(state) == null;
    }
}
