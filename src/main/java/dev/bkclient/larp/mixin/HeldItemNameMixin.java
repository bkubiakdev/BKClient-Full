package dev.bkclient.larp.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.bkclient.larp.feature.Disguise;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Substitute only the HUD label; inventory names remain real for scanners. */
@Mixin(InGameHud.class)
public class HeldItemNameMixin {
    @WrapOperation(method = "renderHeldItemTooltip", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;getName()Lnet/minecraft/text/Text;"))
    private Text larp$heldItemName(ItemStack stack, Operation<Text> original) {
        Text name = Disguise.name(stack);
        return name == null ? original.call(stack) : name.copy().styled(style -> style.withItalic(false));
    }
}
