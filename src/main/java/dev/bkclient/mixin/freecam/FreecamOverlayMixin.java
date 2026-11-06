package dev.bkclient.mixin.freecam;

import dev.bkclient.systems.modules.modules.player.FreecamUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.hud.InGameOverlayRenderer.class)
public class FreecamOverlayMixin {
    @Inject(method = {"renderFireOverlay", "renderInWallOverlay"}, at = @At("HEAD"), cancellable = true)
    private static void freecam$overlay(CallbackInfo ci) {
        if (FreecamUtil.isActive()) ci.cancel();
    }
}

