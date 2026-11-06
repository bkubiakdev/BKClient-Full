package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.msdf.MsdfFont;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

@ModuleInfo(name = "Test", category = ModuleCategory.OTHER, desc = "Render/font preview and UI debug triggers")
public class TestModule extends BaseModule {
   private static final String[] FONT_NAMES = {"Regular", "Medium", "Semibold", "Bold", "RoundBold"};
   private static final float[] FONT_SIZES = {7.0F, 10.0F, 14.0F};
   private static final String SAMPLE_TEXT = "The quick brown fox jumps 0123456789";


   private final ButtonSetting simulateDirectMessageChat = new ButtonSetting(this, "Simulate DM Chat")
      .action(dev.bkclient.ui.hud.impl.DirectMessageChat::showPreview);

   private final ButtonSetting simulatePvpMode = new ButtonSetting(this, "Simulate PVP Mode")
      .action(dev.bkclient.ui.hud.impl.island.impl.PVPStatus::showPreview);

   private boolean webhookTestPending;
   private final ButtonSetting testAutoMarketWebhook = new ButtonSetting(this, "Send AutoMarket Webhook Test")
      .action(this::sendWebhookPreview);

   private void sendWebhookPreview() {
      if (webhookTestPending) return;
      String url = dev.bkclient.systems.config.WebhookConfig.getUrl();
      if (url == null || url.isBlank()) {
         webhookNotice(false, "Najpierw wpisz Webhook URL w AutoMarket.");
         return;
      }
      webhookTestPending = true;
      dev.bkclient.utility.network.DiscordWebhook.sendTestNotification(url, success ->
         net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
            webhookTestPending = false;
            webhookNotice(success, success ? "Wyslano przykladowy zakup na Discord."
                  : "Wysylka nieudana. Sprawdz URL i log klienta.");
         }));
   }

   private void webhookNotice(boolean success, String message) {
      dev.bkclient.BKClientInit.getInstance().getNotificationManager().addNotificationOther(
         success ? dev.bkclient.systems.notifications.NotificationType.SUCCESS
                 : dev.bkclient.systems.notifications.NotificationType.ERROR,
         "AutoMarket Webhook Test", message);
   }

   private boolean renderPreviewVisible;
   private final ButtonSetting toggleRenderPreview = new ButtonSetting(this, "Show / Hide Render Preview")
      .action(() -> {
         if (!isEnabled()) enable();
         renderPreviewVisible = !renderPreviewVisible;
      });
   private final EventListener<HudRenderEvent> onHudRender = event -> {
      if (renderPreviewVisible) this.renderPreview(event.getContext());
   };

   @Override public void onEnable() { renderPreviewVisible = false; }
   @Override public void onDisable() { renderPreviewVisible = false; }

   private void renderPreview(CustomDrawContext context) {
      // Fonts.* is touched lazily here (only while actually rendering the HUD, long
      // after Minecraft's resource manager has loaded the font atlases) rather than
      // in a static field — referencing it too early (e.g. during module
      // registration, before resources exist) crashes with a font FileNotFoundException.
      MsdfFont[] fonts = {Fonts.REGULAR, Fonts.MEDIUM, Fonts.SEMIBOLD, Fonts.BOLD, Fonts.ROUND_BOLD};

      float x = 12.0F;
      float y = 30.0F;

      context.drawText(Fonts.SEMIBOLD.getFont(9.0F), "Render Test — Fonts", x, y, ColorRGBA.WHITE);
      y += 16.0F;

      for (int i = 0; i < fonts.length; i++) {
         MsdfFont font = fonts[i];
         for (float size : FONT_SIZES) {
            Font resolved = font.getFont(size);
            context.drawText(resolved, FONT_NAMES[i] + " " + (int) size + "px — " + SAMPLE_TEXT, x, y, ColorRGBA.WHITE);
            y += size + 4.0F;
         }
         y += 4.0F;
      }

      y += 6.0F;
      context.drawText(Fonts.SEMIBOLD.getFont(9.0F), "Render Test — Colors", x, y, ColorRGBA.WHITE);
      y += 14.0F;

      ColorRGBA[] swatches = {ColorRGBA.WHITE, ColorRGBA.GREEN, ColorRGBA.RED, Colors.getAccent(), Colors.getTextColor()};
      float swatchX = x;
      for (ColorRGBA color : swatches) {
         context.drawRoundedRect(swatchX, y, 24.0F, 14.0F, BorderRadius.all(4.0F), color);
         swatchX += 30.0F;
      }
      y += 24.0F;

      context.drawText(Fonts.SEMIBOLD.getFont(9.0F), "Render Test — Primitives", x, y, ColorRGBA.WHITE);
      y += 14.0F;

      context.drawSquircle(x, y, 60.0F, 30.0F, 8.0F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 40));
      context.drawLiquidGlass(x + 68.0F, y, 60.0F, 30.0F, 8.0F, 0.5F, BorderRadius.all(8.0F), ColorRGBA.WHITE.withAlpha(60));
      context.drawBlurredRect(x + 136.0F, y, 60.0F, 30.0F, 6.0F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, 40));
      context.drawRoundedBorder(x + 204.0F, y, 60.0F, 30.0F, 2.0F, BorderRadius.all(8.0F), Colors.getAccent());
   }
}
