package dev.bkclient.larp.mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.bkclient.larp.feature.Disguise;

@Mixin(value={ItemStack.class})
public class ItemStackBarMixin {
    @Inject(method={"isDamaged"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$hideDamage(CallbackInfoReturnable<Boolean> cir) {
        if (Disguise.hidesDurability((ItemStack)(Object)this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method={"isItemBarVisible"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$hideItemBar(CallbackInfoReturnable<Boolean> cir) {
        if (Disguise.hidesDurability((ItemStack)(Object)this)) {
            cir.setReturnValue(false);
        }
    }
}
