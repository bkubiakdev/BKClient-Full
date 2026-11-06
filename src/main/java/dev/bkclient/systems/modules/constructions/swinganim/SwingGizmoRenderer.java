package dev.bkclient.systems.modules.constructions.swinganim;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;

/** Antialiased strokes for the swing editor and its easing graph. */
final class SwingGizmoRenderer {
   private SwingGizmoRenderer() { }

   static void line(MatrixStack matrices, Vec2f from, Vec2f to, ColorRGBA color) {
      line(matrices, from, to, color, 1.5F);
   }

   static void line(MatrixStack matrices, Vec2f from, Vec2f to, ColorRGBA color, float thickness) {
      stroke(matrices, new Vec2f[][]{{from, to}}, color, thickness);
   }

   static void bezier(MatrixStack matrices, Vec2f a, Vec2f b, Vec2f c, Vec2f d, ColorRGBA color) {
      Vec2f[] points = new Vec2f[51];
      for (int i = 0; i < points.length; i++) {
         float t = i / 50.0F, u = 1 - t;
         points[i] = new Vec2f(u*u*u*a.x + 3*u*u*t*b.x + 3*u*t*t*c.x + t*t*t*d.x,
            u*u*u*a.y + 3*u*u*t*b.y + 3*u*t*t*c.y + t*t*t*d.y);
      }
      stroke(matrices, new Vec2f[][]{points}, color, 1.5F);
   }

   static void dashedBorder(MatrixStack matrices, float x, float y, float size, float radius, ColorRGBA color) {
      float straight = Math.max(0, size - 2 * radius), arc = (float)Math.PI * radius / 2;
      float perimeter = 4 * (straight + arc);
      List<Vec2f[]> dashes = new ArrayList<>();
      for (float offset = 0; offset < perimeter; offset += 6) {
         float length = Math.min(3, perimeter - offset);
         Vec2f[] dash = new Vec2f[5];
         for (int i = 0; i < dash.length; i++) {
            dash[i] = borderPoint(x, y, size, radius, straight, arc, offset + length * i / 4);
         }
         dashes.add(dash);
      }
      stroke(matrices, dashes.toArray(Vec2f[][]::new), color, 0.5F);
   }

   private static Vec2f borderPoint(float x, float y, float size, float radius, float straight, float arc, float distance) {
      float quarter = straight + arc;
      int side = Math.min(3, (int)(distance / quarter));
      float local = distance - side * quarter;
      float px, py;
      if (local <= straight) {
         px = radius + local;
         py = 0;
      } else {
         float angle = (local - straight) / radius - (float)Math.PI / 2;
         px = size - radius + radius * (float)Math.cos(angle);
         py = radius + radius * (float)Math.sin(angle);
      }
      for (int i = 0; i < side; i++) {
         float previous = px;
         px = size - py;
         py = previous;
      }
      return new Vec2f(x + px, y + py);
   }

   private static void stroke(MatrixStack matrices, Vec2f[][] paths, ColorRGBA color, float thickness) {
      boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
      float[] shaderColor = RenderSystem.getShaderColor().clone();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.setShaderColor(1, 1, 1, 1);
      try {
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         int solid = color.mulAlpha(shaderColor[3]).getRGB(), clear = color.withAlpha(0).getRGB();
         float half = thickness / 2, feather = half + 0.65F;
         for (Vec2f[] points : paths) {
            Vec2f[] normals = new Vec2f[points.length];
            for (int i = 0; i < points.length; i++) {
               Vec2f before = points[Math.max(0, i - 1)], after = points[Math.min(points.length - 1, i + 1)];
               float dx = after.x - before.x, dy = after.y - before.y;
               float length = Math.max(0.0001F, (float)Math.hypot(dx, dy));
               normals[i] = new Vec2f(-dy / length, dx / length);
            }
            for (int i = 0; i < points.length - 1; i++) {
               band(buffer, matrix, points[i], points[i + 1], normals[i], normals[i + 1], -feather, -half, clear, solid);
               band(buffer, matrix, points[i], points[i + 1], normals[i], normals[i + 1], -half, half, solid, solid);
               band(buffer, matrix, points[i], points[i + 1], normals[i], normals[i + 1], half, feather, solid, clear);
            }
         }
         BufferRenderer.drawWithGlobalProgram(buffer.end());
      } finally {
         if (cull) RenderSystem.enableCull();
         if (!blend) RenderSystem.disableBlend();
         RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
      }
   }

   private static void band(BufferBuilder buffer, Matrix4f matrix, Vec2f a, Vec2f b, Vec2f na, Vec2f nb,
      float inner, float outer, int inside, int outside) {
      buffer.vertex(matrix, a.x + na.x * inner, a.y + na.y * inner, 0).color(inside);
      buffer.vertex(matrix, a.x + na.x * outer, a.y + na.y * outer, 0).color(outside);
      buffer.vertex(matrix, b.x + nb.x * outer, b.y + nb.y * outer, 0).color(outside);
      buffer.vertex(matrix, b.x + nb.x * inner, b.y + nb.y * inner, 0).color(inside);
   }
}
