package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.concurrent.Translations;
import dev.bkclient.ui.clicksys.port.data.ThemeConfig;
import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.systems.localization.Language;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.ui.clicksys.port.enums.ThemePalette;
import dev.bkclient.ui.clicksys.port.module.Category;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;
import dev.bkclient.ui.clicksys.port.util.Easings;
import java.util.function.Consumer;
import org.joml.Matrix4f;

public class TopBar extends Widget {
   private static final float value239 = 0.1F;
   private static final float value240 = 0.18F;
   private static final float value241 = 6.0F;
   private boolean flag4 = false;
   private boolean flag5;
   private String text = "All";
   private String text2;
   private final Tween tween4 = new Tween(1.0F, 0.18F).getTweenByFunction(Easings::getFloatByFloat6);
   private boolean flag6;
   private int value242 = 1;
   private int value243 = 1;
   private Category category;
   private Consumer<Boolean> consumer;
   private Consumer<Boolean> consumer2;
   private Runnable runnable;
   private Runnable searchAction;
   public void setSearchAction(Runnable action) { this.searchAction = action; }
   private float searchX() { return value235 + (value237 - 290) / 2; }
   private final IconButton iconButton;
   private final IconButton iconButton2;
   private final IconButton iconButton3;
   private final IconButton iconButton4;
   private final IconButton iconButton5;

   private final Runnable languageListener = this::update7;

   public TopBar() {
      this.value237 = 963.0F;
      this.value238 = 49.0F;
      this.iconButton = new IconButton(CategoryType.PANEL, 14.0F);
      this.iconButton.setText("Sidebar");
      this.iconButton.setRunnable(() -> {
         this.flag5 = !this.flag5;
         if (this.consumer2 != null) {
            this.consumer2.accept(this.flag5);
         }
      });
      this.iconButton2 = new IconButton(CategoryType.COLLAPSE_ALL, 14.0F);
      this.iconButton2.setText("Collapse all modules");
      this.iconButton2.setRunnable(() -> {
         if (this.consumer != null) {
            this.consumer.accept(true);
         }
      });
      this.iconButton3 = new IconButton(CategoryType.MOON, 14.0F);
      this.iconButton3.setText("Change theme");
      this.iconButton3.setRunnable(ThemeConfig::update);
      this.iconButton4 = new IconButton(CategoryType.LANGUAGE, 14.0F);
      this.update7();
      Translations.getInstance().addRunnable(this.languageListener);
      this.iconButton4.setRunnable(() -> {
         dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(Interface.class).toggleLanguage();
         AbstractSetting.saveConfig();
      });
      this.iconButton5 = new IconButton(CategoryType.STAR, 14.0F);
      this.iconButton5.setCategoryType2(CategoryType.STAR_FILLED);
      this.iconButton5.setText("Favorites");
      this.iconButton5.setRunnable(() -> {
         if (this.runnable != null) {
            this.runnable.run();
         }
      });
   }

   public void setString(String text3) {
      if (text3 != null) {
         String s = this.flag6 ? this.text2 : this.text;
         if (!text3.equals(s)) {
            this.text2 = text3;
            this.flag6 = true;
            this.value242 = this.value243;
            this.tween4.setValue5(0.1F);
            this.tween4.getTweenByFunction(Easings::getFloatByFloat4);
            this.tween4.setFloat2(0.0F);
         }
      }
   }

   @Override
   public boolean isIntDoubleDouble(int count, double value, double value2) {
      if (count == 0 && value >= searchX() && value <= searchX() + 290
          && value2 >= value236 + 7 && value2 <= value236 + 41 && searchAction != null) {
         searchAction.run();
         return true;
      }
      if (this.iconButton.isIntDoubleDouble(count, value, value2)) {
         return true;
      } else if (this.iconButton2.isIntDoubleDouble(count, value, value2)) {
         return true;
      } else if (this.iconButton3.isIntDoubleDouble(count, value, value2)) {
         return true;
      } else {
         return this.iconButton4.isIntDoubleDouble(count, value, value2) || this.iconButton5.isIntDoubleDouble(count, value, value2);
      }
   }

   public void setConsumer2(Consumer<Boolean> consumer) {
      this.consumer2 = consumer;
   }

   public void setFlag5(boolean flag) {
      this.flag5 = flag;
   }

   public void setCategory(Category category2) {
      this.flag4 = false;
      this.category = category2;
      this.value243 = 1;
      this.setString2(category2 != null ? category2.getString() : "All");
   }

