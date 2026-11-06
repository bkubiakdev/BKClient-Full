package dev.bkclient.systems.modules.modules.combat.backtrack;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/** FIFO owned by the game thread. New attacks never extend an entry's lifetime. */
public final class PacketDelayQueue<T> {
   private record Entry<T>(T value, long received) {}
   private final ArrayDeque<Entry<T>> entries = new ArrayDeque<>();
   public void add(T value, long now) { entries.addLast(new Entry<>(value, now)); }
   public int size() { return entries.size(); }
   public long oldestAge(long now) { return entries.isEmpty() ? 0 : Math.max(0, now - entries.peekFirst().received()); }
   public void clear() { entries.clear(); }
   /** Hold a snapshot until the oldest packet expires, then catch up in FIFO order. */
   public void drainBatch(long now, long delay, Consumer<T> apply) {
      if (!entries.isEmpty() && now - entries.peekFirst().received() >= delay) drain(now, 0, true, apply);
   }
   public void drain(long now, long delay, boolean all, Consumer<T> apply) {
      while (!entries.isEmpty() && (all || now - entries.peekFirst().received() >= delay)) {
         apply.accept(entries.removeFirst().value());
      }
   }
}
