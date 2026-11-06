package dev.bkclient.utility.render.ambience;

/**
 * Decides when Ambience dynamic brightness must rebuild terrain and which chunk sections it covers.
 *
 * <p>Light levels are whole numbers, so terrain only needs rebuilding when the light's block,
 * radius or level changes. The old and new reach are refreshed as one box of sections.
 */
public final class DynamicBrightnessArea {
    private DynamicBrightnessArea() {
    }

    /** Whether terrain must be rebuilt for a light at block {@code x, y, z}. */
    public static boolean needsRefresh(boolean active, int[] previousBlock, int previousRadius, int previousLevel,
                                       int x, int y, int z, int radius, int level) {
        return !active || previousBlock == null
            || previousBlock[0] != x || previousBlock[1] != y || previousBlock[2] != z
            || previousRadius != radius || previousLevel != level;
    }

    /** Section box {@code minX, minY, minZ, maxX, maxY, maxZ} reached by a light at the given block. */
    public static int[] sectionBounds(int x, int y, int z, int radius, int padding) {
        int reach = radius + padding;
        return new int[]{(x - reach) >> 4, (y - reach) >> 4, (z - reach) >> 4,
            (x + reach) >> 4, (y + reach) >> 4, (z + reach) >> 4};
    }

    /** Smallest section box containing both boxes; {@code null} boxes are ignored. */
    public static int[] union(int[] a, int[] b) {
        if (a == null) return b;
        if (b == null) return a;
        return new int[]{Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
            Math.max(a[3], b[3]), Math.max(a[4], b[4]), Math.max(a[5], b[5])};
    }
}
