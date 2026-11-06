package dev.bkclient.utility.render;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;

public final class AnimatedCapeTest {
    public static void main(String[] args) throws Exception {
        require(AnimatedCape.frameIndex(0) == 0, "First frame");
        require(AnimatedCape.frameIndex(AnimatedCape.LOOP_NANOS - 1) == 149, "Last frame");
        require(AnimatedCape.frameIndex(AnimatedCape.LOOP_NANOS) == 0, "Loop boundary");
        require(AnimatedCape.frameIndex(AnimatedCape.LOOP_NANOS / 2) == 75, "Correct playback timing");
        require(AnimatedCape.frameIndex(Long.MAX_VALUE) < 150, "Clock arithmetic stays bounded");
        Path dir = Path.of("src/main/resources/assets/bkclient/textures/capes/animated/bk");
        try (var files = Files.list(dir)) {
            require(files.filter(p -> p.toString().endsWith(".png")).count() == 1, "Only one fallback GPU texture");
        }
        byte[][] frames;
        try (var input = Files.newInputStream(dir.resolve("frames.rgba.gz"))) {
            frames = AnimatedCape.decodeFrames(input);
        }
        require(frames.length == 150, "Complete animation");
        for (byte[] frame : frames) require(frame.length == 160 * 256 * 4, "Full-resolution cape face");
        require(!Arrays.equals(frames[0], frames[75]), "Animation changes over time");
        var image = ImageIO.read(dir.resolve("000.png").toFile());
        require(image.getWidth() == 1024 && image.getHeight() == 512, "Cape atlas dimensions");
        for (int y = 0; y < 256; y++) for (int x = 0; x < 160; x++) {
            int offset = (y * 160 + x) * 4;
            int argb = ((frames[0][offset + 3] & 255) << 24)
                    | ((frames[0][offset] & 255) << 16)
                    | ((frames[0][offset + 1] & 255) << 8)
                    | (frames[0][offset + 2] & 255);
            require(image.getRGB(16 + x, 16 + y) == argb, "Video fills complete front UV region");
            require(image.getRGB(192 + x, 16 + y) == argb, "Video fills complete back UV region");
        }
        var compressed = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(compressed)) { gzip.write(new byte[16]); }
        try {
            AnimatedCape.decodeFrames(new ByteArrayInputStream(compressed.toByteArray()));
            throw new AssertionError("Truncated animation must fail");
        } catch (IOException expected) { }
        System.out.println("Animated cape: timing, loop, HQ frames, full face UV mapping and invalid data checks passed");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
