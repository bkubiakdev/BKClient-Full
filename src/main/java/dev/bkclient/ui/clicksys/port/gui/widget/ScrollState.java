package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.UiMetrics;
import dev.bkclient.ui.clicksys.port.data.ScrollTarget;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.joml.Matrix4f;

public class ScrollState implements UiMetrics {
   private static final float value235 = 18.0F;
   private static final float value236 = 2.0F;
   private final List<ScrollTarget> list = new ArrayList<>();
   private final float buttonSize, gap;
   public ScrollState() { this(18,2); }
   public ScrollState(float buttonSize,float gap) {this.buttonSize=buttonSize;this.gap=gap;}

   public float getFloatByFloatFloatFloatMatrix4fFloatFloat(float value, float value2, float value3, Matrix4f matrix4f, float value4, float value5) {
      float f = value5;
      float f1 = value5;

      for (ScrollTarget scrolltarget : this.list) {
         if (scrolltarget.check()) {
            float f2 = f - buttonSize;
            scrolltarget.iconLabel.onFloatFloat2(value4, f2);
            if(buttonSize>18) dev.bkclient.ui.clicksys.ClickSysMaterial.panel(matrix4f,f2,value4,buttonSize,buttonSize,12,
               dev.bkclient.ui.clicksys.port.api.Theme.elevated(),dev.bkclient.ui.clicksys.port.api.Theme.border(),value3);
            IconLabel iconlabel = scrolltarget.iconLabel;
            float f3 = scrolltarget.getValue(value3);
            iconlabel.onFloatFloatFloatMatrix4f(f3, value, value2, matrix4f);
            if (scrolltarget.iconLabel.isFlag() && scrolltarget.supplier != null) {
               String s = scrolltarget.supplier.get();
               if (s != null && !s.isEmpty()) {
                  float f5 = 18.0F;
                  float f4 = 18.0F;
                  HeaderPainter.onFloatStringFloatFloatFloat(f2, s, value4, f4, f5);
               }
            }

            f1 = f2;
            f = f2 - gap;
         }
      }

      return f1;
   }

   public boolean isDoubleIntDouble(double value, int count, double value2) {
      for (ScrollTarget scrolltarget : this.list) {
         if (scrolltarget.check() && scrolltarget.iconLabel.isIntDoubleDouble(count, value2, value)) {
            return true;
         }
      }

      return false;
   }

   public float getFloat() {
      return buttonSize;
   }

   public ScrollState getScrollStateByFloatBooleanSupplierCategoryTypeRunnableFloatString(
      float value, BooleanSupplier booleanSupplier, CategoryType categoryType, Runnable runnable, float value2, String text
   ) {
      Supplier supplier = () -> text;
      return this.getScrollStateByFloatBooleanSupplierCategoryTypeRunnableFloatSupplier(value, booleanSupplier, categoryType, runnable, value2, supplier);
   }

   public ScrollState getScrollStateByFloatBooleanSupplierCategoryTypeRunnableFloatSupplier(
      float value, BooleanSupplier booleanSupplier, CategoryType categoryType, Runnable runnable, float value2, Supplier supplier
   ) {
      IconLabel iconlabel = new IconLabel(categoryType, buttonSize, buttonSize, value, value2);
      iconlabel.setRunnable(runnable);
      this.list.add(new ScrollTarget(iconlabel, supplier, booleanSupplier));
      return this;
   }

   public ScrollTarget getScrollTargetByInt(int count) {
      return this.list.get(count);
   }

   public ScrollState getScrollStateByStringRunnableFloatFloatCategoryType(String text, Runnable runnable, float value, float value2, CategoryType categoryType) {
      Supplier supplier1 = () -> text;
      Object object = null;
      Supplier supplier = supplier1;
      return this.getScrollStateByFloatBooleanSupplierCategoryTypeRunnableFloatSupplier(value, (BooleanSupplier)object, categoryType, runnable, value2, supplier);
   }
}
