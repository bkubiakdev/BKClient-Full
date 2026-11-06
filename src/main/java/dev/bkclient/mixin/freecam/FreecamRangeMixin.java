package dev.bkclient.mixin.freecam;

import dev.bkclient.systems.modules.modules.player.FreecamUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.entity.player.PlayerEntity.class)
public class FreecamRangeMixin {
    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void freecam$blocks(CallbackInfoReturnable<Double> cir) {
        if (FreecamUtil.isEnabled()) cir.setReturnValue(4.0);
    }
    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void freecam$entities(CallbackInfoReturnable<Double> cir) {
        if (FreecamUtil.isEnabled()) cir.setReturnValue(3.0);
    }
}

