package dev.bkclient.larp.feature;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.SpawnReason;
import net.minecraft.text.MutableText;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class GrinchStrike {
    private static final int COOLDOWN_TICKS = 1200;
    private static long readyAtTick;
    private static int nextBoltId;

    private GrinchStrike() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                GrinchStrike.onClientAttack(entity);
            }
            return ActionResult.PASS;
        });
    }

    private static void onClientAttack(Entity target) {
        ClientWorld NarrationMessageBuilder;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || !((NarrationMessageBuilder = client.world) instanceof ClientWorld)) {
            return;
        }
        ClientWorld world = NarrationMessageBuilder;
        if (!(target instanceof LivingEntity) || target == player) {
            return;
        }
        ItemStack held = player.getMainHandStack();
        if (!Disguise.hasAbility(held, Disguise.Ability.GRINCH_LIGHTNING)) {
            return;
        }
        long now = ClientClock.ticks();
        if (now < readyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(readyAtTick - now) / 20.0));
            return;
        }
        readyAtTick = now + 1200L;
        dev.bkclient.larp.LarpCooldown.set(held, 1200);
        GrinchStrike.strike(world, target.getX(), target.getY(), target.getZ());
        GrinchStrike.showHitMessage(target);
    }

    private static void strike(ClientWorld world, double x, double y, double z) {
        LightningEntity bolt = (LightningEntity)EntityType.LIGHTNING_BOLT.create((World)world, SpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }
        bolt.setCosmetic(true);
        bolt.setId(dev.bkclient.larp.LarpRuntime.nextEntityId());
        bolt.refreshPositionAfterTeleport(x, y, z);
        dev.bkclient.larp.LarpRuntime.addVisual(world, (Entity)bolt);
    }

    private static void showHitMessage(Entity target) {
        Style white = Style.EMPTY.withColor(Formatting.WHITE).withItalic(Boolean.valueOf(false));
        MutableText message = Text.empty().append(LegacyText.parse("&7Uderzy\u0142e\u015b ")).append((Text)Text.empty().setStyle(white).append(target.getName())).append(LegacyText.parse(" &7siekier\u0105 &agrincha&7!"));
        Titles.show((Text)Text.empty(), (Text)message);
    }

    static {
        nextBoltId = -1;
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { readyAtTick = 0; }
}
