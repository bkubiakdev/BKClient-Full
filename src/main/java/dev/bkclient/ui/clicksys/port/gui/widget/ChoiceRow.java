package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.data.ChoiceOption;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.SvgShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.ChoiceSetting;
import org.joml.Matrix4f;

public class ChoiceRow extends SegmentedRow {
   private static final int value260 = 2;
   private static final float value261 = 2.83F;
   private final ChoiceSetting choiceSetting;

   public ChoiceRow(ChoiceSetting choiceSetting2) {
      this(choiceSetting2, false);
   }
   public ChoiceRow(ChoiceSetting choiceSetting2, boolean compact) {
      super(choiceSetting2, compact);
      this.choiceSetting = choiceSetting2;
      this.tween4.setFloat(choiceSetting2.isFlag3() ? 1.0F : 0.0F);
   }

   @Override
   protected float getFloat8() {
      return this.choiceSetting.isFlag3() ? 1.0F : 0.0F;
   }

   @Override
   protected void onMatrix4fFloatFloat(Matrix4f matrix4f, float value, float value2) {
      boolean flag = this.choiceSetting.isFlag3();
      ChoiceOption[] achoiceoption = new ChoiceOption[]{this.choiceSetting.getChoiceOption(), this.choiceSetting.getChoiceOption2()};

      for (int i = 0; i < 2; i++) {
         ChoiceOption choiceoption = achoiceoption[i];
         boolean flag1 = i == 1 == flag;
         int j = flag1 ? Theme.foreground() : Theme.mutedFg();
         if (choiceoption.getCategoryType() != null) {
            float f = value + 11.5F;
            float f1 = this.getFloatByInt2(i) - 7.0F;
            CategoryType categorytype1 = choiceoption.getCategoryType();
            float f4 = 9.0F;
            float f3 = 14.0F;
            CategoryType categorytype = categorytype1;
            SvgShader.onFloatIntMatrix4fFloatCategoryTypeFloatFloatFloat(value2, j, matrix4f, f, categorytype, f4, f1, f3);
         } else {
            float f6 = TextShader.getFloatByStringFloat(dev.bkclient.systems.localization.Localizator.translate(choiceoption.getText()), 12.0F);
            float f7 = this.getFloatByInt2(i) - f6 / 2.0F;
            float f2 = value + 10.0F;
            String s1 = dev.bkclient.systems.localization.Localizator.translate(choiceoption.getText());
            float f5 = 12.0F;
            String s = s1;
            TextShader.onFloatFloatIntFloatFloatStringMatrix4f(f2, f7, j, f5, value2, s, matrix4f);
         }
      }
   }

   @Override
   protected void onInt(int count) {
      this.choiceSetting.setBoolean(count == 1);
   }

   @Override
   protected String getString() {
      return dev.bkclient.systems.localization.Localizator.translate(this.choiceSetting.getChoiceOption3().getText());
   }

   @Override
   protected float getFloat10() {
      return this.value237 / 2.0F - 5.66F;
   }

   @Override
   protected int getInt() {
      return 2;
   }
}
