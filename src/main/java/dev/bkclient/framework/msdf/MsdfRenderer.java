package dev.bkclient.framework.msdf;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import lombok.Generated;
import dev.bkclient.systems.modules.modules.other.NameProtect;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.batching.Batching;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public final class MsdfRenderer {
   /** Text drawn immediately while a non-matching batch is open (that batch owns the shared tessellator). */
   private static final net.minecraft.client.util.BufferAllocator SIDE_ALLOCATOR = new net.minecraft.client.util.BufferAllocator(1 << 16);
   public static final ShaderProgramKey MSDF_FONT_SHADER_KEY = new ShaderProgramKey(
      ResourceProvider.getShaderIdentifier("msdf_font/data"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY
   );
   public static final ShaderProgramKey MSDF_LIQUID_FONT_SHADER_KEY = new ShaderProgramKey(
      ResourceProvider.getShaderIdentifier("msdf_liquid_font/data"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY
   );

   public static void renderText(MsdfFont font, String text, float size, int color, Matrix4f matrix, float x, float y, float z) {
      renderText(font, text, size, color, matrix, x, y, z, false, 0.0F, 1.0F, 0.0F);
   }

   public static void renderText(
      MsdfFont font,
      String text,
      float size,
      int color,
      Matrix4f matrix,
      float x,
      float y,
      float z,
      boolean enableFadeout,
      float fadeoutStart,
      float fadeoutEnd,
      float maxWidth
   ) {
      text = text.replace("і", "i").replace("І", "I");
      float thickness = 0.05F;
      float smoothness = 0.5F;
      float spacing = 0.0F;
      NameProtect nameProtectModule = BKClientInit.getInstance().getModuleManager().getModule(NameProtect.class);
      if (nameProtectModule.isEnabled()) {
         text = nameProtectModule.patchName(text);
      }

      // A batch only takes text it can draw correctly: a FontBatching of the same atlas, without
      // fadeout (the batch draws with fadeout off). Anything else draws now; while another batch is
      // open it must not use the shared tessellator, which that batch is still writing into.
      Batching active = Batching.getActive();
      boolean batchable = !enableFadeout && active instanceof dev.bkclient.utility.render.batching.impl.FontBatching fontBatch
         && fontBatch.getFont() == font;
      if (batchable) {
         font.applyGlyphs(matrix, active.getBuilder(), text, size, thickness * 0.5F * size, spacing, x - 0.75F, y + size * 0.7F, z, color);
      } else {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.setShaderTexture(0, font.getTextureId());
         ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
         shader.getUniform("Range").set(font.getAtlas().range());
         shader.getUniform("Thickness").set(thickness);
         shader.getUniform("Smoothness").set(smoothness);
         shader.getUniform("EnableFadeout").set(enableFadeout ? 1 : 0);
         shader.getUniform("FadeoutStart").set(fadeoutStart);
         shader.getUniform("FadeoutEnd").set(fadeoutEnd);
         shader.getUniform("MaxWidth").set(maxWidth);
         shader.getUniform("TextPosX").set(x);
         BufferBuilder builder = active == null
            ? Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
            : new BufferBuilder(SIDE_ALLOCATOR, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         font.applyGlyphs(matrix, builder, text, size, thickness * 0.5F * size, spacing, x - 0.75F, y + size * 0.7F, z, color);
         BuiltBuffer builtBuffer = builder.endNullable();
         if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
         }

         RenderSystem.setShaderTexture(0, 0);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   public static void renderText(
      MsdfFont font,
      String text,
      float size,
      int color,
      Matrix4f matrix,
      float x,
      float y,
      float z,
      boolean enableFadeout,
      float fadeoutStart,
      float fadeoutEnd
   ) {
      float maxWidth = font.getWidth(text, size) * 2.0F;
      renderText(font, text, size, color, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
   }

   public static void renderLiquidText(
      MsdfFont font,
      String text,
      float size,
      int backgroundTexture,
      dev.bkclient.utility.colors.ColorRGBA tint,
      float alpha,
      float distortStrength,
      Matrix4f matrix,
      float x,
      float y,
      float z
   ) {
      text = text.replace("і", "i").replace("І", "I");
      dev.bkclient.systems.modules.modules.other.NameProtect nameProtectModule = dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.other.NameProtect.class);
      if (nameProtectModule.isEnabled()) {
         text = nameProtectModule.patchName(text);
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderTexture(0, font.getTextureId());
      RenderSystem.setShaderTexture(1, backgroundTexture);
      ShaderProgram shader = RenderSystem.setShader(MSDF_LIQUID_FONT_SHADER_KEY);
      shader.getUniform("Range").set(font.getAtlas().range());
      shader.getUniform("Thickness").set(0.05F);
      shader.getUniform("Smoothness").set(0.5F);
      net.minecraft.client.gl.GlUniform screenSizeUniform = shader.getUniform("ScreenSize");
      if (screenSizeUniform != null) {
         screenSizeUniform.set(
            (float)net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledWidth(),
            (float)net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledHeight()
         );
      }
      shader.getUniform("DistortStrength").set(distortStrength);
      shader.getUniform("TintStrength").set(tint.getAlpha() / 255.0F);
      shader.getUniform("GlobalAlpha").set(alpha);

      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      font.applyGlyphs(matrix, builder, text, size, 0.05F * 0.5F * size, 0.0F, x - 0.75F, y + size * 0.7F, z, tint.withAlpha(255.0F).getRGB());
      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.setShaderTexture(1, 0);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void renderText(MsdfFont font, Text text, float size, Matrix4f matrix, float x, float y, float z) {
      renderText(font, text, size, matrix, x, y, z, false, 0.0F, 1.0F, 0.0F);
   }

   public static void renderText(
      MsdfFont font,
      Text text,
      float size,
      Matrix4f matrix,
      float x,
      float y,
      float z,
      boolean enableFadeout,
      float fadeoutStart,
      float fadeoutEnd,
      float maxWidth
   ) {
      float thickness = 0.05F;
      float smoothness = 0.5F;
      float spacing = 0.0F;
      List<FormattedTextProcessor.TextSegment> segments = FormattedTextProcessor.processText(text, Colors.WHITE.getRGB());
      float currentX = x;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderTexture(0, font.getTextureId());
      ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
      shader.getUniform("Range").set(font.getAtlas().range());
      shader.getUniform("Thickness").set(thickness);
      shader.getUniform("Smoothness").set(smoothness);
      shader.getUniform("EnableFadeout").set(enableFadeout ? 1 : 0);
      shader.getUniform("FadeoutStart").set(fadeoutStart);
      shader.getUniform("FadeoutEnd").set(fadeoutEnd);
      shader.getUniform("MaxWidth").set(maxWidth);
      shader.getUniform("TextPosX").set(x);
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (FormattedTextProcessor.TextSegment segment : segments) {
         font.applyGlyphs(matrix, builder, segment.text, size, thickness * 0.5F * size, spacing - 0.3F, currentX - 0.75F, y + size * 0.7F, z, segment.color);
         currentX += font.getWidth(segment.text, size);
      }

      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void renderText(
      MsdfFont font, Text text, float size, Matrix4f matrix, float x, float y, float z, boolean enableFadeout, float fadeoutStart, float fadeoutEnd
   ) {
      float maxWidth = font.getTextWidth(text, size) * 2.0F;
      renderText(font, text, size, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
   }

   @Generated
   private MsdfRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
