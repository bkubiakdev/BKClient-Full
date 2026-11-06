package dev.bkclient.larp.feature;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.Titles;

public final class RabbitSword {
    private static final int COOLDOWN_TICKS = 800;
    private static long readyAtTick;

    private RabbitSword() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                RabbitSword.onClientAttack(entity);
            }
            return ActionResult.PASS;
        });
    }

    private static void onClientAttack(Entity target) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        if (!(target instanceof LivingEntity) || target == player) {
            return;
        }
        ItemStack held = player.getMainHandStack();
        if (!Disguise.hasAbility(held, Disguise.Ability.RABBIT_SWORD)) {
            return;
        }
        long now = ClientClock.ticks();
        if (now < readyAtTick) {
            return;
        }
        readyAtTick = now + 800L;
        dev.bkclient.larp.LarpCooldown.set(held, 800);
        Titles.playSound(SoundEvents.ENTITY_RABBIT_HURT, 1.0f, 1.0f);
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { readyAtTick = 0; }
}
