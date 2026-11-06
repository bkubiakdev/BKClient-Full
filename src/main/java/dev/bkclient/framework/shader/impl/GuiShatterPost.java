package dev.bkclient.framework.shader.impl;

import dev.bkclient.utility.animation.GuiShatterAnimation;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.shader.HandShaderHelper;
import dev.bkclient.utility.render.shader.Shader;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

/**
 * Draws {@link GuiShatterAnimation}'s shards, textured with a snapshot of the screen.
 *
 * <p>Usage is two calls: {@link #capture} once, at the moment the GUI should freeze,
 * then {@link #render} every frame with the animation progress. The snapshot is a
 * separate framebuffer, so drawing the shards back over the main target is not a
 * feedback loop.
 */
public class GuiShatterPost implements IMinecraft {

    private Framebuffer snapshotFbo;
    private Shader shader;
    private int vao = -1;
    private int vbo = -1;
    private ByteBuffer vertexData;

    private int positionAttrib = -1;
    private int uvAttrib = -1;
    private int alphaAttrib = -1;

    private boolean captured;

    public void initShaders() {
        try {
            this.shader = new Shader("gui", "gui_shatter");
            this.vao = GL30C.glGenVertexArrays();
            this.vbo = GL15C.glGenBuffers();

            int program = this.shader.getProgramId();
            this.positionAttrib = GL20C.glGetAttribLocation(program, "Position");
            this.uvAttrib = GL20C.glGetAttribLocation(program, "UV");
            this.alphaAttrib = GL20C.glGetAttribLocation(program, "Alpha");

            this.vertexData = MemoryUtil.memAlloc(GuiShatterAnimation.MAX_VERTICES * GuiShatterAnimation.VERTEX_BYTES);
        } catch (Exception e) {
            System.err.println("[BKClientInit] Failed to init gui shatter shader: " + e.getMessage());
            this.shader = null;
        }
    }

    public void shutdown() {
        if (this.shader != null) {
            this.shader.delete();
            this.shader = null;
        }
        if (this.vbo != -1) {
            GL15C.glDeleteBuffers(this.vbo);
            this.vbo = -1;
        }
        if (this.vao != -1) {
            GL30C.glDeleteVertexArrays(this.vao);
            this.vao = -1;
        }
        if (this.snapshotFbo != null) {
            this.snapshotFbo.delete();
            this.snapshotFbo = null;
        }
        if (this.vertexData != null) {
            MemoryUtil.memFree(this.vertexData);
            this.vertexData = null;
        }
        this.captured = false;
    }

    public boolean isReady() {
        return this.shader != null && this.positionAttrib >= 0;
    }

    public boolean hasSnapshot() {
        return this.captured && this.snapshotFbo != null;
    }

    /** Freezes the current frame so the shards have something to be textured with. */
    public boolean capture() {
        if (!isReady()) return false;

        Framebuffer main = mc.getFramebuffer();
        if (main == null || main.textureWidth <= 0 || main.textureHeight <= 0) return false;

        int width = main.textureWidth;
        int height = main.textureHeight;
        if (this.snapshotFbo == null
            || this.snapshotFbo.textureWidth != width
            || this.snapshotFbo.textureHeight != height) {
            if (this.snapshotFbo != null) this.snapshotFbo.delete();
            this.snapshotFbo = new SimpleFramebuffer(width, height, false);
            this.snapshotFbo.setTexFilter(GL11C.GL_LINEAR);
        }

        HandShaderHelper.blitColorExact(main, this.snapshotFbo);
        main.beginWrite(true);
        this.captured = true;
        return true;
    }

    public void discard() {
        this.captured = false;
    }

    /**
     * Draws one frame of the shatter.
     *
     * @param progress    0 for intact, 1 for fully scattered
     * @param screenWidth  GUI-scaled width, matching the coordinates the shards were begun with
     * @return false when there was nothing to draw, so the caller can finish closing
     */
    public boolean render(float progress, float screenWidth, float screenHeight) {
        if (!isReady() || !hasSnapshot() || !GuiShatterAnimation.isActive()) {
            return false;
        }

        int vertexCount = GuiShatterAnimation.buildGeometry(this.vertexData, progress, screenWidth, screenHeight);
        if (vertexCount <= 0) {
            return false;
        }

        // The HUD render leaves a scissor box behind on some paths, which would clip
        // the shards away without any error anywhere.
        //
        // Flat 2D shards have no meaningful facing, and the vertex shader flips Y, so the
        // winding coming out of the Voronoi fan is arbitrary. With culling left on the
        // driver silently discarded every triangle.
        RenderSystem.disableCull();
        RenderSystem.disableScissor();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        GL30C.glBindVertexArray(this.vao);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.vbo);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, this.vertexData, GL15C.GL_STREAM_DRAW);

        int stride = GuiShatterAnimation.VERTEX_BYTES;
        GL20C.glEnableVertexAttribArray(this.positionAttrib);
        GL20C.glVertexAttribPointer(this.positionAttrib, 3, GL11C.GL_FLOAT, false, stride, 0L);
        if (this.uvAttrib >= 0) {
            GL20C.glEnableVertexAttribArray(this.uvAttrib);
            GL20C.glVertexAttribPointer(this.uvAttrib, 2, GL11C.GL_FLOAT, false, stride, 12L);
        }
        if (this.alphaAttrib >= 0) {
            GL20C.glEnableVertexAttribArray(this.alphaAttrib);
            GL20C.glVertexAttribPointer(this.alphaAttrib, 1, GL11C.GL_FLOAT, false, stride, 20L);
        }

        GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, this.snapshotFbo.getColorAttachment());

        this.shader.bind();
        this.shader.setUniform1i("Snapshot", 0);
        this.shader.setUniform2f("ScreenSize", new org.joml.Vector2f(screenWidth, screenHeight));

        GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, vertexCount);

        this.shader.unbind();

        GL20C.glDisableVertexAttribArray(this.positionAttrib);
        if (this.uvAttrib >= 0) GL20C.glDisableVertexAttribArray(this.uvAttrib);
        if (this.alphaAttrib >= 0) GL20C.glDisableVertexAttribArray(this.alphaAttrib);

        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
        GL30C.glBindVertexArray(0);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        return true;
    }
}
