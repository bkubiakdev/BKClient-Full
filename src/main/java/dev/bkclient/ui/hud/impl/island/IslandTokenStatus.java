package dev.bkclient.ui.hud.impl.island;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.RingRenderer;
import java.util.ArrayList;
import java.util.List;

/** BK-style token row used by compact Dynamic Island statuses. */
public abstract class IslandTokenStatus extends IslandStatus {
   protected static final ColorRGBA WHITE_75 = ColorRGBA.WHITE.withAlpha(191.0F);

   private static final float FONT_SIZE = 6.0F;
   private static final float ICON_SIZE = 6.0F;
   private static final float DIVIDER_WIDTH = 1.0F;
   private static final float DIVIDER_HEIGHT = 5.0F;
   private static final float RING_RADIUS = 3.5F;
   private static final float RING_THICKNESS = 1.0F;
   private static final int TEXT = 0;
   private static final int ICON = 1;
   private static final int DIVIDER = 2;
   private static final int RING = 3;

   protected record Token(
      int type,
      String text,
      char glyph,
      ColorRGBA color,
      boolean tight,
      float sizeAdd,
      float offsetX,
      float offsetY,
      float progress
   ) {
      public static Token text(String text, ColorRGBA color, boolean tight) {
         return new Token(TEXT, text, '\0', color, tight, 0.0F, 0.0F, 0.0F, 0.0F);
      }

      public static Token icon(char glyph, ColorRGBA color) {
         return new Token(ICON, "", glyph, color, false, 4.0F, -4.0F, -0.25F, 0.0F);
      }

      public static Token divider() {
         return new Token(DIVIDER, "", '\0', ColorRGBA.WHITE, false, 0.0F, 0.0F, 0.0F, 0.0F);
      }

      public static Token ring(float progress) {
         return new Token(RING, "", '\0', Colors.getAccent(), false, 0.0F, 0.0F, 0.0F, progress);
      }
   }

   protected IslandTokenStatus(SelectSetting parent, String name) {
      super(parent, name);
   }

   protected abstract List<Token> buildTokens();

   @Override
   public void draw(CustomDrawContext context) {
      Font font = Fonts.MEDIUM.getFont(FONT_SIZE);
      List<Token> tokens = this.buildTokens();
      float padding = 8.0F;
      float gap = 5.0F;
      float width = padding * 2.0F;

      for (int index = 0; index < tokens.size(); index++) {
         Token token = tokens.get(index);
         if (index > 0 && !token.tight()) {
            width += gap;
         }
         width += tokenWidth(font, token);
      }

      this.size.width = width;
      this.size.height = 15.0F;
      DynamicDock dock = BKClientInit.getInstance().getHud().getIsland();
      float left = sr.getScaledWidth() / 2.0F - dock.getSize().width / 2.0F;
      float centerY = this.size.height / 2.0F;
      float alpha = this.animation.getValue();
      float cursor = left + padding;

      for (int index = 0; index < tokens.size(); index++) {
         Token token = tokens.get(index);
         if (index > 0 && !token.tight()) {
            cursor += gap;
         }

         float tokenWidth = tokenWidth(font, token);
         switch (token.type()) {
            case ICON -> {
               float iconSize = ICON_SIZE + token.sizeAdd();
               context.drawIcon(
                  Fonts.ICONS,
                  token.glyph(),
                  cursor + token.offsetX(),
                  centerY - iconSize / 2.0F + token.offsetY(),
                  iconSize,
                  token.color().withAlpha(token.color().getAlpha() * alpha)
               );
            }
            case DIVIDER -> context.drawRoundedRect(
               cursor,
               centerY - DIVIDER_HEIGHT / 2.0F,
               DIVIDER_WIDTH,
               DIVIDER_HEIGHT,
               BorderRadius.all(0.5F),
               ColorRGBA.WHITE.withAlpha(42.0F * alpha)
            );
            case RING -> RingRenderer.progress(
               context.getMatrices(),
               cursor + RING_RADIUS,
               centerY,
               RING_RADIUS,
               RING_THICKNESS,
               token.progress(),
               0.0F,
               Colors.getAccent().withAlpha(38.0F * alpha),
               Colors.getAccent().withAlpha(255.0F * alpha)
            );
            default -> context.drawText(
               font,
               token.text(),
               cursor,
               centerY - font.height() / 2.0F - 0.5F,
               token.color().withAlpha(token.color().getAlpha() * alpha)
            );
         }
         cursor += tokenWidth;
      }
   }

   protected final List<Token> tokens(Token... tokens) {
      List<Token> result = new ArrayList<>(tokens.length);
      for (Token token : tokens) {
         result.add(token);
      }
      return result;
   }

   private static float tokenWidth(Font font, Token token) {
      return switch (token.type()) {
         case ICON -> ICON_SIZE;
         case DIVIDER -> DIVIDER_WIDTH;
         case RING -> RING_RADIUS * 2.0F;
         default -> font.width(token.text());
      };
   }
}
