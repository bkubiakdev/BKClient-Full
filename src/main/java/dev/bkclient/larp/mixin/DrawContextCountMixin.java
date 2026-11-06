package dev.bkclient.larp.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.bkclient.larp.feature.Disguise;

@Mixin(value={DrawContext.class})
public class DrawContextCountMixin {
    @Inject(method={"drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$disguisedCount(TextRenderer textRenderer, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
        if (countLabel != null) {
            return;
        }
        int count = Disguise.displayCount(stack);
        if (count <= 0) {
            return;
        }
        ((DrawContext)(Object)this).drawStackOverlay(textRenderer, stack, x, y, String.valueOf(count));
        ci.cancel();
    }
}
