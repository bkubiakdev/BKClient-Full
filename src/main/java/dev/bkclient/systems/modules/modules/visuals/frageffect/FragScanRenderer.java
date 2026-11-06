package dev.bkclient.systems.modules.modules.visuals.frageffect;

import dev.bkclient.utility.render.shader.HandShaderHelper;
import dev.bkclient.utility.render.shader.Shader;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Iterator;

import static org.lwjgl.opengl.GL11C.GL_ONE;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_ZERO;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;

public final class FragScanRenderer {
    private static final int MAX_ACTIVE_WAVES = 4;
    private static final float INITIAL_RADIUS = 1.0f;
    private static final float BASE_GROWTH_DURATION = 6400.0f;
    private static final float FLICK_DURATION_MULTIPLIER = 0.5f;
    private final ArrayDeque<WavePulse> activeWaves = new ArrayDeque<>();
    private final Matrix4f inverseProjection = new Matrix4f();
    private final Matrix4f inverseView = new Matrix4f();
    private Shader shader;
    private boolean disabledAfterError;

    public void ping(Vec3d position, float speedMultiplier) {
        if (position == null || disabledAfterError) return;
        while (activeWaves.size() >= MAX_ACTIVE_WAVES) activeWaves.removeFirst();
        activeWaves.addLast(new WavePulse(position, System.currentTimeMillis(),
                MathHelper.clamp(speedMultiplier, 0.25f, 2.0f)));
    }

    public void clear() {
        activeWaves.clear();
    }

    public boolean isIdle() {
        return activeWaves.isEmpty();
    }

    public void render(Matrix4f projection, Matrix4f view, Vec3d cameraPos,
                       int outer, int mid, int inner, int scanline,
                       int flickOuter, int flickMid, int flickInner, int flickScanline) {
        if (disabledAfterError || !cleanupExpiredWaves() || projection == null || view == null || cameraPos == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer mainFbo = mc.getFramebuffer();
        if (mainFbo == null || mainFbo.getDepthAttachment() <= 0) return;
        try {
            if (shader == null) shader = new Shader("effect", "frag_scan");
            inverseProjection.set(projection).invert();
            inverseView.set(view).invert();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE, GL_ONE, GL_ZERO);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.activeTexture(GL_TEXTURE0);
            RenderSystem.bindTexture(mainFbo.getDepthAttachment());

            shader.bind();
            shader.setUniform1i("DepthSampler", 0);
            shader.setUniformMatrix4f("uInvProjection", inverseProjection);
            shader.setUniformMatrix4f("uInvView", inverseView);
            for (WavePulse pulse : activeWaves) {
                float duration = scaleDuration(BASE_GROWTH_DURATION, pulse.speedMultiplier);
                float baseRadius = computeRadius(pulse.startedAt, duration);
                float baseProgress = timeProgress(pulse.startedAt, duration);
                float baseWave = Math.min(wave(1.0f - baseProgress) * 1.75f, 1.0f);
                drawWave(cameraPos, pulse.center, baseRadius, baseRadius, 30.0f, baseProgress, 0.0f,
                        scaleColor(outer, baseWave), scaleColor(mid, baseWave),
                        scaleColor(inner, baseWave), scaleColor(scanline, baseWave));

                float flickRadius = computeFlickRadius(pulse.startedAt, duration, FLICK_DURATION_MULTIPLIER);
                float flickProgress = timeProgress(pulse.startedAt, duration * FLICK_DURATION_MULTIPLIER);
                float flickAlpha = 1.0f - flickProgress;
                float flickWave = Math.min(wave(flickAlpha) * 2.0f, 1.0f);
                drawWave(cameraPos, pulse.center, flickRadius, flickRadius / 1.5f, 40.0f, flickProgress, 1.0f,
                        scaleColor(flickOuter, flickAlpha), scaleColor(flickMid, flickWave),
                        scaleColor(flickInner, flickWave), scaleColor(flickScanline, flickWave));
            }
        } catch (Throwable throwable) {
            disabledAfterError = true;
            activeWaves.clear();
            System.err.println("[BK-Client] Frag scan disabled: " + throwable.getMessage());
        } finally {
            if (shader != null) shader.unbind();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private void drawWave(Vec3d cameraPos, Vec3d center, float radius, float width, float sharpness,
                          float progress, float flick, int outer, int mid, int inner, int scanline) {
        shader.setUniform4f("uCameraPosRadius", (float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z, radius);
        shader.setUniform4f("uCenterWidth", (float) center.x, (float) center.y, (float) center.z, Math.max(width, 0.0001f));
        setColorUniform("uOuterColor", outer);
        setColorUniform("uMidColor", mid);
        setColorUniform("uInnerColor", inner);
        setColorUniform("uScanlineColor", scanline);
        shader.setUniform4f("uMeta", sharpness, progress, flick, 0.0f);
        HandShaderHelper.drawFullScreenQuad();
    }

    private void setColorUniform(String name, int color) {
        shader.setUniform4f(name, ((color >> 16) & 0xFF) / 255.0f, ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f, ((color >>> 24) & 0xFF) / 255.0f);
    }

    private boolean cleanupExpiredWaves() {
        long now = System.currentTimeMillis();
        Iterator<WavePulse> iterator = activeWaves.iterator();
        while (iterator.hasNext()) {
            WavePulse pulse = iterator.next();
            if (now - pulse.startedAt >= scaleDuration(BASE_GROWTH_DURATION, pulse.speedMultiplier)) iterator.remove();
        }
        return !activeWaves.isEmpty();
    }

    private static int scaleColor(int color, float factor) {
        float f = MathHelper.clamp(factor, 0.0f, 1.0f);
        int r = Math.round(((color >> 16) & 0xFF) * f);
        int g = Math.round(((color >> 8) & 0xFF) * f);
        int b = Math.round((color & 0xFF) * f);
        int a = Math.round(255.0f * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float wave(float value) {
        float f = MathHelper.clamp(value, 0.0f, 1.0f);
        return f > 0.5f ? (1.0f - f) * 2.0f : f * 2.0f;
    }

    private static float scaleDuration(float duration, float speedMultiplier) {
        return Math.max(280.0f, duration / MathHelper.clamp(speedMultiplier, 0.25f, 2.0f));
    }

    private static float computeRadius(long start, float duration) {
        float progress = FragEffectEasing.quintOut(timeProgress(start, duration));
        return INITIAL_RADIUS + (viewDistance() - INITIAL_RADIUS) * progress;
    }

    private static float computeFlickRadius(long start, float duration, float multiplier) {
        float progress = FragEffectEasing.expoInOut(timeProgress(start, duration * multiplier));
        return INITIAL_RADIUS + (viewDistance() - INITIAL_RADIUS) * progress;
    }

    private static float timeProgress(long start, float duration) {
        return MathHelper.clamp((System.currentTimeMillis() - start) / duration, 0.0f, 1.0f);
    }

    private static float viewDistance() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return Math.max(96.0f, (mc.options.getViewDistance().getValue() + 1) * 16.0f);
    }

    private record WavePulse(Vec3d center, long startedAt, float speedMultiplier) {
    }
}
