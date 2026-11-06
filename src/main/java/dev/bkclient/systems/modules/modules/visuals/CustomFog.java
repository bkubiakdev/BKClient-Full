package dev.bkclient.systems.modules.modules.visuals;

import lombok.Generated;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.LinkedModeSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.RangeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

@ModuleInfo(name = "Custom Fog", category = ModuleCategory.VISUALS)
public class CustomFog extends BaseModule {

    public CustomFog() {
        LinkedModeSetting.toggle(this, "color.source", this.themeSync, "Theme", "Custom");
    }

   private final RangeSetting distance = new RangeSetting(this, "modules.settings.custom_fog.distance") {
      @Override
      public void setSecondValue(float value) {
         // Keep the far boundary at least 40 blocks away, as in the source effect.
         super.setSecondValue(Math.max(40.0F, value));
      }
   }
      .min(1.0F)
      .max(100.0F)
      .step(1.0F)
      .firstValue(1.0F)
      .secondValue(100.0F);
   private final BooleanSetting fogColorEnabled = new BooleanSetting(this, "modules.settings.custom_fog.color_enabled").enabled(true);
   private final BooleanSetting themeSync = new BooleanSetting(this, "theme.sync", () -> !this.fogColorEnabled.isEnabled()).enabled(true);
   private final SliderSetting syncAlpha = new SliderSetting(this, "modules.settings.custom_fog.sync_alpha",
      () -> !this.fogColorEnabled.isEnabled() || !this.themeSync.isEnabled())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(46.0F).suffix("%");
   private final ColorSetting fogColor = new ColorSetting(
         this, "modules.settings.custom_fog.color", () -> !this.fogColorEnabled.isEnabled() || this.themeSync.isEnabled()
      )
      .color(Colors.getAccent().withAlpha(118.0F))
      .alpha(true);
   private final BooleanSetting depthBlur = new BooleanSetting(this, "modules.settings.custom_fog.depth_blur");
   private final SliderSetting blurStrength = new SliderSetting(this, "modules.settings.custom_fog.blur_strength", () -> !this.depthBlur.isEnabled())
      .min(0.1F)
      .max(10.0F)
      .step(0.05F)
      .currentValue(1.0F);
   private final SliderSetting blurOffset = new SliderSetting(this, "modules.settings.custom_fog.blur_offset", () -> !this.depthBlur.isEnabled())
      .min(0.05F)
      .max(12.0F)
      .step(0.05F)
      .currentValue(1.0F);
   private final BooleanSetting noSkyBlur = new BooleanSetting(this, "modules.settings.custom_fog.no_sky_blur", () -> !this.depthBlur.isEnabled());

   public boolean shouldModifyFog(Camera camera) {
      if (this.isEnabled() && camera != null && mc.world != null && mc.player != null) {
         Entity entity = camera.getFocusedEntity();
         if (camera.getSubmersionType() == CameraSubmersionType.WATER) {
            return false;
         }

         if (camera.getSubmersionType() == CameraSubmersionType.LAVA) {
            return false;
         }

         if (camera.getSubmersionType() == CameraSubmersionType.POWDER_SNOW) {
            return false;
         }

         if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity.hasStatusEffect(StatusEffects.BLINDNESS)) {
               return false;
            }

            if (livingEntity.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean shouldApplyDepthBlur(Camera camera) {
      return this.depthBlur.isEnabled() && this.shouldModifyFog(camera);
   }

   @Generated
   public RangeSetting getDistance() {
      return this.distance;
   }

   public ColorRGBA getEffectiveFogColor() {
      return this.themeSync.isEnabled()
         ? Colors.getAccent().withAlpha(this.syncAlpha.getCurrentValue() / 100.0F * 255.0F)
         : this.fogColor.getColor();
   }

   @Generated
   public BooleanSetting getFogColorEnabled() {
      return this.fogColorEnabled;
   }

   @Generated
   public BooleanSetting getThemeSync() {
      return this.themeSync;
   }

   @Generated
   public ColorSetting getFogColor() {
      return this.fogColor;
   }

   @Generated
   public BooleanSetting getDepthBlur() {
      return this.depthBlur;
   }

   @Generated
   public SliderSetting getBlurStrength() {
      return this.blurStrength;
   }

   @Generated
   public SliderSetting getBlurOffset() {
      return this.blurOffset;
   }

   @Generated
   public BooleanSetting getNoSkyBlur() {
      return this.noSkyBlur;
   }
}