   @Override
   public void onFloatFloatFloatMatrix4f(float value, float value2, float value3, Matrix4f matrix4f) {
      float f28, f29, f30;
      ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(16, value235 + 16,
         Theme.background(), matrix4f, 48, value237 - 32, value, value236);
      float f = this.value235 + 33.0F;
      float f1 = this.value236 + 17.0F;
      this.iconButton.onFloatFloat2(f1, f);
      this.iconButton.onFloatFloatFloatMatrix4f(value, value2, value3, matrix4f);
      float f2 = f + 14.0F + 14.0F;
      float f3 = this.value236 + 15.5F;
      float f4 = f2 + 7.5F;
      int j = Theme.border();
      float f23 = 17.0F;
      float f22 = 1.0F;

      float f5 = f2 + 4.0F;
      float f6 = this.value236 + 17.0F;
      float f7 = this.tween4.getFloat();
      if (this.flag6 && f7 <= 0.001F) {
         if (this.text2 != null) {
            this.text = this.text2;
         }

         this.text2 = null;
         this.flag6 = false;
         this.tween4.setValue5(0.18F);
         this.tween4.getTweenByFunction(Easings::getFloatByFloat6);
         this.tween4.setFloat2(1.0F);
         f7 = this.tween4.getFloat();
      }

      float f8 = (1.0F - f7) * 6.0F * (this.flag6 ? -this.value242 : this.value242);
      float f9 = EasingPresets.getFloatByFloat3(f7);
      TextShader.onMatrix4fStringFloatFloatFloatIntFloat(matrix4f, Translations.getInstance().getStringByString2(this.text), f5, f6 + f8, 14.0F, Theme.foreground(), value * f9);
      f28 = this.value235;
      f29 = this.value236 + 48.0F;
      f30 = this.value237;
      int k = dev.bkclient.ui.clicksys.ClickSysMaterial.layer(Theme.border(), .35f);
      float f27 = 1.0F;
      float f26 = f30;
      float f25 = f29;
      float f24 = f28;

      this.onFloatMatrix4fFloatFloat(value2, matrix4f, value, value3);
      float sx = searchX();
      ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(12, sx, Theme.surface(), matrix4f, 34, 290, value, value236 + 7);
      dev.bkclient.ui.clicksys.port.render.SvgShader.onFloatCategoryTypeIntMatrix4fFloatFloatFloat(
         value236 + 17, CategoryType.SEARCH, Theme.mutedFg(), matrix4f, value, 14, sx + 12);
      TextShader.onMatrix4fStringFloatFloatFloatIntFloat(matrix4f, Translations.getInstance().getStringByString2("Search"),
         sx + 34, value236 + 17, 14, Theme.mutedFg(), value);
      ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(10, sx + 248, Theme.elevated(), matrix4f, 22, 32, value, value236 + 13);
      TextShader.onMatrix4fStringFloatFloatFloatIntFloat(matrix4f, "Tab", sx + 254, value236 + 19, 12, Theme.mutedFg(), value);
   }

   private void update7() {
      String name = Localizator.getCurrentLanguage() == Language.PL_PL ? "Polski" : "English";
      this.iconButton4.setText(Localizator.translate("modules.settings.interface.language") + ": " + name);
   }

   public void setRunnable(Runnable runnable2) {
      this.runnable = runnable2;
   }

   private void onFloatMatrix4fFloatFloat(float value, Matrix4f matrix4f, float value2, float value3) {
      this.iconButton2.setCategoryType(CategoryType.COLLAPSE_ALL);
      this.iconButton2.setText("Collapse all modules");
      this.iconButton3.setCategoryType(ThemeConfig.getThemePalette() == ThemePalette.INSTANCE2 ? CategoryType.SUN : CategoryType.MOON);
      IconButton[] buttons = {this.iconButton2, this.iconButton3, this.iconButton4, this.iconButton5};
      for (int index = 0; index < buttons.length; index++) {
         float x = this.value235 + this.value237 - 46 - index * 40;
         ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(12, x - 10,
            Theme.surface(), matrix4f, 34, 34, value2, this.value236 + 7);
         buttons[index].onFloatFloat2(this.value236 + 17, x);
         buttons[index].onFloatFloatFloatMatrix4f(value2, value, value3, matrix4f);
      }

   }

   public void onBoolean(boolean flag) {
      this.iconButton5.setFlag4(flag);
   }

   public void setString2(String text3) {
      if (text3 != null) {
         this.text = text3;
         this.text2 = null;
         this.flag6 = false;
         this.tween4.setFloat(1.0F);
      }
   }

   public void setConsumer(Consumer<Boolean> consumer2) {
      this.consumer = consumer2;
   }

   public void setCategory2(Category category2) {
      this.flag4 = false;
      String s = category2 != null ? category2.getString() : "All";
      if (this.category != null && category2 != null && category2 != this.category) {
         this.value243 = category2.ordinal() > this.category.ordinal() ? 1 : -1;
      } else {
         this.value243 = 1;
      }

      this.category = category2;
      this.setString(s);
   }
}
