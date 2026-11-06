package dev.bkclient.larp.feature;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeEffects;

public final class FakeConsumable {
    private static long readyAtTick;

    private FakeConsumable() {
    }

    public static boolean blocksUse(ItemStack stack) {
        if (!Disguise.isDisguised(stack)) {
            return false;
        }
        if (Disguise.abilityOf(stack) != Disguise.Ability.NONE) {
            return false;
        }
        if (Disguise.consumableOf(stack) == null) {
            return stack.contains(DataComponentTypes.CONSUMABLE);
        }
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null && ClientClock.ticks() < readyAtTick;
    }

    public static void onConsume(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || entity != player || client.world == null) {
            return;
        }
        ItemStack stack = player.getActiveItem();
        Disguise.Consumable consumable = Disguise.consumableOf(stack);
        if (consumable == null) {
            return;
        }
        for (Disguise.Effect effect : consumable.effects()) {
            FakeEffects.apply(effect.effect(), effect.duration(), effect.amplifier());
        }
        readyAtTick = ClientClock.ticks() + (long)consumable.cooldownTicks();
        dev.bkclient.larp.LarpCooldown.set(stack, consumable.cooldownTicks());
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { readyAtTick = 0; }
}
