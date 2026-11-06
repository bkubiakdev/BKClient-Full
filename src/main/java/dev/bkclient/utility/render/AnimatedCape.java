package dev.bkclient.utility.render;

import dev.bkclient.BKClientInit;
import dev.bkclient.mixin.accessors.NativeImageAccessor;
import dev.bkclient.utility.concurrent.ClientAsyncExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

/** One GPU texture; compressed animation is decoded once off the render thread. */
public final class AnimatedCape {
    public static final int FRAME_COUNT = 150, FACE_WIDTH = 160, FACE_HEIGHT = 256;
    public static final int TEXTURE_WIDTH = 1024, TEXTURE_HEIGHT = 512;
    public static final int FRAME_BYTES = FACE_WIDTH * FACE_HEIGHT * 4;
    public static final long LOOP_NANOS = 4_990_000_000L;
    private static final Identifier ID = Identifier.of("bkclient", "dynamic/bk_animated_cape");
    private static final Identifier FIRST = Identifier.of("bkclient", "textures/capes/animated/bk/000.png");
    private static final Identifier DATA = Identifier.of("bkclient", "textures/capes/animated/bk/frames.rgba.gz");
    private static volatile byte[][] frames;
    private static volatile boolean loading, failed;
    private static long generation;
    private static long started;
    private static NativeImageBackedTexture texture;
    private static ByteBuffer pixels;
    private static int uploaded = -1;
    private AnimatedCape() {}
    public static int frameIndex(long elapsedNanos) {
        return (int)(Math.floorMod(elapsedNanos, LOOP_NANOS) * FRAME_COUNT / LOOP_NANOS);
    }
    /** Fixed-size, bounded decoder also used by the asset regression test. */
    public static byte[][] decodeFrames(InputStream compressed) throws IOException {
        try (var input = new GZIPInputStream(compressed)) {
            byte[][] result = new byte[FRAME_COUNT][];
            for (int i = 0; i < FRAME_COUNT; i++) {
                result[i] = input.readNBytes(FRAME_BYTES);
                if (result[i].length != FRAME_BYTES) throw new IOException("Incomplete animated cape frame " + i);
            }
            if (input.read() != -1) throw new IOException("Unexpected animated cape data");
            return result;
        }
    }
    private static synchronized void requestFrames() {
        if (frames != null || loading || failed) return;
        loading = true;
        long requestedGeneration = generation;
        var resources = MinecraftClient.getInstance().getResourceManager();
        ClientAsyncExecutor.supply(() -> {
            try (var input = resources.getResource(DATA).orElseThrow().getInputStream()) {
                return decodeFrames(input);
            } catch (Exception error) { throw new java.util.concurrent.CompletionException(error); }
        }).whenComplete((decoded, error) -> {
            synchronized (AnimatedCape.class) {
                if (requestedGeneration != generation) return;
                loading = false;
                if (error != null) { failed = true; BKClientInit.LOGGER.warn("Animated cape could not be loaded", error); }
                else frames = decoded;
            }
        });
    }
    public static Identifier currentTexture() {
        requestFrames();
        byte[][] available = frames;
        if (available == null || failed) return FIRST;
        var mc = MinecraftClient.getInstance();
        try {
            if (texture == null || mc.getTextureManager().getTexture(ID) != texture || texture.getImage() == null) {
                try (var input = mc.getResourceManager().getResource(FIRST).orElseThrow().getInputStream()) {
                    NativeImage image = NativeImage.read(NativeImage.Format.RGBA, input);
                    if (image.getWidth() != TEXTURE_WIDTH || image.getHeight() != TEXTURE_HEIGHT) {
                        image.close();
                        throw new IOException("Invalid animated cape atlas dimensions");
                    }
                    texture = new NativeImageBackedTexture(image);
                    pixels = MemoryUtil.memByteBuffer(((NativeImageAccessor)(Object)image).getPointer(), TEXTURE_WIDTH * TEXTURE_HEIGHT * 4);
                    mc.getTextureManager().registerTexture(ID, texture);
                    started = System.nanoTime(); uploaded = -1;
                }
            }
            int index = frameIndex(System.nanoTime() - started);
            if (index != uploaded) {
                byte[] frame = available[index];
                for (int y = 0; y < FACE_HEIGHT; y++) {
                    int source = y * FACE_WIDTH * 4;
                    pixels.position(((y + 16) * TEXTURE_WIDTH + 16) * 4); pixels.put(frame, source, FACE_WIDTH * 4);
                    pixels.position(((y + 16) * TEXTURE_WIDTH + 192) * 4); pixels.put(frame, source, FACE_WIDTH * 4);
                }
                texture.upload(); uploaded = index;
            }
            return ID;
        } catch (Exception error) {
            failed = true; releaseTexture();
            BKClientInit.LOGGER.warn("Animated cape texture upload failed", error);
            return FIRST;
        }
    }
    public static void releaseTexture() {
        if (texture != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(ID);
        texture = null; pixels = null; uploaded = -1;
    }
    public static synchronized void shutdown() {
        generation++; loading = false; frames = null; failed = false;
        releaseTexture();
    }
}
