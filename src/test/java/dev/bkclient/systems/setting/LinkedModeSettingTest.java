package dev.bkclient.systems.setting;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.LinkedModeSetting;
import java.util.ArrayList;
import java.util.List;

public final class LinkedModeSettingTest {
    public static void main(String[] args) {
        List<Setting> settings = new ArrayList<>();
        SettingsContainer parent = () -> settings;
        boolean[] inactive = {false};
        BooleanSetting sync = new BooleanSetting(parent, "legacySync", () -> inactive[0]).enable();
        LinkedModeSetting mode = LinkedModeSetting.toggle(parent, "source", sync, "Theme", "Custom");
        check(sync.getHideCondition().getAsBoolean(), "legacy switch hidden");
        check(!mode.getHideCondition().getAsBoolean(), "selector visible");
        inactive[0] = true;
        check(mode.getHideCondition().getAsBoolean(), "parent condition preserved");
        inactive[0] = false;
        sync.load(new JsonPrimitive(false));
        check(mode.getValue().getName().equals("Custom"), "old config without selector");
        mode.getValues().get(0).select();
        check(sync.isEnabled() && mode.is(mode.getValues().get(0)), "UI selection updates renderer input");
        JsonObject saved = new JsonObject();
        for (Setting setting : settings) saved.add(setting.getName(), setting.save());
        sync.setEnabled(false);
        for (Setting setting : settings) setting.load(saved.get(setting.getName()));
        check(sync.isEnabled(), "config round trip");
        mode.load(new JsonPrimitive("unknown"));
        check(sync.isEnabled(), "unknown value ignored");

        List<Setting> combined = new ArrayList<>();
        BooleanSetting theme = new BooleanSetting(() -> combined, "theme").enable();
        BooleanSetting rainbow = new BooleanSetting(() -> combined, "rainbow").enable();
        LinkedModeSetting source = new LinkedModeSetting(() -> combined, "source", () -> false, theme, rainbow)
            .choice("Theme", () -> theme.isEnabled() && !rainbow.isEnabled(), () -> { theme.setEnabled(true); rainbow.setEnabled(false); })
            .choice("Custom", () -> !theme.isEnabled() && !rainbow.isEnabled(), () -> { theme.setEnabled(false); rainbow.setEnabled(false); })
            .choice("Rainbow", rainbow::isEnabled, () -> { theme.setEnabled(false); rainbow.setEnabled(true); });
        check(source.getValue().getName().equals("Rainbow"), "legacy rainbow precedence");
        source.load(source.save());
        check(theme.isEnabled(), "unchanged choice preserves dormant state");
        source.getValues().get(0).select();
        check(theme.isEnabled() && !rainbow.isEnabled(), "theme excludes rainbow");
        source.getValues().get(2).select();
        check(!theme.isEnabled() && rainbow.isEnabled(), "rainbow excludes theme");
        check(combined.indexOf(source) > combined.indexOf(rainbow), "selector loads after old keys");
        System.out.println("Color selectors: legacy config, round trip, visibility and exclusive choices passed");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
