package dev.bkclient.larp.feature;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.hit.HitResult;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.entity.SpawnReason;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;
import dev.bkclient.larp.mixin.BossBarHudAccessor;

public final class HydroCage {
    private static final int RADIUS = 9;
    private static final int WALL_THICKNESS = 1;
    private static final int CAGE_TICKS = 300;
    private static final int COOLDOWN_TICKS = 1200;
    private static final int LAYERS = 19;
    private static final int BUILD_TICKS = 40;
    private static final int BOSSBAR_RANGE = 13;
    private static final double SPEED = 1.5;
    private static final double GRAVITY = 0.03;
    private static final int MAX_FLIGHT_TICKS = 200;
    private static final int DRIPS_PER_TICK = 12;
    private static final int DUST_COLOR = 5947135;
    private static final float DUST_SIZE = 1.1f;
    private static final float RING_DUST_SIZE = 1.6f;
    private static final int RING_PARTICLES = 20;
    private static final int MOTES_PER_TICK = 4;
    private static final int BLOCK_FLAGS = 19;
    private static final UUID BOSSBAR_ID = UUID.nameUUIDFromBytes("easyeventowka:hydro_cage".getBytes());
    private static Projectile projectile;
    private static Cage cage;
    private static long readyAtTick;
    private static int nextVisualId;
    private static int lastShownSecond;

    private HydroCage() {
    }

