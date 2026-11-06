package dev.bkclient.utility.render.ambience;

import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.colors.ColorRGBA;

/** Atmosphere controls owned and persisted by the existing Ambience module. */
public final class AmbienceEffects {
    public final WorldSaturation saturation;
    public final BooleanSetting colorIsolation;
    public final ColorSetting colorIsolationColor;
    public final ModeSetting colorIsolationMode;
    public final ModeSetting.Value colorIsolationModeStrict;
    public final ModeSetting.Value colorIsolationModeBalanced;
    public final ModeSetting.Value colorIsolationModeLoose;
    public final ModeSetting.Value colorIsolationModeCustom;
    public final SliderSetting colorIsolationSensitivity;
    public final SliderSetting colorIsolationBackground;
    public final BooleanSetting fog;
    public final ModeSetting fogColorMode;
    public final ModeSetting.Value fogColorModeBiome;
    public final ModeSetting.Value fogColorModeTheme;
    public final ModeSetting.Value fogColorModeCustom;
    public final ColorSetting fogColor;
    public final SliderSetting fogDensity;
    public final SliderSetting fogCoverage;
    public final SliderSetting fogLevel;
    public final SliderSetting fogThickness;
    public final ModeSetting fogQuality;
    public final ModeSetting.Value fogQualityLow;
    public final ModeSetting.Value fogQualityHigh;
    public final BooleanSetting wetWorld;
    public final SliderSetting wetWorldReflection;
    public final SliderSetting wetWorldAmount;
    public final ModeSetting wetWorldQuality;
    public final ModeSetting.Value wetWorldQualityLow;
    public final ModeSetting.Value wetWorldQualityMedium;
    public final ModeSetting.Value wetWorldQualityHigh;
    public final BooleanSetting wetWorldRipples;
    public final BooleanSetting wetWorldStepRipples;
    public final SliderSetting wetWorldRippleSize;
    public final SliderSetting wetWorldRippleStrength;
    public final BooleanSetting rain;
    public final SliderSetting rainDensity;
    public final SliderSetting rainOpacity;
    public final SliderSetting rainDrops;
    public final SliderSetting rainSplashes;

    public AmbienceEffects(BaseModule owner) {
        this(owner, () -> false, () -> false);
    }

