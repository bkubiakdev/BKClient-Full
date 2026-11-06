package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.SvgShader;
import java.util.function.Consumer;
import org.joml.Matrix4f;

public class FriendAddForm extends FormWidget {
   private static final float value239 = 220.0F;
   private static final float value240 = 36.0F;
   private static final float value241 = 6.0F;
   private final TextField textField;
   private Consumer<String> consumer;
   private final ToggleButton hiddenServerToggle = new ToggleButton(dev.bkclient.systems.friends.PresencePrivacy.isHidden());

   public FriendAddForm() {
      this.value237 = 300.0F;
      this.value238 = 116.0F;
      this.textField = new TextField(220.0F, 36.0F, "Player name...", 14.0F, 12.0F, 36.0F);
      this.textField.onRunnable(this::update4);
   }

   @Override
   public boolean isIntDoubleDouble(int count, double value, double value2) {
      if (count == 0 && value >= value235 + 16 && value <= value235 + value237 - 16) {
         boolean changed = false;
         if (value2 >= value236 + 72 && value2 < value236 + 102) {
            dev.bkclient.systems.friends.PresencePrivacy.setHidden(!dev.bkclient.systems.friends.PresencePrivacy.isHidden());
            changed = true;
         }
         if (changed) {
            textField.setBoolean(false);
            dev.bkclient.BKClientInit.getInstance().getFileManager().writeFile("client");
            dev.bkclient.utility.game.ClientTelemetryUtility.privacyChanged();
            return true;
         }
      }
      return this.textField.isIntDoubleDouble(count, value, value2);
   }

   @Override
   public boolean isIntIntInt2(int count, int count2, int count3) {
      return this.textField.isIntIntInt2(count, count2, count3);
   }

   @Override
   public boolean isIntChar(int count, char symbol) {
      return this.textField.isIntChar(count, symbol);
   }

   public TextField getTextField() {
      return this.textField;
   }

   public void setConsumer(Consumer<String> consumer2) {
      this.consumer = consumer2;
   }

   private void update4() {
      String s = this.textField.getString().trim();
      if (!s.isEmpty()) {
         if (this.consumer != null) {
            this.consumer.accept(s);
         }

         this.textField.update3();
         this.textField.setBoolean(false);
      }
   }

   @Override
   protected boolean drawBackground() { return false; }

   @Override
   protected float getFloat() {
      return 14.0F;
   }

   @Override
   protected void onFloatMatrix4fFloatFloat(float value, Matrix4f matrix4f, float value2, float value3) {
      drawPrivacyRow(matrix4f, value3, value236 + 72, "Hide my server",
            dev.bkclient.systems.friends.PresencePrivacy.isHidden(), hiddenServerToggle);
      float f = 246.0F;
      float f1 = this.value235 + (this.value237 - f) / 2.0F;
      float f2 = this.value236 + 18.0F;
      float f3 = f2 + 8.0F;
      float f11 = 13.0F;
      float f10 = 14.0F;
      float f9 = 20.0F;
      CategoryType categorytype = CategoryType.FRIENDS;
      SvgShader.onFloatIntMatrix4fFloatCategoryTypeFloatFloatFloat(value3, Theme.mutedFg(), matrix4f, f3 + 3.5F, categorytype, f11, f1 + 3, f10);
      float f4 = f1 + 20.0F + 6.0F;
      this.textField.setValue237(220.0F);
      this.textField.onFloatFloat2(f2, f4);
      this.textField.onFloatFloatFloatMatrix4f(value3, value2, value, matrix4f);
      float f5 = f4 + 220.0F - 12.0F - 24.0F;
      float f6 = f2 + 6.0F;
      int i = Theme.elevated();
      float f14 = 4.0F;
      float f13 = 24.0F;
      float f12 = 24.0F;
      float f7 = f5 + 5.335F;
      float f8 = f6 + 5.335F;
      CategoryType categorytype2 = CategoryType.INFO;
      int j = Theme.mutedFg();
      float f16 = 13.33F;
      float f15 = 13.33F;
      CategoryType categorytype1 = categorytype2;
      SvgShader.onFloatIntMatrix4fFloatCategoryTypeFloatFloatFloat(value3, j, matrix4f, f8, categorytype1, f16, f7, f15);
      double d3 = value;
      double d2 = value2;
      float f18 = 24.0F;
      float f17 = 24.0F;
      double d1 = d2;
      double d0 = d3;
      if (isFloatFloatDoubleFloatFloatDouble(f5, f6, d1, f18, f17, d0)) {
         float f20 = 24.0F;
         float f19 = 24.0F;
         String s = "Enter the player's nickname to add as a friend";
         HeaderPainter.onFloatStringFloatFloatFloat(f5, s, f6, f19, f20);
      }
   }

   private void drawPrivacyRow(Matrix4f matrix, float alpha, float y, String label, boolean enabled, ToggleButton toggle) {
      dev.bkclient.ui.clicksys.port.render.TextShader.onFloatFloatIntFloatFloatStringMatrix4f(
            y + 8, value235 + 26, Theme.foreground(), 13, alpha, label, matrix);
      toggle.setBoolean2(enabled);
      toggle.onFloatFloat2(y + 7, value235 + value237 - 54);
      toggle.onFloatFloatFloatMatrix4f(alpha, 0, 0, matrix);
   }

   public void setFloat(float value) {
      this.value237 = value;
   }
}
