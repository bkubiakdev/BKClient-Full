package dev.bkclient.utility.colors;

import lombok.Generated;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.theme.Theme;

import dev.bkclient.utility.animation.types.ColorAnimation;

public final class Colors {
   public static final ColorRGBA RED = new ColorRGBA(255.0F, 0.0F, 0.0F);
   public static final ColorRGBA GREEN = new ColorRGBA(0.0F, 255.0F, 0.0F);
   public static final ColorRGBA BLUE = new ColorRGBA(0.0F, 0.0F, 255.0F);
   public static final ColorRGBA WHITE = new ColorRGBA(255.0F, 255.0F, 255.0F);
   public static final ColorRGBA BLACK = new ColorRGBA(0.0F, 0.0F, 0.0F);
   public static final ColorRGBA ACCENT = new ColorRGBA(20.0F, 100.0F, 255.0F);
   private static final ColorRGBA DARK_SEPARATOR = ColorRGBA.BLACK.withAlpha(20.4F);
   private static final ColorRGBA LIGHT_SEPARATOR = ColorRGBA.BLACK.withAlpha(12.75F);
   private static final long ANIMATION_DURATION = 500L;
   private static final ColorAnimation BACKGROUND_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation ADDITIONAL_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation TEXT_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation OUTLINE_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation FLAT_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation ACCENT_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation ICONS_COLOR_ANIMATION = new ColorAnimation(500L);
   private static final ColorAnimation ENABLED_MODULES_COLOR_ANIMATION = new ColorAnimation(500L);

   private static Theme getTheme() {
      return BKClientInit.getInstance().getThemeManager().getCurrentTheme();
   }

   public static ColorRGBA getBackgroundColor() {
      if (ClickGuiV2Theme.active()) return ClickGuiV2Theme.current().field;
      return getAnimatedColor(BACKGROUND_COLOR_ANIMATION, getTheme().getBackgroundColor());
   }

   public static ColorRGBA getAdditionalColor() {
      if (ClickGuiV2Theme.active()) return ClickGuiV2Theme.current().field;
      return getAnimatedColor(ADDITIONAL_COLOR_ANIMATION, getTheme().getAdditionalColor());
   }

   public static ColorRGBA getTextColor() {
      if (ClickGuiV2Theme.active()) return ClickGuiV2Theme.current().text;
      return getAnimatedColor(TEXT_COLOR_ANIMATION, getTheme().getTextColor());
   }

   public static ColorRGBA getOutlineColor() {
      if (ClickGuiV2Theme.active()) return ClickGuiV2Theme.current().border;
      return getAnimatedColor(OUTLINE_COLOR_ANIMATION, getTheme().getOutlineColor());
   }

   public static ColorRGBA getFlatColor() {
      if (ClickGuiV2Theme.active()) return ClickGuiV2Theme.current().surface;
      return getAnimatedColor(FLAT_COLOR_ANIMATION, getTheme().getFlatColor());
   }

   public static ColorRGBA getAccent() {
      if (ClickGuiV2Theme.active()) return ClickGuiV2Theme.current().accent;
      return getAnimatedColor(ACCENT_COLOR_ANIMATION, getTheme().getAccentColor());
   }

   public static ColorRGBA getAccentColor() {
      return getAccent();
   }

   public static ColorRGBA getIconsColor() {
      if (ClickGuiV2Theme.active()) return ClickGuiV2Theme.current().text;
      return getAnimatedColor(ICONS_COLOR_ANIMATION, getTheme().getIconsColor());
   }

   public static ColorRGBA getEnabledModulesColor() {
      if (ClickGuiV2Theme.active()) return ClickGuiV2Theme.current().selection;
      return getAnimatedColor(ENABLED_MODULES_COLOR_ANIMATION, getTheme().getEnabledModulesColor());
   }

   public static ColorRGBA getSeparatorColor() {
      if (ClickGuiV2Theme.active()) return ClickGuiV2Theme.current().border;
      return getTheme() == Theme.DARK ? DARK_SEPARATOR : LIGHT_SEPARATOR;
   }

   private static ColorRGBA getAnimatedColor(ColorAnimation animation, ColorRGBA color) {
      animation.update(color);
      return animation.getColor();
   }

   @Generated
   private Colors() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
