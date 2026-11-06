package dev.bkclient.ui.menu.dropdown.components.settings.impl;

import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.NumberSetting;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;

public class NumberSettingComponent extends MenuSettingComponent<NumberSetting> {
   private TextField textField;

   public NumberSettingComponent(NumberSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   public void onInit() {
      this.width = 13.0F;
      this.height = 8.0F;
      this.textField = new TextField(Fonts.REGULAR.getFont(8.0F)) {
         @Override
         public boolean charTyped(char chr, int modifiers) {
            if (Character.isDigit(chr) || chr == '-' || chr == '.' && !NumberSettingComponent.this.setting.isInteger()) {
               return super.charTyped(chr, modifiers);
            }
            return false;
         }
      };
      this.textField.setText(this.setting.isInteger()
         ? String.format("%d", this.setting.getIntValue())
         : String.valueOf(this.setting.getValue()));
      this.textField.setPreview("0");
      super.onInit();
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (!this.setting.isVisible()) {
         return;
      }

      float x = this.x + 8.0F;
      float y = this.y + 15.0F;
      float width = this.width - 16.0F;
      float height = this.height - 20.0F;
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      context.drawFadeoutText(
         nameFont,
         Localizator.translate(this.setting.getName()),
         this.x + 10.0F,
         this.y + GuiUtility.getMiddleOfBox(nameFont.height(), 19.0F) - 0.5F,
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())),
         0.7F,
         0.99F,
         width - 33.0F
      );
      context.drawRoundedRect(x, y, width, height, BorderRadius.all(4.0F),
         Colors.getBackgroundColor().withAlpha(76.5F));
      this.textField.set(x, y, width, height);
      this.textField.setAlpha(1.0F);
      this.textField.setTextColor(Colors.getTextColor());
      this.textField.render(context);

      String text = this.textField.getBuiltText().replace(',', '.').trim();
      if (!text.isEmpty() && !text.equals("-") && !text.equals(".")) {
         try {
            double value = Double.parseDouble(text);
            if (value != this.setting.getValue()) {
               this.setting.setValue(value);
            }
         } catch (NumberFormatException ignored) {
         }
      }
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      this.textField.onKeyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      return this.textField.charTyped(chr, modifiers);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseClicked(mouseX, mouseY, button);
      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      if (!this.setting.isVisible()) {
         this.height = 0.0F;
         return 0.0F;
      }
      this.height = 35.0F;
      return this.height;
   }
}
