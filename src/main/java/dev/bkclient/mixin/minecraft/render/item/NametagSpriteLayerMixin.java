package dev.bkclient.mixin.minecraft.render.item;

import dev.bkclient.ui.clicksys.port.util.IconRenderFlag;
import java.util.function.Function;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers special item sprites (for example shields) during equipment fades. */
@Mixin(SpriteIdentifier.class)
public abstract class NametagSpriteLayerMixin {
    @Shadow public abstract Identifier getAtlasId();

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void bkclient$fadeSprite(Function<Identifier, RenderLayer> layerFactory,
                                    CallbackInfoReturnable<RenderLayer> callback) {
        if (IconRenderFlag.isFlag()) callback.setReturnValue(RenderLayer.getEntityTranslucent(getAtlasId()));
    }
}
