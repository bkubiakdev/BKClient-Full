package dev.bkclient.combatengine.bkengine;

import dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura;
import dev.bkclient.combatengine.bkengine.utils.aura.AttackUtil;
import dev.bkclient.systems.event.impl.network.SendPacketEvent;
import dev.bkclient.systems.modules.modules.combat.backtrack.PacketDelayQueue;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.Packet;
import java.util.concurrent.ThreadLocalRandom;

/** A bounded, game-thread-owned movement batch. Interactions always flush it first. */
public final class WebLagController implements IMinecraft {
    public static final WebLagController INSTANCE = new WebLagController();
    private final PacketDelayQueue<Packet<?>> queue = new PacketDelayQueue<>();
    private ClientPlayNetworkHandler connection;
    private ClientWorld world;
    private long delayMs;
    private long nextDiagnostic;
    private int resumeAge;
    private boolean replaying;
    private volatile boolean corrected;

    public static boolean criticalMode() {
        return AttackAura.get().isEnable() && AttackAura.get().webLag.isState()
            && mc.player != null && mc.world != null
            && AttackUtil.isPlayerInBlock(net.minecraft.block.Blocks.COBWEB);
    }
    private boolean active() {
        var aura = AttackAura.get();
        return criticalMode() && mc.player.isAlive() && !mc.player.isOnGround()
            && mc.player.getVelocity().y < 0 && !mc.player.hasVehicle()
            && !mc.player.isTouchingWater() && !mc.player.isGliding()
            && !mc.player.getAbilities().flying && !aura.isAttackPaused()
            && aura.getTarget() != null && aura.getTarget().isAlive()
            && mc.player.distanceTo(aura.getTarget()) <= aura.attackRange.get() + aura.preRange.get();
    }
    private long now() { return System.nanoTime() / 1_000_000L; }
    private void validateContext() {
        if (corrected || world != mc.world || connection != mc.getNetworkHandler()) {
            discard();
            corrected = false;
            world = mc.world;
            connection = mc.getNetworkHandler();
            resumeAge = mc.player == null ? 0 : mc.player.age + 1;
        }
    }
    public void correction() { corrected = true; }
    public void tick() {
        validateContext();
        if (!active() || (queue.size() > 0 && queue.oldestAge(now()) >= delayMs)) flush();
    }
    public void send(SendPacketEvent event, float minimum, float maximum) {
        if (!mc.isOnThread() || replaying || event.isCancelled()) return;
        validateContext();
        Packet<?> packet = event.getPacket();
        boolean movement = packet instanceof PlayerMoveC2SPacket;
        boolean movementBoundary = packet instanceof ClientTickEndC2SPacket || packet instanceof PlayerInputC2SPacket;
        if (!movement && !movementBoundary) {
            // Never let attacks, slot/sprint changes or interactions overtake old movement.
            flush();
            return;
        }
        tick();
        if (!active() || mc.player.age <= resumeAge) return;
        // A 1.21.4 tick-end must not overtake the movement it terminates.
        if (!movement && queue.size() == 0) return;
        if (queue.size() >= 20) { flush(); return; }
        if (queue.size() == 0) {
            int low = Math.clamp((int)Math.min(minimum, maximum), 0, 500);
            int high = Math.clamp((int)Math.max(minimum, maximum), low, 500);
            delayMs = ThreadLocalRandom.current().nextInt(low, high + 1);
            if (delayMs == 0) return;
        }
        queue.add(packet, now());
        event.cancel();
    }
    public boolean readyToAttack() {
        tick();
        return queue.size() == 0;
    }
    public void flush() {
        if (queue.size() == 0) return;
        if (corrected || connection == null || connection != mc.getNetworkHandler() || world != mc.world) {
            discard(); return;
        }
        long timestamp = now();
        if (timestamp >= nextDiagnostic) {
            nextDiagnostic = timestamp + 2000;
            org.slf4j.LoggerFactory.getLogger("BKClient-WebLag").info(
                "Web Lag release: packets={} heldMs={} selectedDelayMs={}", queue.size(), queue.oldestAge(timestamp), delayMs);
        }
        replaying = true;
        try { queue.drain(now(), 0, true, packet -> connection.sendPacket(packet)); }
        finally {
            replaying = false;
            resumeAge = mc.player == null ? 0 : mc.player.age;
        }
    }
    public void discard() { queue.clear(); }
}
