package dev.bkclient.utility.render.ambience;

import dev.bkclient.systems.modules.modules.visuals.Sky;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import java.util.function.BooleanSupplier;

/** Independent palette and controls; switching skies preserves every mode's settings. */
public final class CyberspaceSkySettings {
    public final ColorSetting starColor, horizonColor;
    public final SliderSetting aurora, starAmount, glow, flow;
    public final BooleanSetting stars;
    public final BooleanSetting streaks;
    public final SliderSetting streakCount, streakSpeed, streakLength, streakSize;
    public final ModeSetting streakShape;
    public final ModeSetting.Value squareHead, roundHead;

    public CyberspaceSkySettings(Sky owner) {
        BooleanSupplier hidden = () -> !"Cyberspace".equals(owner.getSkyEffectMode());
        String key = "modules.settings.sky.cyberspace.";
        starColor = new ColorSetting(owner, key + "star_color", hidden)
            .color(ColorRGBA.fromInt(0xffd9c7ff)).alpha(false);
        horizonColor = new ColorSetting(owner, key + "horizon_color", hidden)
            .color(ColorRGBA.fromInt(0xffbc30ff)).alpha(false);
        aurora = new SliderSetting(owner, key + "aurora", hidden).min(0).max(150).step(5).currentValue(100).suffix("%");
        glow = new SliderSetting(owner, key + "glow", hidden).min(0).max(150).step(5).currentValue(65).suffix("%");
        flow = new SliderSetting(owner, key + "flow", hidden).min(0).max(200).step(5).currentValue(25).suffix("%");
        stars = new BooleanSetting(owner, key + "stars", hidden).enable();
        starAmount = new SliderSetting(owner, key + "star_amount", () -> hidden.getAsBoolean() || !stars.isEnabled()).min(25).max(200).step(5).currentValue(100).suffix("%");
        streaks = new BooleanSetting(owner, key + "streaks", hidden).enable();
        BooleanSupplier streakHidden = () -> hidden.getAsBoolean() || !streaks.isEnabled();
        streakShape = new ModeSetting(owner, key + "streak_shape", streakHidden);
        squareHead = new ModeSetting.Value(streakShape, key + "streak_shape.square").select();
        roundHead = new ModeSetting.Value(streakShape, key + "streak_shape.round");
        streakCount = new SliderSetting(owner, key + "streak_count", streakHidden).min(4).max(32).step(2).currentValue(24);
        streakSpeed = new SliderSetting(owner, key + "streak_speed", streakHidden).min(10).max(200).step(5).currentValue(80).suffix("%");
        streakLength = new SliderSetting(owner, key + "streak_length", streakHidden).min(10).max(100).step(5).currentValue(60).suffix("%");
        streakSize = new SliderSetting(owner, key + "streak_size", streakHidden).min(50).max(200).step(5).currentValue(100).suffix("%");
    }
}
