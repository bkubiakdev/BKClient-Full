package dev.bkclient.utility.sounds;

import java.util.regex.Pattern;

/** Visual typewriter timing within a verse; {@link #timed} follows real per-word timestamps when the source has them. */
public final class LyricsWordReveal {
    private static final Pattern WORD = Pattern.compile("\\S+", Pattern.UNICODE_CHARACTER_CLASS);
    private LyricsWordReveal() { }
    public static int[] wordEnds(String text) {
        return WORD.matcher(text).results().mapToInt(java.util.regex.MatchResult::end).toArray();
    }
    public record Frame(int visibleEnd, int settledEnd, float progress) { }
    public static int visibleEnd(int[] ends, long start, long end, long playback) {
        return frame(ends, start, end, playback).visibleEnd();
    }
    public static Frame frame(int[] ends, long start, long end, long playback) {
        return frame(ends, start, end, playback, .45, 650, 160);
    }
    /** Word-synced sources: each word enters at its own song time; same entrance easing as the estimated reveal. */
    public static Frame timed(int[] ends, long[] wordMs, long playback, double entranceMs) {
        if (ends.length == 0 || wordMs == null || wordMs.length != ends.length || playback < wordMs[0]) return new Frame(0, 0, 1);
        int word = 0;
        while (word + 1 < wordMs.length && playback >= wordMs[word + 1]) word++;
        double next = word + 1 < wordMs.length ? wordMs[word + 1] - wordMs[word] : Double.MAX_VALUE;
        double entrance = Math.max(1, Math.min(Math.max(1, entranceMs), next * .8));
        float t = (float) Math.clamp((playback - wordMs[word]) / entrance, 0, 1);
        float eased = 1 - (1 - t) * (1 - t) * (1 - t);
        return new Frame(ends[word], t >= 1 ? ends[word] : word == 0 ? 0 : ends[word - 1], eased);
    }
    /** Real word times when present and matching the text, the estimated reveal otherwise. */
    public static Frame frame(int[] ends, long[] wordMs, long start, long end, long playback, double fraction, double gapMs, double entranceMs) {
        return wordMs != null && wordMs.length == ends.length && ends.length > 0
            ? timed(ends, wordMs, playback, entranceMs) : frame(ends, start, end, playback, fraction, gapMs, entranceMs);
    }
    public static Frame frame(int[] ends, long start, long end, long playback, double fraction, double gapMs, double entranceMs) {
        if (ends.length == 0 || playback < start) return new Frame(0, 0, 1);
        double duration = Math.max(1, end - start);
        double revealDuration = Math.max(1, Math.min(duration * Math.clamp(fraction,.2,1), Math.max(1, ends.length - 1) * Math.clamp(gapMs,1,650)));
        // Reserve the final word's entrance inside this cue, including at 100% reveal.
        double step = ends.length <= 1 ? Math.min(180,duration) : Math.min(revealDuration / (ends.length - 1), duration / (ends.length - 1 + .8));
        long elapsed = playback - start;
        int word = Math.min(ends.length - 1, (int)Math.floor(elapsed / step));
        double entrance = Math.min(Math.max(1,entranceMs), step * .8);
        float t = (float)Math.clamp((elapsed - word * step) / entrance, 0, 1);
        float eased = 1 - (1 - t) * (1 - t) * (1 - t);
        return new Frame(ends[word], t >= 1 ? ends[word] : word == 0 ? 0 : ends[word - 1], eased);
    }
}
