package dev.bkclient.utility.render.ambience;

import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;

/** Persisted by Ambience. Off preserves the appearance of existing profiles. */
public final class WorldSaturation {
    public final ModeSetting mode;
    public final ModeSetting.Value off, global, radial;
    public final SliderSetting amount, near, far, radius, transition;
    public final ModeSetting shape;
    public final ModeSetting.Value cylinder, sphere;
    public final BooleanSetting sky;

    public WorldSaturation(BaseModule owner) {
        this(owner, () -> false);
    }

    public WorldSaturation(BaseModule owner, java.util.function.BooleanSupplier sectionHidden) {
        String key = "modules.settings.ambience.saturation";
        mode = new ModeSetting(owner, key, sectionHidden);
        off = new ModeSetting.Value(mode, key + ".off").select();
        global = new ModeSetting.Value(mode, key + ".global");
        radial = new ModeSetting.Value(mode, key + ".radial");
        amount = new SliderSetting(owner, key + ".amount", () -> sectionHidden.getAsBoolean() || !global.isSelected())
            .min(0).max(200).step(5).currentValue(100).suffix("%");
        near = new SliderSetting(owner, key + ".near", () -> sectionHidden.getAsBoolean() || !radial.isSelected())
            .min(0).max(200).step(5).currentValue(100).suffix("%");
        far = new SliderSetting(owner, key + ".far", () -> sectionHidden.getAsBoolean() || !radial.isSelected())
            .min(0).max(200).step(5).currentValue(0).suffix("%");
        radius = new SliderSetting(owner, key + ".radius", () -> sectionHidden.getAsBoolean() || !radial.isSelected())
            .min(1).max(128).step(.5f).currentValue(12).suffix("m");
        transition = new SliderSetting(owner, key + ".transition", () -> sectionHidden.getAsBoolean() || !radial.isSelected())
            .min(.5f).max(64).step(.5f).currentValue(6).suffix("m");
        shape = new ModeSetting(owner, key + ".shape", () -> sectionHidden.getAsBoolean() || !radial.isSelected());
        cylinder = new ModeSetting.Value(shape, key + ".cylinder").select();
        sphere = new ModeSetting.Value(shape, key + ".sphere");
        sky = new BooleanSetting(owner, key + ".sky", () -> sectionHidden.getAsBoolean() || off.isSelected()).enable();
    }

    public boolean active() { return !off.isSelected(); }
}
