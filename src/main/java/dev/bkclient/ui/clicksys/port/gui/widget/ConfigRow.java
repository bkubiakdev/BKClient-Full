package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.data.AnimatedInt;
import dev.bkclient.ui.clicksys.port.data.ConfigEntry;
import dev.bkclient.ui.clicksys.port.data.ScrollTarget;
import dev.bkclient.ui.clicksys.port.data.TextTrimmer;
import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.ui.clicksys.port.enums.FontWeight;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.clicksys.port.render.SvgShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.joml.Matrix4f;

public class ConfigRow extends ListRow {
   private ConfigEntry configEntry;
   private final ToggleButton toggleButton;
   private final ScrollState scrollState;
   private final ScrollTarget scrollTarget;
   private static final long time = 2000000000L;
   private long time2;
   private final Tween tween5 = EasingPresets.getTweenByFloatFloat2(1.0F, 0.22F);
   private final Tween tween6 = EasingPresets.getTween();
   private final Tween tween7 = EasingPresets.getTweenByFloatFloat2(0.0F, 0.22F);
   private final TextInputController textInputController;
   private final TextInputController textInputController2;
   private float value239;
   private float value240;
   private float value241;
   private float value242;
   private float value243;
   private float value244;
   private float value245;
   private float value246;
   private float value247;
   private float value248;
   private float value249;
   private float value250;
   private Consumer<ConfigEntry> consumer;
   private Consumer<ConfigEntry> consumer2;
   private Consumer<ConfigEntry> consumer3;
   private Consumer<ConfigEntry> consumer4;
   private Consumer<ConfigEntry> consumer5;
   private Consumer<ConfigEntry> consumer6;
   private Consumer<ConfigEntry> consumer7;
   private Consumer<ConfigEntry> privateShare;
   public void setPrivateShare(Consumer<ConfigEntry> callback) { this.privateShare = callback; }
   public void markCopied() { this.time2 = System.nanoTime() + 2000000000L; }
   private java.util.function.BooleanSupplier sharingState;
   public void setSharingState(java.util.function.BooleanSupplier state) { sharingState=state; }
   private boolean sharing() { return sharingState != null && sharingState.getAsBoolean(); }
   private BiConsumer<ConfigEntry, String> biConsumer;
   private BiConsumer<ConfigEntry, String> biConsumer2;

   public ConfigRow(ConfigEntry configEntry2) {
      Consumer<String> consumerx = this::onString2;
      boolean flag = true;
      float f = 16.0F;
      this.textInputController = this.getTextInputControllerByBooleanFloatConsumer(flag, f, consumerx);
      Consumer<String> consumer1 = this::onString;
      boolean flag1 = false;
      float f1 = 14.0F;
      this.textInputController2 = this.getTextInputControllerByBooleanFloatConsumer(flag1, f1, consumer1);
      this.configEntry = configEntry2;
      this.value237 = 300.0F;
      this.value238 = 144.0F;
      this.toggleButton = new ToggleButton(configEntry2 != null && configEntry2.isFlag());
      ScrollState scrollstate = new ScrollState();
      CategoryType categorytype4 = CategoryType.TRASH;
      Runnable runnable = () -> this.onConsumer(this.consumer5);
      String s = "Delete config";
      float f3 = 14.0F;
      float f2 = 13.0F;
      CategoryType categorytype = categorytype4;
      scrollstate = scrollstate.getScrollStateByFloatBooleanSupplierCategoryTypeRunnableFloatString(f2, this::check3, categorytype, runnable, f3, s);
      categorytype4 = CategoryType.SAVE;
      Runnable runnable1 = () -> this.onConsumer(this.consumer3);
      String s1 = "Save config";
      float f5 = 14.0F;
      float f4 = 14.0F;
      CategoryType categorytype1 = categorytype4;
      scrollstate = scrollstate.getScrollStateByStringRunnableFloatFloatCategoryType(s1, runnable1, f4, f5, categorytype1);
      categorytype4 = CategoryType.SETTINGS;
      BooleanSupplier booleansupplier = this::check3;
      Runnable runnable2 = () -> this.onConsumer(this.consumer6);
      String s2 = "Open configs folder";
      float f7 = 13.0F;
      float f6 = 12.0F;
      CategoryType categorytype2 = categorytype4;
      scrollstate = scrollstate.getScrollStateByFloatBooleanSupplierCategoryTypeRunnableFloatString(f6, booleansupplier, categorytype2, runnable2, f7, s2);
      categorytype4 = CategoryType.SHARE;
      Supplier<String> supplier1 = () -> this.sharing() ? "Uploading..." : System.nanoTime() < this.time2 ? "BK code copied" : "Copy public BK code";
      BooleanSupplier booleansupplier1 = this::check6;
      Runnable runnable3 = () -> {
         this.onConsumer(this.consumer4);
      };
      Supplier<String> supplier = supplier1;
      float f9 = 14.0F;
      float f8 = 13.0F;
      CategoryType categorytype3 = categorytype4;
      scrollstate = scrollstate.getScrollStateByFloatBooleanSupplierCategoryTypeRunnableFloatSupplier(
         f8, booleansupplier1, categorytype3, runnable3, f9, supplier
      );
      this.scrollState = scrollstate.getScrollStateByFloatBooleanSupplierCategoryTypeRunnableFloatString(
         14, this::check6, CategoryType.KEY, () -> this.onConsumer(this.privateShare), 14, "Copy private BK code (single use)");
      this.scrollTarget = this.scrollState.getScrollTargetByInt(3);
      this.tween5.setFloat(configEntry2 != null && configEntry2.isFlag() ? 1.0F : 0.9F);
      this.tween7.setFloat(configEntry2 != null && configEntry2.isFlag2() ? 1.0F : 0.0F);
   }

