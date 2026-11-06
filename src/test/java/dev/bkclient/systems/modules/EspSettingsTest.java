package dev.bkclient.systems.modules;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.bkclient.systems.modules.modules.visuals.ESP;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import java.util.HashSet;

/** Exercise both ESP styles and save/load through the same settings the GUI uses. */
public final class EspSettingsTest {
    public static void main(String[] args) {
        ESP esp = new ESP();
        HashSet<String> names = new HashSet<>();
        for (Setting setting : esp.getSettings()) {
            check(names.add(setting.getName()), "Duplicate config key: " + setting.getName());
        }
        ModeSetting box = (ModeSetting) setting(esp, "esp.box.mode");
        check(box.getValues().size() == 1, "Only Glow box mode remains");
        for (String oldMode : new String[]{"esp.box.mode.2d", "esp.box.mode.corner", "esp.box.mode.3d"}) {
            box.load(new JsonPrimitive(oldMode));
            check(box.getValue() == box.getValues().getFirst(), "Legacy box mode must fall back to Glow");
        }

        ModeSetting style = (ModeSetting) setting(esp, "esp.equipment.style");
        ModeSetting hpSource = (ModeSetting) setting(esp, "HP source");
        check(hpSource.getValue().getName().equals("Default"), "Simple HP defaults to entity health like Detailed");
        for (String legacy : new String[]{"Off", "MB", "FT"}) {
            hpSource.load(new JsonPrimitive(legacy));
            check(hpSource.getValue().getName().equals(legacy), "Existing HP source remains selectable");
        }
        hpSource.load(new JsonPrimitive("Default"));
        check(!style.getHideCondition().getAsBoolean(), "Style selectable with equipment disabled");
        style.load(new JsonPrimitive("esp.equipment.simple"));
        check(esp.isSimpleNametagStyle(), "Simple selection reaches renderer");
        check(!setting(esp, "Size").getHideCondition().getAsBoolean(), "Simple editable while ESP is off");
        check(setting(esp, "esp.armor").getHideCondition().getAsBoolean(), "Detailed equipment hidden in Simple");
        setting(esp, "Background").load(new JsonPrimitive(false));
        check(setting(esp, "Background opacity").getHideCondition().getAsBoolean(), "Background dependent visibility");
        setting(esp, "Size").load(new JsonPrimitive(0.55F));

        JsonObject saved = new JsonObject();
        for (Setting setting : esp.getSettings()) saved.add(setting.getName(), setting.save());
        ESP loaded = new ESP();
        for (Setting setting : loaded.getSettings()) setting.load(saved.get(setting.getName()));
        check(loaded.isSimpleNametagStyle(), "Style survives config round trip");
        check(((ModeSetting) setting(loaded, "HP source")).getValue().getName().equals("Default"), "Default HP source survives config round trip");
        check(Math.abs(setting(loaded, "Size").save().getAsFloat() - 0.55F) < 0.001F, "Embedded renderer size saved by ESP");
        check(!setting(loaded, "Background").save().getAsBoolean(), "Embedded renderer flags saved by ESP");
        ((ModeSetting) setting(loaded, "esp.equipment.style")).load(new JsonPrimitive("esp.equipment.detailed"));
        check(setting(loaded, "Size").getHideCondition().getAsBoolean(), "Simple controls hidden in Detailed");
        check(!setting(loaded, "esp.armor").getHideCondition().getAsBoolean(), "Detailed controls restored");
        check(!ESP.hidesVanillaName(null), "Disabled ESP leaves vanilla names alone");
        System.out.println("ESP: style visibility, embedded settings persistence and legacy box-mode fallback passed");
    }

    private static Setting setting(ESP esp, String name) {
        return esp.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
