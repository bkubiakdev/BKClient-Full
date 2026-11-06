package dev.bkclient.ui.menu.clickgui;

import dev.bkclient.utility.colors.ColorRGBA;

/** Render-scoped V2 palette. Closing the scope restores the HUD/other screens' palette. */
public final class ClickGuiTheme implements AutoCloseable {
    private static final ThreadLocal<ClickGuiTheme> ACTIVE = new ThreadLocal<>();
    private final ClickGuiTheme previous;
    public final boolean dark;
    public final boolean liquid;
    public final ColorRGBA window, surface, field, hover, text, muted, border, accent, selection;

    private ClickGuiTheme(boolean dark, ColorRGBA accent, boolean liquid) {
        this.previous = ACTIVE.get();
        this.dark = dark;
        this.liquid = liquid;
        this.window = liquid ? dev.bkclient.utility.colors.Colors.getBackgroundColor() : dark ? new ColorRGBA(20, 22, 26) : new ColorRGBA(240, 241, 242);
        this.surface = liquid ? dev.bkclient.utility.colors.Colors.getBackgroundColor() : dark ? new ColorRGBA(30, 33, 39) : ColorRGBA.WHITE;
        this.field = liquid ? dev.bkclient.utility.colors.Colors.getAdditionalColor() : dark ? new ColorRGBA(43, 47, 55) : new ColorRGBA(230, 233, 238);
        this.hover = liquid ? dev.bkclient.utility.colors.Colors.getAdditionalColor() : dark ? new ColorRGBA(38, 42, 49) : new ColorRGBA(247, 250, 252);
        this.text = liquid ? dev.bkclient.utility.colors.Colors.getTextColor() : dark ? new ColorRGBA(239, 241, 245) : ColorRGBA.BLACK;
        this.muted = liquid ? this.text.mulAlpha(0.62F) : dark ? new ColorRGBA(165, 172, 184) : new ColorRGBA(91, 98, 109);
        this.border = liquid ? dev.bkclient.utility.colors.Colors.getOutlineColor() : dark ? new ColorRGBA(61, 66, 77) : new ColorRGBA(212, 217, 224);
        this.accent = accent.withAlpha(255);
        this.selection = this.surface.mix(this.accent, dark ? 0.28F : 0.17F);
        ACTIVE.set(this);
    }

    public static ClickGuiTheme open(boolean dark, ColorRGBA accent) { return new ClickGuiTheme(dark, accent, false); }
    public static ClickGuiTheme openLiquid() {
        var theme = dev.bkclient.BKClientInit.getInstance().getThemeManager().getCurrentTheme();
        return new ClickGuiTheme(theme != null && theme.isDark(), dev.bkclient.utility.colors.Colors.getAccent(), true);
    }
    public static ClickGuiTheme current() { return ACTIVE.get(); }
    public static boolean active() { return current() != null && !current().liquid; }
    @Override public void close() {
        if (this.previous == null) ACTIVE.remove(); else ACTIVE.set(this.previous);
    }
}
