package dev.bkclient.utility.render;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import javax.imageio.ImageIO;

public final class BoundedImageReaderTest {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("bk-background-test-");
        var file = directory.resolve("large.png");
        var invalid = directory.resolve("invalid.png");
        try {
            var original = new BufferedImage(4096, 2048, BufferedImage.TYPE_INT_ARGB);
            original.setRGB(0, 0, 0x80402010);
            ImageIO.write(original, "png", file.toFile()); original.flush();
            for (int i = 0; i < 20; i++) {
                var decoded = BoundedImageReader.read(file.toFile(), 256);
                if (decoded.getWidth() > 256 || decoded.getHeight() > 256) throw new AssertionError("Unbounded decoded image");
                if (decoded.getWidth() != 256 || decoded.getHeight() != 128) throw new AssertionError("Aspect ratio changed");
                if (decoded.getRGB(0, 0) != 0x80402010) throw new AssertionError("Alpha/color changed");
                decoded.flush();
            }
            Files.writeString(invalid, "not an image");
            for (int i = 0; i < 20; i++) {
                try { BoundedImageReader.read(invalid.toFile(), 256); throw new AssertionError("Invalid image accepted"); }
                catch (java.io.IOException expected) { }
            }
            // Windows cannot delete these files if an ImageIO stream is still open.
            Files.delete(file); Files.delete(invalid);
            System.out.println("PASS: bounded image dimensions, aspect, alpha, repeated errors and stream cleanup");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(invalid); Files.deleteIfExists(directory); }
    }
}
