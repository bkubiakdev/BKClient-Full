package dev.bkclient.systems.modules.constructions.swinganim;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class SwingPhase extends SwingSettings {
   private final SliderSetting anchorX = new SwingPhase.PhaseSlider(this, "swing.anchorX").step(0.05F).min(-5.0F).max(5.0F).currentValue(0.0F);
   private final SliderSetting anchorY = new SwingPhase.PhaseSlider(this, "swing.anchorY").step(0.05F).min(-5.0F).max(5.0F).currentValue(0.0F);
   private final SliderSetting anchorZ = new SwingPhase.PhaseSlider(this, "swing.anchorZ").step(0.05F).min(-5.0F).max(5.0F).currentValue(0.0F);
   private final SliderSetting moveX = new SwingPhase.PhaseSlider(this, "swing.moveX").step(0.05F).min(-5.0F).max(5.0F).currentValue(0.0F);
   private final SliderSetting moveY = new SwingPhase.PhaseSlider(this, "swing.moveY").step(0.05F).min(-5.0F).max(5.0F).currentValue(0.0F);
   private final SliderSetting moveZ = new SwingPhase.PhaseSlider(this, "swing.moveZ").step(0.05F).min(-3.0F).max(3.0F).currentValue(0.0F);
   private final SliderSetting rotateX = new SwingPhase.PhaseSlider(this, "swing.rotateX").step(15.0F).min(-360.0F).max(360.0F).currentValue(0.0F);
   private final SliderSetting rotateY = new SwingPhase.PhaseSlider(this, "swing.rotateY").step(15.0F).min(-360.0F).max(360.0F).currentValue(0.0F);
   private final SliderSetting rotateZ = new SwingPhase.PhaseSlider(this, "swing.rotateZ").step(15.0F).min(-360.0F).max(360.0F).currentValue(0.0F);

   public SliderSetting[] sliders() {
      return new SliderSetting[]{anchorX, anchorY, anchorZ, moveX, moveY, moveZ, rotateX, rotateY, rotateZ};
   }

   public float[] values() {
      SliderSetting[] sliders = sliders();
      float[] values = new float[sliders.length];
      for (int i = 0; i < values.length; i++) values[i] = sliders[i].getCurrentValue();
      return values;
   }

   /** Programmatic restoration must not link phases when Shift is held. */
   public void restore(float[] values) {
      if (values.length != 9) throw new IllegalArgumentException("A swing phase needs nine values");
      SliderSetting[] sliders = sliders();
      for (int i = 0; i < values.length; i++) ((PhaseSlider)sliders[i]).silentSet(values[i]);
   }

   @Generated
   public SliderSetting getAnchorX() {
      return this.anchorX;
   }

   @Generated
   public SliderSetting getAnchorY() {
      return this.anchorY;
   }

   @Generated
   public SliderSetting getAnchorZ() {
      return this.anchorZ;
   }

   @Generated
   public SliderSetting getMoveX() {
      return this.moveX;
   }

   @Generated
   public SliderSetting getMoveY() {
      return this.moveY;
   }

   @Generated
   public SliderSetting getMoveZ() {
      return this.moveZ;
   }

   @Generated
   public SliderSetting getRotateX() {
      return this.rotateX;
   }

   @Generated
   public SliderSetting getRotateY() {
      return this.rotateY;
   }

   @Generated
   public SliderSetting getRotateZ() {
      return this.rotateZ;
   }

   public static class PhaseSlider extends SliderSetting {
      public PhaseSlider(@NotNull SettingsContainer parent, String name) {
         super(parent, name);
      }

      @Override
      public void setCurrentValue(float currentValue) {
         super.setCurrentValue(currentValue);
         // Construction/config loading must never query screen input. Lunar's
         // keyboard hooks are not initialized while Minecraft is being constructed.
         if (BKClientInit.getInstance().getSwingManager() != null
            && net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof SwingAnimScreen
            && SwingAnimScreen.hasShiftDown()) {
            for (Setting setting : BKClientInit.getInstance().getSwingManager().getStartPhase().getSettings()) {
               if (setting.getName().equals(this.getName()) && setting instanceof SwingPhase.PhaseSlider slider) {
                  slider.silentSet(currentValue);
               }
            }

            for (Setting settingx : BKClientInit.getInstance().getSwingManager().getEndPhase().getSettings()) {
               if (settingx.getName().equals(this.getName()) && settingx instanceof SwingPhase.PhaseSlider slider) {
                  slider.silentSet(currentValue);
               }
            }
         }
      }

      private void silentSet(float value) {
         this.currentValue = MathHelper.clamp((float)(Math.round(value * (1.0 / this.step)) / (1.0 / this.step)), this.min, this.max);
      }
   }
}
