package dev.bkclient.utility.render.lyrics;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.VisualRenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * Lyrics 3D comets: short, fast streaks that cross the sky in front of the player ({@link LyricsCometTimeline}),
 * drawn with the shared world streak shader (ribbon tail + glowing head).
 */
public final class LyricsComets {
    public static final int LIMIT = LyricsCometTimeline.LIMIT;
    private static final ShaderProgramKey KEY = new ShaderProgramKey(Identifier.of("bkclient", "core/sky_effects/world_streak"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    private final LyricsCometTimeline timeline = new LyricsCometTimeline();
    private final Vector3f right = new Vector3f(), up = new Vector3f(), side = new Vector3f();
    private long start, retryAt;

    public void reset() { timeline.reset(); start = 0; }

    public void render(Render3DEvent event, int count, ColorRGBA color, float size, float opacity) {
        count = Math.max(0, Math.min(LIMIT, count));
        if (count == 0 || opacity <= 0) { reset(); return; }
        long now = System.nanoTime();
        if (start == 0) start = now;
        if (now < retryAt) return;
        ShaderProgram program;
        try { program = MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(KEY); }
        catch (Exception error) {
            retryAt = now + 5_000_000_000L;
            BKClientInit.LOGGER.warn("Unable to load comet shader", error);
            return;
        }
        if (program == null) return;
        var camera = event.getCamera();
        var pos = camera.getPos();
        Vector3f forward = new Vector3f(0, 0, -1).rotate(camera.getRotation());
        timeline.sample((now - start) / 1e9, count, size, pos.x, pos.y, pos.z, Math.atan2(forward.x, forward.z));
        float[] heads = timeline.heads, travel = timeline.travel, tails = timeline.tails;
        right.set(1, 0, 0).rotate(camera.getRotation());
        up.set(0, 1, 0).rotate(camera.getRotation());
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        try (var state = new VisualRenderState()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
            RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL); RenderSystem.depthMask(false); RenderSystem.disableCull();
            RenderSystem.setShader(program);
            program.getUniform("WorldView").set(event.getPositionMatrix());
            program.getUniform("WorldProjection").set(event.getProjectionMatrix());
            program.getUniform("SquareHead").set(0f);
            program.getUniform("Brightness").set(1f);
            for (int pass = 0; pass < 2; pass++) {
                program.getUniform("Part").set((float) pass);
                BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (int i = 0; i < count; i++) {
                    int p = i * 4, t = i * 3;
                    int alpha = Math.round(heads[p + 3] * opacity * 255);
                    if (alpha < 1) continue;
                    int rgba = alpha << 24 | color.getRGB() & 0xffffff;
                    float x = heads[p], y = heads[p + 1], z = heads[p + 2], r = travel[p + 3];
                    if (pass == 0) {
                        // Tail ribbon along the flight, turned towards the eye.
                        side.set(travel[p], travel[p + 1], travel[p + 2]).cross(x, y, z);
                        if (side.lengthSquared() < 1e-6f) side.set(up);
                        side.normalize().mul(r * 5);
                        vertex(b, tails[t] - side.x, tails[t + 1] - side.y, tails[t + 2] - side.z, 0, -5, rgba);
                        vertex(b, x - side.x, y - side.y, z - side.z, 1, -5, rgba);
                        vertex(b, x + side.x, y + side.y, z + side.z, 1, 5, rgba);
                        vertex(b, tails[t] + side.x, tails[t + 1] + side.y, tails[t + 2] + side.z, 0, 5, rgba);
                    } else {
                        float w = r * 8;
                        vertex(b, x - right.x * w - up.x * w, y - right.y * w - up.y * w, z - right.z * w - up.z * w, -8, -8, rgba);
                        vertex(b, x + right.x * w - up.x * w, y + right.y * w - up.y * w, z + right.z * w - up.z * w, 8, -8, rgba);
                        vertex(b, x + right.x * w + up.x * w, y + right.y * w + up.y * w, z + right.z * w + up.z * w, 8, 8, rgba);
                        vertex(b, x - right.x * w + up.x * w, y - right.y * w + up.y * w, z - right.z * w + up.z * w, -8, 8, rgba);
                    }
                }
                BuiltBuffer built = b.endNullable();
                if (built != null) BufferRenderer.drawWithGlobalProgram(built);
            }
        } finally { RenderSystem.depthFunc(depthFunc); }
    }

    private static void vertex(BufferBuilder b, float x, float y, float z, float u, float v, int color) { b.vertex(x, y, z).texture(u, v).color(color); }
}
