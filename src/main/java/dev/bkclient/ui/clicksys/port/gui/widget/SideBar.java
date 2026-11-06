package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.ui.clicksys.port.module.Category;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.joml.Matrix4f;

public class SideBar extends Widget {
   private static final float value239 = 0.999F;
   private static final long time = 100000000L;
   private float value240;
   private float value241;
   private long time2 = -1L;
   private final Tween tween4 = EasingPresets.getTweenByFloatFloat2(0.0F, 0.22F);
   private final IconSlider iconSlider;
   private final AvatarSlider avatarSlider;
   private final CategoryTab categoryTab;
   private final List<CategoryTab> list = new ArrayList<>();
   private final AvatarSlider avatarSlider2;
   private final List<IconTab> list2 = new ArrayList<>();
   private final List<IconTab> list3 = new ArrayList<>();
   private final IconTab iconTab;
   private final IconTab iconTab2;
   private final IconTab iconTab3;
   private final ServerLabel serverLabel;
   private IconTab themesTab;
   private IconTab marketTab;
   private final IconTab cosmeticsTab;
   public void selectCosmetics() { update10(); cosmeticsTab.setFlag4(true); }
   public void selectAutoMarket() { update10(); marketTab.setFlag4(true); }
   private Runnable collapseAction;
   public void setCollapseAction(Runnable action) { collapseAction = action; }
   public void selectThemes() { update10(); themesTab.setFlag4(true); }
   private Consumer<Category> consumer;
   private Consumer<Category> consumer2;
   private Runnable runnable;
   private Runnable runnable2;
   private Runnable runnable3;
   private Runnable runnable4;

   public SideBar() {
      this.value237 = 224.0F;
      this.value238 = 700.0F;
      this.iconSlider = new IconSlider(CategoryType.LOGO, "Client");
      this.avatarSlider = new AvatarSlider("Client categories");
      this.categoryTab = new CategoryTab(null);
      this.categoryTab.setFlag4(true);

      for (Category category : Category.values()) {
         if (!category.check() && category != Category.RENDER) {
            this.list.add(new CategoryTab(category));
         }
      }

      this.avatarSlider2 = new AvatarSlider("Settings");
      this.cosmeticsTab = new IconTab(dev.bkclient.ui.components.LucideIcons.SPARKLES, "Cosmetics", () -> {
         if (net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof dev.bkclient.ui.clicksys.ClickSysScreen screen) screen.openCosmetics();
      });
      this.iconTab = new IconTab(CategoryType.CLOUDS, "Configs", this::update6);
      this.iconTab2 = new IconTab(CategoryType.FRIENDS, "Friends", this::update9);
      this.iconTab3 = new IconTab(CategoryType.WAYPOINT, "Waypoints", this::update3);
      marketTab = new IconTab(CategoryType.RACK, "AutoMarket", () -> {
         if (net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof dev.bkclient.ui.clicksys.ClickSysScreen screen) screen.openAutoMarket();
      });
      this.list2.add(this.iconTab);
      this.list2.add(this.iconTab2);
      themesTab = new IconTab(CategoryType.BRUSH, "Themes", () -> {
         if (net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof dev.bkclient.ui.clicksys.ClickSysScreen screen) screen.openThemes();
      });
      this.list2.add(this.iconTab3);
      this.list2.add(marketTab);
      this.list3.add(themesTab);
      this.list3.add(new IconTab(CategoryType.SEARCH, "Search", this::update8));
      this.serverLabel = new ServerLabel();
   }

   public void setRunnable4(Runnable runnable) {
      this.runnable4 = runnable;
   }

   private void update3() {
      if (!this.iconTab3.isFlag4()) {
         this.update10();
         this.iconTab3.setFlag4(true);
         if (this.runnable3 != null) {
            this.runnable3.run();
         }
      }
   }

   public void update4() {
      this.update10();
      this.iconTab2.setFlag4(true);
   }

