package dev.bkclient.larp.feature;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;

public final class FakeEffects {
    private static final Map<RegistryEntry<StatusEffect>, Long> expiry = new HashMap<RegistryEntry<StatusEffect>, Long>();

    public static void resetSession(MinecraftClient client) {
        for (var effect : java.util.List.copyOf(expiry.keySet())) remove(effect);
    }
    private static final Map<RegistryEntry<StatusEffect>, StatusEffectInstance> owned = new HashMap<>();
    public static void relinquish(RegistryEntry<StatusEffect> effect) { expiry.remove(effect); owned.remove(effect); }
    private FakeEffects() {
    }

    static void apply(RegistryEntry<StatusEffect> effect, int durationTicks, int amplifier) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        if (player.hasStatusEffect(effect) && owned.get(effect) != player.getStatusEffect(effect)) return;
        player.addStatusEffect(new StatusEffectInstance(effect, durationTicks, amplifier));
        owned.put(effect, player.getStatusEffect(effect));
        expiry.put(effect, ClientClock.ticks() + (long)durationTicks);
    }

    static void remove(RegistryEntry<StatusEffect> effect) {
        if (expiry.remove(effect) == null) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        var instance = owned.remove(effect);
        if (player != null && instance != null && player.getStatusEffect(effect) == instance) {
            player.removeStatusEffect(effect);
        }
    }

    public static void tick(MinecraftClient client) {
        if (expiry.isEmpty()) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || !player.isAlive()) {
            expiry.clear();
            owned.clear();
            return;
        }
        long now = ClientClock.ticks();
        for (var effect : java.util.List.copyOf(expiry.keySet())) if (now >= expiry.get(effect)) remove(effect);
    }
}
