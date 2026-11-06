package dev.bkclient.ui.hud.old;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.gui.GuiUtility;
import java.util.List;

/** Compact token pill used by the original BK HUD appearance. */
public final class OldHudPill {
   public static final float HEIGHT = 16.0F;
   public static final ColorRGBA TEXT = ColorRGBA.WHITE.withAlpha(191.0F);
   public static final ColorRGBA SECONDARY = ColorRGBA.WHITE.withAlpha(64.0F);

   private static final float PADDING = 7.0F;
   private static final float GAP = 4.0F;
   private static final float FONT_SIZE = 6.0F;
   private static final float ICON_SIZE = 7.5F;

   private OldHudPill() {
   }

   public record Token(String text, char glyph, ColorRGBA color, boolean tight, float iconSize, float offsetX, float offsetY, boolean separator) {
      public static Token text(String text, ColorRGBA color, boolean tight) {
         return new Token(text, '\0', color, tight, 0.0F, 0.0F, 0.0F, false);
      }

      public static Token icon(char glyph, ColorRGBA color, float size, float offsetX, float offsetY) {
         return new Token("", glyph, color, false, size, offsetX, offsetY, false);
      }

      public static Token divider() {
         return new Token("", '\0', SECONDARY, false, 0.0F, 0.0F, 0.0F, true);
      }
   }

   public static float width(List<Token> tokens) {
      Font font = Fonts.MEDIUM.getFont(FONT_SIZE);
      float width = PADDING * 2.0F;
      for (int index = 0; index < tokens.size(); index++) {
         Token token = tokens.get(index);
         if (index > 0 && !token.tight()) width += GAP;
         width += tokenWidth(font, token);
      }
      return width;
   }

   public static void draw(UIContext context, float x, float y, float width, float alpha, List<Token> tokens) {
      HudTheme.drawPanel(context, x, y, width, HEIGHT, alpha, 0.0F, 6.0F);
      Font font = Fonts.MEDIUM.getFont(FONT_SIZE);
      float centerY = y + HEIGHT / 2.0F;
      float textY = y + GuiUtility.getMiddleOfBox(font.height(), HEIGHT);
      float cursor = x + PADDING;

      for (int index = 0; index < tokens.size(); index++) {
         Token token = tokens.get(index);
         if (index > 0 && !token.tight()) cursor += GAP;
         float tokenWidth = tokenWidth(font, token);
         if (token.separator()) {
            context.drawRoundedRect(cursor, centerY - 2.5F, 1.0F, 5.0F, BorderRadius.all(0.0F), token.color());
         } else if (token.glyph() != '\0') {
            float size = token.iconSize() <= 0.0F ? ICON_SIZE : token.iconSize();
            context.drawIcon(Fonts.ICONS, token.glyph(), cursor + token.offsetX(), centerY - size / 2.0F + token.offsetY(), size, token.color());
         } else {
            context.drawText(font, token.text(), cursor, textY, token.color());
         }
         cursor += tokenWidth;
      }
   }

   private static float tokenWidth(Font font, Token token) {
      if (token.separator()) return 1.0F;
      if (token.glyph() != '\0') return token.iconSize() <= 0.0F ? ICON_SIZE : token.iconSize();
      return font.width(token.text());
   }
}