   @Override
   public boolean isIntDoubleDouble(int count, double value, double value2) {
      if (count == 0 && value >= value235 && value <= value235+value237 && value2 >= value236 && value2 < value236+64 && collapseAction != null) { collapseAction.run(); return true; }
      if (this.categoryTab.isIntDoubleDouble(count, value, value2)) {
         this.update5();
         return true;
      } else {
         for (CategoryTab categorytab : this.list) {
            if (categorytab.isIntDoubleDouble(count, value, value2)) {
               this.onCategoryTab(categorytab);
               return true;
            }
         }

         if (this.cosmeticsTab.isIntDoubleDouble(count, value, value2)) return true;

         for (IconTab icontab : this.list2) {
            if (icontab.isIntDoubleDouble(count, value, value2)) {
               return true;
            }
         }

         for (IconTab icontab1 : this.list3) {
            if (icontab1.isIntDoubleDouble(count, value, value2)) {
               return true;
            }
         }

         return false;
      }
   }

   public void setRunnable(Runnable runnable2) {
      this.runnable = runnable2;
   }

   private void update5() {
      if (this.categoryTab.isFlag4()) {
         if (this.consumer2 != null) {
            this.consumer2.accept(null);
         }
      } else {
         this.update10();
         this.categoryTab.setFlag4(true);
         if (this.consumer != null) {
            this.consumer.accept(null);
         }
      }
   }

   public void setConsumer2(Consumer<Category> consumer) {
      this.consumer2 = consumer;
   }

   public void setFloat(float value) {
      float f = Math.clamp(value, 0.0F, 1.0F);
      this.value240 = f;
      this.value241 = f;
      float f2 = 48.0F;
      float f1 = 224.0F;
      this.value237 = EasingPresets.getFloatByFloatFloatFloat(f2, f, f1);
      boolean flag = f >= 0.999F;
      this.time2 = -1L;
      this.tween4.setFloat(flag ? 1.0F : 0.0F);
   }

   @Override
   public void onFloatFloatFloatMatrix4f(float value, float value2, float value3, Matrix4f matrix4f) {
      ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(18, value235,
         Theme.background(), matrix4f, value238, value237, value, value236);
      float x = value235 + 8;
      float width = value237 - 16;
      drawSidebarRow(iconSlider, value235, value236 + 4, value237, value, value2, value3, matrix4f);
      drawSidebarRow(avatarSlider, x, value236 + 56, width, value, value2, value3, matrix4f);
      float y = value236 + 88;
      drawSidebarRow(categoryTab, x, y, width, value, value2, value3, matrix4f);
      for (CategoryTab tab : list) {
         y += 36;
         drawSidebarRow(tab, x, y, width, value, value2, value3, matrix4f);
      }
      y += 36;
      drawSidebarRow(cosmeticsTab, x, y, width, value, value2, value3, matrix4f);
      y += 56;
      drawSidebarRow(avatarSlider2, x, y, width, value, value2, value3, matrix4f);
      y += 32;
      for (IconTab tab : list2) {
         drawSidebarRow(tab, x, y, width, value, value2, value3, matrix4f);
         y += 36;
      }
      float profileY = value236 + value238 - 50;
      float footerY = Math.max(y + 8, profileY - list3.size() * 36 - 12);
      for (IconTab tab : list3) {
         drawSidebarRow(tab, x, footerY, width, value, value2, value3, matrix4f);
         footerY += 36;
      }
      drawSidebarRow(serverLabel, x, profileY, width, value, value2, value3, matrix4f);
   }

   private void drawSidebarRow(SliderWidget row, float x, float y, float width,
         float alpha, float mouseY, float mouseX, Matrix4f matrix) {
      row.setValue239(value240);
      row.setValue237(width);
      row.onFloatFloat2(y, x);
      row.onFloatFloatFloatMatrix4f(alpha, mouseY, mouseX, matrix);
   }

   private void update6() {
      if (!this.iconTab.isFlag4()) {
         this.update10();
         this.iconTab.setFlag4(true);
         if (this.runnable != null) {
            this.runnable.run();
         }
      }
   }

   public void setRunnable2(Runnable runnable) {
      this.runnable2 = runnable;
   }

   public void update7() {
      this.update10();
      this.iconTab.setFlag4(true);
   }

   private void update8() {
      if (this.runnable4 != null) {
         this.runnable4.run();
      }
   }

   private void update9() {
      if (!this.iconTab2.isFlag4()) {
         this.update10();
         this.iconTab2.setFlag4(true);
         if (this.runnable2 != null) {
            this.runnable2.run();
         }
      }
   }

