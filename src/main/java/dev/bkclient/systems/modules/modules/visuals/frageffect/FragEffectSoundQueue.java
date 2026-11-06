package dev.bkclient.systems.modules.modules.visuals.frageffect;

import dev.bkclient.utility.sounds.ClientSoundInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FragEffectSoundQueue {
    private final List<ScheduledSound> scheduled = new ArrayList<>();

    public void schedule(ClientSoundInstance sound, long delayMillis, float volume) {
        if (sound == null) return;
        if (delayMillis <= 0L) {
            sound.play(volume);
            return;
        }
        scheduled.add(new ScheduledSound(sound, System.currentTimeMillis() + delayMillis, volume));
    }

    public void tick() {
        if (scheduled.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<ScheduledSound> iterator = scheduled.iterator();
        while (iterator.hasNext()) {
            ScheduledSound entry = iterator.next();
            if (entry.playAtMillis <= now) {
                entry.sound.play(entry.volume);
                iterator.remove();
            }
        }
    }

    public void clear() {
        scheduled.clear();
    }

    private record ScheduledSound(ClientSoundInstance sound, long playAtMillis, float volume) {
    }
}
