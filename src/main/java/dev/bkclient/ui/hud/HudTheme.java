package dev.bkclient.ui.hud;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.gradient.impl.HorizontalGradient;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

/** Shared HUD palette and panel renderer selected by the active menu appearance. */
public final class HudTheme {
   private static MenuModule menuModule;
   private static final ColorRGBA OLD_GLASS = new ColorRGBA(21.0F, 21.0F, 21.0F, 153.0F);
   private static final ColorRGBA OLD_FILL = new ColorRGBA(21.0F, 21.0F, 21.0F, 235.0F);

   private HudTheme() {
   }

   public static boolean isOld() {
      if (menuModule == null) {
         for (var module : BKClientInit.getInstance().getModuleManager().getModules()) {
            if (module instanceof MenuModule menu) {
               menuModule = menu;
               break;
            }
         }
      }
      return menuModule != null && menuModule.isOld();
   }

   public static void drawPanel(
      UIContext context,
      float x,
      float y,
      float width,
      float height,
      float alpha,
      float dragAnim,
      float squircle
   ) {
      drawPanel(context, x, y, width, height, alpha, dragAnim, squircle, true);
   }

   public static void drawPanel(UIContext context, float x, float y, float width, float height,
         float alpha, float dragAnim, float squircle, boolean accentGlow) {
      if (!isOld()) {
         context.drawClientRect(x, y, width, height, alpha, dragAnim, squircle);
         if (accentGlow) drawAccentGlow(context, x, y, width, height, alpha, 7.0F);
         return;
      }

      alpha *= HudVisualScope.opacity();
      float visibleAlpha = Math.clamp(alpha, 0.0F, 1.0F);
      BorderRadius radius = BorderRadius.all(Math.min(6.0F, Math.max(2.5F, squircle)));
      if (Interface.blurGui()) {
         context.drawBlurredRect(
            x,
            y,
            width,
            height,
            27.5F,
            2.0F,
            radius,
            OLD_GLASS.withAlpha(OLD_GLASS.getAlpha() * visibleAlpha)
         );
      }
      context.drawRoundedRect(
         x,
         y,
         width,
         height,
         radius,
         OLD_FILL.withAlpha(OLD_FILL.getAlpha() * visibleAlpha)
      );
      if (accentGlow) drawAccentGlow(context, x, y, width, height, alpha, radius.topLeftRadius());
      HudVisualScope.panel(context,x,y,width,height,radius.topLeftRadius(),2,alpha);
   }

   public static void drawIslandPanel(UIContext context, float x, float y, float width, float height,
         float bubbleX, float alpha, float dragAnim, boolean accentGlow) {
      boolean glass = !isOld() && Interface.showGlass();
      boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() == dev.bkclient.systems.theme.Theme.DARK;
      float fill = glass ? Math.max(0.0F, 0.8F - 0.3F * Interface.glass()) : (dark ? 0.85F : 0.7F);
      ColorRGBA color = isOld() ? OLD_FILL : Colors.getBackgroundColor().withAlpha(255.0F * fill);
      float backgroundMix = glass ? Interface.glass() * Interface.glass()
         : (isOld() ? (Interface.blurGui() ? 1.0F : 0.0F) : (Interface.showMinimalizm() ? Interface.minimalizm() : 0.0F));
      dev.bkclient.utility.render.DrawUtility.drawIslandMorph(context.getMatrices(), x, y, width, height,
         bubbleX, alpha * HudVisualScope.opacity(), dragAnim, color, backgroundMix);
      if (accentGlow) drawAccentGlow(context, x, y, width, height, alpha, 7.0F);
   }

   /** One liquid surface, including the concave shoulders between header and list. */
   public static void drawJoinedPanel(UIContext context, float x, float y, float width, float height,
         float headerOffset, float headerWidth, float headerHeight, float bodyTop, float alpha, float dragAnim, float selection, float selectionProgress) {
      boolean glass = !isOld() && Interface.showGlass();
      boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() == dev.bkclient.systems.theme.Theme.DARK;
      float fill = glass ? Math.max(0, .8F - .3F * Interface.glass()) : (dark ? .85F : .7F);
      ColorRGBA color = isOld() ? OLD_FILL : Colors.getBackgroundColor().withAlpha(255 * fill);
      float backgroundMix = glass ? Interface.glass() * Interface.glass()
         : (isOld() ? (Interface.blurGui() ? 1 : 0) : (Interface.showMinimalizm() ? Interface.minimalizm() : 0));
      context.draw();
      dev.bkclient.utility.render.DrawUtility.drawJoinedHudPanel(context.getMatrices(), x, y, width, height,
         headerOffset, headerWidth, headerHeight, bodyTop, alpha * HudVisualScope.opacity(), dragAnim, color, backgroundMix, selection, selectionProgress);
   }

   public static void drawAccentGlow(UIContext context, float x, float y, float width, float height, float alpha, float radius) {
      if (!Interface.hudAccentGlow() || width <= 0 || height <= 0) return;
      float rounding = Math.min(radius, Math.min(height, width * 0.69F) / 2.0F);
      ColorRGBA color = Colors.getAccent();
      context.drawRoundedRect(x, y, width * 0.69F, height, BorderRadius.left(rounding, rounding),
         new HorizontalGradient(color.withAlpha(51.0F * Math.clamp(alpha, 0.0F, 1.0F)), color.withAlpha(0)));
   }

   public static ColorRGBA textColor() {
      return isOld() ? CsTheme.Color.TEXT_75 : Colors.getTextColor();
   }

   public static ColorRGBA strongTextColor() {
      return isOld() ? CsTheme.Color.TEXT : Colors.getTextColor();
   }

   public static ColorRGBA separatorColor() {
      return isOld() ? CsTheme.Color.SEPARATOR : Colors.getSeparatorColor();
   }

   public static ColorRGBA surfaceColor() {
      return isOld() ? CsTheme.Color.SUB_FILL : Colors.getBackgroundColor();
   }
}