   public void setRunnable3(Runnable runnable) {
      this.runnable3 = runnable;
   }

   public void onCategory(Category category) {
      if (category == null) {
         if (!this.categoryTab.isFlag4()) {
            this.update10();
            this.categoryTab.setFlag4(true);
         }
      } else {
         for (CategoryTab categorytab : this.list) {
            if (categorytab.getCategory() == category) {
               if (categorytab.isFlag4()) {
                  return;
               }

               this.update10();
               categorytab.setFlag4(true);
               return;
            }
         }
      }
   }

   public void setConsumer(Consumer<Category> consumer2) {
      this.consumer = consumer2;
   }

   public Category getCategory() {
      if (this.categoryTab.isFlag4()) {
         return null;
      } else {
         for (CategoryTab categorytab : this.list) {
            if (categorytab.isFlag4()) {
               return categorytab.getCategory();
            }
         }

         return null;
      }
   }

   private void update10() {
      this.categoryTab.setFlag4(false);
      if (themesTab != null) themesTab.setFlag4(false);
      if (marketTab != null) marketTab.setFlag4(false);
      cosmeticsTab.setFlag4(false);

      for (CategoryTab categorytab : this.list) {
         categorytab.setFlag4(false);
      }

      this.iconTab.setFlag4(false);
      this.iconTab2.setFlag4(false);
      this.iconTab3.setFlag4(false);
   }

   public void setFloat2(float value) {
      this.value240 = Math.clamp(value, 0.0F, 1.0F);
      float f2 = this.value240;
      float f1 = 48.0F;
      float f = 224.0F;
      this.value237 = EasingPresets.getFloatByFloatFloatFloat(f1, f2, f);
      boolean flag = this.value240 + 1.0E-4F < this.value241;
      boolean flag1 = this.value240 >= 0.999F;
      if (!flag && flag1) {
         if (this.time2 < 0L) {
            this.time2 = System.nanoTime();
         }

         if (System.nanoTime() - this.time2 >= 100000000L) {
            this.tween4.setFloat2(1.0F);
         }
      } else {
         this.time2 = -1L;
         this.tween4.setFloat2(0.0F);
      }

      this.value241 = this.value240;
   }

   private void onSliderWidgetFloat(SliderWidget sliderWidget, float value) {
      sliderWidget.setValue239(this.value240);
      sliderWidget.setValue237(value);
   }

   private float getFloatByFloatFloatInt(float value, float value2, int count) {
      return count <= 0 ? value : value + count * 32.0F + Math.max(0, count - 1) * value2;
   }

   private float getFloatByFloatFloatFloatFloatFloatFloatFloatFloatMatrix4fList(
      float value, float value2, float value3, float value4, float value5, float value6, float value7, float value8, Matrix4f matrix4f, List list
   ) {
      float f = value7;
      float f1 = value;

      for (int i = 0; i < list.size(); i++) {
         SliderWidget sliderwidget = (SliderWidget)list.get(i);
         this.onSliderWidgetFloat(sliderwidget, value8);
         float f3 = EasingPresets.getFloatByFloatFloatFloat(f1, value3, f);
         sliderwidget.onFloatFloat2(f3, value2);
         sliderwidget.onFloatFloatFloatMatrix4f(value6, value4, value5, matrix4f);
         float f2 = 32.0F + (i < list.size() - 1 ? 4.0F : 0.0F);
         f += f2;
         f1 += f2;
      }

      return f;
   }

   private void onCategoryTab(CategoryTab categoryTab) {
      if (categoryTab.isFlag4()) {
         if (this.consumer2 != null) {
            this.consumer2.accept(categoryTab.getCategory());
         }
      } else {
         this.update10();
         categoryTab.setFlag4(true);
         if (this.consumer != null) {
            this.consumer.accept(categoryTab.getCategory());
         }
      }
   }

   private void onFloatSliderWidgetFloat(float value, SliderWidget sliderWidget, float value2) {
      sliderWidget.setValue239(value2);
      sliderWidget.setValue237(value);
   }

   public void update12() {
      this.update10();
      this.iconTab3.setFlag4(true);
   }
}
