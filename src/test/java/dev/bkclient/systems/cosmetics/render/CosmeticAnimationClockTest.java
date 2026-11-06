package dev.bkclient.systems.cosmetics.render;

public final class CosmeticAnimationClockTest {
    private static void near(float actual, float expected) {
        if (Math.abs(actual - expected) > .00001f) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        var clock = new CosmeticAnimationClock();
        near(clock.advance(1, 1_000_000_000L, false), 0);
        near(clock.advance(2, 1_016_000_000L, false), .016f);
        near(clock.advance(2, 1_028_000_000L, false), 0); // Flame replay of the same visual frame
        near(clock.advance(3, 1_032_000_000L, false), .016f);
        near(clock.advance(4, 2_032_000_000L, true), 0);
        near(clock.advance(5, 3_032_000_000L, true), 0);
        near(clock.advance(6, 3_048_000_000L, false), .016f);
        near(clock.advance(7, 9_048_000_000L, false), .25f);
        clock.reset();
        near(clock.advance(8, 10_048_000_000L, false), 0);
        near(clock.advance(20, 12_048_000_000L, false), 0); // HUD hidden: intervening frames omitted the hands.
        near(clock.advance(21, 12_064_000_000L, false), .016f);
        System.out.println("PASS: one animation step per visual frame, replay, pause, stall and reset");
    }
}
