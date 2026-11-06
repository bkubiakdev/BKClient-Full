package dev.bkclient.systems.modules.modules.combat.backtrack;

/** Generation is published to Netty; deadlines are owned by the game thread. */
public final class PacketHoldCycle {
   // Odd generations accept packets; even generations are disabled.
   private volatile long generation;
   private long releasedAt;
   private long releaseWindow;

   public long generation() { return generation; }
   public boolean accepts(long captured) { return (captured & 1) != 0 && generation == captured; }
   public boolean isActive() { return (generation & 1) != 0; }

   public void enable() {
      releaseWindow = 0;
      generation = (generation + 2) | 1;
   }

   public void disable() {
      generation = (generation + 1) & ~1L;
      releaseWindow = 0;
   }

   public void released(long now, long window) {
      releasedAt = now;
      releaseWindow = Math.max(0, window);
   }

   public boolean canHold(long now) {
      return isActive() && (releaseWindow == 0 || now - releasedAt >= releaseWindow);
   }
}