   @Override protected String dateLabel() { return dev.bkclient.systems.localization.Localizator.translate("Updated:") + " "; }

   public void setConsumer4(Consumer<ConfigEntry> consumer) {
      this.consumer4 = consumer;
   }

   private boolean check3() {
      return this.configEntry != null && !this.configEntry.isFlag3();
   }

   private void onString(String text) {
      if (this.configEntry != null) {
         String s = this.configEntry.getText2() == null ? "" : this.configEntry.getText2();
         if (!text.equals(s)) {
            if (this.biConsumer2 != null) {
               this.biConsumer2.accept(this.configEntry, text);
            }
         }
      }
   }

   public void setConsumer(Consumer<ConfigEntry> consumer2) {
      this.consumer = consumer2;
   }

   public void setBiConsumer2(BiConsumer<ConfigEntry, String> biConsumer) {
      this.biConsumer2 = biConsumer;
   }

   private boolean check4() { return false; }

   public void setConsumer2(Consumer<ConfigEntry> consumer) {
      this.consumer2 = consumer;
   }

   public void setConsumer7(Consumer<ConfigEntry> consumer) {
      this.consumer7 = consumer;
   }

   public void setConsumer5(Consumer<ConfigEntry> consumer) {
      this.consumer5 = consumer;
   }

   public void setConsumer3(Consumer<ConfigEntry> consumer) {
      this.consumer3 = consumer;
   }

   private boolean check5() {
      return this.configEntry == null || !this.configEntry.isFlag3();
   }

   private void onConsumer(Consumer consumer) {
      if (consumer != null) {
         consumer.accept(this.configEntry);
      }
   }

   public void setBiConsumer(BiConsumer<ConfigEntry, String> biConsumer2) {
      this.biConsumer = biConsumer2;
   }

   private float getFloatByStringFloat(String text, float value) {
      if (text != null && !text.isEmpty()) {
         int i = text.lastIndexOf(10);
         String s = i >= 0 ? text.substring(i + 1) : text;
         return Math.min(TextShader.getFloatByStringFloat(s, 14.0F), value);
      } else {
         return 0.0F;
      }
   }

   private boolean check6() {
      return this.configEntry != null && !this.sharing();
   }

   private void onFloatFloatFloatMatrix4f3(float value, float value2, float value3, Matrix4f matrix4f) {
      boolean flag = this.configEntry != null && this.configEntry.isFlag2();
      this.tween7.setFloat2(flag ? 1.0F : 0.0F);
      float f = this.tween7.getFloat();
      this.scrollTarget.setValue(1.0F - f);
      float f1 = this.value236 + this.value238 - 17.0F - this.scrollState.getFloat();
      float f2 = this.value235 + this.value237 - 18.0F;
      float f3 = this.scrollState.getFloatByFloatFloatFloatMatrix4fFloatFloat(value2, value3, value, matrix4f, f1, f2);
      float f4 = this.value235 + 16.0F;
      float f5 = Math.max(0.0F, f3 - 8.0F - f4);
      float f6 = f1 + (this.scrollState.getFloat() - 12.0F) / 2.0F;
      float f8 = f6 + 1.0F;
      String s = this.configEntry != null ? this.configEntry.getText4() : null;
      float f7 = f8;
      this.onMatrix4fFloatFloatStringFloatFloat(matrix4f, f7, f5, s, f4, value);
   }

