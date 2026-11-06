package dev.bkclient.mixin.freecam;

import dev.bkclient.systems.modules.modules.player.FreecamUtil;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.entity.Entity.class)
public class FreecamEntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void freecam$look(double deltaX, double deltaY, CallbackInfo ci) {
        if (!FreecamUtil.isActive()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || (Object)this != client.player) return;
        FreecamUtil.applyLookDelta(deltaX, deltaY);
        ci.cancel();
    }
}

