package dev.bkclient.larp.feature;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class PhantomBlock {
    private static final int COOLDOWN_TICKS = 6000;
    private static final int EFFECT_TICKS = 150;
    private static long readyAtTick;
    private static long effectEndsAtTick;

    private PhantomBlock() {
    }

    public static boolean onUse(ItemStack stack, BlockPos at) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return true;
        }
        if (ClientClock.ticks() < readyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(readyAtTick - ClientClock.ticks()) / 20.0));
            return true;
        }
        readyAtTick = ClientClock.ticks() + 6000L;
        effectEndsAtTick = ClientClock.ticks() + 150L;
        dev.bkclient.larp.LarpCooldown.set(stack, 6000);
        ClientWorld NarrationMessageBuilder = client.world;
        if (NarrationMessageBuilder instanceof ClientWorld) {
            ClientWorld world = NarrationMessageBuilder;
            PhantomBlock.burst(world, at);
        }
        Titles.playSound(SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.9f, 1.25f);
        Titles.show((Text)Text.empty(), LegacyText.parse("&cPostawi\u0142e\u015b blok widmo"));
        return true;
    }

    private static void burst(ClientWorld world, BlockPos at) {
        double x = (double)at.getX() + 0.5;
        double y = (double)at.getY() + 1.0;
        double z = (double)at.getZ() + 0.5;
        if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0.0, 0.0, 0.0);
        for (int i = 0; i < 30; ++i) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double speed = 0.1 + world.random.nextDouble() * 0.25;
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)ParticleTypes.SCULK_SOUL, x, y, z, Math.cos(angle) * speed, world.random.nextDouble() * 0.3, Math.sin(angle) * speed);
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)ParticleTypes.LARGE_SMOKE, x, y, z, Math.cos(angle) * speed * 0.5, world.random.nextDouble() * 0.15, Math.sin(angle) * speed * 0.5);
        }
    }

    public static void tick(MinecraftClient client) {
        if (effectEndsAtTick == 0L || ClientClock.ticks() < effectEndsAtTick) {
            return;
        }
        effectEndsAtTick = 0L;
        Titles.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.8f);
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { readyAtTick = effectEndsAtTick = 0; }
}
