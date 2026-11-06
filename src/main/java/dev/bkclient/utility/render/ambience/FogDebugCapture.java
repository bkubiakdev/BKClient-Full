package dev.bkclient.utility.render.ambience;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

/** Opt-in, single-frame local capture. No continuous readback and no changes to effect settings. */
public final class FogDebugCapture implements IMinecraft {
    private static boolean enabled, keyDown;
    private static long due, expires;
    private static Object requestedWorld;
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static final java.util.concurrent.ExecutorService WRITER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "bk-fog-debug-writer"); thread.setDaemon(true); return thread;
    });
    private static Capture active;
    private static String lastResult = "No capture yet";

    public static void command(String action) {
        switch (action == null ? "status" : action) {
            case "on" -> { enabled = true; say("Fog debug ON. Press F8 in the world to capture one frame."); }
            case "off" -> { enabled = false; due = 0; requestedWorld = null; say("Fog debug OFF."); }
            case "capture" -> {
                if (mc.world == null) { say("Enter a world first."); return; }
                if (BUSY.get() || due != 0) { say("A capture is already pending or saving."); return; }
                due = System.nanoTime() + 3_000_000_000L; expires = due + 10_000_000_000L;
                requestedWorld = mc.world;
                say("Capture in 3 seconds. Close chat and look at the affected player.");
            }
            default -> say("Fog debug " + (enabled ? "ON (F8)" : "OFF") + "; "
                + (BUSY.get() ? "saving" : due != 0 ? "pending" : lastResult));
        }
    }

    public static void beginFrame(Matrix4f view, Matrix4f projection) {
        if (!enabled && due == 0) return;
        boolean pressed = enabled && GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_F8) == GLFW.GLFW_PRESS;
        boolean trigger = pressed && !keyDown && mc.currentScreen == null;
        keyDown = pressed;
        long now = System.nanoTime();
        if (due != 0 && (requestedWorld != mc.world || now > expires)) {
            due = 0; requestedWorld = null; say("Pending fog capture cancelled (world changed or screen stayed open).");
        }
        if (due != 0 && now >= due && mc.currentScreen == null) {
            trigger = true; due = 0; requestedWorld = null;
        }
        if (!trigger || mc.world == null || !BUSY.compareAndSet(false, true)) return;
        try {
            Capture capture = new Capture();
            active = capture;
            capture.folder = mc.runDirectory.toPath().resolve("BK-Client/fog-debug").resolve(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + "-" + Long.toUnsignedString(now, 36));
            capture.meta.put("schema", 1);
            capture.meta.put("capturedAt", java.time.Instant.now().toString());
            capture.meta.put("view", view.get(new float[16]));
            capture.meta.put("projection", projection.get(new float[16]));
            capture.inverse = new Matrix4f(projection).mul(view).invert();
            var camera = mc.gameRenderer.getCamera();
            var pos = camera.getPos();
            capture.meta.put("camera", new double[]{pos.x, pos.y, pos.z, camera.getYaw(), camera.getPitch()});
            capture.meta.put("submersion", camera.getSubmersionType().name());
            capture.meta.put("renderDistanceChunks", mc.options.getViewDistance().getValue());
            capture.meta.put("glVersion", GL11C.glGetString(GL11C.GL_VERSION));
            capture.meta.put("glRenderer", GL11C.glGetString(GL11C.GL_RENDERER));
            capture.meta.put("worldPostEntry", state());
            JsonObject modules = new JsonObject();
            for (var module : BKClientInit.getInstance().getModuleManager().getModules()) {
                if (!java.util.Set.of("Ambience", "Custom Fog", "Hands Shader", "ESP", "Nametags").contains(module.getName())) continue;
                JsonObject data = new JsonObject(), settings = new JsonObject();
                data.addProperty("enabled", module.isEnabled());
                for (var setting : module.getSettings()) settings.add(setting.getName(), setting.save());
                data.add("settings", settings); modules.add(module.getName(), data);
            }
            capture.meta.put("modules", modules);
        } catch (RuntimeException e) { fail(e); }
    }

    public static void beforeFog(Framebuffer scene, int depth, int noise, int program, float resolution) {
        Capture capture = active;
        if (capture == null) return;
        try {
            capture.meta.put("fogInputState", state());
            capture.meta.put("fogResolution", resolution);
            capture.meta.put("textureIds", new int[]{scene.getColorAttachment(), depth, noise});
            Map<String, Object> uniforms = new LinkedHashMap<>();
            for (String name : new String[]{"Density", "Coverage", "LayerY", "Thickness", "MaxDistance", "TileScale", "StepWorld"}) {
                int location = GL20C.glGetUniformLocation(program, name);
                if (location >= 0) uniforms.put(name, GL20C.glGetUniformf(program, location));
            }
            for (String name : new String[]{"InvViewProj", "CamPos", "FogTint", "Flow", "FlowPatch"}) {
                int location = GL20C.glGetUniformLocation(program, name);
                if (location < 0) continue;
                float[] values = new float[name.equals("InvViewProj") ? 16 : name.startsWith("Flow") ? 2 : 3];
                GL20C.glGetUniformfv(program, location, values); uniforms.put(name, values);
                if (name.equals("InvViewProj")) capture.inverse = new Matrix4f().set(values);
            }
            capture.meta.put("fogUniforms", uniforms);
            capture.images.put("01-before-fog", FogTextureReadback.read(scene.getColorAttachment(), false));
            capture.depth = FogTextureReadback.read(depth, true);
            capture.images.put("02-noise", FogTextureReadback.read(noise, false));
            capture.meta.put("afterInputReadback", state());
        } catch (RuntimeException e) { fail(e); }
    }

    public static void afterFog(Framebuffer fog, Framebuffer result) {
        if (active == null) return;
        try {
            active.images.put("03-fog-layer", FogTextureReadback.read(fog.getColorAttachment(), false));
            active.images.put("04-after-fog", FogTextureReadback.read(result.getColorAttachment(), false));
            active.meta.put("fogOutputState", state());
        } catch (RuntimeException e) { fail(e); }
    }

    public static void markState(String name) {
        if (active == null) return;
        try { active.meta.put(name, state()); }
        catch (RuntimeException e) { fail(e); }
    }

    public static void endFrame() {
        Capture capture = active;
        if (capture == null) return;
        try {
            capture.meta.put("worldPostExit", state());
            capture.images.put("05-world-final", FogTextureReadback.read(mc.getFramebuffer().getColorAttachment(), false));
            capture.meta.put("fogPassCaptured", capture.depth != null && capture.images.containsKey("04-after-fog"));
            active = null;
            WRITER.execute(() -> {
                String result;
                try { write(capture); result = "Saved: " + capture.folder + (capture.depth == null ? " (no fog pass in this frame)" : ""); }
                catch (Exception e) { result = "Fog capture save failed: " + e; }
                String message = result;
                mc.execute(() -> { lastResult = message; BUSY.set(false); say(message); });
            });
        } catch (RuntimeException e) { fail(e); }
    }

    private static Map<String, Object> state() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("program", GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM));
        state.put("readFbo", GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING));
        state.put("drawFbo", GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING));
        state.put("depthTest", GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST));
        state.put("depthWrite", GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK));
        state.put("depthFunc", GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC));
        state.put("blend", GL11C.glIsEnabled(GL11C.GL_BLEND));
        state.put("blendFactors", new int[]{GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB), GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB),
            GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA), GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA)});
        state.put("framebufferSrgb", GL11C.glIsEnabled(GL30C.GL_FRAMEBUFFER_SRGB));
        state.put("shaderColor", RenderSystem.getShaderColor().clone());
        int[] viewport = new int[4]; GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport); state.put("viewport", viewport);
        int unit = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        int[] textures = new int[3], cached = new int[3], samplers = new int[3];
        try {
            for (int i = 0; i < 3; i++) {
                GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + i);
                textures[i] = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
                cached[i] = RenderSystem.getShaderTexture(i);
                if (GL.getCapabilities().OpenGL33) samplers[i] = GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING);
            }
        } finally { GL13C.glActiveTexture(unit); }
        state.put("activeTexture", unit); state.put("textures", textures); state.put("shaderTextures", cached); state.put("samplers", samplers);
        return state;
    }

    private static void write(Capture capture) throws IOException {
        Files.createDirectories(capture.folder);
        Map<String, Object> dimensions = new LinkedHashMap<>();
        for (var entry : capture.images.entrySet()) {
            var pixels = entry.getValue(); dimensions.put(entry.getKey(), new int[]{pixels.width(), pixels.height()});
            BufferedImage image = new BufferedImage(pixels.width(), pixels.height(), BufferedImage.TYPE_INT_ARGB);
            BufferedImage opacity = entry.getKey().equals("03-fog-layer") ? new BufferedImage(pixels.width(), pixels.height(), BufferedImage.TYPE_INT_RGB) : null;
            for (int y = 0; y < pixels.height(); y++) for (int x = 0; x < pixels.width(); x++) {
                int i = (y * pixels.width() + x) * 4;
                int r = pixels.bytes()[i] & 255, g = pixels.bytes()[i + 1] & 255, b = pixels.bytes()[i + 2] & 255, a = pixels.bytes()[i + 3] & 255;
                image.setRGB(x, pixels.height() - 1 - y, a << 24 | r << 16 | g << 8 | b);
                if (opacity != null) opacity.setRGB(x, pixels.height() - 1 - y, a << 16 | a << 8 | a);
            }
            ImageIO.write(image, "png", capture.folder.resolve(entry.getKey() + ".png").toFile());
            if (opacity != null) ImageIO.write(opacity, "png", capture.folder.resolve("03-fog-opacity.png").toFile());
        }
        if (capture.depth != null) {
            var depth = capture.depth;
            dimensions.put("depth", new int[]{depth.width(), depth.height()});
            try (var out = new GZIPOutputStream(Files.newOutputStream(capture.folder.resolve("depth-f32le-bottom-up.bin.gz")))) { out.write(depth.bytes()); }
            BufferedImage image = new BufferedImage(depth.width(), depth.height(), BufferedImage.TYPE_INT_RGB);
            var values = ByteBuffer.wrap(depth.bytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
            Vector4f point = new Vector4f();
            for (int y = 0; y < depth.height(); y++) for (int x = 0; x < depth.width(); x++) {
                float d = values.get(y * depth.width() + x);
                point.set(2f * (x + 0.5f) / depth.width() - 1f, 2f * (y + 0.5f) / depth.height() - 1f, 2f * d - 1f, 1f);
                capture.inverse.transform(point);
                double distance = Math.sqrt(point.x * point.x + point.y * point.y + point.z * point.z) / Math.abs(point.w);
                int shade = d >= 1f ? 255 : (int) Math.round(255 * Math.min(1, Math.log1p(distance) / Math.log(161)));
                if (!Double.isFinite(distance)) shade = 255;
                shade = Math.max(0, Math.min(255, shade));
                image.setRGB(x, depth.height() - 1 - y, shade << 16 | shade << 8 | shade);
            }
            ImageIO.write(image, "png", capture.folder.resolve("02-depth-distance.png").toFile());
        }
        capture.meta.put("dimensions", dimensions);
        Files.writeString(capture.folder.resolve("capture.json"), new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create().toJson(capture.meta));
        Files.writeString(capture.folder.resolve("README.txt"), "One world frame, before HUD/hand. 01 = input to fog; 03 = premultiplied fog RGBA and opaque alpha preview; 04 = immediately after fog; 05 = after remaining world effects.\nDepth raw: IEEE754 float32 little endian, rows bottom-up, dimensions in capture.json. PNGs are top-down. Depth preview: log(1+distance)/log(161), white = 160+ blocks or sky.\nA missing fog pass means fog was off, unavailable or skipped (e.g. submerged camera). Capturing can stall this frame; do not use it as an FPS benchmark.\n");
    }

    private static void fail(Exception e) { active = null; BUSY.set(false); lastResult = "Fog debug failed: " + e; say(lastResult); }
    private static void say(String text) { MessageUtility.info(Text.literal(text)); }
    private static final class Capture {
        Path folder;
        Matrix4f inverse;
        FogTextureReadback.Pixels depth;
        final Map<String, Object> meta = new LinkedHashMap<>();
        final Map<String, FogTextureReadback.Pixels> images = new LinkedHashMap<>();
    }
    private FogDebugCapture() {}
}
