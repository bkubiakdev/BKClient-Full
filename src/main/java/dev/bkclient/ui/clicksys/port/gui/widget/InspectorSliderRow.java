package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.data.TextTrimmer;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.SvgShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.SliderSetting;
import org.joml.Matrix4f;

/** One-line slider used only by the detached clicksys inspector. */
final class InspectorSliderRow extends PanelWidget {
   private final SliderSetting slider;
   private boolean dragging;
   InspectorSliderRow(SliderSetting slider) { super(slider); this.slider = slider; }
   private float trackX() { return value235 + value237 * .55f; }
   private float trackWidth() { return Math.max(44, value237 * .45f - 12 - Math.max(32, TextShader.getFloatByStringFloat(number(), 12))); }
   private String number() { return String.format(java.util.Locale.ROOT, "% ." + slider.getValue6() + "f", slider.getValue()).trim() + (slider.getText() == null ? "" : slider.getText()); }
   @Override public float getFloat5() { return 18; }
   @Override public void onFloatFloatFloatMatrix4f(float alpha, float my, float mx, Matrix4f matrix) {
      value238 = 18;
      SvgShader.onFloatCategoryTypeIntMatrix4fFloatFloatFloat(value236 + 1, CategoryType.SETTING_SLIDER, Theme.mutedFg(), matrix, alpha, 14, value235);
      String name = TextTrimmer.getStringByFloatStringFloat2(value237 * .55f - 28, slider.getDisplayName(), 14);
      TextShader.onFloatStringFloatFloatIntFloatMatrix4f(alpha, name, 14, value235 + 20, Theme.foreground(), value236 + 2, matrix);
      float ratio = (float)Math.clamp((slider.getValue() - slider.getValue3()) / Math.max(.000001, slider.getValue42() - slider.getValue3()), 0, 1);
      float x = trackX(), w = trackWidth(), y = value236 + 8;
      dev.bkclient.ui.clicksys.ClickSysMaterial.slider(matrix, x, y, w, ratio, alpha);
      String number = number();
      TextShader.onFloatStringFloatFloatIntFloatMatrix4f(alpha, number, 12, value235 + value237 - TextShader.getFloatByStringFloat(number, 12), Theme.mutedFg(), value236 + 3, matrix);
      if (mx >= value235 && mx <= value235 + value237 && my >= value236 && my <= value236 + 18)
         HeaderPainter.onFloatFloatStringFloat(value235, value236, slider.getDisplayName() + (slider.getText() == null ? "" : " (" + slider.getText() + ")"), value237);
   }
   private void write(double x) {
      double ratio = Math.clamp((x - trackX()) / trackWidth(), 0, 1);
      slider.setDouble2(slider.getValue3() + ratio * (slider.getValue42() - slider.getValue3()));
   }
   @Override public boolean isIntDoubleDouble(int button, double x, double y) {
      if (button != 0 || x < trackX() - 5 || x > trackX() + trackWidth() + 5 || y < value236 - 2 || y > value236 + 20) return false;
      dragging = true; write(x); return true;
   }
   @Override public boolean isDoubleDoubleIntDoubleDouble(double y, double x, int button, double dx, double dy) {
      if (!dragging) return false; write(x); return true;
   }
   @Override public boolean isDoubleDoubleInt(double x, double y, int button) {
      boolean active = dragging; dragging = false; return active;
   }
   @Override public void update2() { dragging = false; }
}
