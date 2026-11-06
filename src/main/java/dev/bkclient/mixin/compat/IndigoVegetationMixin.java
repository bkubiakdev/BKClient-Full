package dev.bkclient.mixin.compat;

import dev.bkclient.utility.render.ambience.WindWaveTagger;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Indigo tessellates vanilla terrain through its own context on Fabric 1.21.4. */
@Mixin(value=TerrainRenderContext.class,remap=false)
public abstract class IndigoVegetationMixin extends AbstractBlockRenderContext {
    @Inject(method="getVertexConsumer",at=@At("RETURN"),cancellable=true,remap=false)
    private void bk$tag(RenderLayer layer,CallbackInfoReturnable<VertexConsumer> cir){
        if(blockInfo.blockState!=null&&blockInfo.blockPos!=null&&blockInfo.blockView!=null)
            cir.setReturnValue(WindWaveTagger.wrap(cir.getReturnValue(),blockInfo.blockState,blockInfo.blockPos,blockInfo.blockView,layer));
    }
}
