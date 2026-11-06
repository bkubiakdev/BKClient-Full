package dev.bkclient.mixin.accessors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(RenderLayer.MultiPhaseParameters.class)
public interface PopRenderPhasesAccessor {
    @Accessor("texture") RenderPhase.TextureBase bkclient$getTexture();
    @Accessor("layering") RenderPhase.Layering bkclient$getLayering();
}
