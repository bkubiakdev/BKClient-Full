package dev.bkclient.ui.hud.impl.island.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.impl.island.IslandStatus;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.server.ServerUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.RingRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class PVPStatus extends IslandStatus implements IMinecraft {
   private static long previewUntil;
   private static final ColorRGBA DANGER = new ColorRGBA(248, 91, 103);
   private int combatDuration;

   public PVPStatus(SelectSetting setting) {
      super(setting, "pvp");
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> previewUntil = 0L);
   }

   public static void showPreview() {
      if (mc.player != null) previewUntil = System.nanoTime() + 15_000_000_000L;
   }

   public boolean isPreviewActive() {
      return mc.player != null && System.nanoTime() < previewUntil;
   }

   @Override
   public boolean canShow() {
      if (!ServerUtility.hasCT) this.combatDuration = 0;
      return mc.player != null && (isPreviewActive() || ServerUtility.hasCT);
   }

   @Override
   public void draw(CustomDrawContext context) {
      int seconds = isPreviewActive()
         ? Math.max(1, (int)Math.ceil((previewUntil - System.nanoTime()) / 1_000_000_000.0))
         : ServerUtility.ctTime;
      var font = Fonts.MEDIUM.getFont(6.5F);
      var timerFont = Fonts.SEMIBOLD.getFont(6.0F);
      String label = "PVP Mode";
      String timer = seconds + "s";
      float badgeWidth = Math.max(20.0F, timerFont.width(timer) + 9.0F);
      this.size.width = 39.0F + font.width(label) + badgeWidth;
      this.size.height = 18.0F;
      float x = sr.getScaledWidth() / 2.0F - BKClientInit.getInstance().getHud().getIsland().getSize().width / 2.0F;
      context.drawTextureSmooth(BKClientInit.id("icons/category/combat.png"), x + 6, 5, 8, 8, DANGER);
      context.drawText(font, label, x + 19, (18 - font.height()) / 2.0F, Colors.getTextColor());
      float badgeX = x + this.size.width - badgeWidth - 16;
      context.drawRoundedRect(badgeX, 3, badgeWidth, 12, BorderRadius.all(4), DANGER.withAlpha(30));
      context.drawCenteredText(timerFont, timer, badgeX + badgeWidth / 2, (18 - timerFont.height()) / 2.0F, DANGER);
      this.combatDuration = Math.max(this.combatDuration, ServerUtility.ctTime);
      float remaining = isPreviewActive()
         ? Math.clamp((previewUntil - System.nanoTime()) / 15_000_000_000.0F, 0.0F, 1.0F)
         : Math.clamp((float)seconds / Math.max(1, this.combatDuration), 0.0F, 1.0F);
      RingRenderer.progress(context.getMatrices(), x + this.size.width - 7, 9, 3, isPreviewActive() ? 0.6F : 1.0F,
         remaining, 0, DANGER.withAlpha(38), DANGER);
   }
}
