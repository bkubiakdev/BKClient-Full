package dev.bkclient.ui.menu.dropdown.components.settings.impl;

import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;

public class StringSettingComponent extends MenuSettingComponent<StringSetting> {
   private TextField textField;

   public StringSettingComponent(StringSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   private boolean isHidden() {
      return this.setting.getHideCondition().getAsBoolean();
   }

   @Override
   public void onInit() {
      this.width = 13.0F;
      this.height = 8.0F;
      this.textField = new TextField(Fonts.REGULAR.getFont(8.0F));
      this.textField.paste(this.setting.getText());
      this.textField.setPreview(Localizator.translate("type_text"));
      super.onInit();
   }

   @Override
   public void update(UIContext context) {
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (!this.isHidden()) {
         float x = this.x + 8.0F;
         float y = this.y + 15.0F;
         float width = this.width - 16.0F;
         float height = this.height - 20.0F;
         this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
         if (this.isHovered(context.getMouseX(), context.getMouseY())) {
            CursorUtility.set(CursorType.HAND);
         }

         float checkWidth = 13.0F;
         float checkHeight = 8.0F;
         Font nameFont = Fonts.REGULAR.getFont(8.0F);
         float leftPadding = 10.0F;
         float nameHeight = nameFont.height();
         float headerHeight = 19.0F;
         this.drawSettingName(
            context,
            nameFont,
            Localizator.translate(this.setting.getName()),
            this.x + leftPadding,
            this.y + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight) - 0.5F,
            Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())),
            width - checkWidth - 20.0F
         );
         context.drawRoundedRect(x, y, width, height, BorderRadius.all(4.0F), Colors.getBackgroundColor().withAlpha(76.5F));
         this.textField.set(x, y, width, height);
         this.textField.setAlpha(1.0F);
         this.textField.setTextColor(Colors.getTextColor());
         this.textField.render(context);
         this.setting.text(this.textField.getBuiltText());
      }
   }

   @Override
   public void drawRegular8(UIContext context) {
   }

   @Override
   public void drawSplit(UIContext context) {
      if (!this.isHidden()) {
         float separatorHeight = 0.5F;
         context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
      }
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.isHidden()) {
         this.textField.onKeyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      return this.isHidden() ? false : this.textField.charTyped(chr, modifiers);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (!this.isHidden()) {
         this.textField.onMouseClicked(mouseX, mouseY, button);
         super.onMouseReleased(mouseX, mouseY, button);
      }
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (!this.isHidden()) {
         this.textField.onMouseReleased(mouseX, mouseY, button);
      }
   }

   @Override
   public float getHeight() {
      if (this.isHidden()) {
         return 0.0F;
      }

      this.height = 35.0F;
      return 35.0F;
   }
}
