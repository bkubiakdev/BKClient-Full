package dev.bkclient.utility.render.lyrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Scattered display timing: a line is cut into short pieces (a few words, at most about a dozen letters) and every
 * word gets the song time it appears at, spread over the first part of the line by word length.
 */
public final class LyricsPieces {
    public static final int MAX_CHARS = 12;
    /** A piece is placed slightly before its first word is sung. */
    public static final long LEAD_MS = 300;
    private static final String PIECE_ENDS = ",.!?;:…";

    /** {@code emphasisWord} is the index inside the piece (-1 = none); {@code reveal} holds one song time per word. */
    public record Piece(String text, int emphasisWord, long[] reveal, long spawnMs) { }

    private LyricsPieces() { }

    public static List<Piece> plan(String line, long startMs, long endMs, int maxWords) {
        return plan(line, startMs, endMs, maxWords, null);
    }

    /** {@code wordMs}: real song time per word (word-synced sources); null spreads words by length. */
    public static List<Piece> plan(String line, long startMs, long endMs, int maxWords, long[] wordMs) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) return List.of();
        String[] words = trimmed.split("\\s+");
        int total = 0, emphasis = -1, longest = 3;
        for (int i = 0; i < words.length; i++) {
            total += words[i].length();
            int letters = (int) words[i].codePoints().filter(Character::isLetterOrDigit).count();
            if (letters > longest) { longest = letters; emphasis = i; }
        }
        // Words appear across at most 70 % of the line, never slower than one word per 420 ms.
        long span = Math.min((long) ((endMs - startMs) * .7f), 420L * words.length);
        long[] reveal = new long[words.length];
        if (wordMs != null && wordMs.length == words.length) System.arraycopy(wordMs, 0, reveal, 0, words.length);
        else for (int i = 0, before = 0; i < words.length; i++) {
            reveal[i] = startMs + span * before / Math.max(1, total);
            before += words[i].length();
        }
        List<Piece> pieces = new ArrayList<>();
        int first = 0, chars = 0;
        for (int i = 0; i < words.length; i++) {
            chars += words[i].length();
            boolean last = i == words.length - 1;
            boolean ends = last || i - first + 1 >= Math.max(1, maxWords) || chars >= MAX_CHARS
                || chars + words[i + 1].length() > MAX_CHARS
                || PIECE_ENDS.indexOf(words[i].charAt(words[i].length() - 1)) >= 0;
            if (!ends) continue;
            long[] times = new long[i - first + 1];
            System.arraycopy(reveal, first, times, 0, times.length);
            pieces.add(new Piece(String.join(" ", java.util.Arrays.copyOfRange(words, first, i + 1)),
                emphasis >= first && emphasis <= i ? emphasis - first : -1, times, times[0] - LEAD_MS));
            first = i + 1;
            chars = 0;
        }
        return pieces;
    }
}
