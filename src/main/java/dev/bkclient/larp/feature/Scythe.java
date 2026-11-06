package dev.bkclient.larp.feature;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class Scythe {
    private static final int COOLDOWN_TICKS = 800;
    private static long readyAtTick;

    private Scythe() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                Scythe.onClientAttack(entity);
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
        if (!Disguise.hasAbility(held, Disguise.Ability.SCYTHE)) {
            return;
        }
        long now = ClientClock.ticks();
        if (now < readyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(readyAtTick - now) / 20.0));
            return;
        }
        readyAtTick = now + 800L;
        dev.bkclient.larp.LarpCooldown.set(held, 800);
        Scythe.showHitMessage(target);
    }

    private static void showHitMessage(Entity target) {
        Style dark = Style.EMPTY.withColor(Formatting.DARK_GRAY).withItalic(Boolean.valueOf(false));
        MutableText message = Text.empty().append(LegacyText.parse("&7O\u015blepi\u0142e\u015b ")).append((Text)Text.empty().setStyle(dark).append(target.getName())).append(LegacyText.parse("&7!"));
        Titles.show((Text)Text.empty(), (Text)message);
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { readyAtTick = 0; }
}
