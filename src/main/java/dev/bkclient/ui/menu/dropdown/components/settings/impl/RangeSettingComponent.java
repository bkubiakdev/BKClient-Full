package dev.bkclient.ui.menu.dropdown.components.settings.impl;

import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.RangeSetting;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;

public class RangeSettingComponent extends MenuSettingComponent<RangeSetting> {
   private final Animation xAnim = new Animation(500L, Easing.BAKEK_PAGES);
   private final Animation widthAnim = new Animation(500L, Easing.BAKEK_PAGES);
   private boolean dragFirst;
   private boolean dragSecond;

   public RangeSettingComponent(RangeSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   protected void renderComponent(UIContext context) {
      float x = this.x + 9.0F;
      float y = this.y + 2.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float nameHeight = Fonts.REGULAR.getFont(7.0F).height();
      float first = this.setting.getFirstValue();
      float second;
      if (first >= (second = this.setting.getSecondValue())) {
         first = this.setting.getSecondValue();
         second = this.setting.getFirstValue();
      }

      this.xAnim.update(first);
      this.widthAnim.update(second);
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      dev.bkclient.ui.components.SettingControls.cleanSlider(context, x, y + this.height - 12, width,
         GuiUtility.getPercent(this.xAnim.getValue(), this.setting.getMin(), this.setting.getMax()),
         GuiUtility.getPercent(this.widthAnim.getValue(), this.setting.getMin(), this.setting.getMax()), true, 1);
      String value = String.format("from %s to %s", TextUtility.formatNumber(this.xAnim.getValue()), TextUtility.formatNumber(this.widthAnim.getValue()));
      this.drawSettingName(
         context,
         nameFont,
         Localizator.translate(this.setting.getName()),
         this.x + leftPadding,
         y + 11.0F - nameFont.height(),
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())),
         this.getParent().getWidth() - leftPadding - Fonts.REGULAR.getFont(7.0F).width(value) - 10.0F
      );
      context.drawRightText(
         Fonts.REGULAR.getFont(7.0F),
         value,
         x + width,
         y + 11.0F - nameHeight,
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()))
      );
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      if (this.dragFirst) {
         float xValue = GuiUtility.getSliderValue(this.setting.getMin(), this.setting.getMax(), x, width, context.getMouseX());
         this.setting.setFirstValue(xValue);
         CursorUtility.set(CursorType.ARROW_HORIZONTAL);
      } else if (this.dragSecond) {
         float xValue = GuiUtility.getSliderValue(this.setting.getMin(), this.setting.getMax(), x, width, context.getMouseX());
         this.setting.setSecondValue(xValue);
         CursorUtility.set(CursorType.ARROW_HORIZONTAL);
      }
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      float x = this.x + 9.0F;
      float width = this.width - 18.0F;
      if (this.isHovered(mouseX, mouseY)) {
         float firstDist = (float)Math.abs(
            mouseX - (x + width * GuiUtility.getPercent(this.setting.getFirstValue(), this.setting.getMin(), this.setting.getMax()))
         );
         if (firstDist
            < (float)Math.abs(mouseX - (x + width * GuiUtility.getPercent(this.setting.getSecondValue(), this.setting.getMin(), this.setting.getMax())))) {
            this.dragFirst = true;
         } else {
            this.dragSecond = true;
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.dragFirst = false;
      this.dragSecond = false;
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      this.height = 29.0F;
      return 29.0F;
   }
}
