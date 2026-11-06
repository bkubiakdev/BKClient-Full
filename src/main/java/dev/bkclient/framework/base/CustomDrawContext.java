package dev.bkclient.framework.base;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.MsdfFont;
import dev.bkclient.framework.msdf.MsdfGlyph;
import dev.bkclient.framework.msdf.MsdfRenderer;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.gradient.Gradient;
import dev.bkclient.mixin.accessors.DrawContextAccessor;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.theme.Theme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.obj.CustomSprite;
import dev.bkclient.utility.render.obj.Rect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CustomDrawContext extends DrawContext implements IMinecraft {
   private final DrawContext originalContext;

   protected CustomDrawContext(DrawContext originalContext) {
      super(MinecraftClient.getInstance(), ((DrawContextAccessor)originalContext).getVertexConsumers());
      this.originalContext = originalContext;
   }

   public static CustomDrawContext of(DrawContext originalContext) {
      return new CustomDrawContext(originalContext);
   }

   protected DrawContext getOriginalContext() {
      return this.originalContext;
   }

   public void drawClientRect(float x, float y, float width, float height, float alpha, float dragAnim, float squircle) {
      alpha *= dev.bkclient.ui.hud.HudVisualScope.opacity();
      if (ClickGuiV2Theme.active()) {
         DrawUtility.drawRoundedRectScreenAA(this.getMatrices(), x, y, width, height,
            BorderRadius.all(7), ClickGuiV2Theme.current().surface.mulAlpha(alpha));
         dev.bkclient.ui.hud.HudVisualScope.panel(this,x,y,width,height,7,2,alpha);
         return;
      }
      float rounding = 7.0F;

      if (Interface.showMinimalizm()) {
         this.drawBlurredRect(
            x, y, width, height, 45.0F, squircle, BorderRadius.all(rounding),
            ColorRGBA.WHITE.withAlpha(255.0F * alpha * Interface.minimalizm())
         );
      }

      if (Interface.showGlass()) {
         this.drawLiquidGlass(
            x, y, width, height, 2.0F,
            Math.max(0.0F, Interface.glassDistortion() - Interface.glassDistortion() * 0.85F * dragAnim),
            BorderRadius.all(rounding),
            ColorRGBA.WHITE.withAlpha((int)(255.0F * alpha * Interface.glass()))
         );
         float glassBg = Math.max(0.0F, 0.8F - 0.3F * Interface.glass());
         this.drawRoundedRect(x, y, width, height, BorderRadius.all(rounding),
            Colors.getBackgroundColor().withAlpha((int)(255.0F * glassBg * alpha))
         );
      } else {
         boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
         float bgAlpha = dark ? 0.85F : 0.7F;
         this.drawSquircle(x, y, width, height, squircle, BorderRadius.all(rounding), Colors.getBackgroundColor().withAlpha(255.0F * bgAlpha * alpha));
      }
      dev.bkclient.ui.hud.HudVisualScope.panel(this,x,y,width,height,rounding,Interface.showGlass()?2:squircle,alpha);
   }

   public void pushMatrix() {
      this.getMatrices().push();
   }

   public void popMatrix() {
      this.getMatrices().pop();
   }

   public void drawRect(float x, float y, float width, float height, ColorRGBA color) {
      DrawUtility.drawRect(this.getMatrices(), x, y, width, height, color);
   }

   public void drawLine(Vec2f from, Vec2f to, ColorRGBA color) {
      DrawUtility.drawLine(this.getMatrices(), from, to, color);
   }

   public void drawBezier(Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      DrawUtility.drawBezier(this.getMatrices(), p0, p1, p2, p3, color, resolution);
   }

   public void drawSquircle(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawSquircle(this.getMatrices(), x, y, width, height, squirt, borderRadius, color);
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, color);
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      DrawUtility.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, gradient);
   }

   public void drawLiquidGlass(float x, float y, float width, float height, float squirt, float power, BorderRadius borderRadius, ColorRGBA color) {
       if (ClickGuiV2Theme.active()) {
          this.drawRoundedRect(x, y, width, height, borderRadius,
             ClickGuiV2Theme.current().surface.mix(color.withAlpha(255), 0.12F).withAlpha(color.getAlpha()));
          return;
       }
      borderRadius = new BorderRadius(
         borderRadius.topLeftRadius() * squirt / 2.0F,
         borderRadius.topRightRadius() * squirt / 2.0F,
         borderRadius.bottomLeftRadius() * squirt / 2.0F,
         borderRadius.bottomRightRadius() * squirt / 2.0F
      );
      float distortion = Math.abs(power - 0.08F) < 1.0E-4F ? Interface.glassDistortion() : power;
      DrawUtility.drawLiquidGlass(
         this.getMatrices(),
         x - 5.0F * Interface.minimalizm(),
         y - 5.0F * Interface.minimalizm(),
         width + 10.0F * Interface.minimalizm(),
         height + 10.0F * Interface.minimalizm(),
         borderRadius,
         color,
         color.getAlpha() / 255.0F * Interface.glass(),
         (25.0F + (height == 240.0F ? 2.0F : 1.0F)) * Interface.glass(),
         color.withAlpha(255.0F),
         1.0F,
         true,
         0.0F,
         distortion * Interface.glass(),
         squirt,
         false
      );
   }

      public void drawLiquidGlass(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color, boolean clean) {
       if (ClickGuiV2Theme.active()) {
          this.drawRoundedRect(x, y, width, height, borderRadius,
             ClickGuiV2Theme.current().surface.mix(color.withAlpha(255), 0.12F).withAlpha(color.getAlpha()));
          return;
       }
       borderRadius = new BorderRadius(
          borderRadius.topLeftRadius() * squirt / 2.0F,
          borderRadius.topRightRadius() * squirt / 2.0F,
          borderRadius.bottomLeftRadius() * squirt / 2.0F,
          borderRadius.bottomRightRadius() * squirt / 2.0F
       );
       DrawUtility.drawLiquidGlass(
          this.getMatrices(),
          x - 5.0F * Interface.minimalizm(),
          y - 5.0F * Interface.minimalizm(),
          width + 10.0F * Interface.minimalizm(),
          height + 10.0F * Interface.minimalizm(),
          borderRadius,
          color,
          color.getAlpha() / 255.0F * Interface.glass(),
          25.0F + (height == 240.0F ? 2.0F : 1.0F),
          color.withAlpha(255.0F),
          1.0F,
          true,
          0.0F,
          Interface.glassDistortion() * Interface.glass(),
          squirt,
          clean
       );
    }

   public void drawLoadingRect(float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawLoadingRect(this.getMatrices(), x, y, width, height, progress, borderRadius, color);
   }

   public void drawRoundedBorder(float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor) {
      DrawUtility.drawRoundedBorder(this.getMatrices(), x, y, width, height, borderThickness, borderRadius, borderColor);
   }

   public void drawTexture(Identifier identifier, Rect rect) {
      this.drawTexture(identifier, rect, ColorRGBA.WHITE);
   }

   public void drawTexture(Identifier identifier, Rect rect, ColorRGBA color) {
      this.drawTexture(identifier, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), color);
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height) {
      this.drawTexture(identifier, x, y, width, height, ColorRGBA.WHITE);
   }

   /** Draws the complete front-facing 16x32 player model from a 64x64 skin. */
   public void drawSkin2D(Identifier skin, float x, float y, float unit, ColorRGBA color) {
      final float textureSize = 64.0F;
      float[][] parts = {
         {4, 0, 8, 8, 8, 8},
         {4, 8, 8, 12, 20, 20},
         {0, 8, 4, 12, 44, 20},
         {12, 8, 4, 12, 36, 52},
         {4, 20, 4, 12, 4, 20},
         {8, 20, 4, 12, 20, 52},
         {4, 0, 8, 8, 40, 8},
         {4, 8, 8, 12, 20, 36},
         {0, 8, 4, 12, 44, 36},
         {12, 8, 4, 12, 52, 52},
         {4, 20, 4, 12, 4, 36},
         {8, 20, 4, 12, 4, 52}
      };

      com.mojang.blaze3d.systems.RenderSystem.enableBlend();
      for (float[] part : parts) {
         float destinationX = x + part[0] * unit;
         float destinationY = y + part[1] * unit;
         float width = part[2] * unit;
         float height = part[3] * unit;
         float u1 = part[4] / textureSize;
         float v1 = part[5] / textureSize;
         float u2 = (part[4] + part[2]) / textureSize;
         float v2 = (part[5] + part[3]) / textureSize;
         this.drawTexture(skin, destinationX, destinationY, width, height, u1, u2, v1, v2, color);
      }
   }

    public void drawTexture(Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA color) {
       DrawUtility.drawTexture(this.getMatrices(), identifier, x, y, width, height, u1, u2, v1, v2, color);
    }

    public void drawTextureSmooth(Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
       if (dev.bkclient.ui.hud.HudIcons.draw(this, identifier, x, y, width, height, textureColor)) return;
       DrawUtility.drawTextureSmooth(this.getMatrices(), identifier, x, y, width, height, textureColor);
    }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
      if (dev.bkclient.ui.hud.HudIcons.draw(this, identifier, x, y, width, height, textureColor)) return;
      DrawUtility.drawTexture(this.getMatrices(), identifier, x, y, width, height, textureColor);
   }

   public void drawSprite(CustomSprite sprite, float x, float y, float width, float height, ColorRGBA textureColor) {
      DrawUtility.drawSprite(this.getMatrices(), sprite, x, y, width, height, textureColor);
   }

   public void drawRoundedTexture(Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
      DrawUtility.drawRoundedTexture(this.getMatrices(), identifier, x, y, width, height, borderRadius);
   }

   public void drawRoundedTexture(Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawRoundedTexture(this.getMatrices(), identifier, x, y, width, height, borderRadius, color);
   }

   public void drawShadow(float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawShadow(this.getMatrices(), x, y, width, height, softness, borderRadius, color);
   }

   public void drawBlurredRect(float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      if (ClickGuiV2Theme.active()) {
         this.drawRoundedRect(x, y, width, height, borderRadius, ClickGuiV2Theme.current().surface.withAlpha(color.getAlpha()));
         return;
      }
      DrawUtility.drawBlur(this.getMatrices(), x, y, width, height, blurRadius, borderRadius, color);
   }

   public void drawBlurredRect(float x, float y, float width, float height, float blurRadius, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      if (ClickGuiV2Theme.active()) {
         this.drawRoundedRect(x, y, width, height, borderRadius, ClickGuiV2Theme.current().surface.withAlpha(color.getAlpha()));
         return;
      }
      DrawUtility.drawBlur(this.getMatrices(), x, y, width, height, blurRadius, squirt, borderRadius, color);
   }

   public void drawIcon(MsdfFont iconFont, char glyph, float x, float y, float boxSize, ColorRGBA color) {
      MsdfGlyph icon = iconFont.getGlyph(glyph);
      if (icon == null) {
         return;
      }

      float plane = Math.max(icon.getPlaneWidth(), icon.getPlaneHeight());
      if (plane <= 0.0F) {
         return;
      }

      float size = boxSize / plane;
      float glyphWidth = icon.getPlaneWidth() * size;
      float glyphHeight = icon.getPlaneHeight() * size;
      float drawX = x + (boxSize - glyphWidth) / 2.0F + 0.75F;
      float baselineY = y + (boxSize - glyphHeight) / 2.0F + icon.getTopPosition() * size - size * 0.7F;
      MsdfRenderer.renderText(
         iconFont,
         String.valueOf(glyph),
         size,
         color.getRGB(),
         this.getMatrices().peek().getPositionMatrix(),
         drawX,
         baselineY,
         0.0F
      );
   }

   public void drawText(Font font, String text, float x, float y, ColorRGBA color) {
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), color.getRGB(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawText(Font font, Text text, float x, float y) {
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawFadeoutText(Font font, String text, float x, float y, ColorRGBA color, float fadeoutStart, float fadeoutEnd) {
      MsdfRenderer.renderText(
         font.getFont(), text, font.getSize(), color.getRGB(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F, true, fadeoutStart, fadeoutEnd
      );
   }

   public void drawFadeoutText(Font font, String text, float x, float y, ColorRGBA color, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      MsdfRenderer.renderText(
         font.getFont(),
         text,
         font.getSize(),
         color.getRGB(),
         this.getMatrices().peek().getPositionMatrix(),
         x,
         y,
         0.0F,
         true,
         fadeoutStart,
         fadeoutEnd,
         maxWidth
      );
   }

   public void drawCenteredText(Font font, String text, float x, float y, ColorRGBA color) {
      this.drawText(font, text, x - font.getFont().getWidth(text, font.getSize()) / 2.0F, y, color);
   }

   public void drawRightText(Font font, String text, float x, float y, ColorRGBA color) {
      this.drawText(font, text, x - font.getFont().getWidth(text, font.getSize()), y, color);
   }

   public void drawItem(Item item, float x, float y, float size) {
      this.drawItem(item.getDefaultStack(), x, y, size);
   }

   public void drawItem(ItemStack item, float x, float y, float size) {
      if (dev.bkclient.ui.hud.Hud.captureBlendForced()) {
         // Earlier HUD geometry still goes out with the cache's blending; the item draws with its own.
         this.draw();
         dev.bkclient.ui.hud.Hud.drawItemUnforced(() -> this.drawItemScaled(item, x, y, size));
         return;
      }
      this.drawItemScaled(item, x, y, size);
   }

   private void drawItemScaled(ItemStack item, float x, float y, float size) {
      this.getMatrices().push();
      this.getMatrices().translate(x, y, 0.0F);
      this.getMatrices().scale(size, size, size);
      DiffuseLighting.disableGuiDepthLighting();
      this.drawItem(item, 0, 0);
      DiffuseLighting.disableGuiDepthLighting();
      this.getMatrices().pop();
   }

   public void drawHead(AbstractClientPlayerEntity player, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawPlayerHeadWithHat(this.getMatrices(), player, x, y, size, borderRadius, color);
   }

   public void drawHead(LivingEntity entity, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawEntityHeadWithHat(this.getMatrices(), entity, x, y, size, borderRadius, color);
   }

   /** {@link #drawItem(ItemStack, float, float, float)} without the per-item flush; call {@link #flushBatchItems()} after. */
   public void drawBatchItem(ItemStack item, float x, float y, float size) {
      this.getMatrices().push();
      this.getMatrices().translate(x, y, 0.0F);
      this.getMatrices().scale(size, size, size);
      this.drawBatchItem(item, 0, 0);
      this.getMatrices().pop();
   }

   /** The consumers drawBatchItem writes into; flushed by {@link #flushBatchItems()}. */
   public Immediate batchVertexConsumers() {
      return ((DrawContextAccessor)this.originalContext).getVertexConsumers();
   }

   /** Draws every item queued with drawBatchItem in one flush, with the lighting drawItem uses. */
   public void flushBatchItems() {
      DiffuseLighting.disableGuiDepthLighting();
      ((DrawContextAccessor)this.originalContext).getVertexConsumers().draw();
      DiffuseLighting.disableGuiDepthLighting();
   }

   public void drawBatchItem(ItemStack item, int x, int y) {
      this.drawBatchItem(mc.player, mc.world, item, x, y, 0);
   }

   private void drawBatchItem(@Nullable LivingEntity entity, @Nullable World world, ItemStack stack, int x, int y, int seed) {
      this.drawBatchItem(entity, world, stack, x, y, seed, 0);
   }

   private void drawBatchItem(@Nullable LivingEntity entity, @Nullable World world, ItemStack stack, int x, int y, int seed, int z) {
      MatrixStack matrices = this.getMatrices();
      ItemRenderState itemRenderState = ((DrawContextAccessor)this.originalContext).getItemRenderState();
      Immediate vertexConsumers = ((DrawContextAccessor)this.originalContext).getVertexConsumers();
      if (!stack.isEmpty()) {
         mc.getItemModelManager().update(itemRenderState, stack, ModelTransformationMode.GUI, false, world, entity, seed);
         matrices.push();
         matrices.translate(x + 8, y + 8, 150 + (itemRenderState.hasDepth() ? z : 0));

         try {
            matrices.scale(16.0F, -16.0F, 16.0F);
            boolean bl = !itemRenderState.isSideLit();
            if (bl) {
               DiffuseLighting.disableGuiDepthLighting();
            }

            itemRenderState.render(matrices, vertexConsumers, 15728880, OverlayTexture.DEFAULT_UV);
            if (bl) {
               DiffuseLighting.enableGuiDepthLighting();
            }
         } catch (Throwable var15) {
            CrashReport crashReport = CrashReport.create(var15, "Rendering item");
            CrashReportSection crashReportSection = crashReport.addElement("Item being rendered");
            crashReportSection.add("Item Type", () -> String.valueOf(stack.getItem()));
            crashReportSection.add("Item Components", () -> String.valueOf(stack.getComponents()));
            crashReportSection.add("Item Foil", () -> String.valueOf(stack.hasGlint()));
            throw new CrashException(crashReport);
         }

         matrices.pop();
      }
   }
}
