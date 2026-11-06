package dev.bkclient.utility.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import org.lwjgl.opengl.GL30C;

import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;

import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL30C.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_READ_FRAMEBUFFER;

public class HandShaderHelper {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static Shader solidShader;
    private static Shader smokeShader;
    private static Shader glowShader;
    private static Shader glassShader;
    private static Shader snowShader;
    private static Shader blendShader;
    private static Shader viewmodelOutlineShader;

    private static Framebuffer copyFbo;
    private static Framebuffer shader1Fbo;
    private static Framebuffer shader2Fbo;

    private static boolean initialized = false;
    private static boolean failed;

    public static void retryAfterFailure() {
        failed = false;
    }

    public static void initIfNeeded() {
        if (initialized || failed) return;
        try {
            solidShader   = new Shader("hand", "solid");
            smokeShader   = new Shader("hand", "smoke");
            glowShader    = new Shader("hand", "glow");
            glassShader   = new Shader("hand", "glass");
            snowShader    = new Shader("hand", "snow");
            blendShader   = new Shader("hand", "blend");
            viewmodelOutlineShader = new Shader("hand", "viewmodel_outline");
            initialized = true;
        } catch (Exception e) {
            releasePrograms();
            failed = true;
            System.err.println("[BKClientInit] Failed to init hand shaders: " + e.getMessage());
        }
    }

    public static void checkFramebuffers() {
        if (mc.getWindow() == null) return;
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (copyFbo == null || shader1Fbo == null || shader2Fbo == null || copyFbo.textureWidth != w || copyFbo.textureHeight != h) {
            releaseFramebuffers();
            try {
                copyFbo    = new SimpleFramebuffer(w, h, true);
                shader1Fbo = new SimpleFramebuffer(w, h, true);
                shader2Fbo = new SimpleFramebuffer(w, h, true);
            } catch (RuntimeException error) {
                releaseFramebuffers();
                throw error;
            }
        }
    }

    public static void releaseFramebuffers() {
        if (copyFbo != null) copyFbo.delete();
        if (shader1Fbo != null) shader1Fbo.delete();
        if (shader2Fbo != null) shader2Fbo.delete();
        copyFbo = null;
        shader1Fbo = null;
        shader2Fbo = null;
    }

    public static void releasePrograms() {
        for (Shader shader : new Shader[]{solidShader, smokeShader,
                glowShader, glassShader, snowShader, blendShader, viewmodelOutlineShader}) {
            if (shader != null) shader.delete();
        }
        solidShader = smokeShader = null;
        glowShader = glassShader = snowShader = blendShader = viewmodelOutlineShader = null;
        initialized = failed = false;
    }

    public static void drawFullScreenQuad() {
        RenderSystem.assertOnRenderThread();
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buf.vertex(-1f, -1f, 0f);
        buf.vertex( 1f, -1f, 0f);
        buf.vertex( 1f,  1f, 0f);
        buf.vertex(-1f,  1f, 0f);
        BufferRenderer.draw(buf.end());
    }

    /** Copies framebuffer color exactly; also used by other post-processing effects. */
    public static void blitColorExact(Framebuffer src, Framebuffer dst) {
        if (src == null || dst == null) return;
        int w = Math.min(src.textureWidth, dst.textureWidth);
        int h = Math.min(src.textureHeight, dst.textureHeight);
        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, src.fbo);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, dst.fbo);
        GL30C.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public static boolean isInitialized() { return initialized; }

    public static Shader getSolid()   { return solidShader; }
    public static Shader getSmoke()   { return smokeShader; }
    public static Shader getGlow()    { return glowShader; }
    public static Shader getGlass()   { return glassShader; }
    public static Shader getSnow()    { return snowShader; }
    public static Shader getBlend()   { return blendShader; }

    public static Shader getViewModelOutline() { return viewmodelOutlineShader; }

    public static Framebuffer getCopyFbo()    { return copyFbo; }
    public static Framebuffer getShader1Fbo() { return shader1Fbo; }
    public static Framebuffer getShader2Fbo() { return shader2Fbo; }

}
