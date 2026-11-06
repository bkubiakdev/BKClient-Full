package dev.bkclient.utility.render;

import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.framework.shader.GlProgram;
import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;

/** Analytic round-capped arcs, with one coverage sample per pixel and no overlapping cap geometry. */
public final class RingRenderer {
   private static GlProgram program;
   private RingRenderer() {}
   public static void initializeShader() {
      program = new GlProgram(BKClientInit.id("hud_ring/data"), VertexFormats.POSITION_TEXTURE_COLOR);
   }
   public static void ring(MatrixStack m,float x,float y,float r,float t,ColorRGBA c) { arc(m,x,y,r,t,0,360,c,false); }
   public static void arc(MatrixStack m,float x,float y,float r,float t,float start,float sweep,ColorRGBA c) { arc(m,x,y,r,t,start,sweep,c,true); }
   public static void arc(MatrixStack m,float x,float y,float r,float t,float start,float sweep,ColorRGBA c,boolean caps) {
      if (r<=0 || t<=0 || sweep<=0 || c.getAlpha()<=0) return;
      t=Math.min(t,r);
      var matrix=m.peek().getPositionMatrix();
      float scale=Math.max(.01F, Math.min((float)Math.hypot(matrix.m00(),matrix.m01()),(float)Math.hypot(matrix.m10(),matrix.m11())));
      float extent=r+2/scale;
      boolean blend=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
      boolean cull=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
      try {
         RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
         program.use();
         program.findUniform("Extent").set(extent);
         program.findUniform("Radius").set(r-t/2);
         program.findUniform("HalfWidth").set(t/2);
         program.findUniform("Start").set((float)Math.toRadians(start));
         program.findUniform("Sweep").set((float)Math.toRadians(Math.min(360,sweep)));
         program.findUniform("Rounded").set(caps?1:0);
         var b=RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE_COLOR);
         b.vertex(matrix,x-extent,y-extent,0).texture(0,0).color(c.getRGB());
         b.vertex(matrix,x-extent,y+extent,0).texture(0,1).color(c.getRGB());
         b.vertex(matrix,x+extent,y+extent,0).texture(1,1).color(c.getRGB());
         b.vertex(matrix,x+extent,y-extent,0).texture(1,0).color(c.getRGB());
         BufferRenderer.drawWithGlobalProgram(b.end());
      } finally {
         if(cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
         if(blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
      }
   }
   public static void progress(
      MatrixStack matrices,
      float cx,
      float cy,
      float radius,
      float thickness,
      float progress,
      float gapDegrees,
      ColorRGBA track,
      ColorRGBA fill
   ) {
      float span = 360.0F - 2.0F * gapDegrees;
      if (gapDegrees > 0.0F) {
         arc(matrices, cx, cy, radius, thickness, gapDegrees, span, track, true);
      } else {
         ring(matrices, cx, cy, radius, thickness, track);
      }

      float clamped = Math.max(0.0F, Math.min(1.0F, progress));
      if (clamped > 0.0F) {
         arc(matrices, cx, cy, radius, thickness, gapDegrees, span * clamped, fill, true);
      }
   }

}
