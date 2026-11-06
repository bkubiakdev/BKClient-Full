package dev.bkclient.larp.feature;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeEffects;

public final class Lollipop {
    private static final double SCALE = 0.65;
    private static final int EFFECT_TICKS = 120;
    private static boolean shrunk;
    private static double originalScale = 1;

    private Lollipop() {
    }

    public static void tick(MinecraftClient client) {
        boolean holding;
        ClientPlayerEntity player = client.player;
        if (player == null) {
            shrunk = false;
            return;
        }
        boolean bl = holding = Disguise.hasAbility(player.getMainHandStack(), Disguise.Ability.LOLLIPOP) || Disguise.hasAbility(player.getOffHandStack(), Disguise.Ability.LOLLIPOP);
        if (holding) {
            if (player.getAttributeInstance(EntityAttributes.SCALE) != null && (!shrunk || player.getAttributeInstance(EntityAttributes.SCALE).getBaseValue() != SCALE)) originalScale = player.getAttributeInstance(EntityAttributes.SCALE).getBaseValue();
            Lollipop.setScale((PlayerEntity)player, 0.65);
            shrunk = true;
            FakeEffects.apply((RegistryEntry<StatusEffect>)StatusEffects.STRENGTH, 120, 0);
        } else if (shrunk) {
            restoreScale(player);
            shrunk = false;
            FakeEffects.remove((RegistryEntry<StatusEffect>)StatusEffects.STRENGTH);
        }
    }

    private static void restoreScale(PlayerEntity player) {
        var attribute = player.getAttributeInstance(EntityAttributes.SCALE);
        if (attribute != null && attribute.getBaseValue() == SCALE) setScale(player, originalScale);
    }

    private static void setScale(PlayerEntity player, double value) {
        EntityAttributeInstance scale = player.getAttributeInstance(EntityAttributes.SCALE);
        if (scale != null && scale.getBaseValue() != value) {
            scale.setBaseValue(value);
        }
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { if (shrunk && client.player != null) restoreScale(client.player); shrunk = false; }
}
