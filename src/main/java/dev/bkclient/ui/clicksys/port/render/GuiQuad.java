package dev.bkclient.ui.clicksys.port.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/** Immutable geometry for GUI quads; positions change through the draw matrix. */
public final class GuiQuad {
   private static VertexBuffer buffer;
   private static final Matrix4f modelView = new Matrix4f();

   private GuiQuad() { }

   public static void draw(Matrix4f matrix, float x, float y, float width, float height) {
      RenderSystem.assertOnRenderThread();
      // This buffer is separate from BufferRenderer's per-format upload buffer.
      BufferRenderer.reset();
      try {
         if (buffer == null) {
            buffer = VertexBuffer.createAndUpload(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE, vertices -> {
               vertices.vertex(0, 0, 0).texture(0, 0);
               vertices.vertex(0, 1, 0).texture(0, 1);
               vertices.vertex(1, 1, 0).texture(1, 1);
               vertices.vertex(1, 0, 0).texture(1, 0);
            });
         }
         modelView.set(RenderSystem.getModelViewMatrix()).mul(matrix).translate(x, y, 0).scale(width, height, 1);
         buffer.bind();
         buffer.draw(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
      } finally {
         VertexBuffer.unbind();
         BufferRenderer.resetCurrentVertexBuffer();
      }
   }

   public static void close() {
      if (buffer != null) {
         buffer.close();
         buffer = null;
      }
   }
}
