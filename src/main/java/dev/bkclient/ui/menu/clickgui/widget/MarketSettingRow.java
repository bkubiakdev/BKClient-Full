package dev.bkclient.ui.menu.clickgui.widget;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;

/** Compact icon rows used only by AutoMarket's settings panel. */
public final class MarketSettingRow {
    private MarketSettingRow() {}
    public static void draw(UIContext context, String label, float x, float y, float w, float labelWidth, float alpha) {
        boolean hover = GuiUtility.isHovered(x, y, w, 17, context);
        var text = Colors.getTextColor();
        context.drawRoundedRect(x, y, w, 17, BorderRadius.all(4),
                (ClickGuiV2Theme.current().dark ? ColorRGBA.BLACK : ColorRGBA.WHITE).mulAlpha((hover ? .6F : .35F) * alpha));
        icon(label).draw(context, x + 4, y + 4, 9, text.mulAlpha(.7F * alpha));
        var font = Fonts.MEDIUM.getFont(6.5F);
        String shown = dev.bkclient.systems.localization.Localizator.translate(label);
        if (font.width(shown) > labelWidth) {
            while (!shown.isEmpty() && font.width(shown + "...") > labelWidth) shown = shown.substring(0, shown.length() - 1);
            shown += "...";
        }
        context.drawText(font, shown, x + 17, y + 5.5F, text.mulAlpha(alpha));
    }
    private static LucideIcons icon(String label) {
        String key = label.toLowerCase(java.util.Locale.ROOT);
        if (key.contains("sell") || key.contains("tax")) return LucideIcons.SELL;
        if (key.contains("price") || key.contains("buy")) return LucideIcons.BUY;
        if (key.contains("delay") || key.contains("rate") || key.contains("interval")) return LucideIcons.GAUGE;
        if (key.contains("slot")) return LucideIcons.COPY;
        if (key.contains("jump")) return LucideIcons.MOVE;
        if (key.contains("repair")) return LucideIcons.RULER;
        if (key.contains("reconnect")) return LucideIcons.WAVES;
        if (key.contains("login") || key.contains("password")) return LucideIcons.USER;
        if (key.contains("title") || key.contains("regex") || key.contains("keyword")) return LucideIcons.INFO;
        if (key.contains("confirm")) return LucideIcons.CHECK;
        if (key.contains("mine")) return LucideIcons.STAR;
        return LucideIcons.CART;
    }
}
