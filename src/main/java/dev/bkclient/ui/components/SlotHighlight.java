package dev.bkclient.ui.components;

import net.minecraft.client.gui.DrawContext;

/** Original BKClient 16px slot fill and pixel-rounded border, shared by MineHelper and AutoMarket. */
public final class SlotHighlight {
    private SlotHighlight() { }

    public static void draw(DrawContext context, int sx, int sy, int fillColor, int borderColor) {
        int r = 3;
        int w = 16;
        int h = 16;
        if ((fillColor >>> 24) != 0) {
            context.fill(sx + r, sy, sx + w - r, sy + h, fillColor);
            context.fill(sx, sy + r, sx + w, sy + h - r, fillColor);
            context.fill(sx + 1, sy + 1, sx + 3, sy + 2, fillColor);
            context.fill(sx + 1, sy + 2, sx + 2, sy + 3, fillColor);
            context.fill(sx + 13, sy + 1, sx + 15, sy + 2, fillColor);
            context.fill(sx + 14, sy + 2, sx + 15, sy + 3, fillColor);
            context.fill(sx + 1, sy + 14, sx + 3, sy + 15, fillColor);
            context.fill(sx + 1, sy + 13, sx + 2, sy + 14, fillColor);
            context.fill(sx + 13, sy + 14, sx + 15, sy + 15, fillColor);
            context.fill(sx + 14, sy + 13, sx + 15, sy + 14, fillColor);
        }

        context.fill(sx + r, sy, sx + w - r, sy + 1, borderColor);
        context.fill(sx + r, sy + h - 1, sx + w - r, sy + h, borderColor);
        context.fill(sx, sy + r, sx + 1, sy + h - r, borderColor);
        context.fill(sx + w - 1, sy + r, sx + w, sy + h - r, borderColor);
        context.fill(sx + 2, sy + 1, sx + 3, sy + 2, borderColor);
        context.fill(sx + 1, sy + 2, sx + 2, sy + 3, borderColor);
        context.fill(sx + 13, sy + 1, sx + 14, sy + 2, borderColor);
        context.fill(sx + 14, sy + 2, sx + 15, sy + 3, borderColor);
        context.fill(sx + 1, sy + 13, sx + 2, sy + 14, borderColor);
        context.fill(sx + 2, sy + 14, sx + 3, sy + 15, borderColor);
        context.fill(sx + 13, sy + 14, sx + 14, sy + 15, borderColor);
        context.fill(sx + 14, sy + 13, sx + 15, sy + 14, borderColor);
    }
}
