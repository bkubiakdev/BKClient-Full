package dev.bkclient.framework.shader.impl;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IWindow;
import dev.bkclient.utility.render.CustomRenderTarget;
import dev.bkclient.utility.time.Timer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;

public class BlurProgram implements IMinecraft, IWindow {
   private static final BlurTargets GUI = new BlurTargets();
   private static final BlurTargets HOTBAR = new BlurTargets();


   private static KawaseBlurProgram kawaseDownProgram;
   private static KawaseBlurProgram kawaseUpProgram;
   private float blurOffset = 1.0F;
   private float blurDownscale = 0.5F;
   private final CustomRenderTarget[] pingPong = new CustomRenderTarget[2];

   public void initShaders() {
      kawaseDownProgram = new KawaseBlurProgram(BKClientInit.id("kawase_down/data"));
      kawaseUpProgram = new KawaseBlurProgram(BKClientInit.id("kawase_up/data"));
   }

    public void draw() {
       if (mc.currentScreen instanceof dev.bkclient.ui.menu.clickgui.ClickGuiV2Screen
           && dev.bkclient.ui.menu.clickgui.GuiProfiler.hides(dev.bkclient.ui.menu.clickgui.GuiProfiler.Mode.NO_BLUR)) return;
       draw(Interface.glassBlur(), GUI);
    }

    /** Separate framebuffer and fixed strength: Interface settings cannot alter the hotbar blur. */
    public void drawHotbar() {
       draw(0.6F, HOTBAR);
    }

    private void draw(float strength, BlurTargets targets) {
       float quality = Interface.renderQualityStrength();
       long refreshDelay = quality >= 0.9F ? 16L : quality >= 0.5F ? 25L : 40L;
       if (targets.output == null || targets.timer.finished(refreshDelay)) {
          targets.timer.reset();
          Framebuffer mainFbo = mc.getFramebuffer();
         if (mainFbo != null && mainFbo.getColorAttachment() > 0 && mainFbo.textureWidth > 0 && mainFbo.textureHeight > 0) {
            this.blurOffset = strength;
            float effectiveOffset = this.blurOffset <= 0.001F ? 0.00000001F : this.blurOffset;
            float effectiveDownscale = 1.0F / (1.0F + this.blurOffset * 0.5F);
             int maxSteps = quality >= 0.9F ? 6 : quality >= 0.5F ? 4 : 2;
             int steps = Math.clamp((int)Math.ceil(this.blurOffset * 2.0F), 1, maxSteps);

             CustomRenderTarget cache = targets.cacheTarget();
             CustomRenderTarget buffer = targets.bufferTarget();
            cache.setDownscale(effectiveDownscale).setLinear();
            buffer.setDownscale(effectiveDownscale).setLinear();
            boolean scissor = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
            boolean depth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            boolean blend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
            float[] shaderColor = RenderSystem.getShaderColor().clone();
            RenderSystem.disableScissor();
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            try {
            int scaledWidth = mw.getScaledWidth();
            int scaledHeight = mw.getScaledHeight();
            kawaseDownProgram.use();
            kawaseDownProgram.updateUniforms(effectiveOffset, mainFbo.textureWidth, mainFbo.textureHeight);
            cache.setup();
            cache.beginWrite(true);
            mainFbo.beginRead();
            RenderSystem.setShaderTexture(0, mainFbo.getColorAttachment());
            this.drawQuad(0.0F, 0.0F, scaledWidth, scaledHeight);
            cache.stop();
            CustomRenderTarget[] buffers = this.pingPong;
            buffers[0] = cache;
            buffers[1] = buffer;

            for (int i = 1; i < steps; i++) {
               int step = i & 1;
               int previous = step ^ 1;
               CustomRenderTarget source = buffers[previous];
               CustomRenderTarget target = buffers[step];
               target.setup();
               target.beginWrite(true);
               source.beginRead();
               RenderSystem.setShaderTexture(0, source.getColorAttachment());
               kawaseDownProgram.updateUniforms(effectiveOffset, source.textureWidth, source.textureHeight);
               this.drawQuad(0.0F, 0.0F, scaledWidth, scaledHeight);
               source.endRead();
               target.stop();
            }

            kawaseUpProgram.use();

            int last = (steps - 1) & 1;
            for (int i = 0; i < steps; i++) {
               int step = last;
               int next = step ^ 1;
               CustomRenderTarget source = buffers[step];
               CustomRenderTarget target = buffers[next];
               target.setup();
               target.beginWrite(true);
               source.beginRead();
               RenderSystem.setShaderTexture(0, source.getColorAttachment());
               kawaseUpProgram.updateUniforms(effectiveOffset, source.textureWidth, source.textureHeight);
               this.drawQuad(0.0F, 0.0F, scaledWidth, scaledHeight);
               source.endRead();
               target.stop();
               last = next;
            }
            targets.output = buffers[last];
            } finally {
            mainFbo.endRead();
            mainFbo.beginWrite(true);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
            if (scissor) com.mojang.blaze3d.platform.GlStateManager._enableScissorTest();
            if (depth) RenderSystem.enableDepthTest();
            if (blend) RenderSystem.enableBlend();
            }
         }
      }
   }

   private void drawQuad(float x, float y, float width, float height) {
      int color = -1;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(x, y, 0.0F).texture(0.0F, 1.0F).color(color);
      builder.vertex(x, y + height, 0.0F).texture(0.0F, 0.0F).color(color);
      builder.vertex(x + width, y + height, 0.0F).texture(1.0F, 0.0F).color(color);
      builder.vertex(x + width, y, 0.0F).texture(1.0F, 1.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

    public static int getTexture() {
       return GUI.output == null ? GUI.bufferTarget().getColorAttachment() : GUI.output.getColorAttachment();
    }

    public static int getHotbarTexture() {
       return HOTBAR.output == null ? HOTBAR.bufferTarget().getColorAttachment() : HOTBAR.output.getColorAttachment();
    }

    public static void shutdown() {
       GUI.close();
       HOTBAR.close();
    }

    private static final class BlurTargets {
       private CustomRenderTarget cache, buffer, output;
       private final Timer timer = new Timer();

       private CustomRenderTarget cacheTarget() {
          if (cache == null) cache = new CustomRenderTarget(false).setLinear();
          return cache;
       }

       private CustomRenderTarget bufferTarget() {
          if (buffer == null) buffer = new CustomRenderTarget(false).setLinear();
          return buffer;
       }

       private void close() {
          output = null;
          if (cache != null) { cache.delete(); cache = null; }
          if (buffer != null) { buffer.delete(); buffer = null; }
       }
    }
   @Generated
   public void setBlurOffset(float blurOffset) {
      this.blurOffset = blurOffset;
   }

   @Generated
   public void setBlurDownscale(float blurDownscale) {
      this.blurDownscale = blurDownscale;
   }
}
