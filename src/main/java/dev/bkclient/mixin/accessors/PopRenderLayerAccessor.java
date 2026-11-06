package dev.bkclient.mixin.accessors;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(RenderLayer.MultiPhase.class)
public interface PopRenderLayerAccessor {
    @Invoker("getPhases") RenderLayer.MultiPhaseParameters bkclient$getPhases();
}
