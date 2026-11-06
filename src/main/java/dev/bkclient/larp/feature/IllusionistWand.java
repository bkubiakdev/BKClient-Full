package dev.bkclient.larp.feature;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeEffects;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class IllusionistWand {
    private static final int LEFT_COOLDOWN_TICKS = 1200;
    private static final int RIGHT_COOLDOWN_TICKS = 2400;
    private static final int WAVES = 3;
    private static final int FANGS_PER_WAVE = 3;
    private static final double WAVE_SPACING = 2.0;
    private static final int WAVE_DELAY_TICKS = 5;
    private static final int INVISIBILITY_TICKS = 80;
    private static final int CLONE_TICKS = 60;
    private static final double CLONE_HEAD_START = 1.5;
    private static final double CLONE_SPAWN_STEP = 0.25;
    private static final double CLONE_UNSTICK_HEIGHT = 2.0;
    private static final double CLONE_SPEED = 0.2806;
    private static final double CLONE_GRAVITY = 0.08;
    private static final double CLONE_DRAG = 0.98;
    private static final double CLONE_JUMP = 0.42;
    private static final int FANGS_LIFETIME_TICKS = 30;
    private static final EquipmentSlot[] COPIED_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private static long leftReadyAtTick;
    private static long rightReadyAtTick;
    private static int wavesLeft;
    private static long nextWaveAtTick;
    private static Vec3d waveOrigin;
    private static Vec3d waveDirection;
    private static final List<Fangs> activeFangs;
    private static OtherClientPlayerEntity clone;
    private static long cloneEndsAtTick;
    private static long visibleAgainAtTick;
    private static double cloneFallSpeed;
    private static PlayerEntity cloneSkinSource;
    private static boolean wasAttackDown;
    private static int nextVisualId;

    private IllusionistWand() {
    }

    public static boolean onRightClick(ItemStack stack) {
        ClientWorld NarrationMessageBuilder;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || !((NarrationMessageBuilder = client.world) instanceof ClientWorld)) {
            return true;
        }
        ClientWorld world = NarrationMessageBuilder;
        if (ClientClock.ticks() < rightReadyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(rightReadyAtTick - ClientClock.ticks()) / 20.0));
            return true;
        }
        rightReadyAtTick = ClientClock.ticks() + 2400L;
        dev.bkclient.larp.LarpCooldown.set(stack, 2400);
        FakeEffects.apply((RegistryEntry<StatusEffect>)StatusEffects.INVISIBILITY, 80, 0);
        visibleAgainAtTick = ClientClock.ticks() + 80L;
        IllusionistWand.spawnClone(world, (PlayerEntity)player);
        Titles.playSound(SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0f, 1.2f);
        Titles.show((Text)Text.empty(), LegacyText.parse("&aZnikni\u0119cie &7zosta\u0142o &aaktywowane&7!"));
        return true;
    }

    private static void spawnClone(ClientWorld world, PlayerEntity player) {
        IllusionistWand.discardClone();
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d ahead = new Vec3d(look.x, 0.0, look.z).normalize();
        OtherClientPlayerEntity copy = new OtherClientPlayerEntity(world, IllusionistWand.cloneProfile(player));
        copy.setId(dev.bkclient.larp.LarpRuntime.nextEntityId());
        copy.setPosition(IllusionistWand.freeSpotAhead(world, copy, player.getPos(), ahead));
        copy.setYaw(player.getYaw());
        copy.setHeadYaw(player.getYaw());
        copy.setBodyYaw(player.getYaw());
        for (EquipmentSlot slot : COPIED_SLOTS) {
            copy.equipStack(slot, player.getEquippedStack(slot).copy());
        }
        copy.setSprinting(true);
        dev.bkclient.larp.LarpRuntime.addVisual(world, (Entity)copy);
        clone = copy;
        cloneSkinSource = player;
        cloneEndsAtTick = ClientClock.ticks() + 60L;
        cloneFallSpeed = 0.0;
    }

    public static PlayerEntity cloneSkinSource() {
        return cloneSkinSource;
    }

    private static Vec3d freeSpotAhead(ClientWorld world, OtherClientPlayerEntity copy, Vec3d from, Vec3d ahead) {
        Vec3d at;
        Vec3d best = from;
        for (double step = 0.25; step <= 1.5 && IllusionistWand.fits(world, copy, at = from.add(ahead.multiply(step))); step += 0.25) {
            best = at;
        }
        return best;
    }

    private static boolean fits(ClientWorld world, OtherClientPlayerEntity copy, Vec3d at) {
        return world.isSpaceEmpty((Entity)copy, copy.getDimensions(copy.getPose()).getBoxAt(at));
    }

    private static GameProfile cloneProfile(PlayerEntity player) {
        GameProfile source = player.getGameProfile();
        GameProfile copy = new GameProfile(UUID.randomUUID(), source.getName());
        copy.getProperties().putAll((Multimap)source.getProperties());
        return copy;
    }

    public static boolean isClone(Entity entity) {
        return clone != null && entity == clone;
    }

    public static void tickCloneMovement(OtherClientPlayerEntity entity) {
        if (entity != clone) {
            return;
        }
        World class_19372 = entity.getWorld();
        if (!(class_19372 instanceof ClientWorld)) {
            return;
        }
        ClientWorld world = (ClientWorld)class_19372;
        if (!IllusionistWand.unstick(world, entity)) {
            return;
        }
        Vec3d look = Vec3d.fromPolar((float)0.0f, (float)entity.getYaw());
        double dx = look.x * 0.2806;
        double dz = look.z * 0.2806;
        cloneFallSpeed = (cloneFallSpeed - 0.08) * 0.98;
        entity.move(MovementType.SELF, new Vec3d(dx, cloneFallSpeed, dz));
        if (entity.isOnGround()) {
            cloneFallSpeed = 0.42;
        } else if (entity.verticalCollision) {
            cloneFallSpeed = 0.0;
        }
        entity.setVelocity(dx, cloneFallSpeed, dz);
    }

    private static boolean unstick(ClientWorld world, OtherClientPlayerEntity entity) {
        if (IllusionistWand.fits(world, entity, entity.getPos())) {
            return true;
        }
        for (double up = 0.5; up <= 2.0; up += 0.5) {
            Vec3d above = entity.getPos().add(0.0, up, 0.0);
            if (!IllusionistWand.fits(world, entity, above)) continue;
            entity.setPosition(above);
            cloneFallSpeed = 0.0;
            return true;
        }
        IllusionistWand.discardClone();
        return false;
    }

    private static void castFangs(ClientPlayerEntity player, ItemStack stack) {
        leftReadyAtTick = ClientClock.ticks() + 1200L;
        dev.bkclient.larp.LarpCooldown.set(stack, 1200);
        Vec3d look = player.getRotationVec(1.0f);
        waveDirection = new Vec3d(look.x, 0.0, look.z).normalize();
        waveOrigin = player.getPos();
        wavesLeft = 3;
        nextWaveAtTick = ClientClock.ticks();
        Titles.playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
        Titles.show((Text)Text.empty(), LegacyText.parse("&aSzcz\u0119ki Evokera&7 zosta\u0142y &aaktywowane&7!"));
    }

    private static void spawnWave(ClientWorld world, int waveIndex) {
        double distance = (double)(waveIndex + 1) * 2.0;
        Vec3d side = new Vec3d(-IllusionistWand.waveDirection.z, 0.0, IllusionistWand.waveDirection.x);
        for (int i = 0; i < 3; ++i) {
            double offset = (double)i - 1.0;
            Vec3d at = waveOrigin.add(waveDirection.multiply(distance)).add(side.multiply(offset));
            EvokerFangsEntity fangs = (EvokerFangsEntity)EntityType.EVOKER_FANGS.create((World)world, SpawnReason.TRIGGERED);
            if (fangs == null) {
                return;
            }
            fangs.setId(dev.bkclient.larp.LarpRuntime.nextEntityId());
            fangs.setPosition(at.x, IllusionistWand.waveOrigin.y, at.z);
            dev.bkclient.larp.LarpRuntime.addVisual(world, (Entity)fangs);
            fangs.handleStatus((byte)4);
            activeFangs.add(new Fangs(fangs, ClientClock.ticks() + 30L));
        }
    }

    public static void tick(MinecraftClient client) {
        ClientWorld NarrationMessageBuilder;
        ClientPlayerEntity player = client.player;
        if (player == null || !((NarrationMessageBuilder = client.world) instanceof ClientWorld)) {
            IllusionistWand.discardClone();
            activeFangs.forEach(fangs -> fangs.entity().discard());
            activeFangs.clear();
            wasAttackDown = false;
            return;
        }
        ClientWorld world = NarrationMessageBuilder;
        IllusionistWand.tickLeftClick(client, player);
        IllusionistWand.tickWaves(world);
        IllusionistWand.tickFangs();
        IllusionistWand.tickClone();
        IllusionistWand.tickVisibility();
    }

    private static void tickLeftClick(MinecraftClient client, ClientPlayerEntity player) {
        boolean down = client.options.attackKey.isPressed();
        boolean justPressed = down && !wasAttackDown;
        wasAttackDown = down;
        if (!justPressed || client.currentScreen != null) {
            return;
        }
        ItemStack held = player.getMainHandStack();
        if (!Disguise.hasAbility(held, Disguise.Ability.ILLUSIONIST_WAND)) {
            return;
        }
        if (ClientClock.ticks() < leftReadyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(leftReadyAtTick - ClientClock.ticks()) / 20.0));
            return;
        }
        IllusionistWand.castFangs(player, held);
    }

    private static void tickWaves(ClientWorld world) {
        if (wavesLeft <= 0 || ClientClock.ticks() < nextWaveAtTick) {
            return;
        }
        IllusionistWand.spawnWave(world, 3 - wavesLeft);
        --wavesLeft;
        nextWaveAtTick = ClientClock.ticks() + 5L;
    }

    private static void tickFangs() {
        Iterator<Fangs> it = activeFangs.iterator();
        while (it.hasNext()) {
            Fangs fangs = it.next();
            if (ClientClock.ticks() < fangs.despawnAtTick()) continue;
            fangs.entity().discard();
            it.remove();
        }
    }

    private static void tickClone() {
        if (clone == null) {
            return;
        }
        if (ClientClock.ticks() >= cloneEndsAtTick) {
            IllusionistWand.discardClone();
        }
    }

    private static void tickVisibility() {
        if (visibleAgainAtTick == 0L || ClientClock.ticks() < visibleAgainAtTick) {
            return;
        }
        visibleAgainAtTick = 0L;
        Titles.show((Text)Text.empty(), LegacyText.parse("&cZnowu jeste\u015b widoczny!"));
    }

    private static void discardClone() {
        if (clone != null) {
            clone.discard();
            clone = null;
        }
    }

    static {
        waveOrigin = Vec3d.ZERO;
        waveDirection = Vec3d.ZERO;
        activeFangs = new ArrayList<Fangs>();
        nextVisualId = -3000;
    }

    private record Fangs(EvokerFangsEntity entity, long despawnAtTick) {
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { discardClone(); for (var f : activeFangs) f.entity().discard(); activeFangs.clear(); cloneSkinSource = null; wavesLeft = 0; visibleAgainAtTick = 0; leftReadyAtTick = rightReadyAtTick = 0; wasAttackDown = false; }
}
