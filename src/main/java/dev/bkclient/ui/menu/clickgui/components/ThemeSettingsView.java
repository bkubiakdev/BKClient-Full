package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.theme.Theme;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.components.SettingControls;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;


import dev.bkclient.ui.menu.clickgui.ClickGuiScreen;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;

public final class ThemeSettingsView implements IMinecraft {

   private Rect lightPreview = Rect.EMPTY;
   private Rect darkPreview = Rect.EMPTY;
   private Rect accentPrimary = Rect.EMPTY;
   private Rect hudGlowToggle = Rect.EMPTY;
   private float blurStart;
   private float blurEnd;
   private float blurY;
   private boolean draggingBlur;
   private ColorPicker accentPicker;
   private final Rect[] accentSwatches = new Rect[8];
   private boolean quickPalette;
   private static final ColorRGBA[] QUICK_ACCENTS = {
      new ColorRGBA(245, 245, 245), new ColorRGBA(20, 20, 20),
      new ColorRGBA(239, 68, 68), new ColorRGBA(249, 115, 22),
      new ColorRGBA(250, 204, 21), new ColorRGBA(34, 197, 94), new ColorRGBA(0, 208, 171), new ColorRGBA(59, 130, 246)
   };
   private final java.util.Map<String, Animation> animations = new java.util.HashMap<>();

   private float animate(String key, boolean enabled, boolean initialize) {
      Animation animation = this.animations.computeIfAbsent(key,
         ignored -> new Animation(200L, initialize && enabled ? 1.0F : 0.0F, Easing.FIGMA_EASE_IN_OUT));
      return animation.update(enabled ? 1.0F : 0.0F);
   }

   public void render(UIContext context, float x, float y, float width, float height, float alpha) {
      Interface settings = settings();
      this.applyPicker();
      renderAppearance(context, x, y, width, height, alpha, settings);
      updateBlurDrag(context, settings);
   }

