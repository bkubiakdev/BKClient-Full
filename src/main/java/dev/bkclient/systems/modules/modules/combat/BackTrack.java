package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.InternalAttackEvent;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.modules.modules.combat.backtrack.PacketDelayQueue;
import dev.bkclient.systems.modules.modules.combat.backtrack.PacketHoldCycle;
import dev.bkclient.systems.modules.modules.combat.backtrack.BacktrackTrigger;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.TrackedPosition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Holds target movement in timed batches, then replays each batch in order. */
@ModuleInfo(name = "Back Track", desc = "Holds target movement briefly before replaying updates", category = ModuleCategory.COMBAT)
public class BackTrack extends BaseModule {
   private final dev.bkclient.systems.setting.settings.ModeSetting mode = new dev.bkclient.systems.setting.settings.ModeSetting(this, "Mode");
   private final dev.bkclient.systems.setting.settings.ModeSetting.Value distanceMode = new dev.bkclient.systems.setting.settings.ModeSetting.Value(mode, "Distance").select();
   private final dev.bkclient.systems.setting.settings.ModeSetting.Value hitMode = new dev.bkclient.systems.setting.settings.ModeSetting.Value(mode, "Hit");
   private final dev.bkclient.systems.modules.modules.combat.backtrack.BacktrackTrigger trigger = new dev.bkclient.systems.modules.modules.combat.backtrack.BacktrackTrigger();
   private final SliderSetting nemesisDelay = new SliderSetting(this, "Nemesis Delay")
         .min(0).max(12000).step(50).currentValue(200).suffix(ms -> " ms");
   private final SliderSetting nemesisRange = new SliderSetting(this, "Range")
         .min(0).max(10).step(0.1F).currentValue(6);
   private final SliderSetting releaseWindow = new SliderSetting(this, "Release Window")
         .min(0).max(500).step(50).currentValue(100).suffix(ms -> " ms");
   private final BooleanSetting visualize = new BooleanSetting(this, "Visualize Positions").enabled(true);
   private final BooleanSetting debug = new BooleanSetting(this, "Debug").enabled(true);
   private final PacketHoldCycle cycle = new PacketHoldCycle();
   private long nextDebug;
   private long peakAgeNanos;
   private long receivedCount, queuedCount, releasedCount, attackCount;
   private String holdReason = "waiting for target";
   private record BufferedPacket(Packet<?> packet, Vec3d position) {}
   private final PacketDelayQueue<BufferedPacket> queue = new PacketDelayQueue<>();
   private Vec3d appliedPosition = Vec3d.ZERO;
   private final TrackedPosition tracked = new TrackedPosition();
   private PlayerEntity target;
   private ClientWorld world;
   private ClientPlayNetworkHandler handler;

   private final EventListener<InternalAttackEvent> onAttack = event -> {
      if (!cycle.isActive() || !isEnabled()) return;
      syncTrigger();
      if (!event.isCancelled() && hitMode.isSelected() && event.getEntity() instanceof PlayerEntity player && valid(player)) {
         Vec3d position = target == player && world == mc.world && handler == mc.getNetworkHandler()
               ? tracked.getPos() : player.getTrackedPosition().getPos();
         if (!inRange(player, position)) return;
         select(player);
         trigger.attack();
         attackCount++;
      }
   };