    public static boolean tryShoot(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return false;
        }
        if (ClientClock.ticks() < readyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(readyAtTick - ClientClock.ticks()) / 20.0));
            return true;
        }
        if (projectile != null) {
            return true;
        }
        readyAtTick = ClientClock.ticks() + 1200L;
        lastShownSecond = -1;
        dev.bkclient.larp.LarpCooldown.set(stack, 1200);
        Vec3d start = player.getEyePos();
        projectile = new Projectile(start, player.getRotationVec(1.0f).multiply(1.5));
        HydroCage.projectile.visual = HydroCage.spawnFireball(client.world, start);
        Titles.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        return true;
    }

    private static FireballEntity spawnFireball(ClientWorld world, Vec3d at) {
        FireballEntity fireball = (FireballEntity)EntityType.FIREBALL.create((World)world, SpawnReason.TRIGGERED);
        if (fireball == null) {
            return null;
        }
        fireball.setId(dev.bkclient.larp.LarpRuntime.nextEntityId());
        fireball.setNoGravity(true);
        fireball.setVelocity(Vec3d.ZERO);
        fireball.setPosition(at.x, at.y, at.z);
        dev.bkclient.larp.LarpRuntime.addVisual(world, (Entity)fireball);
        return fireball;
    }

    public static void tick(MinecraftClient client) {
        ClientWorld NarrationMessageBuilder = client.world;
        if (!(NarrationMessageBuilder instanceof ClientWorld)) {
            if (projectile != null) {
                projectile.discardVisual();
                projectile = null;
            }
            cage = null;
            HydroCage.hideBossBar(client);
            return;
        }
        ClientWorld world = NarrationMessageBuilder;
        HydroCage.tickProjectile(client, world);
        HydroCage.tickCage(client, world);
        HydroCage.tickCooldownCountdown();
    }

    private static void tickProjectile(MinecraftClient client, ClientWorld world) {
        if (projectile == null) {
            return;
        }
        Vec3d from = HydroCage.projectile.pos;
        Vec3d to = from.add(HydroCage.projectile.velocity);
        BlockHitResult hit = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)client.player));
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            projectile.discardVisual();
            projectile = null;
            HydroCage.createCage(client, world, hit.getBlockPos());
            return;
        }
        HydroCage.projectile.pos = to;
        HydroCage.projectile.velocity = HydroCage.projectile.velocity.subtract(0.0, 0.03, 0.0);
        if (HydroCage.projectile.visual != null) {
            HydroCage.projectile.visual.setPosition(to.x, to.y, to.z);
        }
        if (++HydroCage.projectile.ticks > 200) {
            projectile.discardVisual();
            projectile = null;
        }
    }

    private static void tickCage(MinecraftClient client, ClientWorld world) {
        if (cage == null) {
            return;
        }
        if (cage.isBuilding()) {
            HydroCage.buildNextLayers(world, cage);
        }
        // Server block updates remain authoritative during the local simulation.
        HydroCage.spawnDrips(world, cage);
        HydroCage.spawnMotes(world, cage);
        if (ClientClock.ticks() >= HydroCage.cage.expiresAtTick) {
            HydroCage.removeCage(client, world);
            Titles.playSound(SoundEvents.BLOCK_CONDUIT_DEACTIVATE, 1.0f, 1.0f);
            return;
        }
        HydroCage.updateBossBar(client, (float)(HydroCage.cage.expiresAtTick - ClientClock.ticks()) / 300.0f);
    }

    private static void spawnDrips(ClientWorld world, Cage standing) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int i = 0; i < 12; ++i) {
            double dx = (world.random.nextDouble() * 2.0 - 1.0) * 9.0;
            double dz = (world.random.nextDouble() * 2.0 - 1.0) * 9.0;
            double dy = world.random.nextDouble() * 9.0;
            if (dx * dx + dy * dy + dz * dz > 81.0) continue;
            double x = (double)standing.center.getX() + 0.5 + dx;
            double y = (double)standing.center.getY() + 0.5 + dy;
            double z = (double)standing.center.getZ() + 0.5 + dz;
            pos.set((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
            if (!world.getBlockState((BlockPos)pos).isAir()) continue;
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)ParticleTypes.DRIPPING_WATER, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    private static void spawnLayerRing(ClientWorld world, Cage building, int dy) {
        double radius = Math.sqrt(Math.max(0.0, 81.0 - (double)dy * (double)dy));
        if (radius < 0.5) {
            return;
        }
        double y = (double)building.center.getY() + 0.5 + (double)dy;
        for (int i = 0; i < 20; ++i) {
            double angle = (double)i * 0.3141592653589793 + world.random.nextDouble() * 0.15;
            double x = (double)building.center.getX() + 0.5 + Math.cos(angle) * radius;
            double z = (double)building.center.getZ() + 0.5 + Math.sin(angle) * radius;
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)new DustParticleEffect(5947135, 1.6f), x, y, z, 0.0, 0.0, 0.0);
            if (i % 4 != 0) continue;
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)ParticleTypes.SPLASH, x, y, z, 0.0, 0.05, 0.0);
        }
    }

    private static void spawnMotes(ClientWorld world, Cage standing) {
        for (int i = 0; i < 4; ++i) {
            double dz;
            double dy;
            double dx = (world.random.nextDouble() * 2.0 - 1.0) * 9.0;
            if (dx * dx + (dy = (world.random.nextDouble() * 2.0 - 1.0) * 9.0) * dy + (dz = (world.random.nextDouble() * 2.0 - 1.0) * 9.0) * dz > 81.0) continue;
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)new DustParticleEffect(5947135, 1.1f), (double)standing.center.getX() + 0.5 + dx, (double)standing.center.getY() + 0.5 + dy, (double)standing.center.getZ() + 0.5 + dz, 0.0, 0.02, 0.0);
        }
    }

    private static void holdWalls(ClientWorld world, Cage standing) {
        standing.blocks.forEach((pos, swap) -> {
            if (world.getBlockState(pos) != swap.after()) {
                world.setBlockState(pos, swap.after(), 19);
            }
        });
    }

    private static void tickCooldownCountdown() {
        long left = readyAtTick - ClientClock.ticks();
        if (left <= 0L) {
            lastShownSecond = -1;
            return;
        }
        int seconds = (int)Math.ceil((double)left / 20.0);
        if (seconds == lastShownSecond) {
            return;
        }
        lastShownSecond = seconds;
        Titles.actionBar(LegacyText.parse("&fStrza\u0142 z &3Hydroklatki &fza: &b" + seconds + "s"));
    }

    private static void createCage(MinecraftClient client, ClientWorld world, BlockPos center) {
        HydroCage.buildCage(client, world, center);
        Titles.playSound(SoundEvents.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.0f);
    }

    private static void buildCage(MinecraftClient client, ClientWorld world, BlockPos center) {
        if (cage != null) {
            HydroCage.removeCage(client, world);
        }
        cage = new Cage(center);
        HydroCage.cage.expiresAtTick = ClientClock.ticks() + 300L;
    }

    private static void buildNextLayers(ClientWorld world, Cage building) {
        int from = building.buildTicks * 19 / 40;
        int to = (building.buildTicks + 1) * 19 / 40;
        Box playerSpace = HydroCage.playerSpace();
        for (int layer = from; layer < to; ++layer) {
            int dy = 9 - layer;
            HydroCage.placeLayer(world, building, dy, playerSpace);
            HydroCage.spawnLayerRing(world, building, dy);
        }
        ++building.buildTicks;
    }

    private static Box playerSpace() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player == null ? null : player.getBoundingBox().expand(0.1);
    }

    private static void placeLayer(ClientWorld world, Cage building, int dy, Box playerSpace) {
        int inner = 8;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -9; dx <= 9; ++dx) {
            for (int dz = -9; dz <= 9; ++dz) {
                boolean onShell;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance > 9.0) continue;
                pos.set(building.center.getX() + dx, building.center.getY() + dy, building.center.getZ() + dz);
                if (world.isOutOfHeightLimit(pos.getY())) continue;
                BlockState was = world.getBlockState((BlockPos)pos);
                boolean bl = onShell = distance > (double)inner;
                if (playerSpace != null && playerSpace.intersects(new Box((BlockPos)pos)) && !was.isFullCube((BlockView)world, (BlockPos)pos) || was.isAir() && !onShell) continue;
                BlockState becomes = onShell ? HydroCage.borderBlock() : HydroCage.cageBlock(was);
                BlockPos immutable = pos.toImmutable();
                building.blocks.put(immutable, new Swap(was, becomes));
                world.setBlockState(immutable, becomes, 19);
            }
        }
    }

    private static BlockState borderBlock() {
        return Blocks.BLUE_GLAZED_TERRACOTTA.getDefaultState();
    }

    private static BlockState cageBlock(BlockState was) {
        if (was.isIn(BlockTags.LEAVES)) {
            return Blocks.BLUE_TERRACOTTA.getDefaultState();
        }
        if (was.isIn(BlockTags.LOGS)) {
            return Blocks.BUBBLE_CORAL_BLOCK.getDefaultState();
        }
        if (was.isIn(BlockTags.PLANKS)) {
            return Blocks.PRISMARINE_BRICKS.getDefaultState();
        }
        if (was.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
            return Blocks.PRISMARINE.getDefaultState();
        }
        return Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState();
    }

    private static void removeCage(MinecraftClient client, ClientWorld world) {
        if (cage == null) {
            return;
        }
        HydroCage.cage.blocks.forEach((pos, swap) -> { if (world.getBlockState(pos).equals(swap.after())) world.setBlockState(pos, swap.before(), 19); });
        cage = null;
        HydroCage.hideBossBar(client);
    }

    public static boolean isCageBlock(BlockPos pos) {
        return cage != null && HydroCage.cage.blocks.containsKey(pos);
    }

    public static boolean isInsideCage(BlockPos pos) {
        return cage != null && HydroCage.cage.center.isWithinDistance((Vec3i)pos, 9.0);
    }

    public static void warnBreak(ClientPlayerEntity player) {
        Text message = LegacyText.parse("&cNie mo\u017cesz zniszczy\u0107 granicy podwodnej klatki!");
        Titles.show((Text)Text.empty(), message);
        if (player != null) {
            player.sendMessage(message, false);
        }
    }

    public static void warnPlace() {
        Titles.show((Text)Text.empty(), LegacyText.parse("&cNie mo\u017cesz stawia\u0107 blok\u00f3w w Hydro Klatce!"));
    }

    private static void updateBossBar(MinecraftClient client, float percent) {
        ClientPlayerEntity player = client.player;
        Map<UUID, ClientBossBar> bars = HydroCage.bossBars(client);
        if (player == null || bars == null) {
            return;
        }
        if (!HydroCage.cage.center.isWithinDistance((Vec3i)player.getBlockPos(), 13.0)) {
            bars.remove(BOSSBAR_ID);
            return;
        }
        ClientBossBar bar = bars.get(BOSSBAR_ID);
        if (bar == null) {
            bar = new ClientBossBar(BOSSBAR_ID, LegacyText.parse("&bHydro Klatka"), percent, BossBar.Color.BLUE, BossBar.Style.PROGRESS, false, false, false);
            bars.put(BOSSBAR_ID, bar);
        }
        bar.setPercent(Math.max(0.0f, Math.min(1.0f, percent)));
    }

    private static void hideBossBar(MinecraftClient client) {
        Map<UUID, ClientBossBar> bars = HydroCage.bossBars(client);
        if (bars != null) {
            bars.remove(BOSSBAR_ID);
        }
    }

    private static Map<UUID, ClientBossBar> bossBars(MinecraftClient client) {
        if (client.inGameHud == null) {
            return null;
        }
        return ((BossBarHudAccessor)client.inGameHud.getBossBarHud()).easyeventowka$getBossBars();
    }

    static {
        nextVisualId = -1000;
        lastShownSecond = -1;
    }

    private static final class Projectile {
        Vec3d pos;
        Vec3d velocity;
        int ticks;
        FireballEntity visual;

        Projectile(Vec3d pos, Vec3d velocity) {
            this.pos = pos;
            this.velocity = velocity;
        }

        void discardVisual() {
            if (this.visual != null) {
                this.visual.discard();
                this.visual = null;
            }
        }
    }

    private static final class Cage {
        final BlockPos center;
        final Map<BlockPos, Swap> blocks = new LinkedHashMap<BlockPos, Swap>();
        long expiresAtTick;
        int buildTicks;

        Cage(BlockPos center) {
            this.center = center;
        }

        boolean isBuilding() {
            return this.buildTicks < 40;
        }
    }

    private record Swap(BlockState before, BlockState after) {
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { if (projectile != null && projectile.visual != null) projectile.visual.discard(); projectile = null; if (client.world != null) removeCage(client, client.world); cage = null; hideBossBar(client); readyAtTick = 0; }
}
