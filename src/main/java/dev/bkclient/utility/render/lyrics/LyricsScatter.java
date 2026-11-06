package dev.bkclient.utility.render.lyrics;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Stable placement for the Scattered display: the verse is split into short pieces of one to a few words, each piece
 * gets its own spot around the anchor (spread, depth, tilt, a slight arc) and the longest word of a piece can be
 * emphasised. Everything is derived from a per-verse seed, so a verse never jumps while it is on screen.
 */
public final class LyricsScatter {
    /** {@code start}/{@code width} are in the single-row layout; {@code center} is the word centre inside its piece. */
    public record Word(int chunk, float start, float width, float center, float scale, boolean emphasis, int firstEnd) { }
    /** Piece position in layout units, rotations in radians; {@code arc} is the bend height in layout units. */
    public record Chunk(float x, float y, float z, float yaw, float roll, float arc, float halfWidth, int firstWord) { }
    /** Depth is given in metres; at the usual verse scale one metre is about this many layout units. */
    public static final float UNITS_PER_METRE = 40;
    private static final String PIECE_ENDS = ",.!?;:…";
    public final Word[] words;
    public final Chunk[] chunks;

    private LyricsScatter(Word[] words, Chunk[] chunks) { this.words = words; this.chunks = chunks; }

    /** One centred, flat piece (a Scattered piece placed on its own in the world); {@code emphasisWord} -1 = none. */
    public static LyricsScatter single(LyricsLayout layout, int emphasisWord, float emphasisScale) {
        int count = 0;
        for (var g : layout.glyphs()) count = Math.max(count, g.word() + 1);
        if (count == 0) return new LyricsScatter(new Word[0], new Chunk[0]);
        float[] start = new float[count], end = new float[count];
        int[] firstEnd = new int[count];
        Arrays.fill(start, Float.MAX_VALUE);
        Arrays.fill(end, -Float.MAX_VALUE);
        Arrays.fill(firstEnd, Integer.MAX_VALUE);
        for (var g : layout.glyphs()) {
            int w = g.word();
            firstEnd[w] = Math.min(firstEnd[w], g.end());
            start[w] = Math.min(start[w], g.x());
            end[w] = Math.max(end[w], g.x() + g.advance());
        }
        float gap = 3.5f, cursor = 0;
        float[] center = new float[count];
        for (int w = 0; w < count; w++) {
            float width = (end[w] - start[w]) * (w == emphasisWord ? emphasisScale : 1);
            center[w] = cursor + width / 2;
            cursor += width + gap;
        }
        float total = Math.max(1, cursor - gap);
        Word[] words = new Word[count];
        for (int w = 0; w < count; w++)
            words[w] = new Word(0, start[w], end[w] - start[w], center[w] - total / 2, w == emphasisWord ? emphasisScale : 1, w == emphasisWord, firstEnd[w]);
        return new LyricsScatter(words, new Chunk[]{new Chunk(0, 0, 0, 0, 0, 0, total / 2, 0)});
    }

