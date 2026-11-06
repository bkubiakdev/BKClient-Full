package dev.bkclient.utility.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.HandsShader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;

import static org.lwjgl.opengl.GL33C.*;

/** geometry mask, glow and ping-pong flame trail on the 1.21.4 GL backend. */
public final class FlameHandsRenderer {
    private static Framebuffer hand, glow, trailA, trailB;
    private static Shader glowShader, trailShader, compositeShader;
    private static boolean historyIsA = true;
    private static boolean captured, capturing, historyValid, failed;

    private FlameHandsRenderer() {}

    public static Framebuffer captureTarget() { return capturing ? hand : null; }

    public static void beginFrame(boolean selected) {
        captured = false;
        if (!selected) release();
    }

    public static void capture(Runnable drawHands) {
        if (failed) return;
        RenderSystem.assertOnRenderThread();
        try (State ignored = new State()) {
            if (!ensureTargets()) return;
            hand.setClearColor(0, 0, 0, 0);
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            hand.clear();
            hand.beginWrite(true);
            capturing = true;
            try {
                drawHands.run();
            } finally {
                capturing = false;
            }
            captured = true;
        } catch (RuntimeException error) {
            fail(error);
        }
    }

    public static void render(HandsShader module) {
        if (!captured || failed) {
            // F5, a hidden hand or a frame without geometry must not revive an old trail.
            historyValid = false;
            return;
        }
        captured = false;
        try (State ignored = new State()) {
            ensureShaders();
            RenderSystem.disableScissor();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (!historyValid) {
                clearHistory();
                historyValid = true;
            }

            glow.beginWrite(true);
            glowShader.bind();
            uniforms(glowShader, module);
            texture(glowShader, "HandSampler", 0, hand);
            texture(glowShader, "MaskSampler", 1, hand);
            HandShaderHelper.drawFullScreenQuad();

            Framebuffer previous = historyIsA ? trailA : trailB;
            Framebuffer next = historyIsA ? trailB : trailA;
            next.beginWrite(true);
            trailShader.bind();
            uniforms(trailShader, module);
            texture(trailShader, "TrailSampler", 0, previous);
            texture(trailShader, "GlowSampler", 1, glow);
            texture(trailShader, "HandSampler", 2, hand);
            texture(trailShader, "MaskSampler", 3, hand);
            HandShaderHelper.drawFullScreenQuad();
            historyIsA = !historyIsA;

            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
            RenderSystem.enableBlend();
            // Add RGB while preserving the scene's alpha, matching pipeline.
            RenderSystem.blendFuncSeparate(GL_ONE, GL_ONE, GL_ZERO, GL_ONE);
            compositeShader.bind();
            uniforms(compositeShader, module);
            texture(compositeShader, "MaskSampler", 0, hand);
            texture(compositeShader, "GlowSampler", 1, glow);
            texture(compositeShader, "TrailSampler", 2, next);
            HandShaderHelper.drawFullScreenQuad();
        } catch (RuntimeException error) {
            fail(error);
        }
    }

    private static void uniforms(Shader shader, HandsShader module) {
        float time = (System.currentTimeMillis() % 100000L) / 1000f;
        float speed = module.flameSpeed.getCurrentValue();
        float strength = module.flameStrength.getCurrentValue();
        var color = module.getFlameColor();
        shader.setUniform4f("Screen", hand.textureWidth, hand.textureHeight, time, module.flameFill.isEnabled() ? 1f : 0f);
        shader.setUniform4f("GlowParams", module.flameRadius.getCurrentValue(), strength, speed, module.getFlameColorMix());
        shader.setUniform4f("TrailParams", 0.94f, module.flameHeight.getCurrentValue(), speed, module.flameTrailStrength.getCurrentValue());
        shader.setUniform4f("FireParams", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, strength);
    }

    private static void texture(Shader shader, String name, int unit, Framebuffer buffer) {
        RenderSystem.activeTexture(GL_TEXTURE0 + unit);
        RenderSystem.bindTexture(buffer.getColorAttachment());
        shader.setUniform1i(name, unit);
    }

