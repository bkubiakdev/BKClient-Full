package dev.bkclient.larp.feature;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class GuardianRod {
    private static final int COOLDOWN_TICKS = 3600;
    private static final int LIFETIME_TICKS = 200;
    private static long readyAtTick;
    private static long despawnAtTick;
    private static GuardianEntity guardian;
    private static int nextVisualId;

    private GuardianRod() {
    }

    public static boolean blocksUse(ItemStack stack) {
        if (!Disguise.hasAbility(stack, Disguise.Ability.GUARDIAN_ROD)) {
            return false;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.fishHook != null) {
            return false;
        }
        long left = readyAtTick - ClientClock.ticks();
        if (left <= 0L) {
            return false;
        }
        Titles.cooldown((int)Math.ceil((double)left / 20.0));
        return true;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && guardian != null && entity == guardian) {
                GuardianRod.onKilled();
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    public static void tick(MinecraftClient client) {
        ClientWorld NarrationMessageBuilder;
        ClientPlayerEntity player = client.player;
        if (player == null || !((NarrationMessageBuilder = client.world) instanceof ClientWorld)) {
            GuardianRod.despawn();
            return;
        }
        ClientWorld world = NarrationMessageBuilder;
        if (guardian == null) {
            if (player.fishHook != null && Disguise.hasAbility(player.getMainHandStack(), Disguise.Ability.GUARDIAN_ROD) && ClientClock.ticks() >= readyAtTick) {
                GuardianRod.summon(world, player, player.fishHook);
            }
            return;
        }
        FishingBobberEntity bobber = player.fishHook;
        boolean stillHeld = Disguise.hasAbility(player.getMainHandStack(), Disguise.Ability.GUARDIAN_ROD);
        if (bobber == null || !stillHeld) {
            Titles.show((Text)Text.empty(), LegacyText.parse("&cStra\u017cnik zerwa\u0142 si\u0119 z \u017cy\u0142ki i znikn\u0105\u0142!"));
            Titles.playSound(SoundEvents.ENTITY_GUARDIAN_DEATH, 0.8f, 1.2f);
            GuardianRod.despawn();
            return;
        }
        if (ClientClock.ticks() >= despawnAtTick) {
            GuardianRod.despawn();
            return;
        }
        guardian.setPosition(bobber.getX(), bobber.getY(), bobber.getZ());
        guardian.setVelocity(Vec3d.ZERO);
    }

    private static void summon(ClientWorld world, ClientPlayerEntity player, FishingBobberEntity bobber) {
        GuardianEntity summoned = (GuardianEntity)EntityType.GUARDIAN.create((World)world, SpawnReason.TRIGGERED);
        if (summoned == null) {
            return;
        }
        summoned.setId(dev.bkclient.larp.LarpRuntime.nextEntityId());
        summoned.setAiDisabled(true);
        summoned.setNoGravity(true);
        summoned.setCustomName(LegacyText.parse("&3&lStra\u017cnik G\u0142\u0119bin"));
        summoned.setCustomNameVisible(true);
        summoned.setHealth(20.0f);
        summoned.setPosition(bobber.getX(), bobber.getY(), bobber.getZ());
        dev.bkclient.larp.LarpRuntime.addVisual(world, (Entity)summoned);
        guardian = summoned;
        despawnAtTick = ClientClock.ticks() + 200L;
        readyAtTick = ClientClock.ticks() + 3600L;
        dev.bkclient.larp.LarpCooldown.set(player.getMainHandStack(), 3600);
        Titles.playSound(SoundEvents.ENTITY_GUARDIAN_AMBIENT, 1.0f, 1.0f);
    }

    private static void onKilled() {
        guardian.handleStatus((byte)2);
        Titles.playSound(SoundEvents.ENTITY_GUARDIAN_HURT, 1.0f, 1.0f);
        Titles.show((Text)Text.empty(), LegacyText.parse("&cStra\u017cnik zosta\u0142 zabity i znikn\u0105\u0142!"));
        GuardianRod.despawn();
    }

    private static void despawn() {
        if (guardian != null) {
            guardian.discard();
            guardian = null;
        }
    }

    public static boolean isOurs(Entity entity) {
        return guardian != null && entity == guardian;
    }

    static {
        nextVisualId = -2000;
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { if (guardian != null) guardian.discard(); guardian = null; readyAtTick = despawnAtTick = 0; }
}
