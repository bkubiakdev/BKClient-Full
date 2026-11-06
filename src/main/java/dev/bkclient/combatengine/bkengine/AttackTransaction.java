package dev.bkclient.combatengine.bkengine;

/** Validates the sprint state established by the preceding vanilla movement tick. */
public final class AttackTransaction {
    private AttackTransaction() {}

    public static boolean requiresSprintReset(boolean onlyCriticals, boolean criticalWindow,
                                               boolean movementRestricted, boolean normalHitRequired) {
        return !movementRestricted && !normalHitRequired && (onlyCriticals || criticalWindow);
    }

    public static boolean ready(boolean localSprint, boolean sentSprint, boolean resetRequired) {
        return localSprint == sentSprint && (!resetRequired || !localSprint);
    }

    public static void run(Runnable before, Runnable attack, Runnable after) {
        try {
            if (before != null) before.run();
            attack.run();
        } finally {
            // Sprint packets belong to vanilla movement, never this transaction.
            if (after != null) after.run();
        }
    }
}
