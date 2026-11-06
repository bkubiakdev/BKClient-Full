package dev.bkclient.systems.cosmetics.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.cosmetics.CosmeticEntry;
import dev.bkclient.utility.render.shader.FlameHandsRenderer;
import dev.bkclient.utility.render.shader.HandShaderHelper;
import dev.bkclient.utility.render.shader.Shader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL33C.*;

/** A weapon-only emission pass at the existing Hands Shader boundary, before the HUD. */
public final class CosmeticGlowRenderer {
    private record Snapshot(CosmeticEntry entry, Matrix4f transform, Matrix4f[] pose, Set<String> hidden) {}
    private static final List<Snapshot> SNAPSHOTS = new ArrayList<>(2);
    private static Framebuffer emission, blurA, blurB;
    private static Shader blurShader, compositeShader;
    private static BufferAllocator buffer;
    private static VertexConsumerProvider.Immediate consumers;
    private static boolean captured, hadGeometry, failed;
    private static float strength;

    static {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> release());
    }

    private CosmeticGlowRenderer() {}

    /** Called once per real rendered frame, including frames in which vanilla skips hands. */
    public static void beginFrame() {
        SNAPSHOTS.clear();
        captured = false;
        strength = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        boolean firstPerson = client.player != null && client.world != null
                && client.options.getPerspective().isFirstPerson() && client.getCameraEntity() == client.player
                && !client.player.isSpectator();
        if (!firstPerson || !hadGeometry) {
            if (emission != null || blurA != null || blurB != null) {
                try (State ignored = new State()) {
                    RenderSystem.activeTexture(GL_TEXTURE0);
                    deleteTargets();
                }
            } else deleteTargets();
        }
        hadGeometry = false;
    }

    /** Freeze geometry; replaying the hand for Flame must never tick or queue this effect again. */
    public static void record(CosmeticEntry entry, Matrix4f transform, Matrix4f[] pose, Set<String> hidden) {
        if (failed || FlameHandsRenderer.captureTarget() != null || !CosmeticRenderer.hasEmission(entry)) return;
        float amount = CosmeticRenderer.emissionStrength(entry);
        if (amount <= 0) return;
        Matrix4f[] frozen = new Matrix4f[pose.length];
        for (int i = 0; i < pose.length; i++) frozen[i] = new Matrix4f(pose[i]);
        SNAPSHOTS.add(new Snapshot(entry, new Matrix4f(transform), frozen,
                hidden == null ? Set.of() : Set.copyOf(hidden)));
        hadGeometry = true;
        strength = Math.max(strength, amount);
    }

    public static boolean hasPendingGeometry() { return !SNAPSHOTS.isEmpty(); }

    /** Called after the ordinary provider is flushed, while the hand projection is still active. */
    public static void capture() {
        if (failed || !hasPendingGeometry() || FlameHandsRenderer.captureTarget() != null) return;
        RenderSystem.assertOnRenderThread();
        try (State ignored = new State()) {
            // Allocation binds textures on the active unit; confine it to the saved units.
            RenderSystem.activeTexture(GL_TEXTURE0);
            if (!ensureTargets()) return;
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            emission.clear();
            // Includes weapon, skin arms and the deferred supporting item; LEQUAL accepts the
            // identical visible strip surface while rejecting strips behind those occluders.
            emission.copyDepthFrom(main);
            emission.beginWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (buffer == null) {
                buffer = new BufferAllocator(262144);
                consumers = VertexConsumerProvider.immediate(buffer);
            }
            for (Snapshot snapshot : SNAPSHOTS) {
                CosmeticRenderer.renderEmissionPose(snapshot.entry(), snapshot.transform(), consumers,
                        snapshot.pose(), snapshot.hidden());
            }
            consumers.draw();
            captured = true;
        } catch (RuntimeException error) {
            fail(error);
        } finally {
            SNAPSHOTS.clear();
        }
    }

    /** Adds the cosmetic halo after the selected global hand effect, without changing its settings. */
    public static void composite() {
        if (!captured || failed) return;
        captured = false;
        try (State ignored = new State()) {
            if (blurShader == null) blurShader = new Shader("hand", "cosmetic_blur");
            if (compositeShader == null) compositeShader = new Shader("hand", "cosmetic_composite");
            RenderSystem.disableScissor();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            // Half-resolution separable blur keeps the additional GPU cost bounded.
            blurA.beginWrite(true);
            blurShader.bind();
            texture(blurShader, "SourceSampler", 0, emission);
            blurShader.setUniform2f("Direction", new Vector2f(3f / emission.textureWidth, 0));
            HandShaderHelper.drawFullScreenQuad();
            blurB.beginWrite(true);
            texture(blurShader, "SourceSampler", 0, blurA);
            blurShader.setUniform2f("Direction", new Vector2f(0, 1.5f / blurA.textureHeight));
            HandShaderHelper.drawFullScreenQuad();

            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL_ONE, GL_ONE, GL_ZERO, GL_ONE);
            compositeShader.bind();
            texture(compositeShader, "EmissionSampler", 0, emission);
            texture(compositeShader, "BloomSampler", 1, blurB);
            compositeShader.setUniform1f("Strength", strength);
            HandShaderHelper.drawFullScreenQuad();
        } catch (RuntimeException error) {
            fail(error);
        }
    }

    private static void texture(Shader shader, String uniform, int unit, Framebuffer target) {
        RenderSystem.activeTexture(GL_TEXTURE0 + unit);
        RenderSystem.bindTexture(target.getColorAttachment());
        shader.setUniform1i(uniform, unit);
    }

    private static boolean ensureTargets() {
        var window = MinecraftClient.getInstance().getWindow();
        int width = window.getFramebufferWidth(), height = window.getFramebufferHeight();
        if (width <= 0 || height <= 0) return false;
        if (emission != null && emission.textureWidth == width && emission.textureHeight == height) return true;
        deleteTargets();
        emission = target(width, height, true);
        blurA = target(Math.max(1, width / 2), Math.max(1, height / 2), false);
        blurB = target(Math.max(1, width / 2), Math.max(1, height / 2), false);
        return true;
    }

    private static Framebuffer target(int width, int height, boolean depth) {
        Framebuffer target = new SimpleFramebuffer(width, height, depth);
        target.setTexFilter(GL_LINEAR);
        target.setClearColor(0, 0, 0, 0);
        return target;
    }

    private static void fail(RuntimeException error) {
        if (!failed) BKClientInit.LOGGER.error("Cosmetic bloom failed; reload resources to retry", error);
        failed = true;
        // The failed pass has already restored the caller's state. Deleting an FBO
        // unbinds it, so cleanup itself must not leave the next draw on framebuffer 0.
        try (State ignored = new State()) {
            RenderSystem.activeTexture(GL_TEXTURE0);
            deleteTargets();
        }
    }

    private static void deleteTargets() {
        if (emission != null) emission.delete();
        if (blurA != null) blurA.delete();
        if (blurB != null) blurB.delete();
        emission = blurA = blurB = null;
        if (buffer != null) buffer.close();
        buffer = null;
        consumers = null;
        captured = false;
    }

    public static void release() {
        SNAPSHOTS.clear();
        try (State ignored = new State()) {
            RenderSystem.activeTexture(GL_TEXTURE0);
            deleteTargets();
            if (blurShader != null) blurShader.delete();
            if (compositeShader != null) compositeShader.delete();
        }
        blurShader = compositeShader = null;
        hadGeometry = failed = false;
    }

    /** Restore GL and RenderSystem caches across geometry capture and raw fullscreen shaders. */
    private static final class State implements AutoCloseable {
        private final int drawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        private final int readFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        private final int program = glGetInteger(GL_CURRENT_PROGRAM);
        private final ShaderProgram shader = RenderSystem.getShader();
        private final int activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        private final int[] textures = new int[3], shaderTextures = new int[3], viewport = new int[4], scissorBox = new int[4];
        private final float[] clearColor = new float[4], tint = RenderSystem.getShaderColor().clone();
        private final boolean depth = glIsEnabled(GL_DEPTH_TEST), blend = glIsEnabled(GL_BLEND), cull = glIsEnabled(GL_CULL_FACE);
        private final boolean scissor = glIsEnabled(GL_SCISSOR_TEST), depthWrite = glGetBoolean(GL_DEPTH_WRITEMASK);
        private final int depthFunction = glGetInteger(GL_DEPTH_FUNC);
        private final java.nio.ByteBuffer colorMask = org.lwjgl.BufferUtils.createByteBuffer(4);
        private final int srcRgb = glGetInteger(GL_BLEND_SRC_RGB), dstRgb = glGetInteger(GL_BLEND_DST_RGB);
        private final int srcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA), dstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);

        State() {
            glGetIntegerv(GL_VIEWPORT, viewport);
            glGetIntegerv(GL_SCISSOR_BOX, scissorBox);
            glGetFloatv(GL_COLOR_CLEAR_VALUE, clearColor);
            glGetBooleanv(GL_COLOR_WRITEMASK, colorMask);
            for (int i = 0; i < textures.length; i++) {
                shaderTextures[i] = RenderSystem.getShaderTexture(i);
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
                RenderSystem.setShaderTexture(i, shaderTextures[i]);
                RenderSystem.activeTexture(GL_TEXTURE0 + i);
                RenderSystem.bindTexture(textures[i]);
            }
            RenderSystem.activeTexture(activeTexture);
            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            RenderSystem.depthFunc(depthFunction);
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
