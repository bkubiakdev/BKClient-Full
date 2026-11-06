package dev.bkclient.mixin.minecraft.render.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.bkclient.ui.clicksys.port.util.IconRenderFlag;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** BKClient's translucent item layer while nametag equipment fades. */
@Mixin(ItemRenderState.LayerRenderState.class)
public abstract class NametagItemLayerMixin {
    @ModifyExpressionValue(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/item/ItemRenderState$LayerRenderState;renderLayer:Lnet/minecraft/client/render/RenderLayer;")
    )
    private RenderLayer bkclient$fadeEquipment(RenderLayer original) {
        return IconRenderFlag.isFlag() && original != null
            ? RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE) : original;
    }
}
