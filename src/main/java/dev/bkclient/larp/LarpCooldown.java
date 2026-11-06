package dev.bkclient.larp;

import dev.bkclient.larp.feature.Disguise;
import net.minecraft.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

/** Cosmetic cooldowns never overwrite a server-controlled item cooldown. */
public final class LarpCooldown {
    private record Cooldown(long start, long end) {}
    private static final Map<String, Cooldown> entries = new HashMap<>();
    public static void set(ItemStack stack, int ticks) {
        long now = System.nanoTime();
        entries.put(Disguise.fingerprint(stack), new Cooldown(now, now + ticks * 50_000_000L));
    }
    public static float progress(ItemStack stack) {
        if (!LarpRuntime.abilities() || !Disguise.isDisguised(stack)) return 0;
        var key = Disguise.fingerprint(stack);
        var value = entries.get(key);
        if (value == null) return 0;
        long now = System.nanoTime();
        if (now >= value.end) { entries.remove(key); return 0; }
        return (float)(value.end - now) / (value.end - value.start);
    }
    public static void clear() { entries.clear(); }
}