   public void setConfigEntry(ConfigEntry configEntry2) {
      this.configEntry = configEntry2;
      if (configEntry2 != null) {
         this.toggleButton.setBoolean2(configEntry2.isFlag());
      }
   }

   public ConfigEntry getConfigEntry() {
      return this.configEntry;
   }

   private void onMatrix4fFloat(Matrix4f matrix4f, float value) {
      float f = this.value235 + 16.0F;
      float f1 = this.value236 + 18.0F + 20.0F + 8.0F;
      float f2 = this.getFloat();
      this.value243 = f;
      this.value244 = f1;
      this.value245 = f2;
      this.value246 = 60.0F;
      if (this.textInputController2.isFlag()) {
         String s4 = this.textInputController2.getString();
         float f6 = 14.0F;
         byte b0 = 3;
         String s1 = s4;
         String s3 = TextTrimmer.getStringByFloatFloatIntString(f6, f2, b0, s1);
         int j = Theme.foreground();
         float f7 = 14.0F;
         TextShader.onFloatStringFloatFloatMatrix4fFloatFloatInt(value, s3, f, f7, matrix4f, f2, f1, j);
         if (this.textInputController2.check()) {
            float f3 = this.getFloatByStringFloat(s3, f2);
            float f8 = 14.0F;
            int i = Math.max(1, Math.round(TextShader.getFloatByFloatFloatString(f2, f8, s3) / TextShader.getFloatByFloat3(14.0F)));
            float f4 = f + f3 + 1.0F;
            float f5 = f1 + (i - 1) * TextShader.getFloatByFloat3(14.0F);
            int k = Theme.foreground();
            float f10 = 14.0F;
            float f9 = 1.0F;
            ShapeShader.onFloatFloatFloatFloatFloatMatrix4fInt(value, f10, f4, f5, f9, matrix4f, k);
         }
      } else {
         String s = this.configEntry != null && this.configEntry.getText2() != null ? this.configEntry.getText2() : "";
         if (!s.isEmpty()) {
            float f11 = 14.0F;
            byte b1 = 3;
            String description = java.util.Set.of("Local configuration", "Local profile", "BKClient configuration").contains(s) ? dev.bkclient.systems.localization.Localizator.translate(s) : s;
            String s5 = TextTrimmer.getStringByFloatFloatIntString(f11, f2, b1, description);
            int l = Theme.mutedFg();
            float f12 = 14.0F;
            String s2 = s5;
            TextShader.onFloatStringFloatFloatMatrix4fFloatFloatInt(value, s2, f, f12, matrix4f, f2, f1, l);
         }
      }
   }

   private void onString2(String text) {
      text = text.trim();
      if (this.configEntry != null && !text.isEmpty()) {
         if (!text.equals(this.configEntry.getText())) {
            if (this.biConsumer != null) {
               this.biConsumer.accept(this.configEntry, text);
            }
         }
      }
   }

   @Override
   protected boolean isIntDoubleDouble2(int count, double value, double value2) {
      if (count != 0) {
         return false;
      } else {
         float f16 = this.toggleButton.getValue235();
         float f17 = this.toggleButton.getValue236();
         float f3 = this.toggleButton.getValue238();
         float f2 = this.toggleButton.getValue237();
         float f1 = f17;
         float f = f16;
         if (isFloatFloatDoubleFloatFloatDouble(f, f1, value, f3, f2, value2)) {
            if (this.configEntry != null && !this.configEntry.isFlag()) this.onConsumer(this.consumer);
            return true;
         } else {
            if (this.check4()) {
               float f7 = this.value250;
               float f6 = this.value249;
               float f5 = this.value248;
               float f4 = this.value247;
               if (isFloatFloatDoubleFloatFloatDouble(f4, f5, value, f7, f6, value2)) {
                  this.update4();
                  if (this.consumer7 != null) {
                     this.consumer7.accept(this.configEntry);
                  }

                  return true;
               }
            }

            if (this.check5()) {
               float f11 = this.value242;
               float f10 = this.value241;
               float f9 = this.value240;
               float f8 = this.value239;
               if (isFloatFloatDoubleFloatFloatDouble(f8, f9, value, f11, f10, value2)) {
                  String s = this.configEntry != null ? this.configEntry.getText() : "";
                  TextInputController textinputcontroller = this.textInputController;
                  this.onStringTextInputController(s, textinputcontroller);
                  return true;
               }
            }

            if (this.check5()) {
               float f15 = this.value246;
               float f14 = this.value245;
               float f13 = this.value244;
               float f12 = this.value243;
               if (isFloatFloatDoubleFloatFloatDouble(f12, f13, value, f15, f14, value2)) {
                  String s1 = this.configEntry != null ? this.configEntry.getText2() : "";
                  TextInputController textinputcontroller1 = this.textInputController2;
                  this.onStringTextInputController(s1, textinputcontroller1);
                  return true;
               }
            }

            return false;
         }
      }
   }

