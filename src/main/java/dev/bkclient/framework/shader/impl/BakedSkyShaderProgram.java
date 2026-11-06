package dev.bkclient.framework.shader.impl;

import dev.bkclient.utility.render.shader.Shader;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import dev.bkclient.BKClientInit;
import dev.bkclient.utility.colors.ColorRGBA;

/** Cached equirectangular sky, refreshed a strip per frame while stars animate in the view shader. */
public final class BakedSkyShaderProgram extends SkyShaderProgram {
    private Shader bake;
    private Framebuffer cache;
    private int emptyVao = -1;
    private int strip;
    private int accentRgb;
    private long lastBake;
    private final int[] viewport = new int[4];
    private final int[] scissorBox = new int[4];
    private final java.nio.ByteBuffer colorMask = org.lwjgl.BufferUtils.createByteBuffer(4);

    public BakedSkyShaderProgram(String name) {
        super(BKClientInit.id(name + "/view/data"));
        bake = new Shader("ambience", name);
        emptyVao = GL30C.glGenVertexArrays();
    }

    @Override
    public void updateUniforms(float time, ColorRGBA accent) {
        super.updateUniforms(time, accent);
        bake(time, accent);
    }

    public int texture() { return cache == null ? 0 : cache.getColorAttachment(); }

    public void close() {
        if (cache != null) { cache.delete(); cache = null; }
        if (bake != null) { bake.delete(); bake = null; }
        if (emptyVao != -1) { GL30C.glDeleteVertexArrays(emptyVao); emptyVao = -1; }
    }

    private void bake(float time, ColorRGBA accent) {
        if (bake == null) return;
        // Preserve both actual GL bindings and Minecraft's cached texture state.
        int activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
        int sceneBinding = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
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

        GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, scissorBox);
        try {
            RenderSystem.disableScissor();
            GL11C.glDisable(GL11C.GL_STENCIL_TEST);
            boolean full = cache == null || accentRgb != accent.getRGB() || System.nanoTime() - lastBake > 1_000_000_000L;
            if (cache == null) {
                cache = new SimpleFramebuffer(2048, 1024, false);
                cache.setTexFilter(GL11C.GL_LINEAR);
            }
            cache.beginWrite(true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.colorMask(true,true,true,true);
            if (!full) {
                int bottom = strip * cache.textureHeight / 12;
                int top = (strip+1) * cache.textureHeight / 12;
                RenderSystem.enableScissor(0,bottom,cache.textureWidth,top-bottom);
            }
            bake.bind();
            bake.setUniform1f("Time",time);
            bake.setUniform3f("Accent",new org.joml.Vector3f(accent.getRed()/255f,accent.getGreen()/255f,accent.getBlue()/255f));
            GL30C.glBindVertexArray(emptyVao);
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES,0,6);
            accentRgb = accent.getRGB();
            lastBake = System.nanoTime();
            strip = full ? 0 : (strip+1)%12;
            RenderSystem.disableScissor();
        } finally {
            GL20C.glUseProgram(program);
            GL30C.glBindVertexArray(vao);

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
            if (scissor) RenderSystem.enableScissor(scissorBox[0],scissorBox[1],scissorBox[2],scissorBox[3]);
            if (stencil) GL11C.glEnable(GL11C.GL_STENCIL_TEST);
        }
    }
}
