package dev.bkclient.mixin.minecraft.render.entity;

import dev.bkclient.systems.cosmetics.render.MeleeCosmeticRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemFeatureRenderer.class)
public abstract class CosmeticHeldItemFeatureMixin {
    @Inject(method="renderItem(Lnet/minecraft/client/render/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/util/Arm;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at=@At("HEAD"),cancellable=true)
    private void bkclient$heldCosmetic(ArmedEntityRenderState state,ItemRenderState item,Arm arm,
            MatrixStack matrices,VertexConsumerProvider consumers,int light,CallbackInfo ci) {
        var client=MinecraftClient.getInstance();
        if (client.player==null || !(state instanceof PlayerEntityRenderState playerState)
            || playerState.id!=client.player.getId()) return;
        var held=arm==client.player.getMainArm()?client.player.getMainHandStack():client.player.getOffHandStack();
        if(!MeleeCosmeticRenderer.eligible(held))return;
        matrices.push();
        try {
            ((ModelWithArms)((FeatureRenderer<?,?>)(Object)this).getContextModel()).setArmAngle(arm,matrices);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            matrices.translate(arm==Arm.LEFT?-1f/16:1f/16,.125f,-.625f);
            if (MeleeCosmeticRenderer.thirdPerson(client.player,held,
                arm==Arm.LEFT?ModelTransformationMode.THIRD_PERSON_LEFT_HAND:ModelTransformationMode.THIRD_PERSON_RIGHT_HAND,
                matrices,consumers,light)) ci.cancel();
        } finally { matrices.pop(); }
    }
}
