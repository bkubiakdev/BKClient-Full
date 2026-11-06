package dev.bkclient.utility.game;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Capture only on the render thread. Every native image is closed, including failed PNG writes. */
public final class ScreenshotHelper {
    private ScreenshotHelper() {}
    public static byte[] takeScreenshotBytes() {
        File tmp = null;
        try {
            var fb = MinecraftClient.getInstance().getFramebuffer();
            if (fb == null || fb.textureWidth <= 0 || fb.textureHeight <= 0) return null;
            // Keep the outer ownership scope: a failure during readback must also release pixels.
            try (NativeImage image = new NativeImage(fb.textureWidth, fb.textureHeight, false)) {
                fb.beginRead();
                try { image.loadFromTextureImage(0, false); }
                finally { fb.endRead(); }
                tmp = File.createTempFile("bkclient_shot_", ".png");
                return encode(image, tmp);
            }
        } catch (Exception failure) {
            return null;
        } finally {
            if (tmp != null) tmp.delete();
        }
    }

    /** Takes ownership of source even if output creation/write fails. Package-visible for failure-path tests. */
    static byte[] encode(NativeImage source, File output) throws IOException {
        try (source; NativeImage flipped = new NativeImage(source.getWidth(), source.getHeight(), false)) {
            int w = source.getWidth(), h = source.getHeight();
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
                flipped.setColorArgb(x, h - 1 - y, source.getColorArgb(x, y));
            flipped.writeTo(output);
            return Files.readAllBytes(output.toPath());
        }
    }
}
