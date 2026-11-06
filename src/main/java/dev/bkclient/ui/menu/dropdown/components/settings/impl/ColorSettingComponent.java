package dev.bkclient.ui.menu.dropdown.components.settings.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.menu.dropdown.DropDownScreen;
import dev.bkclient.ui.menu.clickgui.ClickGuiScreen;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.ui.menu.modern.ModernScreen;
import dev.bkclient.ui.menu.spotlight.SpotlightScreen;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;

public class ColorSettingComponent extends MenuSettingComponent<ColorSetting> {
   private ColorPicker picker;

   public ColorSettingComponent(ColorSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   public void onInit() {
      this.width = 13.0F;
      this.height = 8.0F;
      super.onInit();
   }

   @Override
   public void update(UIContext context) {
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      float checkWidth = 13.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float headerHeight = 19.0F;
      this.drawSettingName(
         context,
         nameFont,
         Localizator.translate(this.setting.getName()),
         this.x + leftPadding,
         this.y + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight) - 0.5F,
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())),
         this.width - checkWidth - 20.0F
      );
      dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(),
         this.x + this.width - leftPadding - 8, this.y + 5, 8, 8, BorderRadius.all(4), this.setting.getColor().withAlpha(255));
      dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(),
         this.x + this.width - leftPadding - 7, this.y + 6, 6, 6, BorderRadius.all(3), this.setting.getColor());
      if (this.picker != null) {
         this.setting.color(this.picker.built());
      }
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         if (BKClientInit.getInstance().getMenuScreen() instanceof DropDownScreen dropDownScreen) {
            this.picker = new ColorPicker(
               (float)mouseX, (float)mouseY, 6.0F, this.setting.isAlpha(), this.setting.getColor(), Localizator.translate(this.setting.getName())
            );
            dropDownScreen.getColorPickers().add(this.picker);
          } else if (BKClientInit.getInstance().getMenuScreen() instanceof ModernScreen modernScreen) {
             this.picker = new ColorPicker(
                (float)mouseX, (float)mouseY, 6.0F, this.setting.isAlpha(), this.setting.getColor(), Localizator.translate(this.setting.getName())
             );
             modernScreen.getColorPickers().add(this.picker);
          } else if (BKClientInit.getInstance().getMenuScreen() instanceof SpotlightScreen spotlightScreen) {
             this.picker = new ColorPicker(
                (float)mouseX, (float)mouseY, 6.0F, this.setting.isAlpha(), this.setting.getColor(), Localizator.translate(this.setting.getName())
             );
             spotlightScreen.getColorPickers().add(this.picker);
          } else if (BKClientInit.getInstance().getMenuScreen() instanceof ClickGuiScreen clickGuiScreen) {
             this.picker = new ColorPicker(
                (float)mouseX, (float)mouseY, 6.0F, this.setting.isAlpha(), this.setting.getColor(), Localizator.translate(this.setting.getName())
             );
             clickGuiScreen.getColorPickers().add(this.picker);
          } else if (BKClientInit.getInstance().getMenuScreen() instanceof dev.bkclient.ui.menu.clickgui.ClickGuiV2Screen screen) {
             this.picker = new ColorPicker(
                (float)mouseX, (float)mouseY, 6.0F, this.setting.isAlpha(), this.setting.getColor(), Localizator.translate(this.setting.getName())
             );
             screen.getColorPickers().add(this.picker);
          }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      this.height = 18.0F;
      return 18.0F;
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (this.setting != null && this.setting.getName() != null && this.setting.getName().startsWith("theme.")) {
         BKClientInit.getInstance().getThemeManager().flushSave();
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   public void closePicker() {
      this.picker = null;
   }
}