    public static LyricsScatter build(LyricsLayout layout, long seed, float spread, float depthMetres, float tiltDegrees,
                                      float arcPercent, int maxWords, boolean emphasis, float emphasisScale) {
        int count = 0;
        for (var g : layout.glyphs()) count = Math.max(count, g.word() + 1);
        if (count == 0) return new LyricsScatter(new Word[0], new Chunk[0]);
        float[] start = new float[count], end = new float[count];
        int[] letters = new int[count], firstEnd = new int[count];
        boolean[] stop = new boolean[count];
        Arrays.fill(start, Float.MAX_VALUE);
        Arrays.fill(end, -Float.MAX_VALUE);
        Arrays.fill(firstEnd, Integer.MAX_VALUE);
        for (var g : layout.glyphs()) {
            int w = g.word();
            firstEnd[w] = Math.min(firstEnd[w], g.end()); // the word starts showing once this glyph is revealed
            start[w] = Math.min(start[w], g.x());
            end[w] = Math.max(end[w], g.x() + g.advance());
            if (Character.isLetterOrDigit(g.codepoint())) letters[w]++;
            stop[w] = PIECE_ENDS.indexOf(g.codepoint()) >= 0; // the last glyph of the word decides
        }
        SplittableRandom random = new SplittableRandom(seed);

        // Consecutive words form pieces; punctuation closes a piece early.
        int[] chunkOf = new int[count];
        int chunks = 0;
        for (int w = 0; w < count; ) {
            int size = 1 + random.nextInt(Math.max(1, maxWords)), chunk = chunks++;
            for (int k = 0; k < size && w < count; k++) {
                chunkOf[w] = chunk;
                if (stop[w++]) break;
            }
        }

        // The longest word of a piece carries the emphasis; lone words only about half the time, so it stays an accent.
        boolean[] emph = new boolean[count];
        if (emphasis) for (int c = 0; c < chunks; c++) {
            int best = -1, size = 0;
            for (int w = 0; w < count; w++) {
                if (chunkOf[w] != c) continue;
                size++;
                if (letters[w] >= 4 && (best < 0 || end[w] - start[w] > end[best] - start[best])) best = w;
            }
            if (best >= 0 && (size > 1 || random.nextBoolean())) emph[best] = true;
        }
        float[] scale = new float[count];
        for (int w = 0; w < count; w++) scale[w] = emph[w] ? emphasisScale : .85f;

        // Each piece is laid out on its own centred line.
        float[] center = new float[count], half = new float[chunks], halfHeight = new float[chunks];
        int[] firstWord = new int[chunks];
        for (int c = 0, w = 0; c < chunks; c++) {
            int first = w;
            firstWord[c] = first;
            while (w < count && chunkOf[w] == c) w++;
            float tallest = 0;
            for (int i = first; i < w; i++) tallest = Math.max(tallest, scale[i]);
            float gap = 3.5f * tallest, cursor = 0;
            for (int i = first; i < w; i++) {
                float width = (end[i] - start[i]) * scale[i];
                center[i] = cursor + width / 2;
                cursor += width + gap;
            }
            float total = Math.max(1, cursor - gap);
            for (int i = first; i < w; i++) center[i] -= total / 2;
            half[c] = total / 2;
            halfHeight[c] = 6 * tallest;
        }

        // Golden-angle spiral, wider than tall, then a few passes to push overlapping pieces apart.
        float radius = 70 * spread;
        double phase = random.nextDouble() * Math.PI * 2;
        float[] x = new float[chunks], y = new float[chunks];
        for (int c = 0; c < chunks; c++) {
            double angle = phase + c * 2.39996323;
            double r = chunks == 1 ? radius * .15 : radius * Math.sqrt((c + .5) / chunks);
            x[c] = (float) (Math.cos(angle) * r * 1.7 + (random.nextDouble() - .5) * 18 * spread);
            y[c] = (float) (Math.sin(angle) * r * .7 + (random.nextDouble() - .5) * 10 * spread);
        }
        for (int pass = 0; pass < 24; pass++) {
            boolean moved = false;
            for (int i = 0; i < chunks; i++) for (int j = i + 1; j < chunks; j++) {
                float dx = x[j] - x[i], dy = y[j] - y[i];
                float ox = half[i] + half[j] + 6 - Math.abs(dx), oy = halfHeight[i] + halfHeight[j] + 4 - Math.abs(dy);
                if (ox <= 0 || oy <= 0) continue;
                moved = true;
                if (ox < oy * 2.5f) { float push = ox / 2 * (dx < 0 ? -1 : 1); x[i] -= push; x[j] += push; }
                else { float push = oy / 2 * (dy < 0 ? -1 : 1); y[i] -= push; y[j] += push; }
            }
            if (!moved) break;
        }
        float meanX = 0, meanY = 0;
        for (int c = 0; c < chunks; c++) { meanX += x[c]; meanY += y[c]; }
        meanX /= chunks; meanY /= chunks;

        float tilt = (float) Math.toRadians(tiltDegrees);
        Chunk[] placed = new Chunk[chunks];
        for (int c = 0; c < chunks; c++) {
            float z = (float) (random.nextDouble() * 2 - 1) * depthMetres * UNITS_PER_METRE;
            float yaw = (float) (random.nextDouble() * 2 - 1) * tilt, roll = (float) (random.nextDouble() * 2 - 1) * tilt * .6f;
            float arc = (float) (random.nextDouble() * 2 - 1) * arcPercent / 100f * Math.max(4, half[c] * .35f);
            placed[c] = new Chunk(x[c] - meanX, y[c] - meanY, z, yaw, roll, arc, half[c], firstWord[c]);
        }
        Word[] words = new Word[count];
        for (int w = 0; w < count; w++) words[w] = new Word(chunkOf[w], start[w], end[w] - start[w], center[w], scale[w], emph[w], firstEnd[w]);
        return new LyricsScatter(words, placed);
    }
}
