package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import org.joml.Matrix4f;

public class IconSlider extends SliderWidget {
   private final CategoryType categoryType;

   public IconSlider(CategoryType categoryType2, String text) {
      super(text, 224.0F, 49.0F);
      this.categoryType = categoryType2;
   }

   @Override
   public void onFloatFloatFloatMatrix4f(float value, float value2, float value3, Matrix4f matrix4f) {
      float expanded = Math.clamp(1 - this.value239, 0, 1);
      dev.bkclient.ui.clicksys.ClickSysMaterial.logo(matrix4f, value235 + 16, value236 + 16, 20, value, Theme.foreground());
      if (expanded > .01F) {
         ScissorStack.onFloatFloatFloatFloat(Math.max(0, value237 - 56), 24, value236 + 14, value235 + 44);
         try {
            TextShader.onFloatStringFloatFloatIntFloatMatrix4f(value * expanded, "Client", 16,
               value235 + 44, Theme.foreground(), value236 + 18, matrix4f);
         } finally { ScissorStack.update(); }
      }

   }
}
