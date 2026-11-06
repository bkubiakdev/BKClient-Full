package dev.bkclient.systems.modules.modules.combat.backtrack;
import java.util.ArrayList;
import java.util.List;
public final class PacketDelayQueueTest {
   public static void main(String[] args) {
      var queue = new PacketDelayQueue<String>();
      var applied = new ArrayList<String>();
      queue.add("first", 0);
      queue.add("second", 80);
      queue.drain(149, 150, false, applied::add);
      check(applied.isEmpty(), "must not release early");
      queue.add("third", 150);
      queue.drain(150, 150, false, applied::add);
      check(applied.equals(List.of("first")), "new arrivals must not reset older deadlines");
      queue.drain(230, 150, false, applied::add);
      check(applied.equals(List.of("first", "second")), "release each packet at its own deadline");
      queue.drain(230, 150, true, applied::add);
      check(applied.equals(List.of("first", "second", "third")), "target changes must flush FIFO");
      queue.add("old world", 240);
      queue.clear();
      queue.drain(1000, 0, true, applied::add);
      check(applied.size() == 3 && queue.size() == 0, "discard stale world packets");
      queue.add("zero delay", 1000);
      queue.drain(1000, 0, false, applied::add);
      check(applied.getLast().equals("zero delay"), "zero delay must pass immediately");
      applied.clear();
      queue.add("movement", 2000);
      queue.add("sync", 2100);
      queue.drainBatch(2149, 150, applied::add);
      check(applied.isEmpty(), "hold mode must retain the whole snapshot");
      queue.drainBatch(2150, 150, applied::add);
      check(applied.equals(List.of("movement", "sync")), "hold mode must catch up the complete batch in order");
      queue.add("next movement", 2160);
      queue.drainBatch(2200, 150, applied::add);
      check(queue.size() == 1, "next batch needs its own deadline");
      queue.clear(); applied.clear();
      double[] movingDistances = {2, 2.5, 1.8, 3.2, 1.5};
      for (int i = 0; i < movingDistances.length; i++) {
         queue.add("position" + i, i * 100_000_000L);
         queue.drainBatch(i * 100_000_000L, 500_000_000L, applied::add);
         check(applied.isEmpty(), "approaching/receding target must remain held before batch deadline");
      }
      queue.drainBatch(499_000_000L, 500_000_000L, applied::add);
      check(applied.isEmpty() && queue.size() == 5, "retain all updates for the configured interval");
      queue.drainBatch(500_000_000L, 500_000_000L, applied::add);
      check(applied.equals(List.of("position0", "position1", "position2", "position3", "position4")),
            "deadline must replay the entire batch in order");
      queue.add("disable flush", 510_000_000L);
      queue.drain(510_000_000L, 500_000_000L, true, applied::add);
      check(queue.size() == 0 && applied.getLast().equals("disable flush"), "disable must not leave a held batch");
      queue.clear(); applied.clear();
      queue.add("long hold", 0);
      queue.drainBatch(500_000_000L, 1_000_000_000L, applied::add);
      check(applied.isEmpty(), "one-second hold must not use the former 500 ms limit");
      queue.drainBatch(999_000_000L, 1_000_000_000L, applied::add);
      check(applied.isEmpty(), "retain until the new deadline");
      queue.drainBatch(1_000_000_000L, 1_000_000_000L, applied::add);
      check(applied.equals(List.of("long hold")), "release at one second");
      queue.add("maximum hold", 2_000_000_000L);
      queue.drainBatch(3_999_000_000L, 2_000_000_000L, applied::add);
      check(queue.size() == 1, "two-second delay remains supported");
      queue.drainBatch(4_000_000_000L, 2_000_000_000L, applied::add);
      check(queue.size() == 0, "maximum delay expires");
      Class<?>[] heldTypes = {
         net.minecraft.network.packet.s2c.common.CommonPingS2CPacket.class,
         net.minecraft.network.packet.s2c.play.EntityS2CPacket.class,
         net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket.class,
         net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket.class,
         net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket.class,
         net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket.class,
         net.minecraft.network.packet.s2c.play.ParticleS2CPacket.class,
         net.minecraft.network.packet.s2c.play.ExplosionS2CPacket.class
      };
      for (Class<?> type : heldTypes) check(NemesisPacketPolicy.shouldDelay(type), "Nemesis must queue " + type.getSimpleName());
      Class<?>[] passTypes = {
         net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket.class,
         net.minecraft.network.packet.s2c.common.DisconnectS2CPacket.class,
         net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket.class,
         net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket.class,
         net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket.class,
         net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class
      };
      for (Class<?> type : passTypes) check(!NemesisPacketPolicy.shouldDelay(type), "Nemesis must pass " + type.getSimpleName());
      queue.clear(); applied.clear();
      queue.add("ping without target", 0);
      queue.add("another entity velocity", 100_000_000L);
      queue.drainBatch(199_000_000L, 200_000_000L, applied::add);
      check(applied.isEmpty(), "global queue retains mixed packets for default Nemesis delay");
      queue.drainBatch(200_000_000L, 200_000_000L, applied::add);
      check(applied.equals(List.of("ping without target", "another entity velocity")), "global batch releases together in arrival order");
      checkHoldLifecycle();
      System.out.println("Backtrack queue timing, FIFO, flush and discard tests passed");
   }

   private static void checkHoldLifecycle() {
      var cycle = new PacketHoldCycle();
      var queue = new PacketDelayQueue<String>();
      var applied = new ArrayList<String>();
      check(!cycle.canHold(0) && !cycle.accepts(cycle.generation()), "initial OFF must pass packets");
      cycle.enable();
      long firstEnable = cycle.generation();
      check(cycle.accepts(firstEnable) && cycle.canHold(0), "ON must accept a fresh batch");
      queue.add("held movement", 0);
      queue.drainBatch(200, 200, applied::add);
      cycle.released(200, 100);
      check(!cycle.canHold(200) && !cycle.canHold(299), "release window must pass packets instead of immediately holding again");
      check(cycle.canHold(300), "holding resumes after the release window");
      queue.add("held ping", 300);
      // Model a Netty callback posted before OFF but executed afterwards.
      Runnable staleCallback = () -> {
         if (cycle.accepts(firstEnable)) queue.add("posted velocity", 310);
         else applied.add("posted velocity");
      };
      cycle.disable();
      queue.drain(310, 0, true, applied::add);
      staleCallback.run();
      check(queue.size() == 0 && !cycle.canHold(10_000), "OFF must remain empty beyond every deadline");
      check(applied.equals(List.of("held movement", "held ping", "posted velocity")), "OFF flush and posted packet must preserve FIFO");
      cycle.enable();
      staleCallback.run();
      check(queue.size() == 0, "rapid OFF/ON must not requeue callbacks from the old enable");
      check(!cycle.accepts(firstEnable) && cycle.accepts(cycle.generation()), "only the current enabled generation accepts packets");
      cycle.released(400, 100);
      cycle.disable();
      cycle.enable();
      check(cycle.canHold(410), "new enable must clear the previous release window");
      cycle.released(410, 0);
      check(cycle.canHold(410), "zero release window retains continuous Nemesis batches");
   }
   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
}
