package dev.bkclient.compat;

import dev.bkclient.utility.render.TargetScanTimeline;
import dev.bkclient.utility.render.TargetScanQuality;

public final class TargetScanTest {
    private static void near(float expected, float actual) {
        if (Math.abs(expected - actual) > .0001f) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) {
        for (int fps : new int[]{20, 60, 144}) {
            var timeline = new TargetScanTimeline();
            var sample = timeline.update(0, true, 2);
            for (int frame = 1; frame <= fps; frame++) sample = timeline.update(Math.round(frame * 1_000_000_000.0 / fps), true, 2);
            near(.5f, sample.height()); near(1, sample.alpha()); near(1, sample.direction());
            sample = timeline.update(2_000_000_000L, true, 2); near(1, sample.height());
            sample = timeline.update(3_000_000_000L, true, 2); near(.5f, sample.height()); near(-1, sample.direction());
            sample = timeline.update(4_000_000_000L, true, 2); near(0, sample.height());
            sample = timeline.update(4_180_000_000L, false, 2); near(0, sample.alpha());
            timeline.reset(); sample = timeline.update(9_000_000_000L, true, 2); near(0, sample.height()); near(0, sample.alpha());
        }
        var timeline = new TargetScanTimeline();
        timeline.update(0, true, 2);
        var before = timeline.update(1_000_000_000L, true, 2);
        var changed = timeline.update(1_000_000_000L, true, .5f);
        near(before.height(), changed.height());
        near(before.alpha(), changed.alpha());
        for (long turn : new long[]{2_000_000_000L, 4_000_000_000L}) {
            timeline.reset(); timeline.update(0, true, 2);
            var left = timeline.update(turn - 1_000_000L, true, 2);
            var center = timeline.update(turn, true, 2);
            var right = timeline.update(turn + 1_000_000L, true, 2);
            if (Math.abs(left.direction() - right.direction()) > .004f) throw new AssertionError("Trail flips at turnaround");
            near(0, center.direction()); near(1, center.alpha());
            near(left.height(), right.height());
        }
        var small = TargetScanQuality.maskSize(1280, 720, 16384);
        if (small.width() != 2560 || small.height() != 1440) throw new AssertionError("Expected 2x mask");
        var large = TargetScanQuality.maskSize(3840, 2160, 16384);
        if (large.width() < 3840 || (long)large.width() * large.height() > 8_388_608) throw new AssertionError("4K budget");
        var bounded = TargetScanQuality.maskSize(1920, 1080, 2048);
        if (bounded.width() > 2048 || bounded.height() > 2048) throw new AssertionError("GPU limit");
        System.out.println("PASS: scan timing at 20/60/144 FPS, up/down cycle, fade-out, target/world reset, continuous duration changes");
    }
}
