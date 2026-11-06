package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.audio.SoundEngine;
import dev.bkclient.ui.clicksys.port.data.AnimatedInt;
import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.ui.clicksys.port.enums.SoundEvent;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;
import org.joml.Matrix4f;

public class SmallButton extends LabelWidget {
   private boolean flag4;
   private Runnable runnable;
   private final Tween tween4 = EasingPresets.getTweenByFloat(0.0F);
   private final Tween tween5 = EasingPresets.getTweenByFloatFloat2(0.0F, 0.22F);

   public SmallButton(boolean flag) {
      this.flag4 = flag;
      this.value237 = 16.0F;
      this.value238 = 16.0F;
      this.tween4.setFloat(flag ? 1.0F : 0.0F);
      this.tween5.setFloat(flag ? 1.0F : 0.0F);
   }

   @Override
   public void onFloatFloatFloatMatrix4f(float value, float value2, float value3, Matrix4f matrix4f) {
      float progress = Math.clamp(this.tween5.getFloat(), 0, 1);
      int accent = Theme.foreground();
      int fill = AnimatedInt.getIntByIntFloatInt(accent, progress, Theme.surface());
      // Unchecked box: the idle border is dimmed towards the background inside the menu and the fill matches
      // the row, so a 1 px outline was nearly invisible. Use a clearer outline (surface mixed towards the
      // muted text colour) and a slightly thicker stroke; the checked look is unchanged.
      int idleBorder = ColorRGBA.fromInt(Theme.surface()).mix(ColorRGBA.fromInt(Theme.mutedFg()), 0.55F).getRGB();
      int border = AnimatedInt.getIntByIntFloatInt(accent, progress, idleBorder);
      ShapeShader.onFloatFloatFloatMatrix4fFloatIntFloatFloatIntFloat(
         1.5F, 16, value, matrix4f, value236, fill, 5, value235, border, 16);
      float check = Math.clamp(this.tween4.getFloat(), 0, 1);
      if (check > .001F) {
         float luminance = .2126F*((accent>>16)&255) + .7152F*((accent>>8)&255) + .0722F*(accent&255);
         int ink = luminance > 170 ? 0xff182028 : 0xffffffff;
         LucideIcons.CHECK.draw(matrix4f, value235 + 2, value236 + 2, 12,
            ColorRGBA.fromInt(ink).mulAlpha(value * check));
      }
   }

   public void setRunnable(Runnable runnable2) {
      this.runnable = runnable2;
   }

   public boolean isFlag4() {
      return this.flag4;
   }

   @Override
   protected boolean isDoubleDouble2(double value, double value2) {
      this.setBoolean(!this.flag4);
      SoundEngine.getInstance().onSoundEvent(this.flag4 ? SoundEvent.TOGGLE_ON : SoundEvent.TOGGLE_OFF);
      if (this.runnable != null) {
         this.runnable.run();
      }

      return true;
   }

   public void setBoolean(boolean flag) {
      if (this.flag4 != flag) {
         this.flag4 = flag;
         this.tween4.setFloat2(flag ? 1.0F : 0.0F);
         this.tween5.setFloat2(flag ? 1.0F : 0.0F);
      }
   }
}
