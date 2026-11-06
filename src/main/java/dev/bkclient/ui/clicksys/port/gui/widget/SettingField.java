package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.ListEntry;
import dev.bkclient.ui.clicksys.port.setting.Setting;
import dev.bkclient.ui.clicksys.port.util.Animation;
import dev.bkclient.ui.clicksys.port.util.Easings;
import org.joml.Matrix4f;

public abstract class SettingField implements ListEntry {
   public final Setting setting;
   public final Animation animation;

   SettingField(Setting setting2) {
      this.setting = setting2;
      this.animation = new Animation(4.5F).getAnimationByFunction(Easings::getFloatByFloat7).getAnimation();
   }

   public abstract void onFloatFloatFloatFloatFloatMatrix4fFloat(float value, float value2, float value3, float value4, float value5, Matrix4f matrix4f, float value6);

   public abstract boolean isDoubleDoubleIntFloatFloatFloat(double value, double value2, int count, float value3, float value4, float value5);

   @Override
   public float itemHeight() {
      return 26.0F;
   }

   @Override
   public Animation animation() {
      return this.animation;
   }
}
