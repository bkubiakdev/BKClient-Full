package dev.bkclient.ui.clicksys;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.ui.clicksys.port.render.PixelReader;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL32.*;

/** Real core-profile regression: configure, resize, copy and read without leaking GL state. */
public final class GuiFramebufferGpuTest {
    private static void equal(int expected, int actual, String label) {
        if (expected != actual) throw new AssertionError(label + ": expected " + expected + ", got " + actual);
    }
    private static void noError(String label) { equal(GL_NO_ERROR, glGetError(), label); }
    public static void main(String[] args) {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW initialization failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(64, 64, "BK framebuffer regression", 0, 0);
        if (window == 0) throw new AssertionError("Core context unavailable");
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            RenderSystem.initRenderThread();
            for (int size : new int[]{32, 64, 17}) {
                SimpleFramebuffer source = new SimpleFramebuffer(size, size, false);
                SimpleFramebuffer target = new SimpleFramebuffer(size, size, false);
                try {
                    int preservedTexture = source.getColorAttachment();
                    GlStateManager._activeTexture(GL_TEXTURE0);
                    GlStateManager._bindTexture(preservedTexture);
                    GlStateManager._activeTexture(GL_TEXTURE3);
                    GlStateManager._bindTexture(preservedTexture);
                    noError("setup");
                    PixelReader.onSimpleFramebuffer(target);
                    noError("GUI texture parameters (GL_CLAMP is invalid in core)");
                    equal(GL_TEXTURE3, glGetInteger(GL_ACTIVE_TEXTURE), "active texture preserved");
                    equal(preservedTexture, glGetInteger(GL_TEXTURE_BINDING_2D), "unit 3 texture");
                    GlStateManager._activeTexture(GL_TEXTURE0);
                    equal(preservedTexture, glGetInteger(GL_TEXTURE_BINDING_2D), "unit 0 texture");
                    GlStateManager._bindTexture(target.getColorAttachment());
                    equal(GL_CLAMP_TO_EDGE, glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S), "wrap S");
                    equal(GL_CLAMP_TO_EDGE, glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T), "wrap T");

                    source.setClearColor(1, 0, 0, 1); source.clear();
                    target.setClearColor(0, 0, 0, 0); target.clear();
                    GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, target.fbo);
                    GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, source.fbo);
                    PixelReader.onIntIntIntIntIntIntIntIntIntInt(target.fbo, 0, size, 0, size, size, source.fbo, 0, size, 0);
                    equal(target.fbo, glGetInteger(GL_READ_FRAMEBUFFER_BINDING), "copy read target");
                    equal(source.fbo, glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING), "copy draw target");
                    equal(255, PixelReader.getIntByIntIntInt(target.fbo, size / 2, size / 2), "copied alpha");
                    equal(target.fbo, glGetInteger(GL_READ_FRAMEBUFFER_BINDING), "readback read target");
                    equal(source.fbo, glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING), "readback draw target");
                    noError("copy/readback");
                } finally { source.delete(); target.delete(); }
            }
            System.out.println("PASS: core-profile framebuffer parameters, texture units, distinct read/draw FBOs and copied pixels; GPU=" + glGetString(GL_RENDERER));
        } finally { GLFW.glfwDestroyWindow(window); GLFW.glfwTerminate(); }
    }
}