   private void updateBlurDrag(UIContext context, Interface settings) {
      if (!this.draggingBlur) return;
      if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 0) {
         this.draggingBlur = false;
         save();
         return;
      }
      float percent = Math.clamp((context.getMouseX() - this.blurStart) / Math.max(1, this.blurEnd - this.blurStart), 0, 1);
      settings.getGlassBlur().setCurrentValue(settings.getGlassBlur().getMin()
         + percent * (settings.getGlassBlur().getMax() - settings.getGlassBlur().getMin()));
   }

   private void renderAppearance(UIContext c, float x, float y, float width, float height, float alpha, Interface settings) {
      this.quickPalette = true;
      ColorRGBA ink = Colors.getTextColor();
      float controlsW = Math.min(width, 300);
      x += (width - controlsW) / 2;
      float cardHeight = Math.min(height, 224);
      float left = x + 14, right = x + controlsW - 14;
      contextPanel(c, x, y, controlsW, cardHeight, alpha);
      c.drawText(Fonts.MEDIUM.getFont(9), Localizator.translate("gui.color.title"), left, y + 13, ink.mulAlpha(alpha));
      c.drawText(Fonts.REGULAR.getFont(6), Localizator.translate("gui.color.subtitle"), left, y + 27, ink.mulAlpha(.45F * alpha));

      sectionLabel(c, Localizator.translate("gui.color.mode"), left, y + 47, alpha);
      float segmentW = (right - left) / 2;
      this.lightPreview = new Rect(left, y + 62, segmentW, 23);
      this.darkPreview = new Rect(left + segmentW, y + 62, segmentW, 23);
      c.drawRoundedRect(left, y + 62, right - left, 23, BorderRadius.all(5), ink.mulAlpha(.045F * alpha));
      float selected = animate("themeSegment", currentTheme() == Theme.DARK, true);
      c.drawRoundedRect(left + 2 + segmentW * selected, y + 64, segmentW - 4, 19,
         BorderRadius.all(4), ink.mulAlpha(.1F * alpha));
      Font segmentFont = Fonts.MEDIUM.getFont(7);
      c.drawCenteredText(segmentFont, Localizator.translate("gui.color.light"), this.lightPreview.centerX(), y + 62 + (23 - segmentFont.height()) / 2,
         ink.mulAlpha((1 - .5F * selected) * alpha));
      c.drawCenteredText(segmentFont, Localizator.translate("gui.color.dark"), this.darkPreview.centerX(), y + 62 + (23 - segmentFont.height()) / 2,
         ink.mulAlpha((.5F + .5F * selected) * alpha));
      if (lightPreview.contains(c.getMouseX(), c.getMouseY()) || darkPreview.contains(c.getMouseX(), c.getMouseY())) CursorUtility.set(CursorType.HAND);

      sectionLabel(c, Localizator.translate("gui.color.accent"), left, y + 101, alpha);
      c.drawRightText(Fonts.REGULAR.getFont(6), String.format(java.util.Locale.ROOT, "#%06X",
         currentTheme().getAccentColor().getRGB() & 0xffffff), right, y + 102,
         ink.mulAlpha(.55F * alpha));
      // Palette shares the exact width and alignment of the theme selector.
      drawQuickPalette(c, left - 8, y + 79, right - left + 16, alpha);

      c.drawRect(left, y + 150, right - left, .5F, ink.mulAlpha(.07F * alpha));
      this.hudGlowToggle = new Rect(left, y + 158, right - left, 25);
      sectionLabel(c, Localizator.translate("gui.color.glow"), left, y + 159, alpha);
      c.drawText(Fonts.REGULAR.getFont(6), Localizator.translate("gui.color.glow_hint"), left, y + 172, ink.mulAlpha(.45F * alpha));
      SettingControls.option(c, right - 26, y + 160, animate("hudGlow", settings.getHudAccentGlow().isEnabled(), true), 0, alpha);
      if (hudGlowToggle.contains(c.getMouseX(), c.getMouseY())) CursorUtility.set(CursorType.HAND);

      sectionLabel(c, Localizator.translate("gui.color.blur"), left, y + 191, alpha);
      float value = settings.getGlassBlur().getCurrentValue();
      c.drawRightText(Fonts.REGULAR.getFont(6), String.format(java.util.Locale.ROOT, "%.1f", value), right, y + 192, ink.mulAlpha(.6F * alpha));
      this.blurStart = left + 3;
      this.blurEnd = right - 3;
      this.blurY = y + 209;
      float fraction = (value - settings.getGlassBlur().getMin()) / Math.max(.001F, settings.getGlassBlur().getMax() - settings.getGlassBlur().getMin());
      SettingControls.cleanSlider(c, blurStart, blurY, blurEnd - blurStart, 0, fraction, false, alpha);
   }

   private void contextPanel(UIContext c, float x, float y, float width, float height, float alpha) {
      c.drawClientRect(x, y, width, height, alpha, 0, 8);
   }

   private void sectionLabel(UIContext c, String text, float x, float y, float alpha) {
      c.drawText(Fonts.MEDIUM.getFont(7), text, x, y, Colors.getTextColor().mulAlpha(.9F * alpha));
   }

   private void drawQuickPalette(UIContext context, float x, float y, float width, float alpha) {
         float left = x + 8, top = y + 36, stripWidth = width - 16;
         ColorRGBA ink = Colors.getTextColor();
         context.drawRoundedRect(left, top, stripWidth, 22, BorderRadius.all(5), ink.mulAlpha(.055F * alpha));
         float step = (stripWidth - 22) / QUICK_ACCENTS.length;
         for (int i = 0; i < QUICK_ACCENTS.length; i++) {
            float cx = left + step * (i + .5F);
            this.accentSwatches[i] = new Rect(left + step * i, top, step, 22);
            boolean selected = (currentTheme().getAccentColor().getRGB() & 0xffffff) == (QUICK_ACCENTS[i].getRGB() & 0xffffff);
            boolean hover = this.accentSwatches[i].contains(context.getMouseX(), context.getMouseY());
            float selection = this.animate("accent:" + i, selected, true);
            if (selection > .001F || hover) {
               dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(), cx - 5.3F, top + 5.7F,
                  10.6F, 10.6F, BorderRadius.all(5.3F), currentTheme().getAccentColor().mulAlpha(Math.max(selection * .8F, hover ? .3F : 0) * alpha));
            }
            dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(), cx - 4, top + 7,
               8, 8, BorderRadius.all(4), QUICK_ACCENTS[i].mulAlpha(alpha));
            if (hover) CursorUtility.set(CursorType.HAND);
         }
         float customX = left + stripWidth - 20;
         context.drawRect(customX, top + 6, .5F, 10, ink.mulAlpha(.12F * alpha));
         this.accentPrimary = new Rect(customX + 2, top, 18, 22);
         boolean hover = this.accentPrimary.contains(context.getMouseX(), context.getMouseY());
         context.drawTexture(BKClientInit.id("icons/colorpicker/pipette.png"), customX + 6, top + 7,
            8, 8, ink.mulAlpha((hover ? 1 : .65F) * alpha));
         if (hover) CursorUtility.set(CursorType.HAND);
   }

   public boolean onClick(double mouseX, double mouseY, MouseButton button) {
      if (button != MouseButton.LEFT) return false;
      if (this.quickPalette) {
         for (int i = 0; i < this.accentSwatches.length; i++) {
            if (this.accentSwatches[i] != null && this.accentSwatches[i].contains(mouseX, mouseY)) {
               if (this.accentPicker != null) {
                  this.accentPicker.setShowing(false);
                  this.accentPicker = null;
               }
               setAccent(QUICK_ACCENTS[i]);
               save();
               return true;
            }
         }
      }
      Interface settings = settings();
      if (this.hudGlowToggle.contains(mouseX, mouseY)) {
         settings.getHudAccentGlow().toggle();
         AbstractSetting.saveConfig();
      } else if (this.lightPreview.contains(mouseX, mouseY)) {
         settings.getLight().select();
         setTheme(Theme.LIGHT);
      } else if (this.darkPreview.contains(mouseX, mouseY)) {
         settings.getDark().select();
         setTheme(Theme.DARK);
      } else {
         if (this.accentPrimary.contains(mouseX, mouseY)) {
            this.openAccentPicker(mouseX, mouseY);


         } else if (GuiUtility.isHovered(this.blurStart - 3.0F, this.blurY - 5.0F,
               this.blurEnd - this.blurStart + 6.0F, 12.0F, mouseX, mouseY)) {
            this.draggingBlur = true;
            float percent = Math.clamp(((float)mouseX - this.blurStart) / Math.max(1.0F, this.blurEnd - this.blurStart), 0.0F, 1.0F);
            settings.getGlassBlur().setCurrentValue(
               settings.getGlassBlur().getMin() + percent * (settings.getGlassBlur().getMax() - settings.getGlassBlur().getMin())
            );
         } else {
            return false;
         }
      }
      save();
      return true;
   }

   public void onRelease(MouseButton button) {

      if (button == MouseButton.LEFT && this.draggingBlur) {
         this.draggingBlur = false;
         save();
      }
   }


   private void openAccentPicker(double mouseX, double mouseY) {
      this.accentPicker = new ColorPicker((float)mouseX, (float)mouseY, 6.0F, false,
         currentTheme().getAccentColor(), Localizator.translate("gui.color.accent"));
      if (BKClientInit.getInstance().getMenuScreen() instanceof ClickGuiScreen screen) {
         screen.getColorPickers().add(this.accentPicker);
      } else if (BKClientInit.getInstance().getMenuScreen() instanceof dev.bkclient.ui.menu.clickgui.ClickGuiV2Screen screen) {
         screen.getColorPickers().add(this.accentPicker);
      }
   }

   private void applyPicker() {
      if (this.accentPicker == null) return;
      setAccent(this.accentPicker.built());
      if (!this.accentPicker.isShowing() && this.accentPicker.getAnimation().getValue() <= 0.01F) {
         this.accentPicker = null;
         save();
      }
   }

   private static Interface settings() {
      return BKClientInit.getInstance().getModuleManager().getModule(Interface.class);
   }

   private static Theme currentTheme() {
      return BKClientInit.getInstance().getThemeManager().getCurrentTheme();
   }

   private static void setTheme(Theme theme) {
      BKClientInit.getInstance().getThemeManager().setCurrentTheme(theme);
      save();
   }

   private static void setAccent(ColorRGBA color) {
      Theme.DARK.setAccentColor(color.withAlpha(255.0F));
      Theme.LIGHT.setAccentColor(color.withAlpha(255.0F));
   }

   private static void save() {
      BKClientInit.getInstance().getThemeManager().flushSave();
   }

   private record Rect(float x, float y, float width, float height) {
      private static final Rect EMPTY = new Rect(0, 0, 0, 0);
      boolean contains(double mouseX, double mouseY) {
         return GuiUtility.isHovered(this.x, this.y, this.width, this.height, mouseX, mouseY);
      }
      float centerX() { return this.x + this.width / 2.0F; }
   }
}
