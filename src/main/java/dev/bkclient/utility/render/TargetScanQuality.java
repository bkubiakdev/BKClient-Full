package dev.bkclient.utility.render;

/** Supersample the target mask without allocating four 4K framebuffers' worth of pixels. */
public final class TargetScanQuality {
    private static final long PIXEL_BUDGET = 8_388_608;
    private TargetScanQuality() { }

    public static Size maskSize(int width, int height, int maxTextureSize) {
        if (width < 1 || height < 1 || maxTextureSize < 1) throw new IllegalArgumentException("Invalid viewport");
        double scale = Math.max(1, Math.min(2, Math.sqrt(PIXEL_BUDGET / ((double)width * height))));
        scale = Math.min(scale, Math.min((double)maxTextureSize / width, (double)maxTextureSize / height));
        return new Size(Math.max(1, (int)(width * scale)), Math.max(1, (int)(height * scale)));
    }

    public record Size(int width, int height) { }
}
