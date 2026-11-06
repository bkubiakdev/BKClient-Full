package dev.bkclient.ui.components.popup;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.render.DrawUtility;

/** Shared compact HUD options with native sliders and MSDF pictograms. */
public final class CompactHudSettingRow extends MenuSettingComponent<Setting> {
   private boolean dragging;
   public CompactHudSettingRow(Setting setting, Popup parent) { super(setting, parent); }
   @Override public float getHeight() { return 15; }
   private float trackX() { return x + width - 64; }
   private float trackWidth() { return 34; }
   @Override protected void renderComponent(UIContext c) {
      height = 15;
      if (!setting.isVisible()) { dragging = false; return; }
      if (dragging) {
         if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 0) finishDrag();
         else setSlider(c.getMouseX());
      }
      var ink = Colors.getTextColor();
      var surface = Colors.getBackgroundColor();
      boolean hovered = isHovered(c);
      c.drawRoundedRect(x + 7, y + 1, width - 14, 13, BorderRadius.all(4), surface.mulAlpha(hovered ? .65F : .4F));
      LucideIcons icon = setting instanceof SliderSetting ? LucideIcons.GAUGE : LucideIcons.SPARKLES;
      if (setting.getName().contains("header")) icon = LucideIcons.COPY;
      else if (setting.getName().contains("preview") || setting.getName().contains("display")) icon = LucideIcons.EYE;
      else if (setting.getName().contains("spacing") || setting.getName().contains("scale")) icon = LucideIcons.RULER;
      if (setting.getName().contains("locked")) icon = LucideIcons.LOCK;
      else if (setting.getName().contains("duration")) icon = LucideIcons.CLOCK;
      else if (setting.getName().contains("opacity")) icon = LucideIcons.DROPLETS;
      c.draw();
      icon.draw(c, x + 10, y + 4, 7, ink.mulAlpha(.6F));
      float labelWidth = setting instanceof SliderSetting ? width - 91 : width - 48;
      drawSettingName(c, Fonts.MEDIUM.getFont(6), Localizator.translate(setting.getName()), x + 21, y + 5,
         ink, Math.max(20, labelWidth));
      if (setting instanceof BooleanSetting toggle) {
         float bx = x + width - 21, by = y + 3;
         DrawUtility.drawRoundedRectScreenAA(c.getMatrices(), bx, by, 8, 8, BorderRadius.all(2.5F), toggle.isEnabled() ? Colors.getAccent() : ink.mulAlpha(.25F));
         if (!toggle.isEnabled()) DrawUtility.drawRoundedRectScreenAA(c.getMatrices(), bx + .7F, by + .7F, 6.6F, 6.6F, BorderRadius.all(2.3F), surface);
         c.draw();
         if (toggle.isEnabled()) LucideIcons.CHECK.draw(c, bx + .5F, by + .5F, 7, dev.bkclient.utility.colors.ColorRGBA.WHITE);
      } else if (setting instanceof SliderSetting slider) {
         float fraction = Math.clamp((slider.getCurrentValue() - slider.getMin()) / Math.max(.001F, slider.getMax() - slider.getMin()), 0, 1);
         float tx = trackX(), ty = y + 6.5F, tw = trackWidth();
         dev.bkclient.ui.components.SettingControls.slider(c, tx, ty, tw, fraction, 0, 1);
         String value = java.math.BigDecimal.valueOf((double)slider.getCurrentValue()).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
         c.drawRightText(Fonts.REGULAR.getFont(6), value, x + width - 12, y + 5, ink.mulAlpha(.6F));
      }
      if (hovered) CursorUtility.set(CursorType.HAND);
   }
   @Override public void onMouseClicked(double mx, double my, MouseButton button) {
      if (button != MouseButton.LEFT || !setting.isVisible() || !isHovered(mx, my)) return;
      if (setting instanceof BooleanSetting toggle) { toggle.toggle(); AbstractSetting.saveConfig(); }
      else if (mx >= trackX() - 4 && mx <= trackX() + trackWidth() + 4) { dragging = true; setSlider(mx); }
   }
   private void setSlider(double mx) {
      if (setting instanceof SliderSetting slider) slider.setCurrentValue(slider.getMin()
         + (float)Math.clamp((mx - trackX()) / trackWidth(), 0, 1) * (slider.getMax() - slider.getMin()));
   }
   private void finishDrag() { if (dragging) { dragging = false; AbstractSetting.saveConfig(); } }
   @Override public void onMouseReleased(double mx, double my, MouseButton button) {
      if (button == MouseButton.LEFT) { if (dragging) setSlider(mx); finishDrag(); }
   }
}
