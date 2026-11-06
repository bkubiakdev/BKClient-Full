package dev.bkclient.larp.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.bkclient.larp.feature.Disguise;

@Mixin(value={ItemStack.class})
public class ItemStackNameMixin {
    @Inject(method={"getFormattedName"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$disguiseFormattedName(CallbackInfoReturnable<Text> cir) {
        Text name = Disguise.name((ItemStack)(Object)this);
        if (name != null) {
            cir.setReturnValue(name);
        }
    }

}
