package dev.bkclient.framework.shader.impl;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.mixin.accessors.FramebufferAccessor;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IWindow;
import dev.bkclient.utility.render.CustomRenderTarget;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;

public class FogBlurPost implements IMinecraft, IWindow {
   private static final float NEAR_PLANE = 0.05F;
   private static final float DOWNSCALE = 0.5F;
   private final CustomRenderTarget sharp = new CustomRenderTarget(false).setLinear();
   private final CustomRenderTarget blurA = new CustomRenderTarget(false).setDownscale(0.5F).setLinear();
   private final CustomRenderTarget blurB = new CustomRenderTarget(false).setDownscale(0.5F).setLinear();
   private KawaseBlurProgram kawaseDown;
   private KawaseBlurProgram kawaseUp;
   private FogBlurProgram composite;
   private int frameWidth;
   private int frameHeight;

   public void initShaders() {
      this.kawaseDown = new KawaseBlurProgram(BKClientInit.id("kawase_down/data"));
      this.kawaseUp = new KawaseBlurProgram(BKClientInit.id("kawase_up/data"));
      this.composite = new FogBlurProgram(BKClientInit.id("fog_blur/data"));
   }

   public void apply(float fogStart, float fogEnd, float blurStrength, int passes, float blurOffset, float blurFloor, boolean skipSky) {
      Framebuffer main = mc.getFramebuffer();
      int depthTexture = main == null ? 0 : ((FramebufferAccessor)main).getDepthAttachment();
      if (this.kawaseDown != null && this.kawaseUp != null && this.composite != null && depthTexture > 0) {
         if (main != null && main.getColorAttachment() > 0 && main.textureWidth > 0 && main.textureHeight > 0) {
            this.frameWidth = main.textureWidth;
            this.frameHeight = main.textureHeight;
            this.sharp.setFixedSize(this.frameWidth, this.frameHeight);
            this.blurA.setFixedSize(this.frameWidth, this.frameHeight);
            this.blurB.setFixedSize(this.frameWidth, this.frameHeight);
            int activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
            int[] bindings = new int[3];
            int[] shaderTextures = new int[3];
            for (int i = 0; i < bindings.length; i++) {
               GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + i);
               bindings[i] = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
               shaderTextures[i] = RenderSystem.getShaderTexture(i);
            }
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            boolean depthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
            boolean depthWrite = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
            boolean blend = GL11C.glIsEnabled(GL11C.GL_BLEND);
            float[] shaderColor = RenderSystem.getShaderColor().clone();
            RenderSystem.backupProjectionMatrix();
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();
            modelView.translation(0.0F, 0.0F, -11000.0F);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, this.frameWidth, this.frameHeight, 0.0F, 1000.0F, 21000.0F), ProjectionType.ORTHOGRAPHIC);

            try {
               RenderSystem.setShaderColor(1, 1, 1, 1);
               this.copyMainColor(main);
               int blurredTexture = this.buildBlur(Math.max(1.0F, blurOffset), Math.max(1, passes));
               this.composite(
                  main, depthTexture, blurredTexture, fogStart, fogEnd, Math.max(this.computeFarPlane(), fogEnd + 16.0F), blurStrength, blurFloor, skipSky
               );
            } finally {
               modelView.popMatrix();
               RenderSystem.restoreProjectionMatrix();
               main.beginWrite(true);
               // Sampler2 is also Minecraft's lightmap; never leave our depth texture or zero there.
               for (int i = 0; i < bindings.length; i++) {
                  RenderSystem.setShaderTexture(i, shaderTextures[i]);
                  GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + i);
                  GlStateManager._bindTexture(bindings[i]);
               }
               GlStateManager._activeTexture(activeTexture);
               RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
               RenderSystem.depthMask(depthWrite);
               if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
               if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            }
         }
      }
   }

   private void copyMainColor(Framebuffer main) {
      this.sharp.setDownscale(1.0F).setLinear();
      this.setupTarget(this.sharp);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.disableBlend();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      main.beginRead();
      RenderSystem.setShaderTexture(0, main.getColorAttachment());
      this.drawFullscreenQuad();
      main.endRead();
      RenderSystem.setShaderTexture(0, 0);
      this.sharp.stop();
   }

   private int buildBlur(float offset, int passes) {
      this.blurA.setDownscale(0.5F).setLinear();
      this.blurB.setDownscale(0.5F).setLinear();
      CustomRenderTarget[] buffers = new CustomRenderTarget[]{this.blurA, this.blurB};
      Framebuffer source = this.sharp;
      int steps = Math.min(passes, 16);
      this.kawaseDown.use();

      for (int i = 0; i < steps; i++) {
         CustomRenderTarget target = buffers[i % buffers.length];
         this.setupTarget(target);
         source.beginRead();
         RenderSystem.setShaderTexture(0, source.getColorAttachment());
         this.kawaseDown.updateUniforms(offset, source.textureWidth, source.textureHeight);
         this.drawFullscreenQuad();
         source.endRead();
         target.stop();
         source = target;
      }

      this.kawaseUp.use();

      for (int i = 0; i < steps; i++) {
         CustomRenderTarget target = source == this.blurA ? this.blurB : this.blurA;
         this.setupTarget(target);
         source.beginRead();
         RenderSystem.setShaderTexture(0, source.getColorAttachment());
         this.kawaseUp.updateUniforms(offset, source.textureWidth, source.textureHeight);
         this.drawFullscreenQuad();
         source.endRead();
         target.stop();
         source = target;
      }

      RenderSystem.setShaderTexture(0, 0);
      return source.getColorAttachment();
   }

   private void setupTarget(CustomRenderTarget target) {
      target.setup();
      target.beginWrite(true);
   }

   private float computeFarPlane() {
      return mc.gameRenderer == null ? 1024.0F : Math.max(64.0F, mc.gameRenderer.getFarPlaneDistance());
   }

   private void composite(
      Framebuffer main, int depthTexture, int blurredTexture, float fogStart, float fogEnd, float farPlane, float blurStrength, float blurOffset, boolean skipSky
   ) {
      main.beginWrite(true);
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.disableBlend();
      this.composite.use();
      this.composite.updateUniforms(fogStart, fogEnd, 0.05F, farPlane, blurStrength, Math.max(0.0F, blurOffset), skipSky);
      RenderSystem.setShaderTexture(0, this.sharp.getColorAttachment());
      RenderSystem.setShaderTexture(1, blurredTexture);
      RenderSystem.setShaderTexture(2, depthTexture);
      this.drawFullscreenQuad();
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.setShaderTexture(1, 0);
      RenderSystem.setShaderTexture(2, 0);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
   }

   private void drawFullscreenQuad() {
      this.drawQuad(0.0F, 0.0F, this.frameWidth, this.frameHeight);
   }

   private void drawQuad(float x, float y, float width, float height) {
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(x, y, 0.0F).texture(0.0F, 1.0F).color(-1);
      builder.vertex(x, y + height, 0.0F).texture(0.0F, 0.0F).color(-1);
      builder.vertex(x + width, y + height, 0.0F).texture(1.0F, 0.0F).color(-1);
      builder.vertex(x + width, y, 0.0F).texture(1.0F, 1.0F).color(-1);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   public void shutdown() {
      this.sharp.delete();
      this.blurA.delete();
      this.blurB.delete();
   }
}
