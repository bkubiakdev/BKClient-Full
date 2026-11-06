package dev.bkclient.utility.render.blizzard;

/** Timing must remain continuous across variable FPS, pauses and weather changes. */
public final class BlizzardStateTest {
    private static int checks;
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    public static void main(String[] args) {
        BlizzardState s = new BlizzardState();
        long now = 1_000_000_000L;
        s.update(now, false, .85f, true, true, 5);
        check(s.strength() == 0, "first frame has no artificial time jump");
        for (int i=0; i<120; i++) s.update(now += 50_000_000L, false, .85f, true, true, 5);
        check(Math.abs(s.strength()-.85f)<.00001, "storm reaches configured strength");
        check(s.cover() > .8f && s.cover() <= 1, "snow accumulates over seconds");
        float cover = s.cover(); double seconds = s.seconds();
        s.update(now += 60_000_000_000L, true, .85f, true, true, 5);
        check(s.cover() == cover && s.seconds() == seconds, "pause freezes storm and snow");
        s.update(now += 50_000_000L, false, .85f, true, true, 5);
        check(s.seconds()-seconds < .051, "resume does not catch up paused minute");
        s.update(now += 120_000_000_000L, false, .85f, true, true, 5);
        check(s.delta() <= .1f, "long frame is capped");
        for (int i=0; i<60; i++) s.update(now += 50_000_000L, false, 0, false, true, 5);
        check(s.strength()==0 && s.exposure()==0, "weather and shelter fade out");
        cover = s.cover();
        for (int i=0; i<200; i++) s.update(now += 50_000_000L, false, 0, false, true, 5);
        check(s.cover()<cover && s.cover()>0, "snow melts gradually after weather clears");
        s.update(now += 50_000_000L, false, 0, false, false, 5);
        check(s.cover()==0, "snow cover toggle clears immediately");
        s.reset();
        check(s.strength()==0 && s.cover()==0 && s.seconds()==0, "dimension or disconnect clears all state");
        System.out.println("BlizzardStateTest PASS: "+checks+" checks");
    }
}