    public AmbienceEffects(BaseModule owner, java.util.function.BooleanSupplier colorsHidden, java.util.function.BooleanSupplier weatherHidden) {
        this.saturation = new WorldSaturation(owner, colorsHidden);
        this.colorIsolation = new BooleanSetting(owner, "modules.settings.ambience.color_isolation", colorsHidden);
        this.colorIsolationColor = new ColorSetting(owner, "modules.settings.ambience.color_isolation.color", () -> colorsHidden.getAsBoolean() || !this.colorIsolation.isEnabled()).color(new ColorRGBA(0.0f, 122.0f, 255.0f, 255.0f)).alpha(false);
        this.colorIsolationMode = new ModeSetting(owner, "modules.settings.ambience.color_isolation.mode", () -> colorsHidden.getAsBoolean() || !this.colorIsolation.isEnabled());
        this.colorIsolationModeStrict = new ModeSetting.Value(this.colorIsolationMode, "modules.settings.ambience.color_isolation.mode.strict");
        this.colorIsolationModeBalanced = new ModeSetting.Value(this.colorIsolationMode, "modules.settings.ambience.color_isolation.mode.balanced").select();
        this.colorIsolationModeLoose = new ModeSetting.Value(this.colorIsolationMode, "modules.settings.ambience.color_isolation.mode.loose");
        this.colorIsolationModeCustom = new ModeSetting.Value(this.colorIsolationMode, "modules.settings.ambience.color_isolation.mode.custom");
        this.colorIsolationSensitivity = new SliderSetting(owner, "modules.settings.ambience.color_isolation.sensitivity", () -> colorsHidden.getAsBoolean() || !this.colorIsolation.isEnabled() || !this.colorIsolationModeCustom.isSelected()).min(0.0f).max(100.0f).step(1.0f).currentValue(50.0f);
        this.colorIsolationBackground = new SliderSetting(owner, "modules.settings.ambience.color_isolation.background", () -> colorsHidden.getAsBoolean() || !this.colorIsolation.isEnabled()).min(0.0f).max(100.0f).step(1.0f).currentValue(0.0f);
        this.fog = new BooleanSetting(owner, "modules.settings.ambience.fog", weatherHidden);
        this.fogColorMode = new ModeSetting(owner, "modules.settings.ambience.fog.color_mode", () -> weatherHidden.getAsBoolean() || !this.fog.isEnabled());
        this.fogColorModeBiome = new ModeSetting.Value(this.fogColorMode, "modules.settings.ambience.fog.color_mode.biome").select();
        this.fogColorModeTheme = new ModeSetting.Value(this.fogColorMode, "modules.settings.ambience.fog.color_mode.theme");
        this.fogColorModeCustom = new ModeSetting.Value(this.fogColorMode, "modules.settings.ambience.fog.color_mode.custom");
        this.fogColor = new ColorSetting(owner, "modules.settings.ambience.fog.color", () -> weatherHidden.getAsBoolean() || !this.fog.isEnabled() || !this.fogColorModeCustom.isSelected()).color(new ColorRGBA(205.0f, 210.0f, 220.0f, 255.0f)).alpha(false);
        this.fogDensity = new SliderSetting(owner, "modules.settings.ambience.fog.density", () -> weatherHidden.getAsBoolean() || !this.fog.isEnabled()).min(10.0f).max(250.0f).step(5.0f).currentValue(90.0f).suffix("%");
        this.fogCoverage = new SliderSetting(owner, "modules.settings.ambience.fog.coverage", () -> weatherHidden.getAsBoolean() || !this.fog.isEnabled()).min(0.0f).max(100.0f).step(1.0f).currentValue(55.0f).suffix("%");
        this.fogLevel = new SliderSetting(owner, "modules.settings.ambience.fog.level", () -> weatherHidden.getAsBoolean() || !this.fog.isEnabled()).min(-64.0f).max(320.0f).step(1.0f).currentValue(66.0f);
        this.fogThickness = new SliderSetting(owner, "modules.settings.ambience.fog.thickness", () -> weatherHidden.getAsBoolean() || !this.fog.isEnabled()).min(2.0f).max(48.0f).step(1.0f).currentValue(6.0f);
        this.fogQuality = new ModeSetting(owner, "modules.settings.ambience.fog.quality", () -> weatherHidden.getAsBoolean() || !this.fog.isEnabled());
        this.fogQualityLow = new ModeSetting.Value(this.fogQuality, "modules.settings.ambience.fog.quality.low");
        this.fogQualityHigh = new ModeSetting.Value(this.fogQuality, "modules.settings.ambience.fog.quality.high").select();
        this.wetWorld = new BooleanSetting(owner, "modules.settings.ambience.wet_world", weatherHidden);
        this.wetWorldReflection = new SliderSetting(owner, "modules.settings.ambience.wet_world.reflection", () -> weatherHidden.getAsBoolean() || !this.wetWorld.isEnabled()).min(0.0f).max(100.0f).step(5.0f).currentValue(70.0f).suffix("%");
        this.wetWorldAmount = new SliderSetting(owner, "modules.settings.ambience.wet_world.amount", () -> weatherHidden.getAsBoolean() || !this.wetWorld.isEnabled()).min(0.0f).max(100.0f).step(5.0f).currentValue(60.0f).suffix("%");
        this.wetWorldQuality = new ModeSetting(owner, "modules.settings.ambience.wet_world.quality", () -> weatherHidden.getAsBoolean() || !this.wetWorld.isEnabled());
        this.wetWorldQualityLow = new ModeSetting.Value(this.wetWorldQuality, "modules.settings.ambience.wet_world.quality.low");
        this.wetWorldQualityMedium = new ModeSetting.Value(this.wetWorldQuality, "modules.settings.ambience.wet_world.quality.medium").select();
        this.wetWorldQualityHigh = new ModeSetting.Value(this.wetWorldQuality, "modules.settings.ambience.wet_world.quality.high");
        this.wetWorldRipples = new BooleanSetting(owner, "modules.settings.ambience.wet_world.ripples", () -> weatherHidden.getAsBoolean() || !this.wetWorld.isEnabled()).enable();
        this.wetWorldStepRipples = new BooleanSetting(owner, "modules.settings.ambience.wet_world.ripples.steps", () -> weatherHidden.getAsBoolean() || !this.wetWorld.isEnabled()).enable();
        this.wetWorldRippleSize = new SliderSetting(owner, "modules.settings.ambience.wet_world.ripples.size", () -> weatherHidden.getAsBoolean() || !this.wetWorld.isEnabled() || (!this.wetWorldRipples.isEnabled() && !this.wetWorldStepRipples.isEnabled())).min(1.0f).max(6.0f).step(0.5f).currentValue(3.0f).suffix("m");
        this.wetWorldRippleStrength = new SliderSetting(owner, "modules.settings.ambience.wet_world.ripples.strength", () -> weatherHidden.getAsBoolean() || !this.wetWorld.isEnabled() || (!this.wetWorldRipples.isEnabled() && !this.wetWorldStepRipples.isEnabled())).min(10.0f).max(200.0f).step(10.0f).currentValue(100.0f).suffix("%");
        this.rain = new BooleanSetting(owner, "modules.settings.ambience.rain", weatherHidden);
        this.rainDensity = new SliderSetting(owner, "modules.settings.ambience.rain.density", () -> weatherHidden.getAsBoolean() || !this.rain.isEnabled()).min(10.0f).max(200.0f).step(5.0f).currentValue(100.0f).suffix("%");
        this.rainOpacity = new SliderSetting(owner, "modules.settings.ambience.rain.opacity", () -> weatherHidden.getAsBoolean() || !this.rain.isEnabled()).min(5.0f).max(100.0f).step(5.0f).currentValue(45.0f).suffix("%");
        this.rainDrops = new SliderSetting(owner, "modules.settings.ambience.rain.drops", () -> weatherHidden.getAsBoolean() || !this.rain.isEnabled()).min(0.0f).max(100.0f).step(5.0f).currentValue(60.0f).suffix("%");
        this.rainSplashes = new SliderSetting(owner, "modules.settings.ambience.rain.splashes", () -> weatherHidden.getAsBoolean() || !this.rain.isEnabled()).min(0.0f).max(100.0f).step(5.0f).currentValue(60.0f).suffix("%");
    }
}
