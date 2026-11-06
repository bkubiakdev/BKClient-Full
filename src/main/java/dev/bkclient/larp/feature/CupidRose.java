package dev.bkclient.larp.feature;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeEffects;

public final class CupidRose {
    private static final int EFFECT_TICKS = 100;
    private static final float EXTRA_HEALTH = 10.0f;
    private static float realHealth = -1.0f;
    private static float shownHealth = -1.0f;
    private static double realMax = -1.0;
    private static double shownMax = -1.0;
    private static boolean boosted;

    private CupidRose() {
    }

    public static void tick(MinecraftClient client) {
        boolean holding;
        ClientPlayerEntity player = client.player;
        if (player == null) {
            boosted = false;
            realHealth = -1.0f;
            shownHealth = -1.0f;
            realMax = -1.0;
            shownMax = -1.0;
            return;
        }
        boolean bl = holding = Disguise.hasAbility(player.getMainHandStack(), Disguise.Ability.CUPID_ROSE) || Disguise.hasAbility(player.getOffHandStack(), Disguise.Ability.CUPID_ROSE);
        if (holding) {
            FakeEffects.apply((RegistryEntry<StatusEffect>)StatusEffects.RESISTANCE, 100, 0);
            FakeEffects.apply((RegistryEntry<StatusEffect>)StatusEffects.REGENERATION, 100, 0);
            CupidRose.addHearts(player);
            boosted = true;
            return;
        }
        if (boosted) {
            FakeEffects.remove((RegistryEntry<StatusEffect>)StatusEffects.RESISTANCE);
            FakeEffects.remove((RegistryEntry<StatusEffect>)StatusEffects.REGENERATION);
            CupidRose.removeHearts(player);
            boosted = false;
        }
    }

    private static void addHearts(ClientPlayerEntity player) {
        CupidRose.raiseCeiling(player);
        if (player.getHealth() != shownHealth) {
            realHealth = player.getHealth();
        }
        shownHealth = Math.min(realHealth + 10.0f, player.getMaxHealth());
        player.setHealth(shownHealth);
    }

    private static void raiseCeiling(ClientPlayerEntity player) {
        EntityAttributeInstance health = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        if (health.getBaseValue() != shownMax) {
            realMax = health.getBaseValue();
        }
        shownMax = realMax + 10.0;
        if (health.getBaseValue() != shownMax) {
            health.setBaseValue(shownMax);
        }
    }

    private static void removeHearts(ClientPlayerEntity player) {
        EntityAttributeInstance health;
        if (realHealth >= 0.0f && player.getHealth() == shownHealth) {
            player.setHealth(realHealth);
        }
        if ((health = player.getAttributeInstance(EntityAttributes.MAX_HEALTH)) != null && realMax >= 0.0 && health.getBaseValue() == shownMax) {
            health.setBaseValue(realMax);
        }
        realHealth = -1.0f;
        shownHealth = -1.0f;
        realMax = -1.0;
        shownMax = -1.0;
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { if (boosted && client.player != null) removeHearts(client.player); boosted = false; }
}
