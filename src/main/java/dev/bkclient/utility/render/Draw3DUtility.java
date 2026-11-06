package dev.bkclient.utility.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Draw3DUtility implements IMinecraft {
   public static void renderGlowingBox(MatrixStack matrices, BufferBuilder buffer, Box box, ColorRGBA color) {
      float r = color.getRed();
      float g = color.getGreen();
      float b = color.getBlue();
      float baseAlpha = color.getAlpha();
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      int glowLayers = 3;
      float glowStep = 0.1F;

      for (int i = glowLayers; i >= 1; i--) {
         float expand = i * glowStep;
         float alpha = baseAlpha * (0.15F / i);
         renderFilledBox(matrices, buffer, box.expand(expand), new ColorRGBA(r, g, b, alpha));
      }

      renderFilledBox(matrices, buffer, box, new ColorRGBA(r, g, b, baseAlpha));
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }

   public static void renderFilledBox(MatrixStack matrices, BufferBuilder buffer, Box box, ColorRGBA color) {
      float r = color.getRed() / 255.0F;
      float g = color.getGreen() / 255.0F;
      float b = color.getBlue() / 255.0F;
      float a = color.getAlpha() / 255.0F;
      renderFilledBox(matrices, buffer, box, r, g, b, a);
   }

   public static void renderBoxInternalDiagonals(MatrixStack matrices, BufferBuilder buf, Box box, ColorRGBA color) {
      float r = color.getRed() / 255.0F;
      float g = color.getGreen() / 255.0F;
      float b = color.getBlue() / 255.0F;
      float a = color.getAlpha() / 255.0F;
      float minX = (float)box.minX;
      float minY = (float)box.minY;
      float minZ = (float)box.minZ;
      float maxX = (float)box.maxX;
      float maxY = (float)box.maxY;
      float maxZ = (float)box.maxZ;
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      buf.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
      buf.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
      buf.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
      buf.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
      buf.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
      buf.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
      buf.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
      buf.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
   }

   // Render-thread scratch: the 8 box corners transformed by the current position matrix (x, y, z per corner).
   // Corner indices:
   // 0 (minX,minY,minZ) 1 (maxX,minY,minZ) 2 (maxX,minY,maxZ) 3 (minX,minY,maxZ)
   // 4 (minX,maxY,minZ) 5 (maxX,maxY,minZ) 6 (maxX,maxY,maxZ) 7 (minX,maxY,maxZ)
   private static final float[] CORNERS = new float[24];
   private static final Vector3f CORNER = new Vector3f();
   private static final int[] FILLED_ORDER = {0, 1, 2, 3, 4, 5, 6, 7, 3, 2, 6, 7, 0, 1, 5, 4, 0, 3, 7, 4, 1, 2, 6, 5};
   private static final int[] OUTLINE_ORDER = {0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7};

   /**
    * VertexConsumer#vertex(Matrix4f, x, y, z) is exactly matrix.transformPosition(x, y, z, new Vector3f())
    * followed by vertex(x', y', z'). Boxes emit 24 vertices from only 8 distinct corners, so the corners are
    * transformed once here and emitted directly in the original order: bit-identical vertex data.
    */
   private static void transformCorners(Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
      corner(matrix, 0, minX, minY, minZ);
      corner(matrix, 1, maxX, minY, minZ);
      corner(matrix, 2, maxX, minY, maxZ);
      corner(matrix, 3, minX, minY, maxZ);
      corner(matrix, 4, minX, maxY, minZ);
      corner(matrix, 5, maxX, maxY, minZ);
      corner(matrix, 6, maxX, maxY, maxZ);
      corner(matrix, 7, minX, maxY, maxZ);
   }

   private static void corner(Matrix4f matrix, int index, float x, float y, float z) {
      matrix.transformPosition(x, y, z, CORNER);
      CORNERS[index * 3] = CORNER.x;
      CORNERS[index * 3 + 1] = CORNER.y;
      CORNERS[index * 3 + 2] = CORNER.z;
   }

   private static void emitCorners(BufferBuilder buffer, int[] order, float r, float g, float b, float a) {
      float[] corners = CORNERS;
      for (int i = 0; i < order.length; i++) {
         int c = order[i] * 3;
         buffer.vertex(corners[c], corners[c + 1], corners[c + 2]).color(r, g, b, a);
      }
   }

   public static void renderFilledBox(MatrixStack matrices, BufferBuilder buffer, Box box, float r, float g, float b, float a) {
      renderFilledBox(matrices.peek().getPositionMatrix(), buffer, box, r, g, b, a);
   }

   /** Same as renderFilledBox(MatrixStack, ...) with the position matrix passed directly (no push/pop needed). */
   public static void renderFilledBox(Matrix4f matrix, BufferBuilder buffer, Box box, ColorRGBA color) {
      float r = color.getRed() / 255.0F;
      float g = color.getGreen() / 255.0F;
      float b = color.getBlue() / 255.0F;
      float a = color.getAlpha() / 255.0F;
      renderFilledBox(matrix, buffer, box, r, g, b, a);
   }

   public static void renderFilledBox(Matrix4f matrix, BufferBuilder buffer, Box box, float r, float g, float b, float a) {
      float minX = (float)box.minX;
      float minY = (float)box.minY;
      float minZ = (float)box.minZ;
      float maxX = (float)box.maxX;
      float maxY = (float)box.maxY;
      float maxZ = (float)box.maxZ;
      transformCorners(matrix, minX, minY, minZ, maxX, maxY, maxZ);
      emitCorners(buffer, FILLED_ORDER, r, g, b, a);
   }

   public static void renderOutlinedBox(MatrixStack matrices, BufferBuilder buffer, Box box, ColorRGBA color) {
      float r = color.getRed() / 255.0F;
      float g = color.getGreen() / 255.0F;
      float b = color.getBlue() / 255.0F;
      float a = color.getAlpha() / 255.0F;
      float minX = (float)box.minX;
      float minY = (float)box.minY;
      float minZ = (float)box.minZ;
      float maxX = (float)box.maxX;
      float maxY = (float)box.maxY;
      float maxZ = (float)box.maxZ;
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      transformCorners(matrix, minX, minY, minZ, maxX, maxY, maxZ);
      emitCorners(buffer, OUTLINE_ORDER, r, g, b, a);
   }

   public static void drawLine(MatrixStack matrices, VertexConsumer vertexConsumer, Vec3d startPos, Vec3d endPos, ColorRGBA color) {
      Entry entry = matrices.peek();
      Vec3d normalized = endPos.subtract(startPos).normalize();
      Vector3f startVector = new Vector3f((float)startPos.x, (float)startPos.y, (float)startPos.z);
      vertexConsumer.vertex(entry, startVector)
         .color(color.getRGB())
         .normal(entry, (float)normalized.x, (float)normalized.y, (float)normalized.z);
      vertexConsumer.vertex(entry, (float)endPos.x, (float)endPos.y, (float)endPos.z)
         .color(color.getRGB())
         .normal(entry, (float)normalized.x, (float)normalized.y, (float)normalized.z);
   }

   public static void drawLine(MatrixStack matrices, BufferBuilder builder, Vec3d startPos, Vec3d endPos, ColorRGBA color) {
      Entry matrixEntry = matrices.peek();
      Matrix4f matrix4f = matrixEntry.getPositionMatrix();
      Vec3d normalized = endPos.subtract(startPos).normalize();
      builder.vertex(matrix4f, (float)startPos.x, (float)startPos.y, (float)startPos.z)
         .color(color.getRGB())
         .normal(matrixEntry, (float)normalized.x, (float)normalized.y, (float)normalized.z);
      builder.vertex(matrix4f, (float)endPos.x, (float)endPos.y, (float)endPos.z)
         .color(color.getRGB())
         .normal(matrixEntry, (float)normalized.x, (float)normalized.y, (float)normalized.z);
   }

   public static void renderLineFromPlayer(MatrixStack matrices, BufferBuilder builder, Vec3d endPos, ColorRGBA color) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      Vec3d cameraRotationVector = new Vec3d(0.0, 0.0, 27.0)
         .rotateX((float)(-Math.toRadians(camera.getPitch())))
         .rotateY((float)(-Math.toRadians(camera.getYaw())));
      Vec3d cameraRelatedEndPos = endPos.subtract(cameraPos);
      Vec3d startPos = new Vec3d(cameraRotationVector.getX(), cameraRotationVector.getY(), cameraRotationVector.getZ());
      drawLine(matrices, builder, startPos, cameraRelatedEndPos, color);
   }

   @Generated
   private Draw3DUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