    private static void ensureShaders() {
        if (glowShader == null) glowShader = new Shader("hand", "flame_glow");
        if (trailShader == null) trailShader = new Shader("hand", "flame_trail");
        if (compositeShader == null) compositeShader = new Shader("hand", "flame_composite");
    }

    private static boolean ensureTargets() {
        var window = MinecraftClient.getInstance().getWindow();
        int w = window.getFramebufferWidth(), h = window.getFramebufferHeight();
        if (w <= 0 || h <= 0) return false;
        if (hand != null && hand.textureWidth == w && hand.textureHeight == h) return true;
        deleteTargets();
        hand = target(w, h, true);
        glow = target(w, h, false);
        trailA = target(w, h, false);
        trailB = target(w, h, false);
        return true;
    }

    private static Framebuffer target(int w, int h, boolean depth) {
        Framebuffer result = new SimpleFramebuffer(w, h, depth);
        result.setTexFilter(GL_LINEAR);
        result.setClearColor(0, 0, 0, 0);
        return result;
    }

    private static void clearHistory() {
        trailA.clear();
        trailB.clear();
        historyIsA = true;
    }

    private static void fail(RuntimeException error) {
        if (!failed) BKClientInit.LOGGER.error("Flame hands effect failed; reselect the mode to retry", error);
        failed = true;
        captured = false;
        historyValid = false;
    }

    private static void deleteTargets() {
        if (hand != null) hand.delete();
        if (glow != null) glow.delete();
        if (trailA != null) trailA.delete();
        if (trailB != null) trailB.delete();
        hand = glow = trailA = trailB = null;
        captured = capturing = historyValid = false;
        historyIsA = true;
    }

    public static void release() {
        deleteTargets();
        if (glowShader != null) glowShader.delete();
        if (trailShader != null) trailShader.delete();
        if (compositeShader != null) compositeShader.delete();
        glowShader = trailShader = compositeShader = null;
        failed = false;
    }

    /** Restore both GL and the RenderSystem caches after capture or post-processing. */
    private static final class State implements AutoCloseable {
        private final int drawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        private final int readFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        private final int program = glGetInteger(GL_CURRENT_PROGRAM);
        private final ShaderProgram shader = RenderSystem.getShader();
        private final int activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        private final int[] textures = new int[4];
        private final int[] viewport = new int[4];
        private final float[] clearColor = new float[4];
        private final float[] tint = RenderSystem.getShaderColor().clone();
        private final boolean depth = glIsEnabled(GL_DEPTH_TEST), blend = glIsEnabled(GL_BLEND), cull = glIsEnabled(GL_CULL_FACE);
        private final boolean scissor = glIsEnabled(GL_SCISSOR_TEST), depthWrite = glGetBoolean(GL_DEPTH_WRITEMASK);
        private final int[] scissorBox = new int[4];
        private final java.nio.ByteBuffer colorMask = org.lwjgl.BufferUtils.createByteBuffer(4);
        private final int srcRgb = glGetInteger(GL_BLEND_SRC_RGB), dstRgb = glGetInteger(GL_BLEND_DST_RGB);
        private final int srcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA), dstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);

        State() {
            glGetIntegerv(GL_VIEWPORT, viewport);
            glGetIntegerv(GL_SCISSOR_BOX, scissorBox);
            glGetFloatv(GL_COLOR_CLEAR_VALUE, clearColor);
            glGetBooleanv(GL_COLOR_WRITEMASK, colorMask);
            for (int i = 0; i < textures.length; i++) {
                RenderSystem.activeTexture(GL_TEXTURE0 + i);
                textures[i] = glGetInteger(GL_TEXTURE_BINDING_2D);
            }
            RenderSystem.activeTexture(activeTexture);
        }

        @Override public void close() {
            GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, readFbo);
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFbo);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.setShader(shader);
            glUseProgram(program);
            for (int i = 0; i < textures.length; i++) {
                RenderSystem.activeTexture(GL_TEXTURE0 + i);
                RenderSystem.bindTexture(textures[i]);
            }
            RenderSystem.activeTexture(activeTexture);
            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.depthMask(depthWrite);
            RenderSystem.colorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
            if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            else RenderSystem.disableScissor();
        }
    }
}
