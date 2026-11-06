package dev.bkclient.larp.feature;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.BlockView;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.FakeBossBar;
import dev.bkclient.larp.feature.FakeEffects;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class MagicLantern {
    private static final int RADIUS = 30;
    private static final int DURATION_TICKS = 600;
    private static final int COOLDOWN_TICKS = 2400;
    private static final int DUST_COLOR = 16750280;
    private static final float DUST_SIZE = 1.5f;
    private static final float ABSORPTION_HEALTH = 10.0f;
    private static final int DUST_PER_TICK = 8;
    private static final double DUST_SPREAD = 4.5;
    private static final int TOTEM_PER_TICK = 4;
    private static final int TOTEM_BURST = 60;
    private static final double TOTEM_SPREAD = 6.0;
    private static final float AMBIENT_DUST_SIZE = 0.8f;
    private static final FakeBossBar BOSS_BAR = new FakeBossBar("magic_lantern", BossBar.Color.PINK, BossBar.Style.NOTCHED_10);
    private static final int BLOCK_FLAGS = 19;
    private static final int BREAKING_ID = -2000;
    private static long readyAtTick;
    private static long endsAtTick;
    private static BlockPos fountain;
    private static BlockState replaced;
    private static boolean wasInside;
    private static float breakProgress;
    private static long lastMiningTick;
    private static int hitSoundTicks;
    private static boolean absorbing;

    private MagicLantern() {
    }

    public static boolean onPlaced(ItemStack stack, BlockPos pos) {
        ClientWorld NarrationMessageBuilder;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || !((NarrationMessageBuilder = client.world) instanceof ClientWorld)) {
            return true;
        }
        ClientWorld world = NarrationMessageBuilder;
        if (ClientClock.ticks() < readyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(readyAtTick - ClientClock.ticks()) / 20.0));
            return true;
        }
        if (fountain != null) {
            Titles.show((Text)Text.empty(), LegacyText.parse("&cW tym miejscu jest ju\u017c aktywna &4latarnia&c!"));
            return true;
        }
        fountain = pos;
        replaced = world.getBlockState(pos);
        world.setBlockState(pos, Blocks.BEACON.getDefaultState(), 19);
        endsAtTick = ClientClock.ticks() + 600L;
        readyAtTick = ClientClock.ticks() + 2400L;
        wasInside = false;
        dev.bkclient.larp.LarpCooldown.set(stack, 2400);
        Titles.playSound(SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.6f);
        MagicLantern.totemBurst(world, pos);
        Titles.show((Text)Text.empty(), LegacyText.parse("&5Cudowna Latarnia! Aktywowana na &d30 sekund&7!"));
        return true;
    }

    public static boolean isFountain(BlockPos pos) {
        return fountain != null && fountain.equals(pos);
    }

    public static void onBroken() {
        MagicLantern.stop(LegacyText.parse("&cFontanna zosta\u0142a zniszczona!"), (SoundEvent)SoundEvents.ENTITY_GENERIC_EXPLODE.value());
    }

    public static void mine(PlayerEntity player, BlockPos pos, Direction side) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld NarrationMessageBuilder = client.world;
        if (!(NarrationMessageBuilder instanceof ClientWorld)) {
            return;
        }
        ClientWorld world = NarrationMessageBuilder;
        if (player.getAbilities().creativeMode) {
            MagicLantern.onBroken();
            return;
        }
        if (lastMiningTick == ClientClock.ticks()) {
            return;
        }
        lastMiningTick = ClientClock.ticks();
        BlockState state = world.getBlockState(pos);
        BlockSoundGroup sounds = state.getSoundGroup();
        breakProgress += state.calcBlockBreakingDelta(player, (BlockView)world, pos);
        player.swingHand(Hand.MAIN_HAND, false);
        client.particleManager.addBlockBreakingParticles(pos, side);
        if (hitSoundTicks++ % 4 == 0) {
            Titles.playSound(sounds.getHitSound(), (sounds.getVolume() + 1.0f) / 8.0f, sounds.getPitch() * 0.5f);
        }
        if (breakProgress < 1.0f) {
            world.setBlockBreakingInfo(-2000, pos, (int)(breakProgress * 10.0f) - 1);
            return;
        }
        client.particleManager.addBlockBreakParticles(pos, state);
        Titles.playSound(sounds.getBreakSound(), (sounds.getVolume() + 1.0f) / 2.0f, sounds.getPitch() * 0.8f);
        MagicLantern.stopMining(world);
        MagicLantern.onBroken();
    }

    private static void tickMining(MinecraftClient client) {
        if (lastMiningTick < 0L || ClientClock.ticks() - lastMiningTick <= 1L) {
            return;
        }
        ClientWorld NarrationMessageBuilder = client.world;
        if (NarrationMessageBuilder instanceof ClientWorld) {
            ClientWorld world = NarrationMessageBuilder;
            MagicLantern.stopMining(world);
        } else {
            breakProgress = 0.0f;
            hitSoundTicks = 0;
            lastMiningTick = -1L;
        }
    }

    private static void stopMining(ClientWorld world) {
        if (fountain != null) {
            world.setBlockBreakingInfo(-2000, fountain, -1);
        }
        breakProgress = 0.0f;
        hitSoundTicks = 0;
        lastMiningTick = -1L;
    }

    public static void tick(MinecraftClient client) {
        ClientWorld NarrationMessageBuilder;
        MagicLantern.tickMining(client);
        if (fountain == null) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null || !((NarrationMessageBuilder = client.world) instanceof ClientWorld)) {
            MagicLantern.stop(null, null);
            return;
        }
        ClientWorld world = NarrationMessageBuilder;
        if (!world.getBlockState(fountain).isOf(Blocks.BEACON)) {
            MagicLantern.stop(null, null); return;
        }
        if (ClientClock.ticks() >= endsAtTick) {
            MagicLantern.stop(null, SoundEvents.BLOCK_BEACON_DEACTIVATE);
            return;
        }
        MagicLantern.spawnParticles(world);
        MagicLantern.updateAura(player);
        long left = endsAtTick - ClientClock.ticks();
        BOSS_BAR.show(LegacyText.parse("&5Cudowna Latarnia aktywna! &8(&d" + (int)Math.ceil((double)left / 20.0) + "s&8)"), (float)left / 600.0f);
    }

    private static void updateAura(ClientPlayerEntity player) {
        boolean inside = fountain.isWithinDistance((Vec3i)player.getBlockPos(), 30.0);
        if (inside && !wasInside) {
            Titles.playSound(SoundEvents.BLOCK_BEACON_AMBIENT, 0.8f, 1.5f);
        } else if (!inside && wasInside) {
            Titles.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.8f);
            MagicLantern.clearAura();
        }
        wasInside = inside;
        if (inside) {
            FakeEffects.apply((RegistryEntry<StatusEffect>)StatusEffects.REGENERATION, 400, 4);
            FakeEffects.apply((RegistryEntry<StatusEffect>)StatusEffects.ABSORPTION, 200, 5);
            FakeEffects.apply((RegistryEntry<StatusEffect>)StatusEffects.STRENGTH, 200, 1);
            MagicLantern.giveHearts(player);
        }
    }

    private static void giveHearts(ClientPlayerEntity player) {
        EntityAttributeInstance ceiling = player.getAttributeInstance(EntityAttributes.MAX_ABSORPTION);
        if (ceiling != null && ceiling.getBaseValue() != 10.0) {
            ceiling.setBaseValue(10.0);
        }
        if (player.getAbsorptionAmount() < 10.0f) {
            player.setAbsorptionAmount(10.0f);
        }
        absorbing = true;
    }

    private static void takeHearts() {
        if (!absorbing) {
            return;
        }
        absorbing = false;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        player.setAbsorptionAmount(0.0f);
        EntityAttributeInstance ceiling = player.getAttributeInstance(EntityAttributes.MAX_ABSORPTION);
        if (ceiling != null) {
            ceiling.setBaseValue(0.0);
        }
    }

    private static void spawnParticles(ClientWorld world) {
        double spread;
        double angle;
        int i;
        double x = (double)fountain.getX() + 0.5;
        double y = (double)fountain.getY() + 1.2;
        double z = (double)fountain.getZ() + 0.5;
        if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)new DustParticleEffect(16750280, 1.5f), x, y, z, 0.0, 0.0, 0.0);
        for (i = 0; i < 8; ++i) {
            angle = world.random.nextDouble() * Math.PI * 2.0;
            spread = world.random.nextDouble() * 4.5;
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)new DustParticleEffect(16750280, 0.8f), x + Math.cos(angle) * spread, y + world.random.nextDouble() * 2.5, z + Math.sin(angle) * spread, 0.0, 0.0, 0.0);
        }
        for (i = 0; i < 4; ++i) {
            angle = world.random.nextDouble() * Math.PI * 2.0;
            spread = world.random.nextDouble() * 6.0;
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)ParticleTypes.TOTEM_OF_UNDYING, x + Math.cos(angle) * spread, y + world.random.nextDouble() * 2.5, z + Math.sin(angle) * spread, world.random.nextGaussian() * 0.05, world.random.nextDouble() * 0.1, world.random.nextGaussian() * 0.05);
        }
    }

    private static void totemBurst(ClientWorld world, BlockPos pos) {
        double x = (double)pos.getX() + 0.5;
        double y = (double)pos.getY() + 1.0;
        double z = (double)pos.getZ() + 0.5;
        for (int i = 0; i < 60; ++i) {
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)ParticleTypes.TOTEM_OF_UNDYING, x, y, z, world.random.nextGaussian() * 0.35, world.random.nextGaussian() * 0.35 + 0.2, world.random.nextGaussian() * 0.35);
        }
        if (dev.bkclient.larp.LarpRuntime.effects()) world.playSound(x, y, z, SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.7f, 1.2f, false);
    }

    private static void stop(Text message, SoundEvent sound) {
        ClientWorld NarrationMessageBuilder;
        MinecraftClient client = MinecraftClient.getInstance();
        if (fountain != null && (NarrationMessageBuilder = client.world) instanceof ClientWorld) {
            ClientWorld world = NarrationMessageBuilder;
            MagicLantern.stopMining(world);
            if (replaced != null && world.getBlockState(fountain).isOf(Blocks.BEACON)) {
                world.setBlockState(fountain, replaced, 19);
            }
        }
        fountain = null;
        replaced = null;
        wasInside = false;
        BOSS_BAR.hide();
        MagicLantern.clearAura();
        if (sound != null) {
            Titles.playSound(sound, 1.0f, sound == SoundEvents.BLOCK_BEACON_DEACTIVATE ? 1.0f : 1.5f);
        }
        if (message != null) {
            Titles.show((Text)Text.empty(), message);
        }
    }

    private static void clearAura() {
        MagicLantern.takeHearts();
        FakeEffects.remove((RegistryEntry<StatusEffect>)StatusEffects.REGENERATION);
        FakeEffects.remove((RegistryEntry<StatusEffect>)StatusEffects.ABSORPTION);
        FakeEffects.remove((RegistryEntry<StatusEffect>)StatusEffects.STRENGTH);
    }

    static {
        lastMiningTick = -1L;
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { stop(null, null); readyAtTick = 0; endsAtTick = 0; }
}
