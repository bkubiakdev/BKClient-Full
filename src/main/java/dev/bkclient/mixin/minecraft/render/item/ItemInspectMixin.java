package dev.bkclient.mixin.minecraft.render.item;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.bkclient.systems.modules.modules.visuals.ItemInspect;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HeldItemRenderer.class)
public abstract class ItemInspectMixin {
    // Item-local pivot: keep View Model placement and vanilla resource-pack transforms.
    @WrapMethod(method = "renderItem")
    private void bkclient$inspect(LivingEntity entity, ItemStack stack, ModelTransformationMode mode,
                                 boolean leftHanded, MatrixStack matrices, VertexConsumerProvider consumers,
                                 int light, Operation<Void> original) {
        var pose = ItemInspect.renderPose(entity, stack, mode);
        if (pose == null) {
            original.call(entity, stack, mode, leftHanded, matrices, consumers, light);
            return;
        }
        matrices.push();
        try {
            ItemInspect.apply(matrices, pose);
            original.call(entity, stack, mode, leftHanded, matrices, consumers, light);
        } finally {
            matrices.pop();
        }
    }
}
