package dev.bkclient.ui.clicksys;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.ui.clicksys.port.enums.SoundEvent;
import dev.bkclient.utility.sounds.ClientSoundInstance;
import java.util.EnumMap;
import java.util.Locale;

/** Dedicated ClickGUI samples; Minecraft's sound engine handles volume and reloads. */
public final class ClickSysSounds {
    private static final EnumMap<SoundEvent, ClientSoundInstance> SAMPLES = new EnumMap<>(SoundEvent.class);
    private static final ClientSoundInstance TICK = new ClientSoundInstance("clicksys/6", 1);
    private static long lastTick;
    static {
        for (SoundEvent event : SoundEvent.values()) {
            String file = event.getText().toLowerCase(Locale.ROOT).replace(".wav", "");
            SAMPLES.put(event, new ClientSoundInstance("clicksys/" + file, 1));
        }
    }
    private ClickSysSounds() {}
    private static Sounds settings() {
        var manager = BKClientInit.getInstance().getModuleManager();
        if (manager == null) return null;
        Sounds sounds = manager.getModule(Sounds.class);
        return sounds != null && sounds.isEnabled() ? sounds : null;
    }
    public static void play(SoundEvent event) {
        Sounds sounds = settings();
        if (sounds != null && sounds.getGuiVolume() > 0) SAMPLES.get(event).play(sounds.getGuiVolume(), sounds.getGuiPitch());
    }
    public static void scroll() { tick(.18F, 1.2F); }
    public static void slider() { tick(.14F, 1.45F); }
    private static void tick(float gain, float pitch) {
        Sounds sounds = settings();
        if (sounds == null) return;
        float volume = sounds.getGuiVolume();
        long now = System.nanoTime();
        if (volume <= 0 || now - lastTick < 65_000_000L) return;
        lastTick = now;
        TICK.play(volume * gain, pitch * sounds.getGuiPitch());
    }
}
