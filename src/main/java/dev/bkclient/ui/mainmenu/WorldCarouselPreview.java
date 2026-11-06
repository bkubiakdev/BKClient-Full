package dev.bkclient.ui.mainmenu;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.WorldSavePath;

/** One local, HUD-free thumbnail per world session. The vanilla icon is never overwritten. */
public final class WorldCarouselPreview {
    public static final String FILE_NAME = "bkclient-preview.png";
    private static Object world;
    private static long readyAt;
    private static boolean captured;
    private WorldCarouselPreview() {}

    public static void captureIfReady() {
        var mc = MinecraftClient.getInstance();
        if (world != mc.world) {
            world = mc.world;
            readyAt = System.nanoTime() + 10_000_000_000L;
            captured = false;
        }
        if (captured || mc.world == null || mc.player == null || mc.getServer() == null
                || !MenuPreferences.Option.WORLD_CAROUSEL.enabled || mc.currentScreen != null
                || mc.player.age < 200 || System.nanoTime() < readyAt) return;
        var framebuffer = mc.getFramebuffer();
        if (framebuffer == null || framebuffer.textureWidth < 64 || framebuffer.textureHeight < 64) return;
        captured = true;
        Path output = mc.getServer().getSavePath(WorldSavePath.ROOT).resolve(FILE_NAME);
        try (NativeImage source = new NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);
             NativeImage thumbnail = new NativeImage(512, 512, false)) {
            framebuffer.beginRead();
            try { source.loadFromTextureImage(0, false); }
            finally { framebuffer.endRead(); }
            int side = Math.min(source.getWidth(), source.getHeight());
            source.resizeSubRectTo((source.getWidth()-side)/2, (source.getHeight()-side)/2, side, side, thumbnail);
            var image = new java.awt.image.BufferedImage(512,512,java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int y=0;y<512;y++) for(int x=0;x<512;x++)
                image.setRGB(x,511-y,thumbnail.getColorArgb(x,y));
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                Path temporary = null;
                try {
                    if (!Files.isDirectory(output.getParent()) || Files.isSymbolicLink(output)) return;
                    temporary = Files.createTempFile(output.getParent(), "bk-preview-", ".tmp");
                    javax.imageio.ImageIO.write(image,"png",temporary.toFile());
                    Files.move(temporary,output,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                } finally {
                    if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Exception ignored) {}
                }
            });
        } catch (Exception ignored) { }
    }
}

