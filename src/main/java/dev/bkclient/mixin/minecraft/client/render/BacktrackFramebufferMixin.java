package dev.bkclient.mixin.minecraft.client.render;

import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Entity layers must stay inside the isolated ghost pass, including alternate render targets. */
@Mixin(Framebuffer.class)
public class BacktrackFramebufferMixin {
    @Inject(method = "beginWrite(Z)V", at = @At("HEAD"), cancellable = true)
    private void captureGhost(boolean viewport, CallbackInfo ci) {
        Framebuffer target = dev.bkclient.utility.render.BacktrackGhostGlow.captureTarget();
        if (target != null && (Object)this != target) {
            target.beginWrite(viewport);
            ci.cancel();
        }
    }
}
