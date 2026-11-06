package dev.bkclient.mixin.minecraft.client.render;
import dev.bkclient.utility.render.BKEngineShaderEspRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class ShaderEspWorldMixin {
    @Shadow private Framebuffer entityOutlineFramebuffer;
    // Preserve the solid entity mask; vanilla's Sobel pass would reduce it to an edge.
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gl/PostEffectProcessor;render(Lnet/minecraft/client/render/FrameGraphBuilder;IILnet/minecraft/client/gl/PostEffectProcessor$FramebufferSet;)V",ordinal=0))
    private void bkclient$keepMask(PostEffectProcessor effect,FrameGraphBuilder graph,int width,int height,PostEffectProcessor.FramebufferSet set,Operation<Void> original){
        if(!BKEngineShaderEspRenderer.ready()) original.call(effect,graph,width,height,set);
    }
    @Inject(method="render",at=@At("HEAD"))
    private void bkclient$beginShaderFrame(CallbackInfo ci) {
        BKEngineShaderEspRenderer.queue(null);
    }
    @Inject(method="render",at=@At("RETURN"))
    private void bkclient$queueShaderFrame(CallbackInfo ci) {
        if (BKEngineShaderEspRenderer.ready()) BKEngineShaderEspRenderer.queue(entityOutlineFramebuffer);
    }
    @Inject(method="drawEntityOutlinesFramebuffer",at=@At("HEAD"),cancellable=true)
    private void bkclient$shaderEsp(CallbackInfo ci){
        if(entityOutlineFramebuffer!=null && BKEngineShaderEspRenderer.ready()){
            BKEngineShaderEspRenderer.queue(entityOutlineFramebuffer);ci.cancel();
        }
    }
}
