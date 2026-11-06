package dev.bkclient.larp.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.bkclient.larp.feature.FakeConsumable;

@Mixin(value={LivingEntity.class})
public class LivingEntityConsumeMixin {
    @Inject(method={"consumeItem"}, at={@At(value="HEAD")})
    private void easyeventowka$fakeConsumableEaten(CallbackInfo ci) {
        FakeConsumable.onConsume((LivingEntity)(Object)this);
    }
}
