package dev.bkclient.combatengine.bridge;

import dev.bkclient.BKClientInit;
import dev.bkclient.combatengine.AuraServices;
import dev.bkclient.combatengine.api.events.Event;
import dev.bkclient.combatengine.api.events.EventInvoker;
import dev.bkclient.combatengine.api.events.implement.*;
import dev.bkclient.combatengine.api.storages.implement.FreeLookStorage;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.client.modules.impl.combat.Aura;
import dev.bkclient.combatengine.client.modules.impl.combat.Aura2;
import dev.bkclient.combatengine.client.modules.impl.combat.AdaptiveAura;
import dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura;
import dev.bkclient.combatengine.bkengine.manager.rotation.RotationProcess;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;

/** Connects source events to the host without duplicating the combat logic. */
public final class AuraRuntime {
   private static final CopyOnWriteArrayList<SourceModuleAdapter> adapters = new CopyOnWriteArrayList<>();
   private static boolean initialized;
   private static long lastHookTime = net.minecraft.util.Util.getMeasuringTimeNano();
   private static int accumulatedCalls;
   private static net.minecraft.client.network.ClientPlayerEntity inputPlayer;
   private static float inputYaw;
   private static SourceModuleAdapter selectedAura;
   public static void captureInputYaw() {
      inputPlayer=MinecraftClient.getInstance().player;
      if(inputPlayer!=null) inputYaw=inputPlayer.getYaw();
      AuraMovementTrace.capture();
   }
   public static float lastInputYaw() {
      var player=MinecraftClient.getInstance().player;
      return inputPlayer==player ? inputYaw : player.getYaw();
   }
   public static void attach(SourceModuleAdapter adapter) { adapters.add(adapter); }
   public static void initialize() {
      if (initialized) return;
      new FreeLookStorage();
      new RotationStorage();
      EventInvoker.register(new RotationProcess());
      EventInvoker.register(AuraServices.INSTANCE.tpsCalc);
      initialized = true;
   }
   public static boolean active() { return initialized && !BKClientInit.INSTANCE.isPanic(); }
   public static boolean auraEnabled() {
      if (!active()) return false;
      if (selectedAura != null && selectedAura.isEnabled()
         && (selectedAura.sourceModule() instanceof Aura || selectedAura.sourceModule() instanceof Aura2
         || selectedAura.sourceModule() instanceof AdaptiveAura || selectedAura.sourceModule() instanceof AttackAura)) return true;
      return Aura.INSTANCE.isEnable() || Aura2.INSTANCE.isEnable();
   }
   public static void selectAura(SourceModuleAdapter selected) {
      if (!(selected.sourceModule() instanceof AttackAura) && RotationProcess.isRotating())
         RotationProcess.resetParentTimeout();
      selectedAura = selected;
      for (var adapter : adapters) {
         var source = adapter.sourceModule();
         if (adapter != selected && adapter.isEnabled() && (source instanceof Aura
            || source instanceof Aura2 || source instanceof AdaptiveAura || source instanceof AttackAura)) adapter.setEnabled(false, true);
      }
   }
   public static void clearAura(SourceModuleAdapter adapter) {
      if (selectedAura == adapter) selectedAura = null;
   }
   public static boolean preventSprint() {
      if (selectedAura != null && selectedAura.isEnabled()) {
         var source = selectedAura.sourceModule();
         if (source instanceof Aura2 aura2) return aura2.preventSprint();
         if (source instanceof Aura aura) return aura.preventSprint();
         if (source instanceof AdaptiveAura adaptive) return adaptive.preventSprint();
         if (source instanceof AttackAura bkengine) return bkengine.preventSprint();
      }
      return Aura2.INSTANCE.preventSprint()
         || Aura.INSTANCE.preventSprint();
   }
   public static Float movementReferenceYaw() {
      // Vanilla sprint-jump follows the same body yaw as outgoing movement packets.
      // Do not add a different camera-relative impulse for the primary Aura.
      if(Aura.INSTANCE.isEnable()) return null;
      if (selectedAura != null && selectedAura.isEnabled() && selectedAura.sourceModule() instanceof Aura2 aura2
         && aura2.correctMovement())
         return dev.bkclient.combatengine.api.storages.implement.FreeLookStorage.getFreeYaw();
      if (selectedAura != null && selectedAura.isEnabled() && selectedAura.sourceModule() instanceof AdaptiveAura adaptive
         && adaptive.correctMovement())
         return dev.bkclient.combatengine.api.storages.implement.FreeLookStorage.getFreeYaw();
      if(Aura2.INSTANCE.correctMovement())
         return dev.bkclient.combatengine.api.storages.implement.FreeLookStorage.getFreeYaw();
      return null;
   }
   public static net.minecraft.entity.LivingEntity currentTarget() {
      if (selectedAura != null && selectedAura.isEnabled()) {
         var source = selectedAura.sourceModule();
         if (source instanceof Aura2 aura2) return aura2.getTarget();
         if (source instanceof Aura aura) return aura.getTarget();
         if (source instanceof AdaptiveAura adaptive) return adaptive.getTarget();
         if (source instanceof AttackAura bkengine) return bkengine.getTarget();
      }
      return Aura2.INSTANCE.isEnable() ? Aura2.INSTANCE.getTarget()
         : Aura.INSTANCE.isEnable() ? Aura.INSTANCE.getTarget() : null;
   }
   public static double attackRange() {
      if (selectedAura != null && selectedAura.isEnabled() && selectedAura.sourceModule() instanceof AttackAura bkengine)
         return bkengine.attackRange.get();
      if (selectedAura != null && selectedAura.isEnabled() && selectedAura.sourceModule() instanceof Aura2 aura2)
         return aura2.range.get();
      if (selectedAura != null && selectedAura.isEnabled() && selectedAura.sourceModule() instanceof AdaptiveAura adaptive)
         return adaptive.range.get();
      if (Aura2.INSTANCE.isEnable()) return Aura2.INSTANCE.range.get();
      var host = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.combat.Aura.class);
      return host != null && host.isEnabled() ? host.getAttackDistanceValue() : 3;
   }
   public static boolean ownsRotation() {
      return active() && (auraEnabled() || RotationStorage.instance.isRotating() || RotationProcess.isRotating());
   }
   public static boolean bkengineOwnsRotation() {
      return active() && (AttackAura.INSTANCE.isEnable() || RotationProcess.isRotating());
   }
   public static void clientTickEnd() {
      if (!active()) return;
      // Client ticks continue in menus without a player; keep setting visibility current there too.
      sync();
      var mc = MinecraftClient.getInstance();
      if (mc.player == null || mc.world == null) {
         if (RotationProcess.isRotating()) RotationProcess.resetParentTimeout();
         return;
      }
      if (mc.isPaused()) return;
      updateTarget();
   }
   public static void sync() { for (var adapter : adapters) adapter.syncSourceSettings(); }
   public static void emit(Event event) {
      if (!active()) return;
      if(event instanceof EventPacket packet) AuraMovementTrace.observe(packet);
      try { EventInvoker.invoke(event); }
      catch (ReflectiveOperationException error) { throw new IllegalStateException("Aura event failed", error); }
   }
   public static void tick() {
      if (!active()) return;
      sync();
      dev.bkclient.combatengine.bkengine.utils.math.ServerUtil.tick();
      // Rotation and combat decisions must run before vanilla sends this tick's
      // movement packet. Running them from clientTickEnd put attacks after that
      // packet and triggered Grim's PacketOrderO check.
      emit(new dev.bkclient.combatengine.bkengine.EventBKEngineTick());
      emit(new EventUpdate());
      updateTarget();
   }
   public static void postTick() { emit(new EventUpdatePost()); updateTarget(); }
   private static void updateTarget() {
      var client=BKClientInit.getInstance();
      if (client != null && client.getTargetManager()!=null && auraEnabled())
         client.getTargetManager().setCurrentTarget(currentTarget());
   }
   public static void render() {
      if (!active()) return;
      // Settings are synchronized on tick and before enabling the source module.
      // Repeating every adapter/settings copy per frame also penalizes disabled modules.
      long now=net.minecraft.util.Util.getMeasuringTimeNano();
      if (!EventInvoker.hasListeners(EventGameUpdate.class)) {lastHookTime=now; accumulatedCalls=0; return;}
      long delta=now-lastHookTime;
      accumulatedCalls += (int)(delta/4166666L);
      lastHookTime += accumulatedCalls*4166666L;
      for(accumulatedCalls=Math.min(accumulatedCalls,8); accumulatedCalls>0; accumulatedCalls--) emit(new EventGameUpdate());
   }
}
