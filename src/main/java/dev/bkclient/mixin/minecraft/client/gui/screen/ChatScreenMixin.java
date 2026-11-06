package dev.bkclient.mixin.minecraft.client.gui.screen;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.mixin.accessors.ChatScreenAccessor;
import dev.bkclient.systems.event.impl.render.ChatRenderEvent;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.window.ChatClickEvent;
import dev.bkclient.ui.hud.HudElement;
import dev.bkclient.ui.hud.impl.CustomChat;
import dev.bkclient.ui.hud.impl.DirectMessageChat;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.sounds.ClientSounds;
import net.minecraft.client.gui.DrawContext;
import dev.bkclient.ui.hud.impl.SuggestionPopup;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen implements IMinecraft {

    protected ChatScreenMixin(Text title) {
      super(title);
   }

    @Inject(method = "init", at = @At("TAIL"))
    private void initializeCustomInput(CallbackInfo ci) {
        if (isCustomChatActive()) CustomChat.syncInput(((ChatScreenAccessor) this).bkclient$getChatField().getText());
    }

    @Inject(method = "sendMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String text, boolean addToHistory, CallbackInfo ci) {
       if (!isCustomChatActive()) {
          if (BKClientInit.getInstance().getCommandManager().dispatch(text.trim())) {
             if (addToHistory) mc.inGameHud.getChatHud().addToMessageHistory(text);
             ci.cancel();
          }
          return;
       }
       if (text.trim().isEmpty()) return;

       text = text.replace("\u00A7", "");
       text = StringHelper.stripInvalidChars(text);

       if (addToHistory) {
          mc.inGameHud.getChatHud().addToMessageHistory(text);
       }

       ClientSounds.CHAT_SEND.play(0.2F);

       if (BKClientInit.getInstance().getCommandManager().dispatch(text)) {
          ci.cancel();
          return;
       }

       ci.cancel();
       for (int i = 0; i < text.length(); i += 256) {
          String chunk = text.substring(i, Math.min(text.length(), i + 256));
          if (i == 0 && chunk.startsWith("/")) {
             mc.player.networkHandler.sendCommand(chunk.substring(1));
          } else {
             mc.player.networkHandler.sendChatMessage(chunk);
          }
       }
    }

    private boolean isCustomChatActive() {
      return CustomChat.active();
   }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!isCustomChatActive()) return;
      ci.cancel();

      CustomDrawContext customCtx = CustomDrawContext.of(context);
      BKClientInit.getInstance().getEventManager().triggerEvent(new HudRenderEvent(customCtx, delta));
      BKClientInit.getInstance().getEventManager().triggerEvent(new ChatRenderEvent(customCtx, delta));

      // Cancelling the vanilla render also cancelled the command suggestion popup, even though
      // the suggestor behind it kept running and kept producing results — they just had nowhere
      // to appear. Drawn last so it sits above the replacement chat rather than under it.
      if (!DirectMessageChat.focused()) SuggestionPopup.render(customCtx, ((ChatScreenAccessor) this).bkclient$getSuggestor());
   }

    // HUD popups must also render above the vanilla chat, after its input animation restores matrices.
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("RETURN"), order = 1100)
    private void renderVanillaChatHudEditor(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isCustomChatActive()) {
            BKClientInit.getInstance().getEventManager().triggerEvent(new ChatRenderEvent(CustomDrawContext.of(context), delta));
        }
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (BKClientInit.getInstance().getHud().clickPlayerPopup(mouseX, mouseY, button)) { cir.setReturnValue(true); return; }
      if (isCustomChatActive()) {
      if (DirectMessageChat.click(mouseX, mouseY, button)) { cir.setReturnValue(true); return; }
      for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
          if (element instanceof CustomChat chat && chat.isHovered(mouseX, mouseY)) {
              chat.onMouseClicked(mouseX, mouseY, dev.bkclient.framework.objects.MouseButton.fromButtonIndex(button));
              cir.setReturnValue(true);
              return;
          }
      }
      }
      ChatClickEvent event = new ChatClickEvent((float)mouseX, (float)mouseY, button);
      BKClientInit.getInstance().getEventManager().triggerEvent(event);
      if (event.isHandled()) cir.setReturnValue(true);
    }

    /**
     * Vanilla announces every change to its chat field here, whoever made it — a keystroke, a
     * tab-completion, a history recall, a paste. Hooking the announcement rather than the
     * individual actions means the client's chat picks all of them up without having to know
     * which key did what.
     */
    @Inject(method = "onChatFieldUpdate(Ljava/lang/String;)V", at = @At("TAIL"))
    private void bkclient$mirrorFieldToCustomChat(String text, CallbackInfo ci) {
       CustomChat.syncInput(text);
    }

}
