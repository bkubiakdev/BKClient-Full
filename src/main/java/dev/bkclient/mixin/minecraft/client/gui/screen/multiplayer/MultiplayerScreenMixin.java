package dev.bkclient.mixin.minecraft.client.gui.screen.multiplayer;

import dev.bkclient.mixin.accessors.ScreenAccessor;
import dev.bkclient.ui.mainmenu.InGameAccountSwitcher;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void bkclient$addAccountSwitcher(CallbackInfo ci) {
        if (!InGameAccountSwitcher.isAvailable()) return;
        ButtonWidget accountSwitcher = this.addDrawableChild(ButtonWidget.builder(Text.literal("Account Switcher"), button -> {
            if (!InGameAccountSwitcher.open(this)) {
                button.setMessage(Text.literal("Unavailable"));
                button.active = false;
            }
        }).dimensions(8, 8, 108, 20).build());
        // Ogulniega can extend the server list to y=0. The first hovered child receives
        // the click, so this button must precede the list while still drawing on top.
        var children = ((ScreenAccessor) this).getChildren();
        children.remove(accountSwitcher);
        children.addFirst(accountSwitcher);
    }
}
