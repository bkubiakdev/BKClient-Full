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
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;

public class GlassVaporPost implements IMinecraft, IWindow {
    private Framebuffer copyFbo;
    private Framebuffer outFbo;
    private Shader shader;
    private int ubo = -1;
    private int emptyVao = -1;

    public void initShaders() {
        try {
            this.shader = new Shader("vapor", "glass_vapor");
            this.ubo = GL15C.glGenBuffers();
            this.emptyVao = GL30C.glGenVertexArrays();

            int blockIndex = GL31C.glGetUniformBlockIndex(this.shader.getProgramId(), "Vapor");
            if (blockIndex != -1) {
                GL31C.glUniformBlockBinding(this.shader.getProgramId(), blockIndex, 0);
            }
        } catch (Exception e) {
            System.err.println("[BKClientInit] Failed to init glass vapor shader: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (this.shader != null) {
            this.shader.delete();
            this.shader = null;
        }
        if (this.ubo != -1) {
            GL15C.glDeleteBuffers(this.ubo);
            this.ubo = -1;
        }
        if (this.emptyVao != -1) {
            GL30C.glDeleteVertexArrays(this.emptyVao);
            this.emptyVao = -1;
        }
        if (this.copyFbo != null) {
            this.copyFbo.delete();
            this.copyFbo = null;
        }
        if (this.outFbo != null) {
            this.outFbo.delete();
            this.outFbo = null;
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

    public void apply(Framebuffer main, float[] fArray) {
        if (this.shader == null || fArray == null || fArray.length < 16) return;
        if (main == null || main.getColorAttachment() <= 0 || main.textureWidth <= 0 || main.textureHeight <= 0) {
            return;
        }

        int depthTexture = ((FramebufferAccessor) main).getDepthAttachment();
        if (depthTexture <= 0) return;

        int width = main.textureWidth;
        int height = main.textureHeight;
        this.ensureFramebuffers(width, height);

        RenderSystem.backupProjectionMatrix();

        try {
            // 1. Bitwise GPU copy of main scene color into copy framebuffer
            HandShaderHelper.blitColorExact(main, this.copyFbo);

            // 2. Upload uniform buffer object (std140 layout Vapor block)
            GL15C.glBindBuffer(GL31C.GL_UNIFORM_BUFFER, this.ubo);
            GL15C.glBufferData(GL31C.GL_UNIFORM_BUFFER, fArray, GL15C.GL_DYNAMIC_DRAW);
            GL30C.glBindBufferBase(GL31C.GL_UNIFORM_BUFFER, 0, this.ubo);
            GL15C.glBindBuffer(GL31C.GL_UNIFORM_BUFFER, 0);

            // 3. Render into outFbo
            this.outFbo.beginWrite(true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();

            // Bind through GlStateManager so its per-unit texture cache stays in sync: a raw glBindTexture(…, 0)
            // left it believing the item atlas was still bound, the next atlas bind was skipped and the
            // first-person hand drew with texture 0 (black). The other post effects already bind this way.
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            GlStateManager._bindTexture(this.copyFbo.getColorAttachment());

            GlStateManager._activeTexture(GL13C.GL_TEXTURE1);
            GlStateManager._bindTexture(depthTexture);

            this.shader.bind();
            this.shader.setUniform1i("Scene", 0);
            this.shader.setUniform1i("DepthSampler", 1);

            GL30C.glBindVertexArray(this.emptyVao);
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 6);
            GL30C.glBindVertexArray(0);

            this.shader.unbind();

            GlStateManager._activeTexture(GL13C.GL_TEXTURE1);
            GlStateManager._bindTexture(0);
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            GlStateManager._bindTexture(0);

            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);

            // 4. Copy the result back over main and restore state
            HandShaderHelper.blitColorExact(this.outFbo, main);
        } finally {
            RenderSystem.restoreProjectionMatrix();
            main.beginWrite(false);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }
}
