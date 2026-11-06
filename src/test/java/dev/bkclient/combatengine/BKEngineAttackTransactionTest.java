package dev.bkclient.combatengine;

import dev.bkclient.combatengine.bkengine.AttackTransaction;
import java.util.ArrayList;
import java.util.List;

public final class BKEngineAttackTransactionTest {
    private static final class Player {
        boolean local = true, sent = true;
        boolean sprintActionInTick;
        final List<String> packets = new ArrayList<>();
        void movementTick() {
            if (local != sent) {
                check(!sprintActionInTick, "at most one sprint transition per movement tick");
                sprintActionInTick = true;
                packets.add(local ? "START" : "STOP");
                sent = local;
            }
            packets.add("MOVE");
            sprintActionInTick = false;
        }
        void attack(boolean stopsSprint) {
            check(!sprintActionInTick, "attack must precede sprint actions in its tick");
            check(local == sent, "attack sprint state agrees with the preceding movement tick");
            packets.add("ATTACK");
            if (stopsSprint) local = false;
            packets.add("SWING");
        }
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(!AttackTransaction.requiresSprintReset(false,false,false,false), "ordinary ground hit preserves sprint");
        check(AttackTransaction.requiresSprintReset(false,true,false,false), "falling critical resets sprint");
        check(AttackTransaction.requiresSprintReset(true,false,false,false), "critical-only attack requires reset");
        for (boolean onlyCriticals : new boolean[]{false,true}) {
            check(!AttackTransaction.requiresSprintReset(onlyCriticals,true,true,false), "movement restrictions do not force reset");
            check(!AttackTransaction.requiresSprintReset(onlyCriticals,true,false,true), "water and web normal hits preserve sprint");
        }
        for (boolean local : new boolean[]{false,true})
            for (boolean sent : new boolean[]{false,true}) {
                check(AttackTransaction.ready(local,sent,true) == (!local && !sent),
                    "reset attack requires both states stopped");
                check(AttackTransaction.ready(local,sent,false) == (local == sent),
                    "non-reset attack still requires matching states");
            }
        Player reset = new Player();
        reset.local = false;
        check(!AttackTransaction.ready(reset.local,reset.sent,true), "defer same-tick reset");
        reset.movementTick();
        check(AttackTransaction.ready(reset.local,reset.sent,true), "ready after vanilla transition");
        AttackTransaction.run(null, () -> reset.attack(false), null);
        reset.movementTick();
        reset.local = true;
        reset.movementTick();
        check(reset.packets.equals(List.of("STOP","MOVE","ATTACK","SWING","MOVE","START","MOVE")),
            "reset and attack must span movement boundaries");

        Player normal = new Player();
        AttackTransaction.run(null, () -> normal.attack(true), null);
        normal.movementTick();
        check(normal.packets.equals(List.of("ATTACK","SWING","STOP","MOVE")),
            "vanilla attack slowdown must be followed by vanilla sprint synchronization");

        // Aura must not own the sprint key or erase directional movement. This protects
        // manual sprint, AutoSprint and old configs that previously selected Legit.
        try {
            String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/dev/bkclient/combatengine/bkengine/module/impl/combat/AttackAura.java"));
            check(!source.contains("sprintKey.setPressed"), "Aura leaves sprint intent to input/AutoSprint");
            check(!source.contains("setForward(0)") && !source.contains("setStrafe(0)"), "reset preserves movement input");
            check(!source.contains("typeSprint"), "retired sprint modes cannot change the unified behavior");
        } catch (java.io.IOException failure) {
            throw new AssertionError(failure);
        }

        boolean[] restored = {false};
        RuntimeException failure = new RuntimeException("attack listener failed");
        try {
            AttackTransaction.run(null, () -> { throw failure; }, () -> restored[0] = true);
            throw new AssertionError("failure must propagate");
        } catch (RuntimeException actual) {
            check(actual == failure && restored[0], "restore post-hit state on failure");
        }
        System.out.println("BKEngine: cross-tick sprint agreement, packet ordering, single sprint transition and failure recovery passed");
    }
}
