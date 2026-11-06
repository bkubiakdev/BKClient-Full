package dev.bkclient.utility.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.texture.AbstractTexture;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;

/**
 * Uploads a raw BGRA pixel buffer directly to OpenGL without a NativeImage copy.
 */
public class StreamedTexture extends AbstractTexture {

    private int width;
    private int height;

    /**
     * Uploads one frame, reallocating GPU storage if the source changed size.
     *
     * <p>Must run on the render thread.
     *
     * @param pixels BGRA bytes, top row first, at least {@code width * height * 4} long
     */
    public void upload(ByteBuffer pixels, int width, int height) {
        if (pixels == null || width <= 0 || height <= 0) {
            return;
        }
        RenderSystem.assertOnRenderThread();

        GlStateManager._bindTexture(this.getGlId());

        // Minecraft's own NativeImage upload sets GL_UNPACK_ROW_LENGTH, SKIP_ROWS and
        // SKIP_PIXELS and leaves them set. Whatever texture it touched last therefore decides
        // how the driver strides through our buffer: if that texture was wider than this one,
        // the read runs off the end and the process dies inside the driver with no Java frame
        // to show for it. This state has to be reset every time, not once at startup.
        GlStateManager._pixelStore(GL11C.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11C.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11C.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11C.GL_UNPACK_ALIGNMENT, 4);

        // Clamping matters here: the panel samples right up to the edge, and the default repeat
        // wrap bleeds the opposite side of the window in as a one-pixel seam.
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);

        pixels.rewind();

        if (width != this.width || height != this.height) {
            this.width = width;
            this.height = height;
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, width, height, 0,
                    GL12C.GL_BGRA, GL11C.GL_UNSIGNED_BYTE, pixels);
        } else {
            GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, width, height,
                    GL12C.GL_BGRA, GL11C.GL_UNSIGNED_BYTE, pixels);
        }
    }

    public boolean hasContent() {
        return this.width > 0 && this.height > 0;
    }

    @Override
    public void close() {
        this.width = 0;
        this.height = 0;
        super.close();
    }
}