   private final EventListener<ReceivePacketEvent> onReceive = event -> {
      long generation = cycle.generation();
      if (!cycle.accepts(generation) || !isEnabled() || event.isCancelled() || !relevant(event.getPacket())) return;
      Packet<?> packet = event.getPacket();
      // handlePacket can run on Netty before vanilla schedules the packet. Own only
      // relevant packets and process them once on the game thread, in arrival order.
      if (!mc.isOnThread()) {
         var connection = mc.getNetworkHandler();
         var level = mc.world;
         event.cancel();
         mc.execute(() -> {
            if (connection == null || connection != mc.getNetworkHandler() || level != mc.world) return;
            if (!cycle.accepts(generation) || !isEnabled() || !process(packet)) apply(packet, connection);
         });
      } else if (process(packet)) event.cancel();
   };

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (!cycle.isActive() || !isEnabled()) return;
      tickNemesis();
   };

   private final EventListener<dev.bkclient.systems.event.impl.render.Render3DEvent> onRender = event -> {
      if (!isEnabled() || !visualize.isEnabled() || target == null || mc.player == null
            || world != mc.world || !valid(target)) return;
      if (target instanceof net.minecraft.client.network.AbstractClientPlayerEntity player) {
         dev.bkclient.utility.render.BacktrackSilhouette.render(event, player, tracked.getPos());
      }
   };

   private boolean relevant(Packet<?> p) {
      return dev.bkclient.systems.modules.modules.combat.backtrack.NemesisPacketPolicy.shouldDelay(p.getClass())
            || p instanceof net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
            || p instanceof HealthUpdateS2CPacket || p instanceof PlayerPositionLookS2CPacket
            || p instanceof PlayerRespawnS2CPacket;
   }

   private boolean process(Packet<?> packet) {
      return cycle.isActive() && isEnabled() && processNemesis(packet);
   }

   private double nemesisDistance(PlayerEntity entity, Vec3d position) {
      Box box = entity.getBoundingBox().offset(position.subtract(entity.getPos()));
      Vec3d feet = mc.player.getPos();
      return feet.distanceTo(new Vec3d(MathHelper.clamp(feet.x, box.minX, box.maxX),
            MathHelper.clamp(feet.y, box.minY, box.maxY), MathHelper.clamp(feet.z, box.minZ, box.maxZ)));
   }

   private boolean inRange(PlayerEntity player, Vec3d position) {
      return BacktrackTrigger.inRange(nemesisDistance(player, position), nemesisRange.getCurrentValue());
   }

   private void tickNemesis() {
      syncTrigger();
      if (world != null && (world != mc.world || handler != mc.getNetworkHandler())) reset(false);
      if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) { reset(false); return; }
      if (world == null) { world = mc.world; handler = mc.getNetworkHandler(); }
      if (target != null && (!valid(target) || !inRange(target, tracked.getPos()))) reset(true);
      if (distanceMode.isSelected() && target == null) {
         PlayerEntity nearest = null;
         double best = nemesisRange.getCurrentValue();
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (!valid(player) || player.isSpectator()) continue;
            double distance = nemesisDistance(player, player.getPos());
            if (distance <= best) { nearest = player; best = distance; }
         }
         if (nearest != null) select(nearest);
      }
      releaseDue(System.nanoTime());
      reportDebug();
   }

   private boolean processNemesis(Packet<?> packet) {
      syncTrigger();
      if (world != null && (world != mc.world || handler != mc.getNetworkHandler())) reset(false);
      if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
      if (world == null) { world = mc.world; handler = mc.getNetworkHandler(); }
      if (packet instanceof PlayerRespawnS2CPacket) { reset(false); return false; }
      if (packet instanceof PlayerPositionLookS2CPacket
            || packet instanceof net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
            || packet instanceof HealthUpdateS2CPacket health && health.getHealth() <= 0) {
         reset(true); return false;
      }
      if (!dev.bkclient.systems.modules.modules.combat.backtrack.NemesisPacketPolicy.shouldDelay(packet.getClass())) return false;
      Vec3d targetPosition = null;
      if (target != null) {
         if (packet instanceof EntityS2CPacket move && move.getEntity(mc.world) == target && move.isPositionChanged()) {
            tracked.setPos(tracked.withDelta(move.getDeltaX(), move.getDeltaY(), move.getDeltaZ()));
            targetPosition = tracked.getPos();
         } else if (packet instanceof EntityPositionSyncS2CPacket pos && pos.id() == target.getId()) {
            tracked.setPos(pos.values().position()); targetPosition = tracked.getPos();
         } else if (packet instanceof EntityPositionS2CPacket pos && pos.entityId() == target.getId()) {
            var change = pos.change().position(); var previous = tracked.getPos();
            tracked.setPos(new Vec3d(change.x + (pos.relatives().contains(PositionFlag.X) ? previous.x : 0),
                  change.y + (pos.relatives().contains(PositionFlag.Y) ? previous.y : 0),
                  change.z + (pos.relatives().contains(PositionFlag.Z) ? previous.z : 0)));
            targetPosition = tracked.getPos();
         }
      }
      receivedCount++;
      long now = System.nanoTime();
      if (target != null && (!valid(target) || !inRange(target, tracked.getPos()))) {
         reset(true);
         return false;
      }
      if (!trigger.canHold(target != null) || delayNanos() == 0) {
         queue.drain(now, 0, true, this::applyCurrent);
         if (targetPosition != null) appliedPosition = targetPosition;
         holdReason = hitMode.isSelected() ? "waiting for attack" : "waiting for target in range";
         return false;
      }
      if (!cycle.canHold(now)) {
         if (targetPosition != null) appliedPosition = targetPosition;
         holdReason = "release window / packets passing";
         return false;
      }
      queue.add(new BufferedPacket(packet, targetPosition), now); queuedCount++;
      holdReason = "holding incoming batch";
      // A global queue needs a larger bound than the target-only queue.
      if (queue.size() >= 16384) {
         queue.drain(now, 0, true, this::applyCurrent);
         beginReleaseWindow();
      } else releaseDue(now);
      return true;
   }

   private void reportDebug() {
      long now = System.nanoTime();
      if (!debug.isEnabled() || mc.player == null || now < nextDebug) return;
      nextDebug = now + 3_000_000_000L;
      String position = target == null || world != mc.world ? "none" : String.format(java.util.Locale.ROOT,
            "%s shown=%.2f real=%.2f visualGap=%.2f bufferedMove=%.2f", target.getName().getString(),
            distance(target.getPos()), distance(tracked.getPos()), target.getPos().distanceTo(tracked.getPos()),
            appliedPosition.distanceTo(tracked.getPos()));
      String message = String.format(java.util.Locale.ROOT,
            "[BackTrack] mode=%s target=%s | recv=%d queued=%d released=%d pending=%d attacks=%d age=%dms peak=%dms delay=%dms | %s",
            mode.getValue().getName(), position, receivedCount, queuedCount, releasedCount, queue.size(), attackCount,
            queue.oldestAge(now) / 1_000_000L, peakAgeNanos / 1_000_000L, delayNanos() / 1_000_000L, holdReason);
      BKClientInit.LOGGER.info(message);
      peakAgeNanos = queue.oldestAge(now);
   }

   private void releaseDue(long now) {
      peakAgeNanos = Math.max(peakAgeNanos, queue.oldestAge(now));
      boolean pending = queue.size() > 0;
      queue.drainBatch(now, delayNanos(), this::applyCurrent);
      if (pending && queue.size() == 0) beginReleaseWindow();
   }

   private void beginReleaseWindow() {
      cycle.released(System.nanoTime(), (long)MathHelper.clamp(releaseWindow.getCurrentValue(), 0, 500) * 1_000_000L);
      holdReason = "release window / packets passing";
   }

   private long delayNanos() {
      return (long)MathHelper.clamp(nemesisDelay.getCurrentValue(), 0, 12000) * 1_000_000L;
   }

   private boolean valid(PlayerEntity player) {
      return player != null && mc.player != null && mc.player.isAlive() && player != mc.player && player.isAlive()
            && !player.isRemoved() && player.getWorld() == mc.world
            && !BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString());
   }

   private double distance(Vec3d position) {
      Box box = target.getBoundingBox().offset(position.subtract(target.getPos()));
      Vec3d eye = mc.player.getEyePos();
      return eye.distanceTo(new Vec3d(MathHelper.clamp(eye.x, box.minX, box.maxX),
            MathHelper.clamp(eye.y, box.minY, box.maxY), MathHelper.clamp(eye.z, box.minZ, box.maxZ)));
   }

   private void select(PlayerEntity player) {
      if (target == player && world == mc.world && handler == mc.getNetworkHandler()) return;
      reset(true);
      target = player;
      world = mc.world;
      handler = mc.getNetworkHandler();
      tracked.setPos(player.getTrackedPosition().getPos());
      appliedPosition = tracked.getPos();
   }

   private void reset(boolean release) {
      try {
         if (release && world == mc.world && handler == mc.getNetworkHandler())
            queue.drain(System.nanoTime(), 0, true, this::applyCurrent);
      } finally {
         queue.clear(); target = null; world = null; handler = null;
         trigger.reset();
         holdReason = release ? "target reset / flushed" : "state cleared";
      }
   }

   private void syncTrigger() {
      if (trigger.setHitMode(hitMode.isSelected())) reset(true);
   }

   private void applyCurrent(BufferedPacket entry) {
      apply(entry.packet(), handler);
      if (entry.position() != null) appliedPosition = entry.position();
      releasedCount++;
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void apply(Packet<?> packet, ClientPlayNetworkHandler connection) {
      if (connection != null) ((Packet)packet).apply(connection);
   }

   @Override public void onEnable() {
      cycle.disable();
      reset(false); nextDebug = 0;
      receivedCount = queuedCount = releasedCount = attackCount = peakAgeNanos = 0;
      cycle.enable();
   }

   public void onWorldChanged() {
      reset(false);
      dev.bkclient.utility.render.BacktrackGhostGlow.release();
      dev.bkclient.utility.render.BacktrackSilhouette.release();
   }

   @Override public void onDisable() {
      dev.bkclient.utility.render.BacktrackGhostGlow.release();
      dev.bkclient.utility.render.BacktrackSilhouette.release();
      cycle.disable();
      reset(true);
      if (debug.isEnabled() && mc.player != null) {
         BKClientInit.LOGGER.info("[BackTrack] OFF; pending=0; packets passing");
      }
   }
}
