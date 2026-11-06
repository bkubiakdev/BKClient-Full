package dev.bkclient.utility.gui;

import lombok.Generated;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.BezierSetting;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.NumberSetting;
import dev.bkclient.systems.setting.settings.RangeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.BezierSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.BindSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.BooleanSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.ButtonSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.ColorSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.ModeSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.NumberSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.RangeSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.SelectSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.SliderSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.StringSettingComponent;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.obj.Rect;
import net.minecraft.client.util.math.Vector2f;

public final class GuiUtility {
   public static float getMiddleOfBox(float objectHeight, float boxHeight) {
      return (float)Math.ceil(boxHeight / 2.0F - objectHeight / 2.0F);
   }

   public static double getMiddleOfBox(double objectHeight, double boxHeight) {
      return Math.ceil(boxHeight / 2.0 - objectHeight / 2.0);
   }

   public static boolean isHovered(double x, double y, double width, double height, int mouseX, int mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
   }

   public static boolean isHovered(double x, double y, double width, double height, UIContext context) {
      return isHovered(x, y, width, height, context.getMouseX(), context.getMouseY());
   }

   public static boolean isHovered(Rect rect, double mouseX, double mouseY) {
      return isHovered(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), mouseX, mouseY);
   }

   public static boolean isHovered(CustomComponent rect, double mouseX, double mouseY) {
      return isHovered(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), mouseX, mouseY);
   }

   public static boolean isHovered(double x, double y, double width, double height, double mouseX, double mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
   }

   public static float getSliderValue(float min, float max, float start, float size, double mouse) {
      // size == 0 on the exact frame the mouse sits at `start` divides 0/0 into NaN, which
      // Math.min/max never clamp away (Math.max(0, NaN) is NaN). That NaN then gets written
      // straight into the setting (e.g. HUD scale), permanently poisoning any Animation that
      // reads it. Fall back to the lowest value instead of dividing by zero.
      if (size == 0.0F) {
         return min;
      }
      return (float)(Math.min(1.0, Math.max(0.0, (mouse - start) / size)) * (max - min)) + min;
   }

   public static float getSliderValueWithoutClamp(float min, float max, float start, float size, double mouse) {
      return (float)((mouse - start) / size * (max - min)) + min;
   }

   public static float getPercent(float value, float min, float max) {
      return (value - min) / (max - min);
   }

   public static Vector2f getMouse() {
      return new Vector2f(
         (float)(IMinecraft.mc.mouse.getX() / IScaledResolution.sr.getScaleFactor()),
         (float)(IMinecraft.mc.mouse.getY() / IScaledResolution.sr.getScaleFactor())
      );
   }

   public static MenuSettingComponent settinge(Setting setting, CustomComponent parent) {
      MenuSettingComponent settingComponent = null;
      if (setting instanceof BooleanSetting s) {
         settingComponent = new BooleanSettingComponent(s, parent);
      } else if (setting instanceof BindSetting s) {
         settingComponent = new BindSettingComponent(s, parent);
      } else if (setting instanceof ColorSetting s) {
         settingComponent = new ColorSettingComponent(s, parent);
      } else if (setting instanceof ModeSetting s) {
         settingComponent = new ModeSettingComponent(s, parent);
      } else if (setting instanceof NumberSetting s) {
         settingComponent = new NumberSettingComponent(s, parent);
      } else if (setting instanceof RangeSetting s) {
         settingComponent = new RangeSettingComponent(s, parent);
      } else if (setting instanceof BezierSetting s) {
         settingComponent = new BezierSettingComponent(s, parent);
      } else if (setting instanceof ButtonSetting s) {
         settingComponent = new ButtonSettingComponent(s, parent);
      } else if (setting instanceof SelectSetting s) {
         settingComponent = new SelectSettingComponent(s, parent);
      } else if (setting instanceof SliderSetting s) {
         settingComponent = new SliderSettingComponent(s, parent);
      } else if (setting instanceof StringSetting s) {
         settingComponent = new StringSettingComponent(s, parent);
      }

      if (settingComponent != null) {
         settingComponent.onInit();
      }

      return settingComponent;
   }

   @Generated
   private GuiUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
