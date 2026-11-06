package dev.bkclient.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.shader.Shader;
import dev.bkclient.utility.render.shader.HandShaderHelper;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.*;
import static org.lwjgl.opengl.GL14C.*;
import static org.lwjgl.opengl.GL20C.*;

/** Isolated ghost mask: does not reuse or alter the hand effect's framebuffer. */
public final class BacktrackGhostGlow implements IMinecraft {
    private static SimpleFramebuffer mask;
    private static Shader shader;
    private static boolean failed;
    private static boolean capturing;
    public static net.minecraft.client.gl.Framebuffer captureTarget() { return capturing ? mask : null; }
    private BacktrackGhostGlow() {}

    public static void render(Runnable drawGhost) {
        if (failed) return;
        try {
            if (shader == null) shader = new Shader("hand", "backtrack_glow");
            int width = mc.getWindow().getFramebufferWidth(), height = mc.getWindow().getFramebufferHeight();
            if (mask == null || mask.textureWidth != width || mask.textureHeight != height) {
                if (mask != null) mask.delete();
                mask = new SimpleFramebuffer(width, height, true);
                mask.setClearColor(0, 0, 0, 0);
            }
        } catch (RuntimeException error) {
            failed = true;
            dev.bkclient.BKClientInit.LOGGER.warn("Backtrack glow unavailable; drawing plain ghost", error);
            mc.getFramebuffer().beginWrite(true);
            return;
        }
        var main = mc.getFramebuffer();
        mc.getBufferBuilders().getEntityVertexConsumers().draw();
        int program = glGetInteger(GL_CURRENT_PROGRAM);
        int activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL_TEXTURE0);
        int texture = glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean depth = glIsEnabled(GL_DEPTH_TEST), blend = glIsEnabled(GL_BLEND), cull = glIsEnabled(GL_CULL_FACE);
        boolean depthWrite = glGetBoolean(GL_DEPTH_WRITEMASK);
        int srcRGB = glGetInteger(GL_BLEND_SRC_RGB), dstRGB = glGetInteger(GL_BLEND_DST_RGB);
        int srcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA), dstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        try {
            mask.clear();
            mask.copyDepthFrom(main);
            mask.beginWrite(true);
            capturing = true;
            try { drawGhost.run(); } finally { capturing = false; }
            main.beginWrite(true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.activeTexture(GL_TEXTURE0);
            RenderSystem.bindTexture(mask.getColorAttachment());
            shader.bind();
            shader.setUniform1i("ColorTexture", 0);
            shader.setUniform1f("time", (System.nanoTime() % 60_000_000_000L) / 1_000_000_000F);
            var color = Colors.getAccent();
            shader.setUniform3f("glowColor", new Vector3f(color.getRed()/255F,color.getGreen()/255F,color.getBlue()/255F));
            HandShaderHelper.drawFullScreenQuad();
        } finally {
            capturing = false;
            main.beginWrite(true);
            glUseProgram(program);
            RenderSystem.activeTexture(GL_TEXTURE0);
            RenderSystem.bindTexture(texture);
            RenderSystem.activeTexture(activeTexture);
            RenderSystem.depthMask(depthWrite);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
        }
    }
    public static void release() {
        if (!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(BacktrackGhostGlow::release); return; }
        if (mask != null) { mask.delete(); mask = null; }
        if (shader != null) { shader.delete(); shader = null; }
        failed = false;
    }
}
