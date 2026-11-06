package dev.bkclient.ui.components;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.MsdfFont;
import dev.bkclient.framework.msdf.MsdfGlyph;
import dev.bkclient.utility.colors.ColorRGBA;

/** Centers isolated keys by their rendered glyph instead of the font advance. */
public final class BadgeText {
    private BadgeText() {}

    /** Centers a label using the glyph bounds and spacing actually used by MsdfRenderer. */
    public static void drawGlyphCentered(dev.bkclient.framework.base.CustomDrawContext context, Font font, String text, float centerX, float centerY, ColorRGBA color) {
        float left = Float.POSITIVE_INFINITY, right = Float.NEGATIVE_INFINITY;
        float top = Float.POSITIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;
        float size = font.getSize();
        for (int i = 0; i < text.length(); i++) {
            MsdfGlyph glyph = font.getFont().getGlyph(MsdfFont.normalizeCharacter(text.charAt(i)));
            if (glyph == null || glyph.getPlaneWidth() <= 0) continue;
            // Width measurement adds 0.25 per character; rendering adds 0.025 * size.
            float pen = font.width(text.substring(0, i + 1)) - (i + 1) * .25F
                + i * .025F * size - glyph.getWidth(size);
            left = Math.min(left, pen);
            right = Math.max(right, pen + glyph.getPlaneWidth() * size);
            top = Math.min(top, (0.7F - glyph.getTopPosition()) * size);
            bottom = Math.max(bottom, (0.7F - glyph.getTopPosition() + glyph.getPlaneHeight()) * size);
        }
        if (!Float.isFinite(left)) return;
        context.drawText(font, text, centerX + .75F - (left + right) / 2,
            centerY - (top + bottom) / 2, color);
    }

    public static void draw(UIContext context, Font font, String text, float centerX, float centerY, ColorRGBA color) {
        if (text.length() == 1) {
            MsdfGlyph glyph = font.getFont().getGlyph(MsdfFont.normalizeCharacter(text.charAt(0)));
            if (glyph != null && glyph.getPlaneWidth() > 0 && glyph.getPlaneHeight() > 0) {
                float size = font.getSize();
                // MsdfRenderer offsets x by -0.75 and the baseline by +0.7 * size.
                float x = centerX + 0.75F - glyph.getPlaneWidth() * size / 2;
                float y = centerY - size * (0.7F - glyph.getTopPosition() + glyph.getPlaneHeight() / 2);
                context.drawText(font, text, x, y, color);
                return;
            }
        }
        context.drawCenteredText(font, text, centerX, centerY - font.height() / 2, color);
    }
}
