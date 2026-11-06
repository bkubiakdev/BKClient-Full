package dev.bkclient.mixin.freecam;

import dev.bkclient.systems.modules.modules.player.FreecamUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.network.ClientPlayerEntity.class)
public class FreecamPlayerMixin {
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void freecam$noPush(double x, double z, CallbackInfo ci) {
        if (FreecamUtil.isActive()) ci.cancel();
    }
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void freecam$packets(CallbackInfo ci) {
        if (FreecamUtil.shouldCancelPackets()) ci.cancel();
    }
    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void freecam$movement(CallbackInfo ci) {
        if (FreecamUtil.handleMovement((net.minecraft.client.network.ClientPlayerEntity)(Object)this)) ci.cancel();
    }
}

