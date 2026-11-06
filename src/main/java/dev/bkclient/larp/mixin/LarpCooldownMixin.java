package dev.bkclient.larp.mixin;
import dev.bkclient.larp.LarpCooldown;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCooldownManager.class)
public class LarpCooldownMixin {
    @Inject(method="getCooldownProgress", at=@At("RETURN"), cancellable=true)
    private void larp$cosmeticCooldown(ItemStack stack,float delta,CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValue(),LarpCooldown.progress(stack)));
    }
}
