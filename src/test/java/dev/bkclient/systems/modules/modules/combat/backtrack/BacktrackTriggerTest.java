package dev.bkclient.systems.modules.modules.combat.backtrack;

import java.util.ArrayList;
import java.util.List;

public final class BacktrackTriggerTest {
    public static void main(String[] args) {
        var trigger = new BacktrackTrigger();
        check(!trigger.canHold(false), "No target must pass packets");
        check(trigger.canHold(true), "Distance starts without an attack");
        check(trigger.canHold(true), "Distance can start another batch while in range");
        check(trigger.setHitMode(true), "Mode switch requires flushing old queue");
        check(!trigger.canHold(true), "Hit must not start from proximity");
        trigger.attack();
        check(trigger.canHold(true), "Attack arms Hit");
        check(!trigger.canHold(false), "Hit cannot hold without target");
        var queue = new PacketDelayQueue<Integer>();
        var applied = new ArrayList<Integer>();
        queue.add(1, 100); queue.add(2, 150);
        queue.drainBatch(199, 100, applied::add);
        check(applied.isEmpty(), "Batch waits for delay");
        trigger.attack(); // Repeated attacks cannot postpone already buffered packets.
        queue.drainBatch(200, 100, applied::add);
        check(applied.equals(List.of(1,2)), "Batch replays in order at its original deadline");
        check(trigger.canHold(true), "Hit stays armed after a batch while the target remains in range");
        checkHitPursuit(trigger);
        trigger.attack(); trigger.reset();
        check(!trigger.canHold(true), "World/target reset clears Hit");
        trigger.setHitMode(false);
        check(trigger.canHold(true), "Distance resumes after mode change");
        check(!BacktrackTrigger.inRange(6.001, 6), "Outside range");
        check(BacktrackTrigger.inRange(6, 6), "Range boundary included");
        check(BacktrackTrigger.inRange(0, 0), "Zero range includes touching hitbox");
        check(!BacktrackTrigger.inRange(.01, 0), "Zero range excludes separated hitbox");
        check(!BacktrackTrigger.inRange(Double.NaN, 6), "Reject missing distance");
        check(!BacktrackTrigger.inRange(Double.POSITIVE_INFINITY, 6), "Reject infinite distance");
        check(!BacktrackTrigger.inRange(2, Double.NaN), "Reject invalid range");
        check(!BacktrackTrigger.inRange(-1, 6), "Reject negative distance");
        check(!BacktrackTrigger.inRange(2, -1), "Reject negative range");
        System.out.println("BackTrack Hit pursuit across batches, release window, range boundaries, FIFO and reset PASS");
    }

    private static void checkHitPursuit(BacktrackTrigger trigger) {
        var cycle = new PacketHoldCycle();
        cycle.enable();
        var queue = new PacketDelayQueue<String>();
        var applied = new ArrayList<String>();
        // One hit has already armed the target. Moving away inside Range must
        // permit more than one delay/release cycle, without another attack.
        double[] serverDistances = {3, 4.5, 6};
        for (int i = 0; i < serverDistances.length; i++) {
            long start = i * 300L;
            check(BacktrackTrigger.inRange(serverDistances[i], 6), "Escaping target still in range");
            check(trigger.canHold(true) && cycle.canHold(start), "Next batch starts without another hit");
            queue.add("move" + i, start);
            queue.drainBatch(start + 199, 200, applied::add);
            check(queue.size() == 1, "Batch keeps original delay");
            queue.drainBatch(start + 200, 200, applied::add);
            cycle.released(start + 200, 100);
            check(!cycle.canHold(start + 299), "Release window still passes packets");
            check(trigger.canHold(true), "Release window does not forget attacked target");
        }
        check(applied.equals(List.of("move0", "move1", "move2")), "Continuous pursuit preserves FIFO");
        queue.add("last held move", 900);
        check(!BacktrackTrigger.inRange(6.01, 6), "Use real position beyond range, not the delayed display");
        queue.drain(901, 0, true, applied::add);
        trigger.reset();
        check(queue.size() == 0 && applied.getLast().equals("last held move"), "Leaving range flushes pending data");
        check(!trigger.canHold(true), "Returning into range requires a new hit");
        trigger.attack();
        check(trigger.canHold(true), "New hit re-arms after range exit");
        trigger.reset();
        check(!trigger.canHold(true), "Switching target must not inherit the previous hit");
        trigger.attack();
        trigger.setHitMode(false);
        trigger.setHitMode(true);
        check(!trigger.canHold(true), "Switching modes must clear the old hit");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
