package dev.bkclient.mixin.minecraft.client.gui.screen;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.mainmenu.CustomTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leaving a multiplayer server from the pause menu returns to the BK menu instead of vanilla's server list. */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {
   @Inject(method = "disconnect", at = @At("TAIL"))
   private void bkclient$backToBkMenu(CallbackInfo ci) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (!BKClientInit.INSTANCE.isPanic() && client.currentScreen instanceof MultiplayerScreen) {
         client.setScreen(new CustomTitleScreen());
      }
   }
}
