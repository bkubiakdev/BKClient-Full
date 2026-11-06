package dev.bkclient.combatengine;
import dev.bkclient.systems.modules.modules.combat.backtrack.PacketDelayQueue;
import java.util.ArrayList;
import java.util.List;

public final class WebLagBatchTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        for (long delay : new long[]{100, 125, 150}) {
            PacketDelayQueue<String> queue = new PacketDelayQueue<>();
            List<String> sent = new ArrayList<>();
            queue.add("move1", 0);
            queue.add("tickEnd1", 0);
            queue.add("move2", 50);
            queue.add("tickEnd2", 50);
            queue.drainBatch(delay - 1, delay, sent::add);
            check(sent.isEmpty());
            queue.add("move3", delay - 1);
            queue.drainBatch(delay, delay, sent::add);
            sent.add("attack");
            check(sent.equals(List.of("move1", "tickEnd1", "move2", "tickEnd2", "move3", "attack")));
            check(queue.size() == 0);
            // A block interaction must release old movement before its own packet.
            sent.clear(); queue.add("move", 200);
            queue.drain(220, 0, true, sent::add); sent.add("place");
            check(sent.equals(List.of("move", "place")));
            // Teleport/world change discards the old coordinates, never replays them.
            sent.clear(); queue.add("old world", 300); queue.clear();
            queue.drainBatch(1000, delay, sent::add);
            check(sent.isEmpty());
        }
        check(dev.bkclient.combatengine.bkengine.CriticalWindow.falling(false, .001F, -.0001, false));
        check(!dev.bkclient.combatengine.bkengine.CriticalWindow.falling(true, .001F, -.0001, false));
        check(dev.bkclient.combatengine.bkengine.CriticalWindow.webFalling(false, -.0001, false));
        check(!dev.bkclient.combatengine.bkengine.CriticalWindow.webFalling(true, -.0001, false));
        check(!dev.bkclient.combatengine.bkengine.CriticalWindow.webFalling(false, 0, false));
        check(!dev.bkclient.combatengine.bkengine.CriticalWindow.webFalling(false, .01, false));
        check(!dev.bkclient.combatengine.bkengine.CriticalWindow.webFalling(false, -.01, true));
        System.out.println("Web Lag: 100-150ms FIFO batches, no extension, interaction flush, correction discard, slow descent passed");
    }
}
