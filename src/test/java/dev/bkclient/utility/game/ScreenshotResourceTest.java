package dev.bkclient.utility.game;

import net.minecraft.client.texture.NativeImage;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

public final class ScreenshotResourceTest {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("bk-screenshot-test-");
        var output = directory.resolve("image.png");
        try {
            NativeImage image = new NativeImage(2, 2, true);
            image.setColorArgb(0, 0, 0xffff0000);
            image.setColorArgb(0, 1, 0xff00ff00);
            byte[] png = ScreenshotHelper.encode(image, output.toFile());
            assertClosed(image);
            var decoded = ImageIO.read(new ByteArrayInputStream(png));
            if (decoded.getRGB(0, 0) != 0xff00ff00 || decoded.getRGB(0, 1) != 0xffff0000)
                throw new AssertionError("Screenshot orientation/color changed");
            decoded.flush();
            for (int i = 0; i < 100; i++) {
                NativeImage failed = new NativeImage(64, 64, true);
                try {
                    ScreenshotHelper.encode(failed, directory.resolve("missing/shot.png").toFile());
                    throw new AssertionError("Expected PNG write failure");
                } catch (java.io.IOException expected) { }
                assertClosed(failed);
            }
            System.out.println("PASS: PNG orientation; 100 failed writes release source native pixels");
        } finally { Files.deleteIfExists(output); Files.deleteIfExists(directory); }
    }
    private static void assertClosed(NativeImage image) {
        try { image.getColorArgb(0, 0); throw new AssertionError("Native image still allocated"); }
        catch (IllegalStateException expected) { }
    }
}
