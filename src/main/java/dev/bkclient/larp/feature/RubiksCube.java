package dev.bkclient.larp.feature;

import java.util.List;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class RubiksCube {
    private static final int COOLDOWN_TICKS = 1200;
    private static final int SCRAMBLE_TICKS = 30;
    private static final List<Item> SCRAMBLED = List.of(Items.DIRT, Items.EGG, Items.CARROT, Items.BONE, Items.SLIME_BALL, Items.PUFFERFISH, Items.BRICK, Items.WHEAT_SEEDS, Items.STICK, Items.ROTTEN_FLESH);
    private static long readyAtTick;
    private static int used;
    private static PlayerEntity scrambled;
    private static Item scrambledLook;
    private static long scrambleEndsAtTick;

    private RubiksCube() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                RubiksCube.onClientAttack(entity);
            }
            return ActionResult.PASS;
        });
    }

    private static void onClientAttack(Entity target) {
        PlayerEntity victim;
        ClientPlayerEntity player;
        MinecraftClient client;
        block7: {
            block6: {
                client = MinecraftClient.getInstance();
                player = client.player;
                if (player == null || client.world == null) {
                    return;
                }
                if (!(target instanceof PlayerEntity)) break block6;
                victim = (PlayerEntity)target;
                if (target != player) break block7;
            }
            return;
        }
        ItemStack held = player.getMainHandStack();
        if (!Disguise.hasAbility(held, Disguise.Ability.RUBIKS_CUBE)) {
            return;
        }
        long now = ClientClock.ticks();
        if (now < readyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(readyAtTick - now) / 20.0));
            return;
        }
        readyAtTick = now + 1200L;
        dev.bkclient.larp.LarpCooldown.set(held, 1200);
        ++used;
        scrambled = victim;
        scrambledLook = SCRAMBLED.get(client.world.random.nextInt(SCRAMBLED.size()));
        scrambleEndsAtTick = now + 30L;
        Titles.show(LegacyText.parse("&e&lKostka rubika"), LegacyText.parse("&fPrzemiesza\u0142e\u015b ekwipunek gracza &6" + victim.getName().getString() + "&f!"));
    }

    public static void tick(MinecraftClient client) {
        if (scrambled == null) {
            return;
        }
        if (client.world == null || scrambled.isRemoved() || ClientClock.ticks() >= scrambleEndsAtTick) {
            scrambled = null;
            scrambledLook = null;
        }
    }

    public static ItemStack shownItem(Entity holder, ItemStack stack) {
        LivingEntity living;
        if (scrambled == null || holder != scrambled || scrambledLook == null) {
            return stack;
        }
        if (stack.isEmpty()) {
            return stack;
        }
        if (!(holder instanceof LivingEntity) || (living = (LivingEntity)holder).getMainHandStack() != stack) {
            return stack;
        }
        return new ItemStack((ItemConvertible)scrambledLook);
    }

    static int shownCount(ItemStack stack) {
        int shown = stack.getCount() - used;
        return shown > 1 ? shown : 0;
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { scrambled = null; scrambledLook = null; used = 0; readyAtTick = scrambleEndsAtTick = 0; }
}
