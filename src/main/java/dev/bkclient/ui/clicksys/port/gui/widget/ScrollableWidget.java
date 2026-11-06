package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;

public abstract class ScrollableWidget extends Widget {
   private static final float value239 = 48.0F;
   protected final Tween tween4 = EasingPresets.getTweenByFloatFloat(0.0F, 0.28F);
   protected float value240;
   protected float value241;

   protected void setFloat(float value) {
      this.value241 = value;
      float f = this.getFloat2();
      if (this.value240 < 0.0F) {
         this.value240 = 0.0F;
      }

      if (this.value240 > f) {
         this.value240 = f;
      }

      this.tween4.setFloat2(this.value240);
   }

   protected float getFloat() {
      return this.tween4.getFloat();
   }

   protected float getFloat2() {
      return Math.max(0.0F, this.value241 - this.value238 + 16.0F);
   }

   @Override
   public boolean isDoubleDoubleDouble(double value, double value2, double value3) {
      if (!this.isDoubleDouble(value2, value3)) {
         return false;
      } else {
         float previous = this.value240;
         this.value240 = Math.clamp(this.value240 - (float)(value * 48.0), 0.0F, this.getFloat2());
         if (previous != this.value240) dev.bkclient.ui.clicksys.ClickSysSounds.scroll();
         this.tween4.setFloat2(this.value240);
         return true;
      }
   }
}
