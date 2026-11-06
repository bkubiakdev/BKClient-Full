package dev.bkclient.utility.render.ambience;

public final class AmbienceTimeLightTest {
    static void check(boolean b, String m) { if (!b) throw new AssertionError(m); }

    public static void main(String[] args) {
        // Restored time follows the server clock from the last sample.
        AmbienceTime ticking = new AmbienceTime(1_000L, 6_000L, true);
        check(ticking.restoredTimeOfDay(1_000L) == 6_000L, "same tick restores the sample");
        check(ticking.restoredTimeOfDay(1_250L) == 6_250L, "ticking clock advances with world age");
        check(ticking.restoredTimeOfDay(900L) == 6_000L, "older age never rewinds the clock");
        AmbienceTime frozen = new AmbienceTime(1_000L, 18_000L, false);
        check(frozen.restoredTimeOfDay(5_000L) == 18_000L, "frozen daylight cycle keeps its time");

        // Terrain is rebuilt per block, radius or level change, not per small movement.
        int[] block = {10, 64, -3};
        check(DynamicBrightnessArea.needsRefresh(false, block, 12, 15, 10, 64, -3, 12, 15), "inactive light refreshes");
        check(DynamicBrightnessArea.needsRefresh(true, null, 12, 15, 10, 64, -3, 12, 15), "first position refreshes");
        check(!DynamicBrightnessArea.needsRefresh(true, block, 12, 15, 10, 64, -3, 12, 15), "same block does not refresh");
        check(DynamicBrightnessArea.needsRefresh(true, block, 12, 15, 11, 64, -3, 12, 15), "new block refreshes");
        check(DynamicBrightnessArea.needsRefresh(true, block, 12, 15, 10, 64, -3, 11, 15), "radius change refreshes");
        check(DynamicBrightnessArea.needsRefresh(true, block, 12, 15, 10, 64, -3, 12, 14), "level change refreshes");

        // Walking 10 blocks in 0.05-block steps: old 0.15-block rule vs new per-block rule.
        int oldRefreshes = 0, newRefreshes = 0;
        double last = Double.NaN;
        int[] lastBlock = null;
        for (int i = 0; i <= 200; i++) {
            double x = 0.02 + i * 0.05;
            if (Double.isNaN(last) || (x - last) * (x - last) >= 0.0225) { oldRefreshes++; last = x; }
            int bx = (int)Math.floor(x);
            if (DynamicBrightnessArea.needsRefresh(true, lastBlock, 12, 15, bx, 64, 0, 12, 15)) { newRefreshes++; lastBlock = new int[]{bx, 64, 0}; }
        }
        check(newRefreshes == 11, "one refresh per block entered (10 steps + start), got " + newRefreshes);
        check(oldRefreshes >= newRefreshes * 4, "old rule refreshed " + oldRefreshes + " times");

        // Section boxes cover the whole reach, including negative coordinates.
        int[] box = DynamicBrightnessArea.sectionBounds(-1, 64, 15, 12, 1);
        check(box[0] == -1 && box[3] == 0, "x: blocks -14..12 are sections -1..0");
        check(box[1] == 3 && box[4] == 4, "y: blocks 51..77 are sections 3..4");
        check(box[2] == 0 && box[5] == 1, "z: blocks 2..28 are sections 0..1");
        int[] moved = DynamicBrightnessArea.sectionBounds(20, 64, 15, 12, 1);
        int[] both = DynamicBrightnessArea.union(box, moved);
        check(both[0] == box[0] && both[3] == moved[3], "union covers the old and the new reach");
        check(DynamicBrightnessArea.union(null, moved) == moved && DynamicBrightnessArea.union(box, null) == box, "missing box is ignored");

        System.out.println("Ambience: time restore, per-block light refresh (" + oldRefreshes + " -> " + newRefreshes
            + " on a 10-block walk) and section bounds PASS");
    }
}
