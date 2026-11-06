package dev.bkclient.larp.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.bkclient.larp.feature.DivineAxe;
import dev.bkclient.larp.feature.IllusionistWand;

@Mixin(value={OtherClientPlayerEntity.class})
public class OtherClientPlayerEntityCloneMixin {
    @Inject(method={"tick"}, at={@At(value="HEAD")})
    private void easyeventowka$moveClone(CallbackInfo ci) {
        OtherClientPlayerEntity self = (OtherClientPlayerEntity)(Object)this;
        IllusionistWand.tickCloneMovement(self);
        DivineAxe.tickPushed((PlayerEntity)self);
    }

    @Inject(method={"tickMovement"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$holdPushed(CallbackInfo ci) {
        if (DivineAxe.controls((PlayerEntity)((OtherClientPlayerEntity)(Object)this))) {
            ci.cancel();
        }
    }
}
