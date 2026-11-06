package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.module.Category;
import dev.bkclient.ui.clicksys.port.module.CategoryType;

public class CategoryTab extends TabButton {
   private final Category category;
   private boolean flag4;

   public CategoryTab(Category category2) {
      super(category2 != null ? category2.getCategoryType() : CategoryType.ALL, category2 != null ? category2.getString() : "All");
      this.category = category2;
   }

   @Override
   public boolean isIntDoubleDouble(int count, double value, double value2) {
      return count == 0 && this.isDoubleDouble(value, value2);
   }

   @Override
   protected boolean check() {
      return this.flag4;
   }

   public void setFlag4(boolean flag) {
      this.flag4 = flag;
   }

   public boolean isFlag4() {
      return this.flag4;
   }

   public Category getCategory() {
      return this.category;
   }

   @Override
   public void onFloatFloatFloatMatrix4f(float alpha, float mouseY, float mouseX, org.joml.Matrix4f matrix) {
      super.onFloatFloatFloatMatrix4f(alpha, mouseY, mouseX, matrix);
      if (!flag4 || value239 > .01F) return;
      String count = Integer.toString(dev.bkclient.ui.clicksys.port.data.ClientAccess.getListByCategory(category).size());
      float width = Math.max(24, dev.bkclient.ui.clicksys.port.render.TextShader.getFloatByStringFloat(count, 11) + 10);
      float x = value235 + value237 - width - 8, y = value236 + 8;
      dev.bkclient.ui.clicksys.port.render.ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(
         8, x, dev.bkclient.ui.clicksys.port.api.Theme.foreground(), matrix, 16, width, alpha, y);
      dev.bkclient.ui.clicksys.port.render.TextShader.onMatrix4fStringFloatFloatFloatIntFloat(matrix, count,
         x + (width - dev.bkclient.ui.clicksys.port.render.TextShader.getFloatByStringFloat(count, 11)) / 2,
         y + 2, 11, dev.bkclient.ui.clicksys.port.api.Theme.background(), alpha);
   }
}
