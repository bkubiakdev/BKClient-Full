package dev.bkclient.ui.menu.dropdown.components.settings.impl;

import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.time.Timer;

public class SliderSettingComponent extends MenuSettingComponent<SliderSetting> {
   private final Animation animation = new Animation(120L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation moving = new Animation(500L, Easing.FIGMA_EASE_IN_OUT);
   private final Timer timer = new Timer();
   private boolean drag;
   private final TextField numberField = new TextField(Fonts.REGULAR.getFont(7.0F));
   private float fieldX;
   private float fieldY;
   private float fieldW;
   private float fieldH;
   private static SliderSettingComponent current;

   public SliderSettingComponent(SliderSetting setting, CustomComponent parent) {
      super(setting, parent);
      this.animation.setValue(setting.getCurrentValue());
   }

   @Override
   protected void renderComponent(UIContext context) {
      float x = this.x + 9.0F;
      float y = this.y + 2.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float nameHeight = Fonts.REGULAR.getFont(7.0F).height();
      float headerHeight = 19.0F;
      if (this.drag && !this.numberField.isFocused()) {
         long windowHandle = mc.getWindow().getHandle();
         if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            this.drag = false;
         } else {
            float progress = Math.clamp((context.getMouseX() - x) / Math.max(1.0F, width), 0.0F, 1.0F);
            float rawValue = this.setting.getMin() + progress * (this.setting.getMax() - this.setting.getMin());
            this.setting.setCurrentValue(rawValue);
            // Follow the pointer every rendered frame; quantize only the stored value.
            this.animation.setValue(rawValue);
            CursorUtility.set(CursorType.ARROW_HORIZONTAL);
            current = this;
         }
      }
      if (!this.drag) this.animation.update(this.setting.getCurrentValue());
      this.moving.setDuration(200L);
      this.moving.update(this.drag ? 1.0F : 0.0F);
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      dev.bkclient.ui.components.SettingControls.slider(context, x, y + this.height - 12.0F, width,
         GuiUtility.getPercent(this.animation.getValue(), this.setting.getMin(), this.setting.getMax()), this.moving.getValue(), 1.0F);
      String value = this.formatSliderValue(this.setting.getCurrentValue(), this.setting.getStep()) + this.setting.getSuffix();
      Font valueFont = Fonts.REGULAR.getFont(7.0F);
      float valueTextWidth = valueFont.width(value);
      float fieldWidth = Math.max(valueTextWidth + 4.0F, 24.0F);
      this.fieldX = x + width - fieldWidth;
      this.fieldY = y + 11.0F - nameHeight - 1.0F;
      this.fieldW = fieldWidth;
      this.fieldH = nameHeight + 2.0F;
      this.drawSettingName(
         context,
         nameFont,
         Localizator.translate(this.setting.getName()),
         this.x + leftPadding,
         y + 11.0F - nameFont.height(),
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())),
         this.getParent().getWidth() - leftPadding - valueTextWidth - 10.0F
      );
      if (this.numberField.isFocused()) {
         this.numberField.set(this.fieldX, this.fieldY, this.fieldW, this.fieldH);
         this.numberField.setAlpha(1.0F);
         this.numberField.setTextColor(Colors.getTextColor());
         this.numberField.render(context);
      } else {
         context.drawRightText(
            valueFont, value, x + width, y + 11.0F - nameHeight, Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()))
         );
      }

      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

   }

   private String formatSliderValue(double number, float step) {
      int decimals = 0;
      if (step < 1.0F) {
         String s = Float.toString(step);
         int dot = s.indexOf(46);
         if (dot >= 0) {
            decimals = Math.min(s.substring(dot + 1).replaceAll("0+$", "").length(), 4);
         }
      }

      String formatted = String.format("%." + decimals + "f", number).replace(",", ".");
      if (formatted.contains(".")) {
         formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
      }

      return formatted;
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (!this.numberField.isFocused()) {
         if (button == MouseButton.RIGHT && this.isHovered(mouseX, mouseY)) {
            SliderSetting s = this.setting;
            String pre = this.formatSliderValue(s.getCurrentValue(), s.getStep());
            this.numberField.clear();
            this.numberField.paste(pre);
            this.numberField.setFocused(true);
            super.onMouseClicked(mouseX, mouseY, button);
         } else {
            if (button == MouseButton.LEFT && this.isHovered(mouseX, mouseY)) {
               this.drag = true;
               current = this;
            }

            super.onMouseClicked(mouseX, mouseY, button);
         }
      } else {
         boolean inside = mouseX >= this.fieldX && mouseX <= this.fieldX + this.fieldW && mouseY >= this.fieldY && mouseY <= this.fieldY + this.fieldH;
         if (inside) {
            this.numberField.onMouseClicked(mouseX, mouseY, button);
         } else {
            this.numberField.setFocused(false);
            this.numberField.clear();
         }

         super.onMouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.drag = false;
      if (this.numberField.isFocused()) {
         this.numberField.onMouseReleased(mouseX, mouseY, button);
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.numberField.isFocused()) {
         if (keyCode == 257 || keyCode == 335) {
            this.applyNumberFieldValue();
         } else if (keyCode == 256) {
            this.numberField.setFocused(false);
            this.numberField.clear();
         } else {
            this.numberField.onKeyPressed(keyCode, scanCode, modifiers);
         }
      } else {
         if ((keyCode == 262 || keyCode == 263) && current == this) {
            SliderSetting s = current.getSetting();
            s.setCurrentValue(s.getCurrentValue() + s.getStep() * 0.7F * (keyCode == 262 ? 1 : -1));
         }
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (!this.numberField.isFocused()) {
         return false;
      } else if (chr >= '0' && chr <= '9') {
         return this.numberField.charTyped(chr, modifiers);
      } else if (chr == '.' || chr == ',') {
         return this.numberField.getBuiltText().indexOf(46) < 0 && this.numberField.getBuiltText().indexOf(44) < 0
            ? this.numberField.charTyped('.', modifiers)
            : false;
      } else {
         return chr == '-' && this.numberField.getBuiltText().isEmpty() ? this.numberField.charTyped(chr, modifiers) : false;
      }
   }

   private void applyNumberFieldValue() {
      String raw = this.numberField.getBuiltText().replace(',', '.').trim();
      this.numberField.setFocused(false);
      this.numberField.clear();
      if (!raw.isEmpty() && !raw.equals("-") && !raw.equals(".") && !raw.equals("-.")) {
         float parsed;
         try {
            parsed = Float.parseFloat(raw);
         } catch (NumberFormatException nfe) {
            return;
         }

         if (!Float.isNaN(parsed) && !Float.isInfinite(parsed)) {
            SliderSetting s = this.setting;
            float min = s.getMin();
            float max = s.getMax();
            float step = s.getStep();
            float clamped = Math.max(min, Math.min(max, parsed));
            if (step > 0.0F) {
               float steps = Math.round((clamped - min) / step);
               clamped = min + steps * step;
               if (clamped > max) {
                  clamped = max;
               }

               if (clamped < min) {
                  clamped = min;
               }
            }

            s.setCurrentValue(clamped);
         }
      }
   }

   @Override
   public float getHeight() {
      this.height = 29.0F;
      return 29.0F;
   }
}
