package dev.bkclient.larp.feature;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeBossBar;
import dev.bkclient.larp.feature.LegacyText;

public final class ArcusMagnus {
    private static final double[] DAMAGE = new double[]{0.5, 1.0, 2.0, 4.0, 8.0};
    private static final int COMBO_TICKS = 80;
    private static final double RANGE = 48.0;
    private static final FakeBossBar BOSS_BAR = new FakeBossBar("arcus_magnus", BossBar.Color.RED, BossBar.Style.PROGRESS);
    private static int stage;
    private static long comboEndsAtTick;
    private static final Map<UUID, Integer> lastHurt;

    private ArcusMagnus() {
    }

    public static void tick(MinecraftClient client) {
        ClientWorld NarrationMessageBuilder;
        ClientPlayerEntity player = client.player;
        if (player == null || !((NarrationMessageBuilder = client.world) instanceof ClientWorld)) {
            lastHurt.clear();
            ArcusMagnus.reset();
            return;
        }
        ClientWorld world = NarrationMessageBuilder;
        boolean holding = Disguise.hasAbility(player.getMainHandStack(), Disguise.Ability.ARCUS_MAGNUS);
        ArcusMagnus.watchHits(world, player, holding);
        if (comboEndsAtTick == 0L) {
            return;
        }
        long left = comboEndsAtTick - ClientClock.ticks();
        if (left <= 0L) {
            ArcusMagnus.reset();
            return;
        }
        ArcusMagnus.showBar(left);
    }

    private static void watchHits(ClientWorld world, ClientPlayerEntity player, boolean holding) {
        for (PlayerEntity target : world.getPlayers()) {
            if (target == player) continue;
            UUID id = target.getUuid();
            int previous = lastHurt.getOrDefault(id, 0);
            lastHurt.put(id, target.hurtTime);
            if (target.hurtTime <= previous || !holding || target.squaredDistanceTo((Entity)player) > 2304.0) continue;
            ArcusMagnus.onHit(world, target);
        }
    }

    private static void onHit(ClientWorld world, PlayerEntity target) {
        boolean comboRunning = comboEndsAtTick > 0L && ClientClock.ticks() < comboEndsAtTick;
        stage = comboRunning ? Math.min(stage + 1, DAMAGE.length - 1) : 0;
        comboEndsAtTick = ClientClock.ticks() + 80L;
        ArcusMagnus.spawnHearts(world, target, DAMAGE[stage]);
    }

    private static void spawnHearts(ClientWorld world, PlayerEntity target, double hearts) {
        int count = Math.max(1, (int)Math.round(hearts * 2.0));
        for (int i = 0; i < count; ++i) {
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)ParticleTypes.DAMAGE_INDICATOR, target.getX() + (world.random.nextDouble() - 0.5) * (double)target.getWidth(), target.getBodyY(0.5) + (world.random.nextDouble() - 0.5) * 0.6, target.getZ() + (world.random.nextDouble() - 0.5) * (double)target.getWidth(), (world.random.nextDouble() - 0.5) * 0.3, 0.1 + world.random.nextDouble() * 0.2, (world.random.nextDouble() - 0.5) * 0.3);
        }
    }

    private static void showBar(long ticksLeft) {
        int millis = (int)(ticksLeft * 50L);
        String damage = ArcusMagnus.trim(DAMAGE[Math.min(stage + 1, DAMAGE.length - 1)]);
        BOSS_BAR.show(LegacyText.parse("&aArcus Magnus &7ma aktywne &eCOMBO&7! Traf przeciwnika w czasie &a" + millis + "ms &7aby zada\u0107 mu &c" + damage + "\u2764"), (float)ticksLeft / 80.0f);
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((int)value) : String.valueOf(value);
    }

    private static void reset() {
        stage = 0;
        comboEndsAtTick = 0L;
        BOSS_BAR.hide();
    }

    static {
        lastHurt = new HashMap<UUID, Integer>();
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { reset(); lastHurt.clear(); }
}
