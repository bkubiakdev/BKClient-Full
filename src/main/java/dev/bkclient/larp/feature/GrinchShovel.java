package dev.bkclient.larp.feature;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.Titles;

public final class GrinchShovel {
    private static final int COOLDOWN_TICKS = 1000;
    private static final int STUN_TICKS = 60;
    private static long readyAtTick;
    private static LivingEntity twisted;
    private static long twistUntilTick;
    private static float twistYaw;
    private static float twistBodyYaw;
    private static float twistPitch;

    private GrinchShovel() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                GrinchShovel.onClientAttack(entity);
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
        if (!Disguise.hasAbility(held, Disguise.Ability.GRINCH_SHOVEL)) {
            return;
        }
        long now = ClientClock.ticks();
        if (now < readyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(readyAtTick - now) / 20.0));
            return;
        }
        readyAtTick = now + 1000L;
        dev.bkclient.larp.LarpCooldown.set(held, 1000);
        twisted = (LivingEntity)target;
        twistUntilTick = now + 60L;
        twistYaw = target.getRandom().nextFloat() * 360.0f;
        twistBodyYaw = target.getRandom().nextFloat() * 360.0f;
        twistPitch = target.getRandom().nextFloat() * 180.0f - 90.0f;
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || twisted == null) {
            return;
        }
        if (ClientClock.ticks() >= twistUntilTick || twisted.isRemoved()) {
            twisted = null;
            return;
        }
        twisted.setHeadYaw(twistYaw);
        twisted.setBodyYaw(twistBodyYaw);
        twisted.setYaw(twistYaw);
        twisted.setPitch(twistPitch);
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { twisted = null; readyAtTick = twistUntilTick = 0; }
}
