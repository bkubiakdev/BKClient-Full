package dev.bkclient.mixin.minecraft.client.gui.screen;

import dev.bkclient.systems.modules.modules.other.tinytask.TinyTaskBridge;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenTinyTaskMixin {
    @Inject(method = "hasControlDown", at = @At("HEAD"), cancellable = true)
    private static void bkClient$controlDown(CallbackInfoReturnable<Boolean> cir) {
        if (TinyTaskBridge.hasModifier("CTRL")) cir.setReturnValue(true);
    }

    @Inject(method = "hasShiftDown", at = @At("HEAD"), cancellable = true)
    private static void bkClient$shiftDown(CallbackInfoReturnable<Boolean> cir) {
        if (TinyTaskBridge.hasModifier("SHIFT")) cir.setReturnValue(true);
    }

    @Inject(method = "hasAltDown", at = @At("HEAD"), cancellable = true)
    private static void bkClient$altDown(CallbackInfoReturnable<Boolean> cir) {
        if (TinyTaskBridge.hasModifier("ALT")) cir.setReturnValue(true);
    }
}
