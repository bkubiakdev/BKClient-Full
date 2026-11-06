package dev.bkclient.ui.clicksys.port.render;

import com.mojang.blaze3d.platform.GlStateManager;
import java.nio.ByteBuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.*;

public final class PixelReader {
   private static final ByteBuffer byteBuffer = BufferUtils.createByteBuffer(4);

   public static int getIntByIntIntInt(int count, int count2, int count3) {
      int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
      try {
         GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, count);
         byteBuffer.clear();
         GL11.glReadPixels(count2, count3, 1, 1, 6408, 5121, byteBuffer);
         return byteBuffer.get(3) & 0xFF;
      } catch (Throwable throwable) {
         return -1;
      } finally {
         GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
      }
   }

   public static void onSimpleFramebuffer(SimpleFramebuffer simpleFramebuffer) {
      int activeTexture = GL11.glGetInteger(GL_ACTIVE_TEXTURE);
      GlStateManager._activeTexture(GL_TEXTURE0);
      int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      try {
         GlStateManager._bindTexture(simpleFramebuffer.getColorAttachment());
         GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
         GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
         // GL_CLAMP was removed from the core profile used by Minecraft.
         GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
         GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
      } finally {
         GlStateManager._bindTexture(texture);
         GlStateManager._activeTexture(activeTexture);
      }
   }

   public static void onIntIntIntIntIntIntIntIntIntInt(int count, int count2, int count3, int count4, int count5, int count6, int count7, int count8, int count9, int count10) {
      int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
      int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
      try {
         GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, count7);
         GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, count);
         GL30.glBlitFramebuffer(count8, count4, count9, count6, count2, count10, count5, count3, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
      } finally {
         GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
         GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
      }
   }
}
