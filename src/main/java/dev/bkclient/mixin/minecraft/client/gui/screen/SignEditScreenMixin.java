package dev.bkclient.mixin.minecraft.client.gui.screen;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.other.AutoSign;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public class SignEditScreenMixin {
    @Inject(method = "init", at = @At("RETURN"))
    private void bkClient$confirmSign(CallbackInfo ci) {
        AutoSign module = BKClientInit.getInstance().getModuleManager().getModule(AutoSign.class);
        if (module.isEnabled()) ((AbstractSignEditScreen) (Object) this).close();
    }
}
