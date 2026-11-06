package dev.bkclient.larp.feature;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class FlightlessRod {
    private static final int COOLDOWN_TICKS = 600;
    private static final int CATCH_TICKS = 300;
    private static final double MAX_DISTANCE = 30.0;
    private static long readyAtTick;
    private static PlayerEntity caught;
    private static long releaseAtTick;
    private static Entity handled;

    private FlightlessRod() {
    }

    private static boolean holdsRod(ClientPlayerEntity player) {
        return Disguise.hasAbility(player.getMainHandStack(), Disguise.Ability.FLIGHTLESS_ROD) || Disguise.hasAbility(player.getOffHandStack(), Disguise.Ability.FLIGHTLESS_ROD);
    }

    public static void tick(MinecraftClient client) {
        PlayerEntity target;
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            caught = null;
            handled = null;
            return;
        }
        FishingBobberEntity bobber = player.fishHook;
        boolean holding = FlightlessRod.holdsRod(player);
        if (bobber == null || !holding) {
            handled = null;
            if (caught != null) {
                FlightlessRod.release();
            }
            return;
        }
        if (caught != null) {
            FlightlessRod.tickCaught(player);
            return;
        }
        Entity hooked = bobber.getHookedEntity();
        if (hooked == null) {
            handled = null;
            return;
        }
        if (hooked == handled) {
            return;
        }
        handled = hooked;
        if (hooked instanceof PlayerEntity && (target = (PlayerEntity)hooked) != player) {
            FlightlessRod.onHooked(player, target);
        }
    }

    private static void tickCaught(ClientPlayerEntity player) {
        if (caught.isRemoved() || (double)caught.distanceTo((Entity)player) > 30.0 || ClientClock.ticks() >= releaseAtTick) {
            FlightlessRod.release();
        }
    }

    private static void onHooked(ClientPlayerEntity player, PlayerEntity target) {
        long left = readyAtTick - ClientClock.ticks();
        if (left > 0L) {
            Titles.cooldown((int)Math.ceil((double)left / 20.0));
            Titles.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.0f);
            return;
        }
        caught = target;
        releaseAtTick = ClientClock.ticks() + 300L;
        readyAtTick = ClientClock.ticks() + 600L;
        dev.bkclient.larp.LarpCooldown.set(player.getMainHandStack(), 600);
        String nick = target.getName().getString();
        Titles.playSound(SoundEvents.ENTITY_FISHING_BOBBER_THROW, 1.0f, 0.8f);
        Titles.show(LegacyText.parse("&a&lZ\u0142apa\u0142e\u015b gracza!"), LegacyText.parse("&7Z\u0142apany gracz: &e" + nick));
        player.sendMessage(LegacyText.parse("&aZ\u0142apa\u0142e\u015b gracza &e" + nick + "&7!"), false);
    }

    private static void release() {
        caught = null;
        Titles.show(LegacyText.parse("&c&lPu\u015bci\u0142e\u015b gracza!"), LegacyText.parse("&cPu\u015bci\u0142e\u015b gracza!"));
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { caught = null; handled = null; readyAtTick = releaseAtTick = 0; }
}
