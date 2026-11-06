package dev.bkclient.ui.menu.dropdown.components.settings.impl;

import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.animation.types.ColorAnimation;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;

public class BooleanSettingComponent extends MenuSettingComponent<BooleanSetting> {
   private Animation circleOpacityAnimation;
   private Animation enableAnimation;
   private ColorAnimation backgroundColorAnimation;
   private Animation dragAnim;
   private boolean drag;
   private boolean checkboxStyle;
   private float dragProgress;
   private double dragStartX;

   public BooleanSettingComponent(BooleanSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   public void onInit() {
      this.circleOpacityAnimation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
      this.enableAnimation = new Animation(300L, Easing.BAKEK);
      this.backgroundColorAnimation = new ColorAnimation(300L, new ColorRGBA(24.0F, 24.0F, 27.0F), Easing.FIGMA_EASE_IN_OUT);
      this.dragAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
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
      this.checkboxStyle = dev.bkclient.ui.components.SettingControls.cleanOptions();
      this.circleOpacityAnimation.update(this.setting.isEnabled() ? 1.0F : 0.75F);
      this.enableAnimation.update(this.setting.isEnabled() ? 1.0F : 0.0F);
      this.backgroundColorAnimation
         .update(this.setting.isEnabled() ? new ColorRGBA(0.0F, 216.0F, 32.0F) : new ColorRGBA(173.0F, 173.0F, 173.0F));
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      this.dragAnim.update(this.drag ? 1.0F : 0.0F);
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      float checkWidth = 26.0F;
      float checkHeight = 10.0F;
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
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.enableAnimation.getValue() + 0.25F * this.hoverAnimation.getValue())),
         this.width - checkWidth - 20.0F
      );
      float trackX = this.x + this.width - checkWidth - 9.0F;
      float trackPad = 1.0F;
      dev.bkclient.ui.components.SettingControls.option(context, trackX, this.y + 4.0F,
         this.drag ? this.dragProgress : this.enableAnimation.getValue(), this.dragAnim.getValue(), 1.0F);
      if (this.drag) {
         this.dragProgress = (float)Math.max(0.0F, Math.min(1.0F, (context.getMouseX() - trackX) / (checkWidth - trackPad * 2.0F)));
      }
   }

   @Override
   public void drawRegular8(UIContext context) {
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         if (this.checkboxStyle) {
            this.drag = false;
            this.setting.toggle();
            return;
         }
         this.drag = true;
         this.dragProgress = this.setting.isEnabled() ? 1.0F : 0.0F;
         this.dragStartX = mouseX;
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (this.drag && button == MouseButton.LEFT) {
         if (Math.abs(mouseX - this.dragStartX) > 3.0) {
            this.setting.setEnabled(this.dragProgress > 0.5F);
         } else {
            this.setting.toggle();
         }
         this.drag = false;
      }
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      this.height = 18.0F;
      return 18.0F;
   }
}
