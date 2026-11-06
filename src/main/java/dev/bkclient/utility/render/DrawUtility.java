package dev.bkclient.utility.render;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashSet;
import java.util.Set;
import lombok.Generated;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.gradient.Gradient;
import dev.bkclient.framework.shader.GlProgram;
import dev.bkclient.framework.shader.impl.BlurProgram;
import dev.bkclient.framework.shader.impl.FogBlurPost;
import dev.bkclient.framework.shader.impl.GuiShatterPost;
import dev.bkclient.framework.shader.impl.GlassVaporPost;
import dev.bkclient.framework.shader.impl.JumpDistortPost;
import dev.bkclient.framework.shader.impl.SkyShaderProgram;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IWindow;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.render.batching.Batching;
import dev.bkclient.utility.render.batching.impl.IconBatching;
import dev.bkclient.utility.render.batching.impl.RectBatching;
import dev.bkclient.utility.render.batching.impl.RoundedRectBatching;
import dev.bkclient.utility.render.batching.impl.SquircleBatching;
import dev.bkclient.utility.render.obj.CustomSprite;
import dev.bkclient.utility.render.sprite.SpriteFrame;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderer;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public final class DrawUtility implements IMinecraft, IWindow {
   public static final float DEFAULT_SMOOTHNESS = 0.5F;
   public static final HookLimiter limiter = new HookLimiter(true);
   public static GlProgram rectangleProgram;
   private static GlProgram squircleProgram;
   private static GlProgram roundedTextureProgram;
   private static GlProgram squircleTextureProgram;
   private static GlProgram borderProgram;
   private static GlProgram loadingProgram;
   private static GlProgram glassProgram;
   private static GlProgram islandMorphProgram;
   private static GlProgram gradientRectangleProgram;
   public static SkyShaderProgram skyNebulaProgram;
   public static SkyShaderProgram skyCausticProgram;
   public static SkyShaderProgram skyRadiantProgram;
   public static SkyShaderProgram skySunsetProgram;
   public static dev.bkclient.framework.shader.impl.BakedSkyShaderProgram skyGalaxyProgram;
   public static dev.bkclient.framework.shader.impl.BakedSkyShaderProgram skySpaceProgram;
   public static BlurProgram blurProgram;
    public static FogBlurPost fogBlurPost;
    public static JumpDistortPost jumpDistortPost;
    public static dev.bkclient.framework.shader.impl.ParticleLightPost particleLightPost;
    public static dev.bkclient.framework.shader.impl.TargetLensPost targetLensPost;
    public static dev.bkclient.framework.shader.impl.AmbiencePost ambiencePost;
    public static GlassVaporPost glassVaporPost;
    public static GuiShatterPost guiShatterPost;
    private static final CustomRenderTarget buffer = new CustomRenderTarget(false);

   public static void initializeShaders() {
      RingRenderer.initializeShader();
      rectangleProgram = new GlProgram(BKClientInit.id("rectangle/data"), VertexFormats.POSITION_COLOR);
      squircleProgram = new GlProgram(BKClientInit.id("squircle/data"), VertexFormats.POSITION_COLOR);
      squircleTextureProgram = new GlProgram(BKClientInit.id("squircle_texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      roundedTextureProgram = new GlProgram(BKClientInit.id("texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      borderProgram = new GlProgram(BKClientInit.id("border/data"), VertexFormats.POSITION_COLOR);
      loadingProgram = new GlProgram(BKClientInit.id("loading/data"), VertexFormats.POSITION_COLOR);
      islandMorphProgram = new GlProgram(BKClientInit.id("island_morph/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      glassProgram = new GlProgram(BKClientInit.id("liquidglass/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      gradientRectangleProgram = new GlProgram(BKClientInit.id("gradient_rectangle/data"), VertexFormats.POSITION_COLOR);
      skyNebulaProgram = new SkyShaderProgram(BKClientInit.id("sky_nebula/data"));
      skyCausticProgram = new SkyShaderProgram(BKClientInit.id("sky_caustic/data"));
      skyRadiantProgram = new SkyShaderProgram(BKClientInit.id("sky_radiant/data"));
      skySunsetProgram = new SkyShaderProgram(BKClientInit.id("sky_sunset/data"));
      skyGalaxyProgram = new dev.bkclient.framework.shader.impl.BakedSkyShaderProgram("sky_galaxy");
      skySpaceProgram = new dev.bkclient.framework.shader.impl.BakedSkyShaderProgram("sky_space");
      blurProgram = new BlurProgram();
      blurProgram.initShaders();
      fogBlurPost = new FogBlurPost();
      fogBlurPost.initShaders();
      jumpDistortPost = new JumpDistortPost();
      jumpDistortPost.initShaders();
      particleLightPost = new dev.bkclient.framework.shader.impl.ParticleLightPost();
      particleLightPost.initShaders();
      targetLensPost = new dev.bkclient.framework.shader.impl.TargetLensPost();
      targetLensPost.initShaders();
      ambiencePost = new dev.bkclient.framework.shader.impl.AmbiencePost();
      ambiencePost.initShaders();
      dev.bkclient.utility.render.ambience.AmbienceRenderer.initialize();
      glassVaporPost = new GlassVaporPost();
      glassVaporPost.initShaders();
      guiShatterPost = new GuiShatterPost();
      guiShatterPost.initShaders();
   }

   public static void releaseWorldTargets() {
      if (targetLensPost != null) targetLensPost.releaseTargets();
      if (particleLightPost != null) particleLightPost.releaseTargets();
   }

   public static void shutdown() {
      BlurProgram.shutdown();
      if (fogBlurPost != null) fogBlurPost.shutdown();
      if (jumpDistortPost != null) jumpDistortPost.shutdown();
      if (particleLightPost != null) particleLightPost.shutdown();
      if (targetLensPost != null) targetLensPost.shutdown();
      if (ambiencePost != null) ambiencePost.shutdown();
      dev.bkclient.utility.render.ambience.AmbienceRenderer.close();
      if (glassVaporPost != null) glassVaporPost.shutdown();
      if (guiShatterPost != null) guiShatterPost.shutdown();
      buffer.delete();
      dev.bkclient.utility.render.shader.HandShaderHelper.releaseFramebuffers();
      dev.bkclient.utility.render.shader.HandShaderHelper.releasePrograms();
      SkyboxRenderer.close();
      if (skyGalaxyProgram != null) skyGalaxyProgram.close();
      if (skySpaceProgram != null) skySpaceProgram.close();
      LINEAR_TEXTURES.clear();
   }

   public static void updateBuffer() {
      buffer.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
      buffer.setup();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      mc.getFramebuffer().beginRead();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, mc.getFramebuffer().getColorAttachment());
      drawQuad(0.0F, 0.0F, mw.getScaledWidth(), mw.getScaledHeight(), true);
      mc.getFramebuffer().endRead();
      RenderSystem.disableBlend();
      mc.getFramebuffer().beginWrite(true);
      buffer.stop();
   }

   private static void drawQuad(float x, float y, float width, float height, boolean flip) {
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      int color = -1;
      float vTop = flip ? 0.0F : 1.0F;
      float vBottom = flip ? 1.0F : 0.0F;
      builder.vertex(x, y, 0.0F).texture(0.0F, vBottom).color(-1);
      builder.vertex(x, y + height, 0.0F).texture(0.0F, vTop).color(-1);
      builder.vertex(x + width, y + height, 0.0F).texture(1.0F, vTop).color(-1);
      builder.vertex(x + width, y, 0.0F).texture(1.0F, vBottom).color(-1);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   public static void drawLine(MatrixStack matrices, Vec2f from, Vec2f to, ColorRGBA color) {
      matrices.push();

      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, from.x, from.y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, to.x, to.y, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }
   }

   public static void drawBezier(MatrixStack matrices, Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      matrices.push();

      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

         for (int i = 0; i <= resolution; i++) {
            float t = (float)i / resolution;
            float x = (float)MathUtility.cubicBezier(t, p0.x, p1.x, p2.x, p3.x);
            float y = (float)MathUtility.cubicBezier(t, p0.y, p1.y, p2.y, p3.y);
            builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }
   }

   private static float cubicBezier(float t, float p0, float p1, float p2, float p3) {
      float u = 1.0F - t;
      float tt = t * t;
      float uu = u * u;
      return uu * u * p0 + 3.0F * uu * t * p1 + 3.0F * u * tt * p2 + tt * t * p3;
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, ColorRGBA color) {
      if (Batching.getActive() instanceof RectBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
         builder.vertex(matrix4f, x, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
      } else {
         matrices.push();
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, x, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
         matrices.pop();
      }
   }

   public static void drawSquircle(MatrixStack matrices, float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f m = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (Batching.getActive() instanceof SquircleBatching sb) {
         sb.add(
            m,
            x,
            y,
            width,
            height,
            borderRadius.topLeftRadius() * squirt / 2.0F,
            borderRadius.bottomLeftRadius() * squirt / 2.0F,
            borderRadius.topRightRadius() * squirt / 2.0F,
            borderRadius.bottomRightRadius() * squirt / 2.0F,
            color.getRGB()
         );
         matrices.pop();
      } else {
         squircleProgram.use();
         squircleProgram.findUniform("Size").set(width, height);
         squircleProgram.findUniform("Radius")
            .set(
               borderRadius.topLeftRadius() * squirt / 2.0F,
               borderRadius.bottomLeftRadius() * squirt / 2.0F,
               borderRadius.topRightRadius() * squirt / 2.0F,
               borderRadius.bottomRightRadius() * squirt / 2.0F
            );
         squircleProgram.findUniform("Smoothness").set(smoothness);
         squircleProgram.findUniform("CornerSmoothness").set(squirt);
         drawSetup();
         float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
         float verticalPadding = smoothness / 2.0F + smoothness;
         float ax = x - horizontalPadding / 2.0F;
         float ay = y - verticalPadding / 2.0F;
         float aw = width + horizontalPadding;
         float ah = height + verticalPadding;
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(m, ax, ay, 0.0F).color(color.getRGB());
         builder.vertex(m, ax, ay + ah, 0.0F).color(color.getRGB());
         builder.vertex(m, ax + aw, ay + ah, 0.0F).color(color.getRGB());
         builder.vertex(m, ax + aw, ay, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
         matrices.pop();
      }
   }

   public static void drawLoadingRect(
      MatrixStack matrices, float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color
   ) { drawLoadingRect(matrices,x,y,width,height,progress,borderRadius,color,2); }
   public static void drawLoadingRect(MatrixStack matrices,float x,float y,float width,float height,float progress,BorderRadius borderRadius,ColorRGBA color,float corner) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      loadingProgram.use();
      loadingProgram.findUniform("CornerSmoothness").set(Math.max(1,corner));
      loadingProgram.findUniform("Size").set(width, height);
      loadingProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      loadingProgram.findUniform("Smoothness").set(smoothness);
      loadingProgram.findUniform("Progress").set(progress);
      loadingProgram.findUniform("StripeWidth").set(0.0F);
      loadingProgram.findUniform("Fade").set(0.5F);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x;
      float adjustedY = y;
      float adjustedWidth = width;
      float adjustedHeight = height;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawLiquidGlass(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color,
      float globalAlpha,
      float fresnelPower,
      ColorRGBA fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      boolean clean
   ) {
      drawLiquidGlass(matrices, x, y, width, height, borderRadius, color, globalAlpha, fresnelPower,
         fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, clean,
         clean ? mc.getFramebuffer().getColorAttachment() : BlurProgram.getTexture());
   }

   public static void drawLiquidGlass(
      MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius,
      ColorRGBA color, float globalAlpha, float fresnelPower, ColorRGBA fresnelColor, float baseAlpha,
      boolean fresnelInvert, float fresnelMix, float distortStrength, float squirt, boolean clean, int texture
   ) {
       Matrix4f matrix = matrices.peek().getPositionMatrix();
       Vector4f topLeft = new Vector4f(x, y, 0.0F, 1.0F).mul(matrix);
       Vector4f bottomLeft = new Vector4f(x, y + height, 0.0F, 1.0F).mul(matrix);
       Vector4f bottomRight = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(matrix);
       Vector4f topRight = new Vector4f(x + width, y, 0.0F, 1.0F).mul(matrix);
       float renderedWidth = Math.max(0.001F, topLeft.distance(topRight));
       float renderedHeight = Math.max(0.001F, topLeft.distance(bottomLeft));
       float scaleX = renderedWidth / Math.max(0.001F, width);
       float scaleY = renderedHeight / Math.max(0.001F, height);
       float radiusScale = Math.min(scaleX, scaleY);
       drawSetup();
       RenderSystem.disableCull();
       RenderSystem.setShaderTexture(0, texture);
       glassProgram.use();
      glassProgram.findUniform("GlobalAlpha").set(globalAlpha);
      // The vertices can be scaled/rotated by HUD animations. Feed the shader the
      // actual on-screen dimensions so its edge, glare and distortion stay attached
      // to the panel instead of rendering at the unscaled position and size.
      glassProgram.findUniform("Size").set(renderedWidth, renderedHeight);
      glassProgram.findUniform("Radius")
         .set(
            borderRadius.topLeftRadius() * radiusScale,
            borderRadius.bottomLeftRadius() * radiusScale,
            borderRadius.topRightRadius() * radiusScale,
            borderRadius.bottomRightRadius() * radiusScale
         );
      glassProgram.findUniform("Smoothness").set(0.5F);
      glassProgram.findUniform("FresnelPower").set(fresnelPower);
      glassProgram.findUniform("FresnelColor").set(ColorUtility.getRGBf(fresnelColor.getRGB()));
      glassProgram.findUniform("FresnelAlpha").set(ColorUtility.alphaf(fresnelColor.getRGB()));
      glassProgram.findUniform("BaseAlpha").set(baseAlpha);
      glassProgram.findUniform("FresnelInvert").set(fresnelInvert ? 1 : 0);
      glassProgram.findUniform("FresnelMix").set(fresnelMix);
        glassProgram.findUniform("DistortStrength").set(distortStrength);
       glassProgram.findUniform("CornerSmoothness").set(squirt);
        glassProgram.findUniform("GlareStrength").set(Interface.glassGlare());
        glassProgram.findUniform("ChromaticStrength").set(Interface.glassChromatic());
        glassProgram.findUniform("ScreenSize").set((float) mw.getScaledWidth(), (float) mw.getScaledHeight());

       int screenWidth = mw.getScaledWidth();
       int screenHeight = mw.getScaledHeight();
       float uTopLeft = topLeft.x / screenWidth;
       float vTopLeft = (screenHeight - topLeft.y) / screenHeight;
       float uBottomLeft = bottomLeft.x / screenWidth;
       float vBottomLeft = (screenHeight - bottomLeft.y) / screenHeight;
       float uBottomRight = bottomRight.x / screenWidth;
       float vBottomRight = (screenHeight - bottomRight.y) / screenHeight;
       float uTopRight = topRight.x / screenWidth;
       float vTopRight = (screenHeight - topRight.y) / screenHeight;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix, x, y, 0.0F).texture(uTopLeft, vTopLeft).color(color.getRGB());
      builder.vertex(matrix, x, y + height, 0.0F).texture(uBottomLeft, vBottomLeft).color(color.getRGB());
      builder.vertex(matrix, x + width, y + height, 0.0F).texture(uBottomRight, vBottomRight).color(color.getRGB());
      builder.vertex(matrix, x + width, y, 0.0F).texture(uTopRight, vTopRight).color(color.getRGB());
       BufferRenderer.drawWithGlobalProgram(builder.end());
       RenderSystem.setShaderTexture(0, 0);
       RenderSystem.enableCull();
       drawEnd();
   }

   public static void drawIslandMorph(MatrixStack matrices, float x, float y, float pillWidth, float pillHeight,
         float bubbleX, float globalAlpha, float dragAnim, ColorRGBA panelColor, float glassMix) {
      float width = Math.max(pillWidth, bubbleX + 7.5F) + 10.0F;
      float height = pillHeight + 10.0F;
      x -= 5.0F;
      y -= 5.0F;
      drawMorphPanel(matrices, x, y, width, height,
         new Vector4f(5 + pillWidth / 2 - width / 2, 0, pillWidth / 2, pillHeight / 2),
         new Vector4f(), 5 + bubbleX - width / 2,
         Math.max(0, 8 - 2 * Math.abs(bubbleX - 7.5F - pillWidth)),
         false, globalAlpha, dragAnim, panelColor, glassMix, dev.bkclient.ui.hud.HudVisualScope.strength(), dev.bkclient.ui.hud.HudVisualScope.progress());
   }

   public static void drawJoinedHudPanel(MatrixStack matrices, float x, float y, float width, float height,
         float headerOffset, float headerWidth, float headerHeight, float bodyTop,
         float alpha, float dragAnim, ColorRGBA color, float glassMix, float selection, float selectionProgress) {
      drawMorphPanel(matrices, x - 1, y - 1, width + 2, height + 2,
         new Vector4f(0, bodyTop / 2, width / 2, Math.max(.5F, (height - bodyTop) / 2)),
         new Vector4f(headerOffset + headerWidth / 2 - width / 2, (headerHeight - height) / 2,
            headerWidth / 2, headerHeight / 2), 0, 8, true, alpha, dragAnim, color, glassMix, selection, selectionProgress);
   }

   private static void drawMorphPanel(MatrixStack matrices, float x, float y, float width, float height,
         Vector4f pill, Vector4f header, float bubbleX, float merge, boolean joined,
         float globalAlpha, float dragAnim, ColorRGBA panelColor, float glassMix, float selection, float selectionProgress) {
      BorderRadius borderRadius = BorderRadius.all(Math.min(7.0F, pill.w));
      ColorRGBA color = ColorRGBA.WHITE;
      float fresnelPower = 26.0F * Interface.glass();
      ColorRGBA fresnelColor = ColorRGBA.WHITE;
      float baseAlpha = 1.0F;
      boolean fresnelInvert = true;
      float fresnelMix = 0.0F;
      float distortStrength = (dev.bkclient.ui.hud.HudTheme.isOld() || !Interface.showGlass() ? 0.0F : Interface.glassDistortion()) * Interface.glass() * (1.0F - 0.85F * dragAnim);
      float squirt = 2.0F;
       Matrix4f matrix = matrices.peek().getPositionMatrix();
       Vector4f topLeft = new Vector4f(x, y, 0.0F, 1.0F).mul(matrix);
       Vector4f bottomLeft = new Vector4f(x, y + height, 0.0F, 1.0F).mul(matrix);
       Vector4f bottomRight = new Vector4f(x + width, y + height, 0.0F, 1.0F).mul(matrix);
       Vector4f topRight = new Vector4f(x + width, y, 0.0F, 1.0F).mul(matrix);
       float renderedWidth = Math.max(0.001F, topLeft.distance(topRight));
       float renderedHeight = Math.max(0.001F, topLeft.distance(bottomLeft));
       float scaleX = renderedWidth / Math.max(0.001F, width);
       float scaleY = renderedHeight / Math.max(0.001F, height);
       float radiusScale = Math.min(scaleX, scaleY);
       drawSetup();
       RenderSystem.disableCull();
       RenderSystem.setShaderTexture(0, BlurProgram.getTexture());
       islandMorphProgram.use();
      islandMorphProgram.findUniform("GlobalAlpha").set(globalAlpha);
      islandMorphProgram.findUniform("Selection").set(selection);
      islandMorphProgram.findUniform("SelectionProgress").set(selectionProgress);
      islandMorphProgram.findUniform("Pill").set(pill.x * scaleX, pill.y * scaleY, pill.z * scaleX, pill.w * scaleY);
      islandMorphProgram.findUniform("Bubble").set(bubbleX * scaleX, 0.0F, 7.5F * radiusScale);
      islandMorphProgram.findUniform("Merge").set(merge * radiusScale);
      islandMorphProgram.findUniform("JoinedPanel").set(joined ? 1 : 0);
      islandMorphProgram.findUniform("JoinedHeader").set(header.x * scaleX, header.y * scaleY, header.z * scaleX, header.w * scaleY);
      islandMorphProgram.findUniform("JoinedBounds").set((width / 2 - 1) * scaleX, (height / 2 - 1) * scaleY);
      ColorRGBA accent = Colors.getAccent();
      islandMorphProgram.findUniform("AccentColor").set(accent.getRed() / 255F, accent.getGreen() / 255F,
         accent.getBlue() / 255F, joined && Interface.hudAccentGlow() ? .2F : 0F);
      islandMorphProgram.findUniform("PanelColor").set(panelColor.getRed() / 255.0F, panelColor.getGreen() / 255.0F, panelColor.getBlue() / 255.0F, panelColor.getAlpha() / 255.0F);
      islandMorphProgram.findUniform("GlassMix").set(glassMix);
      // The vertices can be scaled/rotated by HUD animations. Feed the shader the
      // actual on-screen dimensions so its edge, glare and distortion stay attached
      // to the panel instead of rendering at the unscaled position and size.
      islandMorphProgram.findUniform("Size").set(renderedWidth, renderedHeight);
      islandMorphProgram.findUniform("Radius")
         .set(
            borderRadius.topLeftRadius() * radiusScale,
            borderRadius.bottomLeftRadius() * radiusScale,
            borderRadius.topRightRadius() * radiusScale,
            borderRadius.bottomRightRadius() * radiusScale
         );
      islandMorphProgram.findUniform("Smoothness").set(0.5F);
      islandMorphProgram.findUniform("FresnelPower").set(fresnelPower);
      islandMorphProgram.findUniform("FresnelColor").set(ColorUtility.getRGBf(fresnelColor.getRGB()));
      islandMorphProgram.findUniform("FresnelAlpha").set(ColorUtility.alphaf(fresnelColor.getRGB()));
      islandMorphProgram.findUniform("BaseAlpha").set(baseAlpha);
      islandMorphProgram.findUniform("FresnelInvert").set(fresnelInvert ? 1 : 0);
      islandMorphProgram.findUniform("FresnelMix").set(fresnelMix);
        islandMorphProgram.findUniform("DistortStrength").set(distortStrength);
       islandMorphProgram.findUniform("CornerSmoothness").set(squirt);
        islandMorphProgram.findUniform("GlareStrength").set(Interface.showGlass() && !dev.bkclient.ui.hud.HudTheme.isOld() ? Interface.glassGlare() : 0.0F);
        islandMorphProgram.findUniform("ChromaticStrength").set(Interface.glassChromatic());
        islandMorphProgram.findUniform("ScreenSize").set((float) mw.getScaledWidth(), (float) mw.getScaledHeight());

       int screenWidth = mw.getScaledWidth();
       int screenHeight = mw.getScaledHeight();
       float uTopLeft = topLeft.x / screenWidth;
       float vTopLeft = (screenHeight - topLeft.y) / screenHeight;
       float uBottomLeft = bottomLeft.x / screenWidth;
       float vBottomLeft = (screenHeight - bottomLeft.y) / screenHeight;
       float uBottomRight = bottomRight.x / screenWidth;
       float vBottomRight = (screenHeight - bottomRight.y) / screenHeight;
       float uTopRight = topRight.x / screenWidth;
       float vTopRight = (screenHeight - topRight.y) / screenHeight;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix, x, y, 0.0F).texture(uTopLeft, vTopLeft).color(color.getRGB());
      builder.vertex(matrix, x, y + height, 0.0F).texture(uBottomLeft, vBottomLeft).color(color.getRGB());
      builder.vertex(matrix, x + width, y + height, 0.0F).texture(uBottomRight, vBottomRight).color(color.getRGB());
      builder.vertex(matrix, x + width, y, 0.0F).texture(uTopRight, vTopRight).color(color.getRGB());
       BufferRenderer.drawWithGlobalProgram(builder.end());
       RenderSystem.setShaderTexture(0, 0);
       RenderSystem.enableCull();
       drawEnd();
   }

   /** Rounded surface with one physical pixel of coverage antialiasing, independent of GUI scale. */
   public static void drawRoundedRectScreenAA(MatrixStack matrices, float x, float y, float width, float height, BorderRadius radius, ColorRGBA color) {
      if (Batching.getActive() instanceof dev.bkclient.utility.render.batching.impl.ScreenRectBatching batch) {
         batch.add(matrices.peek().getPositionMatrix(), x, y, width, height, radius, color.getRGB());
         return;
      }
      float padding = 2.0F;
      rectangleProgram.use();
      rectangleProgram.findUniform("Size").set(width + padding * 2, height + padding * 2);
      rectangleProgram.findUniform("Radius").set(radius.topLeftRadius(), radius.bottomLeftRadius(), radius.topRightRadius(), radius.bottomRightRadius());
      // Negative Smoothness selects screen-space coverage; padding leaves room for the outer half of the edge.
      rectangleProgram.findUniform("Smoothness").set(-padding);
      drawSetup();
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix, x - padding, y - padding, 0).color(color.getRGB());
      builder.vertex(matrix, x - padding, y + height + padding, 0).color(color.getRGB());
      builder.vertex(matrix, x + width + padding, y + height + padding, 0).color(color.getRGB());
      builder.vertex(matrix, x + width + padding, y - padding, 0).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f m = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (Batching.getActive() instanceof RoundedRectBatching rb) {
         rb.add(
            m,
            x,
            y,
            width,
            height,
            borderRadius.topLeftRadius(),
            borderRadius.bottomLeftRadius(),
            borderRadius.topRightRadius(),
            borderRadius.bottomRightRadius(),
            color.getRGB()
         );
         matrices.pop();
      } else {
         rectangleProgram.use();
         rectangleProgram.findUniform("Size").set(width, height);
         rectangleProgram.findUniform("Radius")
            .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         rectangleProgram.findUniform("Smoothness").set(smoothness);
         drawSetup();
         if ((color.getRGB() >>> 24) == 0) {
            // Fully transparent: the fragment shader discards every pixel, so only the GL state changes above
            // (program, uniforms, blend) are observable. Keep those and skip building and submitting the quad.
            drawEnd();
            matrices.pop();
            return;
         }
         float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
         float verticalPadding = smoothness / 2.0F + smoothness;
         float ax = x - horizontalPadding / 2.0F;
         float ay = y - verticalPadding / 2.0F;
         float aw = width + horizontalPadding;
         float ah = height + verticalPadding;
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(m, ax, ay, 0.0F).color(color.getRGB());
         builder.vertex(m, ax, ay + ah, 0.0F).color(color.getRGB());
         builder.vertex(m, ax + aw, ay + ah, 0.0F).color(color.getRGB());
         builder.vertex(m, ax + aw, ay, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
         matrices.pop();
      }
   }

   public static void drawRoundedRect(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color1,
      ColorRGBA color2,
      ColorRGBA color3,
      ColorRGBA color4
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      gradientRectangleProgram.use();
      gradientRectangleProgram.findUniform("Size").set(width, height);
      gradientRectangleProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      gradientRectangleProgram.findUniform("Smoothness").set(smoothness);
      gradientRectangleProgram.findUniform("TopLeftColor")
         .set(color1.getRed() / 255.0F, color1.getGreen() / 255.0F, color1.getBlue() / 255.0F, color1.getAlpha() / 255.0F);
      gradientRectangleProgram.findUniform("BottomLeftColor")
         .set(color2.getRed() / 255.0F, color2.getGreen() / 255.0F, color2.getBlue() / 255.0F, color2.getAlpha() / 255.0F);
      gradientRectangleProgram.findUniform("BottomRightColor")
         .set(color3.getRed() / 255.0F, color3.getGreen() / 255.0F, color3.getBlue() / 255.0F, color3.getAlpha() / 255.0F);
      gradientRectangleProgram.findUniform("TopRightColor")
         .set(color4.getRed() / 255.0F, color4.getGreen() / 255.0F, color4.getBlue() / 255.0F, color4.getAlpha() / 255.0F);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color1.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color2.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color3.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color4.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      drawRoundedRect(
         matrices,
         x,
         y,
         width,
         height,
         borderRadius,
         gradient.getTopLeftColor(),
         gradient.getBottomLeftColor(),
         gradient.getBottomRightColor(),
         gradient.getTopRightColor()
      );
   }

   public static void drawRoundedBorder(
      MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float internalSmoothness = 0.5F;
      float externalSmoothness = 1.0F;
      borderProgram.use();
      borderProgram.findUniform("Size").set(width, height);
      borderProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      borderProgram.findUniform("Smoothness").set(internalSmoothness, externalSmoothness);
      borderProgram.findUniform("Thickness").set(borderThickness);
      drawSetup();
      float horizontalPadding = -externalSmoothness / 2.0F + externalSmoothness * 2.0F;
      float verticalPadding = externalSmoothness / 2.0F + externalSmoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(borderColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

    public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
       if (Batching.getActive() instanceof IconBatching batching) {
          BufferBuilder builder = batching.getBuilder();
          Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
          RenderSystem.setShaderTexture(0, identifier);
          builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
       } else {
          matrices.push();
          Matrix4f matrix4f = matrices.peek().getPositionMatrix();
          RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
          RenderSystem.setShaderTexture(0, identifier);
          drawSetup();
          BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
          builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
          BufferRenderer.drawWithGlobalProgram(builder.end());
          drawEnd();
          RenderSystem.setShaderTexture(0, 0);
          matrices.pop();
       }
    }

    /** Trilinear minification for standalone UI artwork displayed much smaller than its source. */
    public static void drawTextureMinified(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, ColorRGBA color) {
       var texture = mc.getTextureManager().getTexture(identifier);
       if (texture != null) {
          int previousBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
          RenderSystem.bindTexture(texture.getGlId());
          try {
             // Resource reloads reset this parameter, so their new image gets fresh mip levels too.
             if (GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER) != GL11.GL_LINEAR_MIPMAP_LINEAR) {
                int sourceWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
                int sourceHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
                int levels = 31 - Integer.numberOfLeadingZeros(Math.max(1, Math.max(sourceWidth, sourceHeight)));
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL, levels);
                org.lwjgl.opengl.GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
             }
             LINEAR_TEXTURES.add(identifier);
          } finally {
             RenderSystem.bindTexture(previousBinding);
          }
       }
       drawTextureSmooth(matrices, identifier, x, y, width, height, color);
    }

    public static void drawTextureSmooth(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
       Identifier standalone = identifier;
       if (!LINEAR_TEXTURES.contains(identifier)) {
          var texture = mc.getTextureManager().getTexture(identifier);
          if (texture != null) {
             texture.setFilter(true, false);
             LINEAR_TEXTURES.add(identifier);
          }
       }
       if (Batching.getActive() instanceof IconBatching batching) {
          BufferBuilder builder = batching.getBuilder();
          Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
          RenderSystem.setShaderTexture(0, standalone);
          builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
       } else {
          matrices.push();
          Matrix4f matrix4f = matrices.peek().getPositionMatrix();
          RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
          RenderSystem.setShaderTexture(0, standalone);
          drawSetup();
          BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
          builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
          builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
          BufferRenderer.drawWithGlobalProgram(builder.end());
          drawEnd();
          RenderSystem.setShaderTexture(0, 0);
          matrices.pop();
       }
    }

   public static void drawTexture(
      MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA clor
   ) {
      if (Batching.getActive() instanceof IconBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, identifier);
         int color = clor.getRGB();
         float x2 = x + width;
         float y2 = y + height;
         builder.vertex(matrix4f, x, y, 0.0F).texture(u1, v1).color(color);
         builder.vertex(matrix4f, x, y2, 0.0F).texture(u1, v2).color(color);
         builder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
         builder.vertex(matrix4f, x2, y, 0.0F).texture(u2, v1).color(color);
      } else {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         matrices.push();
         int color = clor.getRGB();
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         float x2 = x + width;
         float y2 = y + height;
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, identifier);
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, x, y, 0.0F).texture(u1, v1).color(color);
         builder.vertex(matrix4f, x, y2, 0.0F).texture(u1, v2).color(color);
         builder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
         builder.vertex(matrix4f, x2, y, 0.0F).texture(u2, v1).color(color);
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
         RenderSystem.setShaderTexture(0, 0);
         matrices.pop();
         RenderSystem.disableBlend();
      }
   }

   public static void drawAnimationSprite(MatrixStack matrices, SpriteFrame sprite, float x, float y, float width, float height, ColorRGBA color) {
      if (sprite != null) {
         drawTexture(matrices, sprite.texture(), x, y, width, height, sprite.u1(), sprite.u2(), sprite.v1(), sprite.v2(), color);
      }
   }

   public static void drawSprite(MatrixStack matrices, CustomSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      drawTexture(
         matrices,
         BKClientInit.id(sprite.getTexture().getTexture()),
         x,
         y,
         width,
         height,
         sprite.x / sprite.getTexture().getWidth(),
         (sprite.x + sprite.getTexture().getStep()) / sprite.getTexture().getWidth(),
         0.0F,
         1.0F,
         color
      );
   }

   public static void drawRoundedTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
      drawRoundedTexture(matrices, identifier, x, y, width, height, borderRadius, Colors.WHITE);
   }

   private static final Set<Identifier> LINEAR_TEXTURES = new HashSet<>();

   public static void releaseTexture(Identifier identifier) {
      LINEAR_TEXTURES.remove(identifier);
      mc.getTextureManager().destroyTexture(identifier);
   }

   public static void drawRoundedTexture(
      MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      roundedTextureProgram.use();
      RenderSystem.setShaderTexture(0, identifier);
      var texture = mc.getTextureManager().getTexture(identifier);
      if (texture != null && !LINEAR_TEXTURES.contains(identifier)) {
         texture.setFilter(true, false);
         LINEAR_TEXTURES.add(identifier);
      }
      roundedTextureProgram.findUniform("Size").set(width, height);
      roundedTextureProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      roundedTextureProgram.findUniform("Smoothness").set(smoothness);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(0.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(1.0F, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawShadow(MatrixStack matrices, float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      if (Batching.getActive() instanceof IconBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         float horizontalPadding = softness * 3.0F;
         float verticalPadding = softness * 3.0F;
         float adjustedX = x - horizontalPadding / 2.0F;
         float adjustedY = y - verticalPadding / 2.0F;
         float adjustedWidth = width + horizontalPadding;
         float adjustedHeight = height + verticalPadding;
         builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      } else {
         rectangleProgram.use();
         rectangleProgram.findUniform("Size").set(width, height);
         rectangleProgram.findUniform("Radius")
            .set(
               borderRadius.topLeftRadius() * 3.0F,
               borderRadius.bottomLeftRadius() * 3.0F,
               borderRadius.topRightRadius() * 3.0F,
               borderRadius.bottomRightRadius() * 3.0F
            );
         rectangleProgram.findUniform("Smoothness").set(softness);
         drawSetup();
         float horizontalPadding = softness * 3.0F;
         float verticalPadding = softness * 3.0F;
         float adjustedX = x - horizontalPadding / 2.0F;
         float adjustedY = y - verticalPadding / 2.0F;
         float adjustedWidth = width + horizontalPadding;
         float adjustedHeight = height + verticalPadding;
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
      }
      matrices.pop();
   }

   public static void drawBlur(
      MatrixStack matrices, float x, float y, float width, float height, float blurRadius, float squirt, BorderRadius borderRadius, ColorRGBA color
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.03F;
      blurRadius /= 22.5F;
      if (!(blurRadius <= 0.0F)) {
         blurProgram.setBlurOffset(2.0F);
         squircleTextureProgram.use();
         RenderSystem.setShaderTexture(0, BlurProgram.getTexture());
         squircleTextureProgram.findUniform("Size").set(width, height);
         squircleTextureProgram.findUniform("Radius")
            .set(
               borderRadius.topLeftRadius() * squirt / 2.0F,
               borderRadius.bottomLeftRadius() * squirt / 2.0F,
               borderRadius.topRightRadius() * squirt / 2.0F,
               borderRadius.bottomRightRadius() * squirt / 2.0F
            );
         squircleTextureProgram.findUniform("Smoothness").set(0.1F);
         squircleTextureProgram.findUniform("CornerSmoothness").set(squirt);
         drawSetup();
         float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
         float verticalPadding = smoothness / 2.0F + smoothness;
         float adjustedX = x - horizontalPadding / 2.0F;
         float adjustedY = y - verticalPadding / 2.0F;
         float adjustedWidth = width + horizontalPadding;
         float adjustedHeight = height + verticalPadding;
         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         float u = adjustedX / screenWidth;
         float v = (screenHeight - adjustedY - adjustedHeight) / screenHeight;
         float texWidth = adjustedWidth / screenWidth;
         float texHeight = adjustedHeight / screenHeight;
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(u, v + texHeight).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(u, v).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(u + texWidth, v).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(u + texWidth, v + texHeight).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
         RenderSystem.setShaderTexture(0, 0);
      }
      matrices.pop();
   }

   public static void drawBlur(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      blurRadius /= 22.5F;
      if (!(blurRadius <= 0.0F)) {
         blurProgram.setBlurOffset(2.0F);
         roundedTextureProgram.use();
         RenderSystem.setShaderTexture(0, BlurProgram.getTexture());
         roundedTextureProgram.findUniform("Size").set(width, height);
         roundedTextureProgram.findUniform("Radius")
            .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         roundedTextureProgram.findUniform("Smoothness").set(0.01F);
         drawSetup();
         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         float u = x / screenWidth;
         float v = (screenHeight - y - height) / screenHeight;
         float texWidth = width / screenWidth;
         float texHeight = height / screenHeight;
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, x, y, 0.0F).texture(u, v + texHeight).color(color.getRGB());
         builder.vertex(matrix4f, x, y + height, 0.0F).texture(u, v).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(u + texWidth, v).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).texture(u + texWidth, v + texHeight).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
         RenderSystem.setShaderTexture(0, 0);
      }
      matrices.pop();
   }

   public static void drawImage(MatrixStack matrices, BufferBuilder builder, double x, double y, double z, double width, double height, ColorRGBA color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float)x, (float)(y + height), (float)z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)(y + height), (float)z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)y, (float)z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float)x, (float)y, (float)z).texture(0.0F, 0.0F).color(color.getRGB());
   }

   public static void drawImage(MatrixStack matrices, Identifier identifier, double x, double y, double z, double width, double height, ColorRGBA color) {
      RenderSystem.setShaderTexture(0, identifier);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float)x, (float)(y + height), (float)z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)(y + height), (float)z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)y, (float)z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float)x, (float)y, (float)z).texture(0.0F, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   public static void drawPlayerHeadWithHat(MatrixStack matrices, AbstractClientPlayerEntity player, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      Identifier skinTexture = player.getSkinTextures().texture();
      drawPlayerHeadWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
      drawPlayerHatLayerWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
   }

   public static <T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> void drawEntityHeadWithHat(
      MatrixStack matrices, T entity, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      EntityRenderer<? super T, ?> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
      if (renderer instanceof LivingEntityRenderer<?, ?, ?> renderer1) {
         LivingEntityRenderer<T, S, M> livingRenderer = (LivingEntityRenderer<T, S, M>)renderer;
         S state = (S)livingRenderer.createRenderState();
         Identifier skinTexture = livingRenderer.getTexture(state);
         drawPlayerHeadWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
         drawPlayerHatLayerWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
      }
   }

   public static void drawPlayerHeadWithRoundedShader(
      MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.125F, 0.125F, 0.25F, 0.25F);
   }

   /** Preserve skin pixels when enlarging notification avatars; restore shared texture state. */
   public static void drawCrispPlayerHead(
      MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius radius, ColorRGBA color
   ) {
      var texture = mc.getTextureManager().getTexture(skinTexture);
      if (texture == null) return;
      int active = GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
      RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
      int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      RenderSystem.bindTexture(texture.getGlId());
      int min = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
      int mag = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
      try {
         GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
         GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
         drawPlayerHeadWithRoundedShader(matrices, skinTexture, x, y, size, radius, color);
         drawPlayerHatLayerWithRoundedShader(matrices, skinTexture, x, y, size, radius, color);
      } finally {
         RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
         RenderSystem.bindTexture(texture.getGlId());
         GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min);
         GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mag);
         RenderSystem.bindTexture(binding);
         RenderSystem.activeTexture(active);
      }
   }

   public static void drawPlayerHatLayerWithRoundedShader(
      MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.625F, 0.125F, 0.75F, 0.25F);
      RenderSystem.disableBlend();
   }

   public static void drawRoundedTextureWithUV(
      MatrixStack matrices,
      Identifier identifier,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color,
      float u1,
      float v1,
      float u2,
      float v2
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      roundedTextureProgram.use();
      RenderSystem.setShaderTexture(0, identifier);
      roundedTextureProgram.findUniform("Size").set(width, height);
      roundedTextureProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      roundedTextureProgram.findUniform("Smoothness").set(smoothness);
      drawSetup();
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(u1, v1).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(u1, v2).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(u2, v2).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(u2, v1).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawSetup() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static void drawEnd() {
      RenderSystem.disableBlend();
   }

   @Generated
   private DrawUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static GlProgram getSquircleProgram() {
      return squircleProgram;
   }

   record HeadUV(float u1, float v1, float uSize, float vSize) {
   }
}
