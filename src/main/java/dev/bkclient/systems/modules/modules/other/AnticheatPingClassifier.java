package dev.bkclient.systems.modules.modules.other;

/**
 * Matches a window of ping (transaction) ids against the numbering of known anticheats.
 * Start ids for Grim, Intave, Karhu, Vulcan/Negativity and AGC follow the observations published in
 * NotRequiem/Anticheat-Detector (MIT); the remaining ranges are community-known patterns.
 * Kept free of Minecraft classes so it can be checked without starting the game.
 */
public final class AnticheatPingClassifier {
    public static final String CUSTOM = "Custom";

    private AnticheatPingClassifier() {
    }

    public record Detection(String name, int confidence) {
    }

    /** Returns null while the window is too short or the pattern is irregular (e.g. spans a reconnect). */
    public static Detection classify(int[] ids) {
        if (ids.length < 5) return null;
        int first = ids[0];
        int[] diffs = new int[ids.length - 1];
        for (int i = 1; i < ids.length; i++) diffs[i - 1] = ids[i] - ids[i - 1];

        if (allEqual(diffs, 0, 1)) {
            if (first == 1) return new Detection("AntiGamingChair", 85);
            if (first == -32768) return new Detection("Intave", 85);
            if (between(first, -23772, -23762)) return new Detection("Vulcan", 88);
            if (between(first, 95, 105) || between(first, -20005, -19995)) return new Detection("Matrix", 82);
            if (between(first, -32773, -32762)) return new Detection("Grizzly", 80);
            // Verus starts at a random id; a private anticheat counting up looks the same.
            return new Detection("Verus / " + CUSTOM, 55);
        }
        if (allEqual(diffs, 0, -1)) {
            if (between(first, -5, 0)) return new Detection("Grim", 88);
            if (between(first, -3000, -2995)) return new Detection("Karhu", 82);
            if (between(first, -8287, -8280)) return new Detection("Errata", 80);
            if (first < -3000) return new Detection("Intave", 75);
            return new Detection("Polar / " + CUSTOM, 55);
        }
        if (first == -32767 && between(ids[1], -3000, -2995) && allEqual(diffs, 1, -1)) return new Detection("Karhu", 88);
        // The step right after the start sequence is not part of these patterns.
        if (first == -30767 && ids[1] == -30766 && ids[2] == -25767 && allEqual(diffs, 3, 1)) {
            return new Detection("Vulcan / Negativity", 88);
        }
        if (ids[0] == ids[1] && allEqual(diffs, 2, 1)) return new Detection("Verus", 82);
        if (diffs[0] >= 100 && allEqual(diffs, 1, -1)) return new Detection("Grim / Polar", 75);
        if (first < -3000) {
            for (int id : ids) if (id == 0) return new Detection("Intave", 70);
        }
        return null;
    }

    private static boolean allEqual(int[] values, int from, int expected) {
        if (from >= values.length) return false;
        for (int i = from; i < values.length; i++) if (values[i] != expected) return false;
        return true;
    }

    private static boolean between(int value, int min, int max) {
        return value >= min && value <= max;
    }
}
