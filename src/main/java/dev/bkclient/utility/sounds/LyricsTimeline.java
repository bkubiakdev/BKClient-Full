package dev.bkclient.utility.sounds;

/** Selects timed verses without inventing timestamps for plain lyrics. */
public final class LyricsTimeline {
    private LyricsTimeline() { }

    /** {@code wordMs}: one song time per word of {@code text}, or null when the source times only the line. */
    public record Cue(int index, String text, long startMs, long endMs, long[] wordMs) {
        public Cue(int index, String text, long startMs, long endMs) { this(index, text, startMs, endMs, null); }
    }

    public static Cue at(SyncedLyrics lyrics, long positionMs, long offsetMs, long durationMs) {
        if (lyrics == null || !lyrics.isSynced() || lyrics.isEmpty()) return null;
        if (durationMs > 0 && positionMs >= durationMs) return null;
        long time = positionMs + Math.clamp(offsetMs, -30000L, 30000L);
        int index = lyrics.getActiveIndex(time);
        if (index < 0) return null;
        var lines = lyrics.getLines();
        var line = lines.get(index);
        long end = Math.min(line.timeMs() + 8000L,
            index + 1 < lines.size() ? lines.get(index + 1).timeMs() : Long.MAX_VALUE);
        if (durationMs > 0) end = Math.min(end, durationMs);
        if (time >= end || line.text().isBlank()) return null;
        // Bound external text before it reaches font layout; never interpret formatting codes.
        String text = line.text().replaceAll("§.", "").replaceAll("[\\p{Cntrl}]", " ").strip();
        if (text.length() > 320) text = text.substring(0, text.offsetByCodePoints(0, Math.min(240, text.codePointCount(0, text.length())))) + "…";
        if (text.isBlank()) return null;
        // Word times survive only if sanitising kept the same words they were measured for.
        long[] words = line.wordMs() != null && LyricsWordReveal.wordEnds(text).length == line.wordMs().length ? line.wordMs() : null;
        return new Cue(index, text, line.timeMs(), end, words);
    }
}
