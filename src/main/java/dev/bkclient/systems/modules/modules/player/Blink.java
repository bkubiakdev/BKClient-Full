package dev.bkclient.systems.modules.modules.player;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.network.SendPacketEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.network.FakeLagBatch;
import dev.bkclient.utility.time.Timer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;

/** Nemesis Module0405 Classic cycle adapted to BK events and Island status. */
@ModuleInfo(name = "FakeLag", category = ModuleCategory.MOVEMENT)
public class Blink extends BaseModule {
   // Preserve the Java identity and saved delay/bind/Island configuration keys.
   private final SliderSetting delay = new SliderSetting(this, "FakeLag delay (ms)").min(50).max(400).step(10).currentValue(200);
   private final BooleanSetting display = new BooleanSetting(this, "modules.settings.blink.display");
   private final BooleanSetting svo = new BooleanSetting(this, "modules.settings.blink.hide_first_person", () -> !display.isEnabled());
   private final BooleanSetting pulse = new BooleanSetting(this, "modules.settings.blink.pulse");
   private final SliderSetting time = new SliderSetting(this, "modules.settings.blink.time", () -> true).min(1).max(40).step(1).currentValue(12);
   private final BooleanSetting adaptiveDelay = new BooleanSetting(this, "modules.settings.blink.adaptive_delay");
   private final BooleanSetting onlyMovement = new BooleanSetting(this, "modules.settings.blink.only_movement");
   private final BooleanSetting onlyGround = new BooleanSetting(this, "modules.settings.blink.only_ground");


   private final BooleanSetting releaseOnInteract = new BooleanSetting(this,"Release on interact").enabled(true);
   private final BooleanSetting releaseOnKnockback = new BooleanSetting(this,"Release on knockback").enabled(true);
   private final FakeLagBatch<Packet<?>> packets = new FakeLagBatch<>(8192);
   private final Timer timer = new Timer();
   private ClientWorld queuedWorld;
   private ClientPlayNetworkHandler queuedConnection;
   private boolean replaying;
   private long correctionAt;
   private boolean correctionPause;

   public Blink() {
      pulse.hideControl(); adaptiveDelay.hideControl(); onlyMovement.hideControl(); onlyGround.hideControl();
      display.hideControl(); svo.hideControl();
   }
   public static double holdMillisFor(double delay) { return FakeLagBatch.holdMillisFor(delay); }
   public synchronized double getHoldMillis() { return packets.isEmpty() ? holdMillisFor(delay.getCurrentValue()) : packets.holdMillis(); }
   public synchronized float getRemainingFraction() { return packets.remainingFraction(System.nanoTime()); }
   public Timer getTimer() { return timer; }

   public static boolean isReleaseOnInteractPacket(Class<?> type) {
      return PlayerInteractEntityC2SPacket.class.isAssignableFrom(type)
         || UpdateSelectedSlotC2SPacket.class.isAssignableFrom(type)
         || HandSwingC2SPacket.class.isAssignableFrom(type)
         || PlayerInteractBlockC2SPacket.class.isAssignableFrom(type)
         || PlayerInteractItemC2SPacket.class.isAssignableFrom(type)
         || ClickSlotC2SPacket.class.isAssignableFrom(type);
   }
   public static boolean isTimingReply(Class<?> type) {
      return net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket.class.isAssignableFrom(type)
         || net.minecraft.network.packet.c2s.common.CommonPongC2SPacket.class.isAssignableFrom(type);
   }
   private boolean sameSession() {
      return mc.player != null && queuedWorld != null && mc.world == queuedWorld
         && queuedConnection != null && mc.getNetworkHandler() == queuedConnection;
   }
   private final EventListener<SendPacketEvent> sendListener = this::savePacket;
   public synchronized void savePacket(SendPacketEvent event) {
      if (replaying || !isEnabled() || event.isCancelled()) return;
      if (!sameSession()) { clearSession(); queuedWorld=mc.world; queuedConnection=mc.getNetworkHandler(); }
      if (!sameSession() || mc.isInSingleplayer()) return;
      long now=System.nanoTime();
      if (correctionPause) {
         if (now-correctionAt < 500_000_000L) return;
         correctionPause=false;
      }
      // Classic owns outgoing traffic together, including timing replies, so movement
      // and its acknowledgements cannot overtake each other as with a movement-only queue.
      if (event.getPacket() instanceof TeleportConfirmC2SPacket) { releaseAll(); return; }
      if (releaseOnInteract.isEnabled() && isReleaseOnInteractPacket(event.getPacket().getClass())) {
         releaseAll(); return;
      }
      releaseDue(now);
      if (packets.isFull()) releaseAll();
      if (packets.isEmpty()) timer.reset();
      packets.add(event.getPacket(),now,delay.getCurrentValue());
      event.cancel();
   }
   private final EventListener<ClientPlayerTickEvent> tickListener = event -> {
      synchronized (this) {
         if (!sameSession()) { clearSession(); return; }
         releaseDue(System.nanoTime());
      }
   };
   private final EventListener<ReceivePacketEvent> receiveListener = event -> {
      synchronized (this) {
         if (!isEnabled() || !sameSession()) return;
         Packet<?> packet=event.getPacket();
         if (packet instanceof PlayerPositionLookS2CPacket || packet instanceof PlayerRespawnS2CPacket
               || packet instanceof net.minecraft.network.packet.s2c.common.DisconnectS2CPacket) {
            // Never replay pre-teleport positions after an authoritative reset.
            var pending=packets.drain();
            if (!(packet instanceof net.minecraft.network.packet.s2c.common.DisconnectS2CPacket))
               send(pending.stream().filter(p -> isTimingReply(p.getClass())).toList());
            timer.reset(); correctionAt=System.nanoTime(); correctionPause=true;
         } else if (releaseOnKnockback.isEnabled() && (packet instanceof ExplosionS2CPacket
               || packet instanceof EntityVelocityUpdateS2CPacket velocity && velocity.getEntityId()==mc.player.getId())) {
            releaseAll();
         }
      }
   };
   private final EventListener<WorldChangeEvent> world = event -> { onWorldChanged(); disable(); };
   private void releaseDue(long now) {
      send(packets.drainBatchDue(now));
   }
   private void releaseAll() { send(packets.drain()); }
   private void send(java.util.List<Packet<?>> batch) {
      if (batch.isEmpty()) return;
      if (!sameSession()) { clearSession(); return; }
      replaying=true;
      try { for (Packet<?> packet:batch) queuedConnection.sendPacket(packet); }
      finally { replaying=false; timer.reset(); }
   }
   private void clearSession() {
      packets.clear(); timer.reset(); queuedWorld=null; queuedConnection=null; correctionPause=false;
   }
   public synchronized void onWorldChanged() { clearSession(); }
   @Override public synchronized void onEnable() {
      clearSession(); queuedWorld=mc.world; queuedConnection=mc.getNetworkHandler();
   }
   @Override public synchronized void onDisable() {
      try { if (sameSession()) releaseAll(); } finally { clearSession(); }
   }
}
