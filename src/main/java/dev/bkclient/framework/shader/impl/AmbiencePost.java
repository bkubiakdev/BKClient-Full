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

public class AmbiencePost implements IMinecraft, IWindow {
    private final java.util.Map<String, Shader> shaders = new java.util.HashMap<>();

    private Framebuffer copyFbo;
    private Framebuffer outFbo;
    private Framebuffer fogFbo;

    private int emptyVao = -1;
    private final int[] viewport = new int[4];
    private final java.nio.ByteBuffer colorMask = org.lwjgl.BufferUtils.createByteBuffer(4);

    // Chain state. Effects applied between begin() and end() hand their result to the next effect through
    // copyFbo/outFbo (ping-pong); the scene is copied out of main once at the start of a chain and back once
    // at its end. Before, every effect copied main out and back (2 full-screen blits and ~20 glGet* each).
    private boolean chainOpen;
    private Framebuffer chainSource;
    private Framebuffer chainMain;
    private int savedActiveTexture, savedScene, savedDepth, savedAux, savedProgram, savedVao, savedRead, savedDraw;
    private boolean savedDepthTest, savedDepthMask, savedBlend, savedCull, savedScissor, savedStencil;

    public void initShaders() {
        this.emptyVao = GL30C.glGenVertexArrays();
        for (String name : new String[]{"wet_world", "fog", "rain", "isolation", "saturation", "fog_composite"}) {
            try { shaders.put(name, new Shader("ambience", name)); }
            catch (Exception e) { System.err.println("[BKClient] Ambience " + name + ": " + e.getMessage()); }
        }
    }

    public void shutdown() {
        shaders.values().forEach(Shader::delete);
        shaders.clear();
        if (this.emptyVao != -1) {
            GL30C.glDeleteVertexArrays(this.emptyVao);
            this.emptyVao = -1;
        }
        releaseTargets();
    }

