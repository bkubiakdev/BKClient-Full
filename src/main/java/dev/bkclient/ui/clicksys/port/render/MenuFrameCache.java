package dev.bkclient.ui.clicksys.port.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL30C.*;

/** Captures the completed menu frame; owns no game state or input handlers. Render-thread only. */
public final class MenuFrameCache implements AutoCloseable {
    private static final long INTERVAL_NANOS = 16_000_000L;
    private SimpleFramebuffer buffer;
    private long capturedAt;
    private boolean valid, rendering, releasePending;

    public void invalidate() { valid = false; }

    public void render(Framebuffer main, long now, Runnable live) {
        if (main == null || main.fbo < 0 || main.textureWidth <= 0 || main.textureHeight <= 0) {
            close();
            live.run();
            return;
        }
        if (valid && buffer != null && buffer.textureWidth == main.textureWidth
                && buffer.textureHeight == main.textureHeight
                && now - capturedAt >= 0 && now - capturedAt < INTERVAL_NANOS) {
            blit(buffer, main, false);
            return;
        }
        valid = false;
        rendering = true;
        releasePending = false;
        boolean finished = false;
        try {
            live.run();
            finished = true;
        } finally {
            rendering = false;
            if (releasePending || !finished) deleteBuffer();
        }
        // Closing animations can call setScreen(null) -> removed() inside live.run().
        if (releasePending) { releasePending = false; return; }
        ensureSize(main.textureWidth, main.textureHeight);
        blit(main, buffer, true);
        capturedAt = now;
        valid = true;
    }

    private void ensureSize(int width, int height) {
        if (buffer != null && buffer.textureWidth == width && buffer.textureHeight == height) return;
        // Framebuffer allocation also clears/binds textures and changes viewport/masks.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING), draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
            int active = glGetInteger(GL_ACTIVE_TEXTURE), texture = glGetInteger(GL_TEXTURE_BINDING_2D);
            var viewport = stack.mallocInt(4); glGetIntegerv(GL_VIEWPORT, viewport);
            var color = stack.mallocFloat(4); glGetFloatv(GL_COLOR_CLEAR_VALUE, color);
            var mask = stack.malloc(4); glGetBooleanv(GL_COLOR_WRITEMASK, mask);
            boolean depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
            try {
                deleteBuffer();
                buffer = new SimpleFramebuffer(width, height, false);
            } finally {
                GlStateManager._activeTexture(active);
                GlStateManager._bindTexture(texture);
                GlStateManager._viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                GlStateManager._clearColor(color.get(0), color.get(1), color.get(2), color.get(3));
                GlStateManager._colorMask(mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
                GlStateManager._depthMask(depthMask);
                GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
                GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
            }
        }
    }

    /** Opaque full-frame copy: no shader, texture sampling, blending or depth-test side effects. */
    private static void blit(Framebuffer source, Framebuffer target, boolean forceOpaque) {
        int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING), draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor = glIsEnabled(GL_SCISSOR_TEST);
        try {
            if (scissor) GlStateManager._disableScissorTest();
            GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, source.fbo);
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, target.fbo);
            glBlitFramebuffer(0, 0, source.textureWidth, source.textureHeight,
                    0, 0, target.textureWidth, target.textureHeight, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            if (forceOpaque) {
                // MSDF icon quads may leave alpha=0; keep their RGB and normalize only alpha.
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    var color = stack.mallocFloat(4); glGetFloatv(GL_COLOR_CLEAR_VALUE, color);
                    var mask = stack.malloc(4); glGetBooleanv(GL_COLOR_WRITEMASK, mask);
                    try {
                        GlStateManager._colorMask(false, false, false, true);
                        GlStateManager._clearColor(0, 0, 0, 1);
                        GlStateManager._clear(GL_COLOR_BUFFER_BIT);
                    } finally {
                        GlStateManager._clearColor(color.get(0), color.get(1), color.get(2), color.get(3));
                        GlStateManager._colorMask(mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
                    }
                }
            }
        } finally {
            GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
            if (scissor) GlStateManager._enableScissorTest();
        }
    }

    @Override public void close() {
        valid = false;
        if (rendering) releasePending = true;
        else deleteBuffer();
    }

    private void deleteBuffer() {
        valid = false;
        if (buffer != null) { buffer.delete(); buffer = null; }
    }
}
