package dev.bkclient.larp;

import dev.bkclient.BKClientInit;
import dev.bkclient.larp.feature.*;
import dev.bkclient.systems.modules.modules.visuals.LarpAna;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;
import java.util.ArrayList;
import java.util.List;

public final class LarpRuntime {
    private static boolean initialized, wasSimulating;
    private static int nextId = -1_500_000;
    private static final List<Entity> visuals = new ArrayList<>();
    public static boolean active() {
        return LarpAna.instance != null && LarpAna.instance.isEnabled() && !BKClientInit.INSTANCE.isPanic();
    }
    public static boolean abilities() { return active() && LarpAna.instance.simulate.isEnabled(); }
    public static boolean effects() { return abilities() && LarpAna.instance.effects.isEnabled(); }
    public static int nextEntityId() { return nextId--; }
    public static void addVisual(ClientWorld world, Entity entity) { world.addEntity(entity); visuals.add(entity); }

    public static void init() {
        if (initialized) return;
        initialized = true;
        LarpCatalog.init();
        GrinchStrike.register(); GuardianRod.register(); PoisonPencil.register(); Scythe.register();
        GrinchShovel.register(); RubiksCube.register(); RabbitSword.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (wasSimulating && !abilities()) resetEffects();
            wasSimulating = abilities();
            if (!active() || client.world == null || client.player == null) return;
            ClientClock.tick();
            visuals.removeIf(Entity::isRemoved);
            if (!abilities()) return;
            FakeEffects.tick(client); HydroCage.tick(client); MagicLantern.tick(client); PhantomBlock.tick(client);
            IllusionistWand.tick(client); GuardianRod.tick(client); Lollipop.tick(client); CupidRose.tick(client);
            ArcusMagnus.tick(client); RubiksCube.tick(client); FlightlessRod.tick(client);
            GrinchShovel.tick(client); DivineAxe.tick(client);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> { if (active()) DroppedItemLabels.render(context); });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!abilities() || !world.isClient) return ActionResult.PASS;
            if (HydroCage.isCageBlock(pos)) { HydroCage.warnBreak(MinecraftClient.getInstance().player); return ActionResult.FAIL; }
            if (MagicLantern.isFountain(pos)) { MagicLantern.mine(player, pos, direction); return ActionResult.FAIL; }
            return ActionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!abilities() || !world.isClient) return ActionResult.PASS;
            if (HydroCage.isInsideCage(hit.getBlockPos())) { HydroCage.warnPlace(); return ActionResult.FAIL; }
            var stack = player.getStackInHand(hand);
            var at = hit.getBlockPos().offset(hit.getSide());
            if (Disguise.hasAbility(stack, Disguise.Ability.PHANTOM_BLOCK) && PhantomBlock.onUse(stack, at)
                || Disguise.hasAbility(stack, Disguise.Ability.ILLUSIONIST_WAND) && IllusionistWand.onRightClick(stack)
                || Disguise.hasAbility(stack, Disguise.Ability.MAGIC_LANTERN) && MagicLantern.onPlaced(stack, at)
                || Disguise.blocksPlacing(stack, world, hit.getBlockPos())) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
    }

    public static void resetEffects() {
        if (!initialized) return;
        var mc = MinecraftClient.getInstance();
        dev.bkclient.utility.concurrent.ShutdownTasks.run(error -> BKClientInit.LOGGER.error("Larp Ana cleanup failed", error),
            () -> HydroCage.resetSession(mc), () -> MagicLantern.resetSession(mc),
            () -> IllusionistWand.resetSession(mc), () -> GuardianRod.resetSession(mc),
            () -> CupidRose.resetSession(mc), () -> Lollipop.resetSession(mc),
            () -> FakeEffects.resetSession(mc), () -> ArcusMagnus.resetSession(mc),
            () -> DivineAxe.resetSession(mc), () -> FlightlessRod.resetSession(mc),
            () -> GrinchShovel.resetSession(mc), () -> RubiksCube.resetSession(mc),
            () -> PhantomBlock.resetSession(mc), () -> GrinchStrike.resetSession(mc),
            () -> PoisonPencil.resetSession(mc), () -> Scythe.resetSession(mc),
            () -> RabbitSword.resetSession(mc), () -> FakeConsumable.resetSession(mc),
            () -> { visuals.forEach(Entity::discard); visuals.clear(); LarpCooldown.clear(); });
    }

    public static void reset() {
        resetEffects();
        Disguise.clear();
        LarpCatalog.clearAssignments();
        BackpackScreen.resetSession(MinecraftClient.getInstance());
    }
}