    public void releaseTargets() {
        if (fogFbo != null) { fogFbo.delete(); fogFbo = null; }
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

    public void apply(String effect, Framebuffer main, int auxiliaryTexture, java.util.function.Consumer<Shader> uniforms) {
        apply(effect, main, auxiliaryTexture, uniforms, 1.0F);
    }

    public void apply(String effect, Framebuffer main, int auxiliaryTexture, java.util.function.Consumer<Shader> uniforms, float resolution) {
        // Standalone use: a chain of one effect.
        boolean standalone = !chainOpen;
        if (standalone && !begin(main)) return;
        try { apply(effect, main, auxiliaryTexture, uniforms, resolution, true); }
        finally { if (standalone) end(); }
    }

    /** Snapshots the GL state once and opens a chain; returns false when main cannot be post-processed. */
    public boolean begin(Framebuffer main) {
        if (chainOpen) return true;
        if (this.emptyVao == -1 || main == null || main.getColorAttachment() <= 0 || main.textureWidth <= 0 || main.textureHeight <= 0) return false;
        if (((FramebufferAccessor) main).getDepthAttachment() <= 0) return false;
        // Preserve both actual GL bindings and Minecraft's cached texture state.
        savedActiveTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
        savedScene = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE1);
        savedDepth = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE2);
        savedAux = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
        savedProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        savedVao = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
        savedRead = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        savedDraw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
        savedDepthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
        savedDepthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
        savedBlend = GL11C.glIsEnabled(GL11C.GL_BLEND);
        savedCull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        savedScissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
        savedStencil = GL11C.glIsEnabled(GL11C.GL_STENCIL_TEST);
        GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMask);
        RenderSystem.disableScissor();
        GL11C.glDisable(GL11C.GL_STENCIL_TEST);
        chainOpen = true; chainSource = null; chainMain = main;
        return true;
    }

    /** Flushes a result still held in a ping-pong target and restores the GL state snapshot. */
    public void end() {
        if (!chainOpen) return;
        try {
            if (chainSource != null && chainMain != null) HandShaderHelper.blitColorExact(chainSource, chainMain);
        } finally {
            chainOpen = false; chainSource = null; chainMain = null;
            GL20C.glUseProgram(savedProgram);
            GL30C.glBindVertexArray(savedVao);
            GlStateManager._activeTexture(GL13C.GL_TEXTURE2);
            GlStateManager._bindTexture(savedAux);
            GlStateManager._activeTexture(GL13C.GL_TEXTURE1);
            GlStateManager._bindTexture(savedDepth);
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            GlStateManager._bindTexture(savedScene);
            GlStateManager._activeTexture(savedActiveTexture);
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, savedRead);
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, savedDraw);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.colorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
            if (savedDepthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (savedBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (savedCull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (savedScissor) GlStateManager._enableScissorTest();
            if (savedStencil) GL11C.glEnable(GL11C.GL_STENCIL_TEST);
        }
    }

    /**
     * One effect inside an open chain. The first effect copies main out once; each effect renders from the
     * previous result into the other ping-pong target; with {@code last} the result is copied back to main.
     * Main is never sampled while bound, so there is no framebuffer feedback.
     */
    public void apply(String effect, Framebuffer main, int auxiliaryTexture, java.util.function.Consumer<Shader> uniforms, float resolution, boolean last) {
        if (!chainOpen || main != chainMain) { apply(effect, main, auxiliaryTexture, uniforms, resolution); return; }
        Shader shader = shaders.get(effect);
        if (shader == null) return;
        int depthTexture = ((FramebufferAccessor) main).getDepthAttachment();
        int width = main.textureWidth;
        int height = main.textureHeight;
        if (effect.equals("fog")) dev.bkclient.utility.render.ambience.FogDebugCapture.markState("fogEntry");
        this.ensureFramebuffers(width, height);
        Framebuffer source;
        if (chainSource == null) {
            // 1. Bitwise GPU copy of the main scene color, once per chain.
            HandShaderHelper.blitColorExact(main, this.copyFbo);
            source = this.copyFbo;
        } else source = chainSource;
        Framebuffer target = source == this.copyFbo ? this.outFbo : this.copyFbo;

        // Render the ambience pass into the other color target.
        target.beginWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.colorMask(true, true, true, true);

        GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
        GlStateManager._bindTexture(source.getColorAttachment());

        GlStateManager._activeTexture(GL13C.GL_TEXTURE1);
        GlStateManager._bindTexture(depthTexture);

        GlStateManager._activeTexture(GL13C.GL_TEXTURE2);
        GlStateManager._bindTexture(auxiliaryTexture);
        shader.bind();
        shader.setUniform1i("Sampler0", 0);
        shader.setUniform1i("Sampler1", 1);
        shader.setUniform1i("Sampler2", 2);
        uniforms.accept(shader);
        if (effect.equals("fog")) {
            int fogWidth = Math.max(1, Math.round(width * resolution));
            int fogHeight = Math.max(1, Math.round(height * resolution));
            if (fogFbo == null || fogFbo.textureWidth != fogWidth || fogFbo.textureHeight != fogHeight) {
                if (fogFbo != null) fogFbo.delete();
                fogFbo = new SimpleFramebuffer(fogWidth, fogHeight, false);
                fogFbo.setTexFilter(GL11C.GL_LINEAR);
            }
            fogFbo.beginWrite(true);
            // Allocating a framebuffer may bind its color texture on the active unit.
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            GlStateManager._bindTexture(source.getColorAttachment());
            GlStateManager._activeTexture(GL13C.GL_TEXTURE1);
            GlStateManager._bindTexture(depthTexture);
            GlStateManager._activeTexture(GL13C.GL_TEXTURE2);
            GlStateManager._bindTexture(auxiliaryTexture);
        }

        GL30C.glBindVertexArray(this.emptyVao);
        if (effect.equals("fog")) dev.bkclient.utility.render.ambience.FogDebugCapture.beforeFog(
            source, depthTexture, auxiliaryTexture, shader.getProgramId(), resolution);
        GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 6);

        if (effect.equals("fog")) {
            target.beginWrite(true);
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            GlStateManager._bindTexture(source.getColorAttachment());
            GlStateManager._activeTexture(GL13C.GL_TEXTURE2);
            GlStateManager._bindTexture(fogFbo.getColorAttachment());
            Shader composite = shaders.get("fog_composite");
            if (composite == null) return;
            composite.bind();
            composite.setUniform1i("Sampler0",0);
            composite.setUniform1i("Sampler2",2);
            composite.setUniform1f("FilterFog", resolution < 1.0F ? 1.0F : 0.0F);
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES,0,6);
            dev.bkclient.utility.render.ambience.FogDebugCapture.afterFog(fogFbo, target);
        }

        if (last) {
            // Copy the chain result back over main; a following chain starts from main again.
            HandShaderHelper.blitColorExact(target, main);
            chainSource = null;
        } else chainSource = target;
        if (effect.equals("fog")) dev.bkclient.utility.render.ambience.FogDebugCapture.markState("fogRestored");
    }
}
