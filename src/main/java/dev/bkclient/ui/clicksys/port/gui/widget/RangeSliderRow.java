package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.RangeSliderSetting;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;
import org.joml.Matrix4f;

public class RangeSliderRow extends PanelWidget {
   private final RangeSliderSetting sliderSetting;
   private final Tween tween4 = EasingPresets.getTweenByFloatFloat(0.0F, 0.22F);
   private boolean flag4;
   private boolean upperHandle;
   private float value252;
   private long time3;
   private float value253 = Float.NaN;
   private String text;
   private float value254 = -1.0F;
   private String text2;

   public RangeSliderRow(RangeSliderSetting sliderSetting2) {
      super(sliderSetting2);
      this.sliderSetting = sliderSetting2;
      this.value252 = (float)sliderSetting2.getValue();
      this.time3 = System.nanoTime();
   }

   @Override
   public boolean isIntDoubleDouble(int count, double value, double value2) {
      if (count == 0 && this.isDoubleDouble3(value2, value)) {
         double picked = sliderSetting.getValue3() + Math.clamp((value-value235)/value237,0,1)*(sliderSetting.getValue42()-sliderSetting.getValue3());
         upperHandle = Math.abs(picked-sliderSetting.upper.getValue()) < Math.abs(picked-sliderSetting.getValue())
             || sliderSetting.getValue() == sliderSetting.upper.getValue() && picked >= sliderSetting.getValue();
         this.flag4 = true;
         this.onDouble(value);
         return true;
      } else {
         return false;
      }
   }

   private float getFloat8() {
      float f = this.getFloat9();
      float f1 = this.getFloatByFloat2(this.value237 - f - 22.0F);
      float f2 = this.getFloatByFloat(this.value237);
      return this.getFloatByFloatFloat2(f2, f1);
   }

   @Override
   public void onFloatFloatFloatMatrix4f(float value, float value2, float value3, Matrix4f matrix4f) {
      float f = (float)this.sliderSetting.getValue();
      long i = System.nanoTime();
      float f1 = (float)((i - this.time3) / 1.0E9);
      this.time3 = i;
      this.value252 = this.value252 + (f - this.value252) * (1.0F - (float)Math.exp(-14.0F * f1));
      float f2 = this.value252;
      String s = this.getStringByFloat(f2);
      float f3 = this.getFloat9();
      float f13 = this.value237 - f3 - 22.0F;
      float f4 = this.getFloatByFloatMatrix4fFloat(f13, matrix4f, value);
      float f14 = this.value237;
      float f5 = this.getFloatByMatrix4fFloatFloatFloat(matrix4f, value, f4, f14);
      float f6 = this.value236 + 1.0F;
      float f30 = this.value235 + this.value237 - f3 - 7;
      int j = Theme.foreground();
      float f16 = 12.0F;
      float f15 = f30;
      ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(9, f15 - 7,
         Theme.elevated(), matrix4f, 20, f3 + 14, value, f6 - 3);
      TextShader.onFloatFloatIntFloatFloatStringMatrix4f(f6, f15, j, f16, value, s, matrix4f);
      float f7 = this.getFloatByFloatFloat2(f5, f4);
      float ratio = (float)Math.clamp((f2 - this.sliderSetting.getValue3()) / Math.max(.000001, this.sliderSetting.getValue42() - this.sliderSetting.getValue3()), 0, 1);
      dev.bkclient.ui.clicksys.ClickSysMaterial.rangeSlider(matrix4f, this.value235, f7+1.5F, this.value237, ratio, (float)((sliderSetting.upper.getValue()-sliderSetting.getValue3())/Math.max(.000001,sliderSetting.getValue42()-sliderSetting.getValue3())), value);
      this.value238 = f7 + 10.0F - this.value236;
   }

   @Override
   public float getFloat5() {
      float f = this.getFloat9();
      float f1 = this.getFloatByFloat2(this.value237 - f - 22.0F);
      float f2 = this.getFloatByFloat(this.value237);
      return this.getFloatByFloatFloat3(f1, f2) + 8.0F + 10.0F;
   }

   private boolean isDoubleDouble3(double value, double value2) {
      float f = this.getFloat8();
      return value2 >= this.value235 && value2 <= this.value235 + this.value237 && value >= f - 10.0F && value <= f + 10.0F;
   }

   @Override
   protected CategoryType getCategoryType() {
      return CategoryType.SETTING_SLIDER;
   }

   private void onDouble(double value) {
      if (this.value237 <= 0 || !Double.isFinite(value)) return;
      float f = (float)Math.clamp((value - this.value235) / this.value237, 0.0, 1.0);
      double d0 = this.sliderSetting.getValue3() + f * (this.sliderSetting.getValue42() - this.sliderSetting.getValue3());
      double previousLow = sliderSetting.getValue();
      double previousHigh = sliderSetting.upper.getValue();
      if (upperHandle) sliderSetting.upper.setDouble2(Math.max(d0,sliderSetting.getValue()));
      else sliderSetting.setDouble2(Math.min(d0,sliderSetting.upper.getValue()));
      if (previousLow != sliderSetting.getValue() || previousHigh != sliderSetting.upper.getValue())
         dev.bkclient.ui.clicksys.ClickSysSounds.slider();
   }

   @Override
   public boolean isDoubleDoubleIntDoubleDouble(double value, double value2, int count, double value3, double value4) {
      if (this.flag4) {
         this.onDouble(value2);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean isDoubleDoubleInt(double value, double value2, int count) {
      if (this.flag4) {
         this.flag4 = false;
         return true;
      } else {
         return false;
      }
   }

   @Override public void update2() { this.flag4 = false; }

   private String getStringByFloat(float value) {
      int digits = sliderSetting.getValue6();
      String suffix = !sliderSetting.getText().isEmpty() ? sliderSetting.getText() : sliderSetting.getName().contains("ms") ? " ms" : "";
      return String.format(java.util.Locale.ROOT, "%1$."+digits+"f – %2$."+digits+"f%3$s", sliderSetting.getValue(), sliderSetting.upper.getValue(), suffix);
   }

   private float getFloat9() {
      String s = this.getStringByFloat((float)sliderSetting.getValue());
      if (s == this.text2) {
         return this.value254;
      } else {
         this.value254 = TextShader.getFloatByStringFloat(s, 12.0F);
         this.text2 = s;
         return this.value254;
      }
   }
}
