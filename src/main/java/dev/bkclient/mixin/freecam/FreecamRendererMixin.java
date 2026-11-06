package dev.bkclient.mixin.freecam;

import dev.bkclient.systems.modules.modules.player.FreecamUtil;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.render.GameRenderer.class)
public class FreecamRendererMixin {
    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void freecam$crosshair(float delta, CallbackInfo ci) {
        FreecamUtil.updateCrosshair(MinecraftClient.getInstance(), delta);
    }
}