   private float getFloat() {
      return Math.max(0.0F, this.value237 - 32.0F);
   }

   @Override
   protected void onFloatMatrix4fFloatFloat(float value, Matrix4f matrix4f, float value2, float value3) {
      this.tween5.setFloat2(this.configEntry != null && this.configEntry.isFlag() ? 1.0F : 0.9F);
      float f = value * this.tween5.getFloat();
      float f1 = 14.0F;
      this.onFloatMatrix4fFloat(f1, matrix4f, f);
      this.onFloatFloatMatrix4fFloat(value3, f, matrix4f, value2);
      this.onMatrix4fFloat(matrix4f, f);
      this.onFloatFloatFloatMatrix4f3(f, value2, value3, matrix4f);
   }

   @Override
   protected ScrollState getScrollState() {
      return this.scrollState;
   }

   private void onFloatFloatMatrix4fFloat(float value, float value2, Matrix4f matrix4f, float value3) {
      float f = this.value235 + 16.0F;
      float f1 = this.value236 + 18.0F;
      SvgShader.onFloatIntMatrix4fFloatCategoryTypeFloatFloatFloat(
         value2, Theme.foreground(), matrix4f, f1 + 3.5F, CategoryType.CLOUDS, 13, f + 3, 14);
      float f2 = f + 20.0F + 8.0F;
      float f3 = this.value235 + this.value237 - 16.0F - this.toggleButton.getValue237();
      float f4 = 15.0F;
      float f5 = f3 - 8.0F - f2 - f4;
      float f6 = f1 + 2.0F;
      this.value239 = f2 - 2.0F;
      this.value240 = f1;
      this.value241 = f5 + 4.0F;
      this.value242 = 20.0F;
      String s = this.configEntry != null && this.configEntry.getText() != null ? this.configEntry.getText() : "";
      TextInputController textinputcontroller1 = this.textInputController;
      int k1 = Theme.foreground();
      FontWeight fontweight = FontWeight.MEDIUM;
      int j = k1;
      float f14 = 16.0F;
      TextInputController textinputcontroller = textinputcontroller1;
      float f7 = EmptyRow.getFloatByIntFloatFloatFloatFloatMatrix4fFontWeightFloatStringTextInputController(
         j, f14, f2, f6, f5, matrix4f, fontweight, value2, s, textinputcontroller
      );
      float f8 = f2 + Math.min(f7, f5) + 6.0F;
      float f9 = f1 + 5.0F;
      boolean flag = this.configEntry != null && this.configEntry.isFlag2();
      boolean flag1 = this.check4();
      boolean flag2 = flag1 && value >= f8 - 2.0F && value <= f8 + 9.0F + 2.0F && value3 >= f9 - 2.0F && value3 <= f9 + 10.0F + 2.0F;
      this.tween6.setFloat2(flag2 ? 1.0F : 0.0F);
      int j1 = Theme.mutedFg();
      int i1 = Theme.foreground();
      float f15 = this.tween6.getFloat();
      int l = i1;
      int k = j1;
      int i = AnimatedInt.getIntByIntFloatInt(l, f15, k);
      CategoryType categorytype2 = CategoryType.LOCK_CLOSED;
      float f17 = 10.0F;
      float f16 = 9.0F;
      CategoryType categorytype1 = categorytype2;
      if (this.configEntry != null && this.configEntry.isFlag3())
         SvgShader.onFloatIntMatrix4fFloatCategoryTypeFloatFloatFloat(value2, i, matrix4f, f9, categorytype1, f17, f8, f16);
      this.value247 = f8 - 2.0F;
      this.value248 = f9 - 2.0F;
      this.value249 = 13.0F;
      this.value250 = 14.0F;
      float f10 = f1 + (20.0F - this.toggleButton.getValue238()) / 2.0F;
      this.toggleButton.onFloatFloat2(f10, f3);
      if (this.configEntry != null) {
         this.toggleButton.setBoolean2(this.configEntry.isFlag());
      }

      this.toggleButton.onFloatFloatFloatMatrix4f(value2, value3, value, matrix4f);
   }

   public void setConsumer6(Consumer<ConfigEntry> consumer) {
      this.consumer6 = consumer;
   }
}
