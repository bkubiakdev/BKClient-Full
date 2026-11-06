package dev.bkclient.framework.shader.impl;

import dev.bkclient.mixin.accessors.FramebufferAccessor;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IWindow;
import dev.bkclient.utility.render.shader.HandShaderHelper;
import dev.bkclient.utility.render.shader.Shader;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

public class TargetLensPost implements IMinecraft, IWindow {
    public record Lens(float u, float v, float depth, float radius) {}

    private Framebuffer copyFbo;
    private Framebuffer outFbo;
    private Shader shader;
    private int emptyVao = -1;
    private final int[] viewport = new int[4];
    private final java.nio.ByteBuffer colorMask = org.lwjgl.BufferUtils.createByteBuffer(4);

    public void initShaders() {
        try {
            this.shader = new Shader("target_esp", "lens");
            this.emptyVao = GL30C.glGenVertexArrays();


        } catch (Exception e) {
            System.err.println("[BKClientInit] Failed to init target lens shader: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (this.shader != null) {
            this.shader.delete();
            this.shader = null;
        }

        if (this.emptyVao != -1) {
            GL30C.glDeleteVertexArrays(this.emptyVao);
            this.emptyVao = -1;
        }
        releaseTargets();
    }

    /** Release resolution-sized storage while keeping the shader ready for reuse. */
    public void releaseTargets() {
        if (copyFbo == null && outFbo == null) return;
        try (var ignored = new dev.bkclient.utility.render.VisualRenderState()) {
            if (this.copyFbo != null) {
                this.copyFbo.delete();
                this.copyFbo = null;
            }
            if (this.outFbo != null) {
                this.outFbo.delete();
                this.outFbo = null;
            }
        }
    }

    private void ensureFramebuffers(int w, int h) {
        if (this.copyFbo == null || this.copyFbo.textureWidth != w || this.copyFbo.textureHeight != h) {
            if (this.copyFbo != null) {
                this.copyFbo.delete();
            }
            this.copyFbo = new SimpleFramebuffer(w, h, false);
            this.copyFbo.setTexFilter(GL11C.GL_LINEAR);
        }
        if (this.outFbo == null || this.outFbo.textureWidth != w || this.outFbo.textureHeight != h) {
            if (this.outFbo != null) {
                this.outFbo.delete();
            }
            this.outFbo = new SimpleFramebuffer(w, h, false);
            this.outFbo.setTexFilter(GL11C.GL_NEAREST);
        }
    }

    public void apply(Framebuffer main, float aspect, float strength, java.util.List<Lens> lenses) {
        if (this.shader == null || this.emptyVao == -1 || lenses.isEmpty() || strength <= 0.001F) return;
        if (main == null || main.getColorAttachment() <= 0 || main.textureWidth <= 0 || main.textureHeight <= 0) {
            return;
        }

        int depthTexture = ((FramebufferAccessor) main).getDepthAttachment();
        if (depthTexture <= 0) return;

        int width = main.textureWidth;
        int height = main.textureHeight;
        // Preserve both actual GL bindings and Minecraft's cached texture state.
        int activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
        int sceneBinding = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE1);
        int depthBinding = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
        int program = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        int vao = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
        int readFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int drawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
        boolean depthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
        boolean depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
        boolean blend = GL11C.glIsEnabled(GL11C.GL_BLEND);
        boolean cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        boolean scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
        boolean stencil = GL11C.glIsEnabled(GL11C.GL_STENCIL_TEST);
        GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMask);

        try {
            RenderSystem.disableScissor();
            GL11C.glDisable(GL11C.GL_STENCIL_TEST);
            this.ensureFramebuffers(width, height);
            // 1. Bitwise GPU copy of main scene color into copy framebuffer
            HandShaderHelper.blitColorExact(main, this.copyFbo);

            // Render the lens distortion into a separate color target.
            this.outFbo.beginWrite(true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.colorMask(true, true, true, true);

            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            GlStateManager._bindTexture(this.copyFbo.getColorAttachment());

            GlStateManager._activeTexture(GL13C.GL_TEXTURE1);
            GlStateManager._bindTexture(depthTexture);

            this.shader.bind();
            this.shader.setUniform1i("Sampler0", 0);
            this.shader.setUniform1i("Sampler1", 1);
            int count = Math.min(18, lenses.size());
            this.shader.setUniform1f("Count", count);
            this.shader.setUniform1f("Aspect", aspect);
            this.shader.setUniform1f("Strength", strength);
            for (int i = 0; i < count; i++) {
                Lens lens = lenses.get(i);
                this.shader.setUniform4f("Lens" + i, lens.u(), lens.v(), lens.depth(), lens.radius());
            }

            GL30C.glBindVertexArray(this.emptyVao);
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 6);

            // 4. Copy the result back over main and restore state
            HandShaderHelper.blitColorExact(this.outFbo, main);
        } finally {
            GL20C.glUseProgram(program);
            GL30C.glBindVertexArray(vao);

            GlStateManager._activeTexture(GL13C.GL_TEXTURE1);
            GlStateManager._bindTexture(depthBinding);
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            GlStateManager._bindTexture(sceneBinding);
            GlStateManager._activeTexture(activeTexture);
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, readFramebuffer);
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.depthMask(depthMask);
            RenderSystem.colorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (scissor) GlStateManager._enableScissorTest();
            if (stencil) GL11C.glEnable(GL11C.GL_STENCIL_TEST);
        }
    }
}
