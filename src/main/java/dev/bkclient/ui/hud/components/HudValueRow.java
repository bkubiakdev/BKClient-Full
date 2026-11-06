package dev.bkclient.ui.hud.components;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;

/** Shared icon / label / accent pill row used by Cooldowns, Friends and Staff. */
public final class HudValueRow {
   private HudValueRow() { }
   @FunctionalInterface public interface Icon {
      void draw(float x, float y, float size);
   }
   public static Font labelFont() { return HudTheme.isOld() ? Fonts.MEDIUM.getFont(5.5F) : Fonts.REGULAR.getFont(7); }
   public static Font valueFont() { return HudTheme.isOld() ? Fonts.SEMIBOLD.getFont(5.5F) : Fonts.MEDIUM.getFont(6.5F); }
   public static float height() { return HudTheme.isOld() ? 9 : 18; }
   public static float contentTop() { return HudTheme.isOld() ? 18 : 22; }
   public static float badgeWidth(String value) { return valueFont().width(value) + (HudTheme.isOld() ? 5 : 7); }
   public static float width(String label, String value) {
      return (HudTheme.isOld() ? 34 : 39) + labelFont().width(label) + valueFont().width(value);
   }
   public static float width(String label, String value, String prefix) {
      return width(label, value) + badgeWidth(prefix) + 4;
   }

   public static float timerHeight() { return HudTheme.isOld() ? 11 : 14; }
   public static float timerBadgeWidth(String value) { return badgeWidth(value) + 10; }
   public static float timerWidth(String label, String value) { return width(label, value) + 10; }

   /** Compact timer pill using the same badge, fonts and progress ring as the BK HUD. */
   public static void drawTimer(UIContext context, float x, float y, float width, String label,
         String value, float remaining, float alpha, Icon icon) {
      drawTimer(context, x, y, width, label, value, remaining, alpha, icon, true, true, true);
   }
   public static void drawTimer(UIContext context, float x, float y, float width, String label,
         String value, float remaining, float alpha, Icon icon, boolean showIcon, boolean showRing, boolean showBadge) {
      boolean old = HudTheme.isOld();
      float rowHeight = timerHeight();
      float badgeHeight = old ? 7 : 10;
      float badgeWidth = badgeWidth(value) + (showRing ? 10 : 0);
      float badgeX = x + width - badgeWidth - 6;
      float badgeY = y + (rowHeight - badgeHeight) / 2;
      float iconSize = old ? 7 : 10;
      if (showIcon) icon.draw(x + 6, y + (rowHeight - iconSize) / 2, iconSize);
      if (showBadge) drawBadgeBackground(context, badgeX, badgeY, badgeWidth, HudTheme.surfaceColor(), alpha * .7f);
      float labelX = x + (showIcon ? 21 : 6);
      context.drawFadeoutText(labelFont(), label, labelX,
         y + GuiUtility.getMiddleOfBox(labelFont().height(), rowHeight), HudTheme.textColor().mulAlpha(alpha),
         .9f, 1, Math.max(1, badgeX - labelX - 4));
      dev.bkclient.ui.components.BadgeText.draw(context, valueFont(), value,
         badgeX + (badgeWidth - (showRing ? 9 : 0)) / 2, y + rowHeight / 2, HudTheme.textColor().mulAlpha(.7f * alpha));
      context.draw();
      if (showRing)
      dev.bkclient.utility.render.RingRenderer.progress(context.getMatrices(),
         badgeX + badgeWidth - 5, y + rowHeight / 2, old ? 2.2f : 2.8f, .85f,
         Math.clamp(remaining, 0, 1), 0,
         HudTheme.textColor().mulAlpha(.12f * alpha), Colors.getAccent().mulAlpha(alpha));
   }

   public static void drawBadgeBackground(dev.bkclient.framework.base.CustomDrawContext context,
         float x, float y, float width, ColorRGBA background, float alpha) {
      boolean old = HudTheme.isOld();
      context.drawRoundedRect(x, y, width, old ? 7 : 10, BorderRadius.all(old ? 2 : 4),
         background.withAlpha((old ? 255 : 190) * alpha));
   }

   /** The same timer pill, also used for the red ADMIN rank and staff status. */
   public static void drawBadge(UIContext context, float x, float rowY, String value, ColorRGBA background, float alpha) {
      boolean old = HudTheme.isOld();
      Font font = valueFont();
      float badgeWidth = badgeWidth(value);
      float badgeY = rowY + (old ? 1 : 4);
      drawBadgeBackground(context, x, badgeY, badgeWidth, background, alpha);
      dev.bkclient.ui.components.BadgeText.draw(context, font, value, x + badgeWidth / 2,
         badgeY + (old ? 3.5F : 5.0F),
         (old ? ColorRGBA.BLACK.withAlpha(191) : HudTheme.strongTextColor()).mulAlpha(alpha));
   }

   public static void draw(UIContext context, float x, float y, float width, String label, String value,
      ColorRGBA labelColor, float alpha, Icon icon) {
      draw(context, x, y, width, label, value, labelColor, alpha, icon, null, null);
   }

   public static void draw(UIContext context, float x, float y, float width, String label, String value,
      ColorRGBA labelColor, float alpha, Icon icon, String prefix, ColorRGBA prefixColor) {
      draw(context, x, y, width, label, value, labelColor, alpha, icon, prefix, prefixColor, true);
   }
   public static void draw(UIContext context, float x, float y, float width, String label, String value,
      ColorRGBA labelColor, float alpha, Icon icon, String prefix, ColorRGBA prefixColor, boolean badge) {
      boolean old = HudTheme.isOld();
      Font labelFont = labelFont();
      if (icon != null) icon.draw(x + (old ? 7.5F : 6), y + 1, old ? 7 : 11.52F);
      float badgeX = x + width - (value.isEmpty() ? 0 : badgeWidth(value)) - (old ? 8 : 6);
      float labelX = x + (icon == null ? (old ? 7.5F : 6) : (old ? 17 : 22));
      if (prefix != null && !prefix.isEmpty()) {
         drawBadge(context, labelX, y, prefix, prefixColor, alpha);
         labelX += badgeWidth(prefix) + 4;
      }
      context.drawFadeoutText(labelFont, label, labelX, y + GuiUtility.getMiddleOfBox(labelFont.height(), height()),
         labelColor.mulAlpha(alpha), 0.9F, 1, Math.max(1, badgeX - labelX - 4));
      if (!value.isEmpty()) {
         if (badge) drawBadge(context, badgeX, y, value, Colors.getAccent(), alpha);
         else context.drawRightText(valueFont(), value, x + width - (old ? 8 : 6),
            y + GuiUtility.getMiddleOfBox(valueFont().height(), height()), labelColor.mulAlpha(alpha));
      }
   }
}
