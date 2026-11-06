package dev.bkclient.utility.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class CustomCapeImagesTest {
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static int pixel(byte[] rgba, int x, int y) {
        int i = (y * CustomCapeImages.WIDTH + x) * 4;
        return ((rgba[i] & 255) << 16) | ((rgba[i+1] & 255) << 8) | (rgba[i+2] & 255);
    }
    public static void main(String[] args) throws Exception {
        BufferedImage plain = new BufferedImage(10, 16, BufferedImage.TYPE_INT_RGB);
        var graphics = plain.createGraphics(); graphics.setColor(java.awt.Color.RED); graphics.fillRect(0, 0, 10, 16); graphics.dispose();
        var output = new ByteArrayOutputStream(); ImageIO.write(plain, "png", output);
        var image = CustomCapeImages.decode(output.toByteArray());
        require(image.frames().size() == 1, "Static PNG");
        byte[] face = image.frames().getFirst().rgba();
        require(pixel(face, 16, 16) == 0xff0000 && pixel(face, 175, 271) == 0xff0000, "Full front coverage");
        require(pixel(face, 192, 16) == 0xff0000 && pixel(face, 351, 271) == 0xff0000, "Full back coverage");
        BufferedImage atlas = new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB);
        graphics = atlas.createGraphics(); graphics.setColor(java.awt.Color.BLUE); graphics.fillRect(1, 1, 10, 16);
        graphics.setColor(java.awt.Color.GREEN); graphics.fillRect(12, 1, 10, 16); graphics.dispose();
        byte[] mapped = CustomCapeImages.map(atlas);
        require(pixel(mapped, 80, 128) == 0x0000ff && pixel(mapped, 256, 128) == 0x00ff00, "Standard atlas keeps different faces");
        var gif = CustomCapeImages.decode(Files.readAllBytes(Path.of("src/test/resources/capes/disposal.gif")));
        require(gif.frames().size() == 3 && gif.durationMillis() == 360, "GIF frame count and timing");
        require(gif.index(79) == 0 && gif.index(80) == 1 && gif.index(200) == 2 && gif.index(360) == 0, "GIF playback and loop");
        require(pixel(gif.frames().get(1).rgba(), 32, 32) == 0x0000ff, "GIF partial overlay");
        require(pixel(gif.frames().get(2).rgba(), 32, 32) == 0xff0000, "GIF restore-to-previous disposal");
        try { CustomCapeImages.decode(new byte[]{1,2,3}); throw new AssertionError("Invalid input accepted"); }
        catch (IOException expected) { }
        var huge = new BufferedImage(2049, 1, BufferedImage.TYPE_INT_RGB);
        output.reset(); ImageIO.write(huge, "png", output);
        try { CustomCapeImages.decode(output.toByteArray()); throw new AssertionError("Oversized input accepted"); }
        catch (IOException expected) { }
        System.out.println("Custom cape: PNG fit, atlas UVs, GIF timing/disposal, invalid and oversized inputs passed");
    }
}
