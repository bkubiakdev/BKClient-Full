package dev.bkclient.mixin.minecraft.client.gui.screen;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.modules.modules.bkmods.automarket.ReconnectController;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the reference AutoMarket reconnect countdown and stop control. */
@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

   @Unique
   private ButtonWidget bkclient$stopReconnectButton;

   protected DisconnectedScreenMixin(Text title) {
      super(title);
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void bkclient$initAutoReconnect(CallbackInfo ci) {
      this.bkclient$stopReconnectButton = null;
      try {
         AutoMarket autoMarket = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
         if (autoMarket == null || !autoMarket.autoReconnect.isEnabled()) return;

         ReconnectController reconnect = autoMarket.getReconnectController();
         if (reconnect == null || reconnect.isAutoLeavePaused()) return;
         reconnect.onDisconnectScreenInit((Screen)(Object)this);

         this.bkclient$stopReconnectButton = ButtonWidget.builder(Text.literal("§c✕ STOP"), button -> {
            reconnect.cancel();
            button.active = false;
            button.setMessage(Text.literal("§7Stopped"));
         }).dimensions(this.width / 2 - 50, this.height - 40, 100, 20).build();
         this.addDrawableChild(this.bkclient$stopReconnectButton);
      } catch (RuntimeException ignored) {
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      try {
         AutoMarket autoMarket = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
         if (autoMarket == null || !autoMarket.autoReconnect.isEnabled()) return;

         ReconnectController reconnect = autoMarket.getReconnectController();
         if (reconnect == null || reconnect.isAutoLeavePaused()) return;

         if (reconnect.isPending() && !reconnect.isCancelled()) {
            String message = "§eReconnecting in §f" + reconnect.getSecondsUntilReconnect() + "s§e...";
            if (reconnect.getLastServerAddress() != null) {
               message += " §7(" + reconnect.getLastServerAddress() + ")";
            }
            context.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal(message), this.width / 2, this.height - 60, 0xFFFFFF55
            );
         } else if (reconnect.isCancelled()) {
            context.drawCenteredTextWithShadow(
               this.textRenderer, Text.literal("§7Auto-reconnect cancelled"),
               this.width / 2, this.height - 60, 0xFF888888
            );
         }

         if (this.bkclient$stopReconnectButton != null) {
            this.bkclient$stopReconnectButton.active = reconnect.isPending() && !reconnect.isCancelled();
         }
      } catch (RuntimeException ignored) {
      }
   }
}
