package dev.bkclient.mixin.minecraft.client.render;

import dev.bkclient.utility.render.shader.FlameHandsRenderer;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Framebuffer.class)
public abstract class FramebufferMixin {
    @Inject(method = "beginWrite", at = @At("HEAD"), cancellable = true)
    private void bkclient$keepFlameCaptureTarget(boolean setViewport, CallbackInfo ci) {
        Framebuffer capture = FlameHandsRenderer.captureTarget();
        // Item/glint layers rebind their own output targets in 1.21.4. During the
        // isolated replay all of them must write into the same transparent mask.
        if (capture != null && capture != (Object)this) {
            capture.beginWrite(setViewport);
            ci.cancel();
        }
    }
}
