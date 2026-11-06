package dev.bkclient.utility.render.ambience;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.system.MemoryUtil;

/** Diagnostic-only readback. Restores texture and every modified pixel-pack parameter. */
public final class FogTextureReadback {
    private static final int[] PACK = {GL11C.GL_PACK_ALIGNMENT, GL11C.GL_PACK_ROW_LENGTH,
        GL11C.GL_PACK_SKIP_ROWS, GL11C.GL_PACK_SKIP_PIXELS, GL11C.GL_PACK_SWAP_BYTES,
        GL11C.GL_PACK_LSB_FIRST, GL12C.GL_PACK_IMAGE_HEIGHT, GL12C.GL_PACK_SKIP_IMAGES};

    public record Pixels(int width, int height, byte[] bytes) {}

    public static Pixels read(int texture, boolean depth) {
        int binding = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        int pbo = GL11C.glGetInteger(GL21C.GL_PIXEL_PACK_BUFFER_BINDING);
        int[] previous = new int[PACK.length];
        for (int i = 0; i < PACK.length; i++) previous[i] = GL11C.glGetInteger(PACK[i]);
        ByteBuffer buffer = null;
        try {
            // Raw GL binding is restored before returning; Minecraft's cache is never changed.
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);
            int width = GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_WIDTH);
            int height = GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_HEIGHT);
            if (width <= 0 || height <= 0 || (long) width * height > 16_777_216L)
                throw new IllegalStateException("Unsupported diagnostic texture size: " + width + "x" + height);
            GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, 0);
            for (int parameter : PACK) GL11C.glPixelStorei(parameter, parameter == GL11C.GL_PACK_ALIGNMENT ? 1 : 0);
            buffer = MemoryUtil.memAlloc(Math.multiplyExact(Math.multiplyExact(width, height), 4));
            GL11C.glGetTexImage(GL11C.GL_TEXTURE_2D, 0, depth ? GL11C.GL_DEPTH_COMPONENT : GL11C.GL_RGBA,
                depth ? GL11C.GL_FLOAT : GL11C.GL_UNSIGNED_BYTE, buffer);
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            if (depth && ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
                for (int i = 0; i < bytes.length; i += 4) {
                    byte a = bytes[i], b = bytes[i + 1];
                    bytes[i] = bytes[i + 3]; bytes[i + 1] = bytes[i + 2];
                    bytes[i + 2] = b; bytes[i + 3] = a;
                }
            }
            return new Pixels(width, height, bytes);
        } finally {
            if (buffer != null) MemoryUtil.memFree(buffer);
            for (int i = 0; i < PACK.length; i++) GL11C.glPixelStorei(PACK[i], previous[i]);
            GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, pbo);
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, binding);
        }
    }

    private FogTextureReadback() {}
}
