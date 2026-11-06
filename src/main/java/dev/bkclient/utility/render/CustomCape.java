package dev.bkclient.utility.render;

import dev.bkclient.mixin.accessors.NativeImageAccessor;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.utility.concurrent.ClientAsyncExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.system.MemoryUtil;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class CustomCape {
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final Identifier ID = Identifier.of("bkclient", "dynamic/custom_cape");
    private static final Identifier FALLBACK = Identifier.of("bkclient", "textures/capes/default/minecon_2011.png");
    private static String requested;
    private static boolean loading;
    private static long generation, started;
    private static CustomCapeImages.Animation animation;
    private static NativeImageBackedTexture texture;
    private static ByteBuffer pixels;
    private static int uploaded = -1;
    private CustomCape() {}
    public static Path folder() { return FileManager.DIRECTORY.toPath().resolve("capes"); }
    private static void message(String text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("Custom Cape: " + text));
    }
    public static void openFolder() {
        try { Files.createDirectories(folder()); Util.getOperatingSystem().open(folder().toFile()); }
        catch (Exception error) { message("Cannot open cape folder."); }
    }
    public static void load(String source, boolean refresh) {
        String normalized = source == null ? "" : source.trim();
        if (loading && normalized.equals(requested)) return;
        requested = normalized;
        long token = ++generation;
        loading = false;
        if (requested.isEmpty()) { animation = null; release(); message("Enter a PNG/GIF filename or direct URL."); return; }
        String location = requested;
        loading = true;
        message("Loading...");
        ClientAsyncExecutor.supply(() -> {
            try { return read(location, refresh); }
            catch (Exception e) { throw new java.util.concurrent.CompletionException(e); }
        }).whenComplete((result, error) -> MinecraftClient.getInstance().execute(() -> {
            if (token != generation) return;
            loading = false;
            if (error != null) {
                Throwable cause = error;
                while (cause.getCause() != null) cause = cause.getCause();
                if (cause instanceof NoSuchFileException) message("File not found. Use Open Folder and check the filename.");
                else if (cause instanceof java.net.SocketTimeoutException) message("Connection timed out. Try again.");
                else if (cause instanceof java.net.UnknownHostException) message("Cannot reach the server. Check your link and connection.");
                else if (cause instanceof IOException) message(cause.getMessage() == null ? "Could not read the image." : cause.getMessage());
                else message("Invalid file or link. Use PNG, GIF or a Tenor share link.");
                return;
            }
            animation = result; started = System.nanoTime(); uploaded = -1;
            message("Loaded " + result.frames().size() + " frame(s).");
        }));
    }
    private static byte[] bounded(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) throw new IOException("Cape file exceeds 16 MB");
        return bytes;
    }
    private static CustomCapeImages.Animation read(String source, boolean refresh) throws Exception {
        Files.createDirectories(folder());
        String lower = source.toLowerCase(java.util.Locale.ROOT);
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
            Path path = Path.of(source);
            if (!path.isAbsolute()) path = folder().resolve(path);
            try (var input = Files.newInputStream(path)) { return CustomCapeImages.decode(bounded(input)); }
        }
        Path cache = folder().resolve(".cache"); Files.createDirectories(cache);
        String key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
        Path cached = cache.resolve(key + ".cape");
        if (!refresh && Files.isRegularFile(cached)) {
            try (var input = Files.newInputStream(cached)) { return CustomCapeImages.decode(bounded(input)); }
            catch (IOException ignored) { /* Retry damaged cache from source. */ }
        }
        byte[] bytes = CustomCapeLinks.download(URI.create(source));
        var result = CustomCapeImages.decode(bytes);
        try { Files.write(cached, bytes); } catch (IOException ignored) { }
        return result;
    }

    public static Identifier currentTexture(String source) {
        String normalized = source == null ? "" : source.trim();
        if (!normalized.equals(requested)) load(normalized, false);
        if (animation == null) return FALLBACK;
        var mc = MinecraftClient.getInstance();
        try {
            if (texture == null || mc.getTextureManager().getTexture(ID) != texture || texture.getImage() == null) {
                var image = new NativeImage(NativeImage.Format.RGBA, 1024, 512, true);
                texture = new NativeImageBackedTexture(image);
                pixels = MemoryUtil.memByteBuffer(((NativeImageAccessor)(Object)image).getPointer(), 1024 * 512 * 4);
                mc.getTextureManager().registerTexture(ID, texture); uploaded = -1;
            }
            int index = animation.index((System.nanoTime() - started) / 1_000_000L);
            if (index != uploaded) {
                byte[] frame = animation.frames().get(index).rgba();
                for (int y = 0; y < CustomCapeImages.HEIGHT; y++) {
                    pixels.position(y * 1024 * 4);
                    pixels.put(frame, y * CustomCapeImages.WIDTH * 4, CustomCapeImages.WIDTH * 4);
                }
                texture.upload(); uploaded = index;
            }
            return ID;
        } catch (Exception error) { animation = null; release(); message("Texture upload failed."); return FALLBACK; }
    }
    private static void release() {
        if (texture != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(ID);
        texture = null; pixels = null; uploaded = -1;
    }
    public static void shutdown() { generation++; loading = false; requested = null; animation = null; release(); }
}
