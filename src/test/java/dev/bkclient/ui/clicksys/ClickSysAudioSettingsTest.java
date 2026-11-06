package dev.bkclient.ui.clicksys;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.systems.setting.Setting;

/** Verify legacy volume and additive GUI settings through real config serialization. */
public final class ClickSysAudioSettingsTest {
    public static void main(String[] args) {
        Sounds sounds = new Sounds();
        sounds.getVolume().load(new JsonPrimitive(0.5F));
        check(Math.abs(sounds.getGuiVolume() - 0.1F) < 0.00001F, "legacy volume still applies");
        setting(sounds, "gui_volume").load(new JsonPrimitive(0));
        check(sounds.getGuiVolume() == 0, "GUI can be muted independently");
        check(sounds.getVolume().getCurrentValue() == 0.5F, "muting GUI preserves other sounds");
        setting(sounds, "gui_volume").load(new JsonPrimitive(100));
        float previous = 0;
        for (String mode : new String[]{"low", "medium", "high"}) {
            setting(sounds, "gui_pitch").load(new JsonPrimitive("modules.settings.sounds.pitch_" + mode));
            check(sounds.getGuiPitch() > previous, "pitch modes are ordered");
            previous = sounds.getGuiPitch();
        }
        JsonObject saved = new JsonObject();
        for (Setting setting : sounds.getSettings()) {
            check(!saved.has(setting.getName()), "unique setting key");
            saved.add(setting.getName(), setting.save());
        }
        Sounds restored = new Sounds();
        for (Setting setting : restored.getSettings()) setting.load(saved.get(setting.getName()));
        check(restored.getGuiVolume() == sounds.getGuiVolume(), "volume round trip");
        check(restored.getGuiPitch() == sounds.getGuiPitch(), "pitch round trip");
        System.out.println("ClickGUI audio: legacy volume, independent mute, pitch modes and config round trip passed");
    }

    private static Setting setting(Sounds sounds, String suffix) {
        return sounds.getSettings().stream().filter(s -> s.getName().equals("modules.settings.sounds." + suffix)).findFirst().orElseThrow();
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
