package dev.bkclient.mixin.minecraft.client;

import dev.bkclient.systems.modules.modules.other.tinytask.TinyTaskBridge;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public class KeyBindingTinyTaskMixin {
    @Inject(method = "unpressAll", at = @At("HEAD"), cancellable = true)
    private static void bkClient$keepPlaybackKeysPressed(CallbackInfo ci) {
        if (TinyTaskBridge.isPlaying()) ci.cancel();
    }
}
