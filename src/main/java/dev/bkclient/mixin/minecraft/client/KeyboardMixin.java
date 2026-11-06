package dev.bkclient.mixin.minecraft.client;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.ui.hud.HudElement;
import dev.bkclient.ui.hud.impl.CustomChat;
import dev.bkclient.ui.hud.impl.DirectMessageChat;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin implements IMinecraft {
   @Inject(method = "onKey(JIIII)V", at = @At("HEAD"), cancellable = true)
   public void triggerKeyEvent(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
      if (DirectMessageChat.focused()) {
         if (action == 1 || action == 2) DirectMessageChat.key(key, scancode, modifiers);
         ci.cancel();
         return;
      }
      if (key != -1) {
         KeyPressEvent event = new KeyPressEvent(action, key);
         BKClientInit.getInstance().getEventManager().triggerEvent(event);
         if (event.isCancelled()) {
            ci.cancel();
            return;
         }
         if (mc.currentScreen == null && key == 46 && action == 1) {
            mc.setScreen(new ChatScreen(""));
         }
          if (mc.currentScreen instanceof ChatScreen && (action == 1 || action == 2) && CustomChat.active()) {
            for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
               if (element instanceof CustomChat cc) {
                  if (cc.onKeyPressed(key, scancode, modifiers)) ci.cancel();
                  break;
               }
            }
         }
      }
   }

   @Inject(method = "onChar(JII)V", at = @At("HEAD"), cancellable = true)
   public void triggerCharEvent(long window, int codepoint, int modifiers, CallbackInfo ci) {
      if (DirectMessageChat.focused()) {
         DirectMessageChat.character(codepoint, modifiers);
         ci.cancel();
         return;
      }
      if (mc.currentScreen instanceof ChatScreen && CustomChat.active()) {
         char chr = (char) codepoint;
         for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
            if (element instanceof CustomChat cc) {
               if (cc.charTyped(chr, modifiers)) ci.cancel();
               break;
            }
         }
      }
   }
}
