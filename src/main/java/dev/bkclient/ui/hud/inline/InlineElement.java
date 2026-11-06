package dev.bkclient.ui.hud.inline;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.HudElement;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.render.batching.Batching;
import dev.bkclient.utility.render.batching.impl.FontBatching;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class InlineElement extends HudElement {
   /**
    * Lucide icons use 48×48 source images; the display size keeps strokes aligned.
    */
   private static final float HUD_ICON_SIZE = 8.0F;
   private static final float HUD_ICON_Y = 5.0F;

   protected final SelectSetting elements = new SelectSetting(this, "elements", HudTheme::isOld).draggable().min(1);

   public InlineElement(String name, String icon) {
      super(name, icon);
      this.height = 18.0F;
   }

   @Override
   protected boolean repositionOnResize() {
      // Player and World constantly change width as FPS, speed, TPS and coordinates change.
      // Their saved top-left position must stay fixed instead of being treated as auto-layout.
      return false;
   }

   /**
    * Measures the row before the panel, scissor box and scale pivot use its width.
    */
   @Override
   public void update(UIContext context) {
      // The old theme draws a pill whose width its own element already worked out; measuring the
      // inline row on top of that would throw that away.
      if (!HudTheme.isOld()) {
         // HudElement animates by replacing the current height with the intermediate value.
         // Restore the real target every frame; otherwise the first near-zero animation frame
         // becomes the next target and Player/World collapse into a horizontal line.
         this.height = 18.0F;
         this.width = 15.0F + this.measureContent();
      }
      super.update(context);
   }

   /**
    * Measures selected values using the same layout as renderComponent.
    * Hover animation uses the value from the preceding frame.
    */
   private float measureContent() {
      float xOffset = 0.0F;
      for (SelectSetting.Value value : this.elements.getValues()) {
         InlineValue elmt = (InlineValue)value;
         if (!elmt.isSelected()) {
            continue;
         }
         float iconOffset = !elmt.texture().isEmpty() ? 11.0F : 0.0F;
         String[] segs = elmt.graySegments();
         float contentWidth;
         if (segs != null) {
            contentWidth = 0.0F;
            for (int i = 0; i < segs.length; i++) {
               contentWidth += Fonts.MEDIUM.getFont(i % 2 == 1 ? 7 : 8).width(segs[i]);
            }
         } else {
            contentWidth = Fonts.MEDIUM.getFont(8.0F).width(elmt.text())
               + 8.0F * elmt.copyAnim().getValue()
               + Fonts.MEDIUM.getFont(7.0F).width(elmt.suffix());
         }
         xOffset += iconOffset + contentWidth + 6.0F;
      }
      return xOffset;
   }

   @Override
   protected void renderComponent(UIContext context) {
      HudTheme.drawPanel(context, this.x, this.y, this.width, this.height, this.animation.getValue(), this.dragAnim.getValue(), 3.0F);
      ScissorUtility.push(context.getMatrices(), this.x, this.y, this.width, this.height);
      this.drawHudIcon(context, BKClientInit.id(this.icon), this.x + 5.0F, this.y + HUD_ICON_Y, HudTheme.textColor());
      float xOffset = 0.0F;
      Batching fontBatching = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, Fonts.MEDIUM);

      for (SelectSetting.Value value : this.elements.getValues()) {
         InlineValue elmt = (InlineValue)value;
         if (elmt.isSelected()) {
             float iconOffset = !elmt.texture().isEmpty() ? 11.0F : 0.0F;
            float textWidth = Fonts.MEDIUM.getFont(8.0F).width(elmt.text()) + 8.0F * elmt.copyAnim().getValue();
            boolean hover = GuiUtility.isHovered(this.x + 19.0F + xOffset + iconOffset, this.y + 6.0F, Fonts.MEDIUM.getFont(8.0F).width(elmt.text()), 8.0, context)
               && !elmt.copy().isEmpty();
            if (!hover || elmt.copyTimer().finished(1000L)) {
               elmt.copied(false);
            }

            elmt.copyAnim().update(hover);
            elmt.successAnim().update(elmt.copied());
            String[] segs = elmt.graySegments();
            float totalSegWidth;
            if (segs != null) {
               float segOffset = 0.0F;
               for (int i = 0; i < segs.length; i++) {
                  boolean isGray = i % 2 == 1;
                  int fontSize = isGray ? 7 : 8;
                  float y = isGray ? this.y + 7.0F : this.y + 6.0F;
                  context.drawText(
                     Fonts.MEDIUM.getFont(fontSize), segs[i],
                     this.x + 19.0F + xOffset + iconOffset + segOffset + 8.0F * elmt.copyAnim().getValue(),
                     y, isGray ? HudTheme.textColor().mulAlpha(0.5F) : HudTheme.textColor()
                  );
                  segOffset += Fonts.MEDIUM.getFont(fontSize).width(segs[i]);
               }
               totalSegWidth = segOffset;
            } else {
               context.drawText(
                  Fonts.MEDIUM.getFont(8.0F), elmt.text(), this.x + 19.0F + xOffset + iconOffset + 8.0F * elmt.copyAnim().getValue(), this.y + 6.0F, HudTheme.textColor()
               );
               if (!elmt.suffix().isEmpty()) {
                  context.drawText(
                     Fonts.MEDIUM.getFont(7.0F), elmt.suffix(), this.x + 19.0F + xOffset + iconOffset + textWidth, this.y + 7.0F, HudTheme.textColor().mulAlpha(0.5F)
                  );
               }
               totalSegWidth = textWidth + Fonts.MEDIUM.getFont(7.0F).width(elmt.suffix());
            }

            xOffset += iconOffset + totalSegWidth + 6.0F;
         }
      }

      fontBatching.draw();
      xOffset = 0.0F;

      for (SelectSetting.Value valuex : this.elements.getValues()) {
         InlineValue elmt = (InlineValue)valuex;
         if (elmt.isSelected()) {
             float iconOffset = !elmt.texture().isEmpty() ? 11.0F : 0.0F;
            if (iconOffset > 0.0F) {
               this.drawHudIcon(
                  context,
                  BKClientInit.id(elmt.texture()),
                  this.x + 19.0F + xOffset,
                  this.y + HUD_ICON_Y,
                  HudTheme.textColor()
               );
            }
            float textWidthx = Fonts.MEDIUM.getFont(8.0F).width(elmt.text()) + 8.0F * elmt.copyAnim().getValue();

            RenderUtility.rotate(context.getMatrices(), this.x + 19.0F + xOffset + iconOffset + 3.0F, this.y + 6.0F + 3.0F, 90.0F * elmt.successAnim().getValue());
            context.drawTexture(
               BKClientInit.id("icons/hud/copy.png"),
               this.x + 19.0F + xOffset + iconOffset,
               this.y + 6.0F,
               6.0F,
               6.0F,
               HudTheme.textColor().mulAlpha(elmt.copyAnim().getValue() * (1.0F - elmt.successAnim().getValue()))
            );
            RenderUtility.end(context.getMatrices());
            RenderUtility.rotate(context.getMatrices(), this.x + 19.0F + xOffset + iconOffset + 3.0F, this.y + 6.0F + 3.0F, -90.0F + 90.0F * elmt.successAnim().getValue());
            context.drawTexture(
               BKClientInit.id("icons/check.png"),
               this.x + 19.0F + xOffset + iconOffset,
               this.y + 6.0F,
               6.0F,
               6.0F,
               Colors.GREEN.mulAlpha(elmt.copyAnim().getValue() * elmt.successAnim().getValue())
            );
            RenderUtility.end(context.getMatrices());
            String[] segsx = elmt.graySegments();
            float totalWidth;
            if (segsx != null) {
               totalWidth = 0.0F;
               for (int i = 0; i < segsx.length; i++) {
                  totalWidth += Fonts.MEDIUM.getFont(i % 2 == 1 ? 7 : 8).width(segsx[i]);
               }
            } else {
               totalWidth = textWidthx + Fonts.MEDIUM.getFont(7.0F).width(elmt.suffix());
            }
            xOffset += iconOffset + totalWidth + 6.0F;
         }
      }

      ScissorUtility.pop();
   }

   /** Draw every inline HUD icon through the same filtered, centered 8x8 slot. */
   private void drawHudIcon(UIContext context, Identifier identifier, float x, float y, ColorRGBA color) {
      context.drawTextureSmooth(identifier, x, y, HUD_ICON_SIZE, HUD_ICON_SIZE, color);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      float xOffset = 0.0F;

      for (SelectSetting.Value value : this.elements.getValues()) {
         InlineValue elmt = (InlineValue)value;
         if (elmt.isSelected()) {
            float textWidth = Fonts.MEDIUM.getFont(8.0F).width(elmt.text());
            boolean hover = GuiUtility.isHovered(this.x + 19.0F + xOffset, this.y + 6.0F, Fonts.MEDIUM.getFont(8.0F).width(elmt.text()), 8.0, mouseX, mouseY)
               && !elmt.copy().isEmpty();
            if (hover && button == MouseButton.LEFT) {
               TextUtility.copyText(elmt.copy());
               elmt.copyTimer().reset();
               elmt.copied(true);
               return;
            }

            xOffset += textWidth + Fonts.MEDIUM.getFont(7.0F).width(elmt.suffix()) + 10.0F;
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }
}
