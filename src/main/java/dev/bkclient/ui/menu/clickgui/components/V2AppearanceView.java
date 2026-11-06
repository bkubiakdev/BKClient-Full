package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.menu.clickgui.ClickGuiBaseScreen;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Screen;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.gui.GuiUtility;

/** Appearance controls for V2 only; does not modify the HUD or liquid GUI theme. */
public final class V2AppearanceView {
    private final java.util.List<ColorPicker> colorPickers;
    private ColorPicker picker;
    private Rect light = Rect.EMPTY, dark = Rect.EMPTY, custom = Rect.EMPTY;
    private final Rect[] swatches = new Rect[6];
    private static final ColorRGBA[] ACCENTS = {
        new ColorRGBA(36,125,218), new ColorRGBA(119,92,211), new ColorRGBA(198,75,137),
        new ColorRGBA(211,113,42), new ColorRGBA(38,155,116), new ColorRGBA(40,145,166)
    };

    public V2AppearanceView(ClickGuiV2Screen host) { this.colorPickers = host.getColorPickers(); }
    public V2AppearanceView(ClickGuiBaseScreen host) { this.colorPickers = host.getColorPickers(); }
    private static MenuModule settings() { return BKClientInit.getInstance().getModuleManager().getModule(MenuModule.class); }

    public void updatePicker() {
        if (this.picker == null) return;
        settings().getV2Accent().setColor(this.picker.built().withAlpha(255));
        if (!this.picker.isShowing()) {
            AbstractSetting.saveConfig();
            this.picker = null;
        }
    }

    public void render(UIContext context, float x, float y, float width, float height, float alpha) {
        var palette = ClickGuiV2Theme.current();
        float gap = 10, column = (width - gap) / 2;
        context.drawClientRect(x, y, column, 158, alpha, 0, 7);
        context.drawClientRect(x + column + gap, y, column, 158, alpha, 0, 7);
        context.drawText(Fonts.MEDIUM.getFont(9), "Appearance", x + 12, y + 12, palette.text.mulAlpha(alpha));
        context.drawText(Fonts.REGULAR.getFont(6.5F), "Choose your menu theme", x + 12, y + 28, palette.muted.mulAlpha(alpha));
        float previewWidth = (column - 34) / 2;
        this.light = new Rect(x + 12, y + 48, previewWidth, 79);
        this.dark = new Rect(x + 22 + previewWidth, y + 48, previewWidth, 79);
        preview(context, this.light, false, !settings().isV2Dark(), alpha);
        preview(context, this.dark, true, settings().isV2Dark(), alpha);
        context.drawText(Fonts.REGULAR.getFont(6), "Applies to every tab in this menu", x + 12, y + 142, palette.muted.mulAlpha(alpha));

        float ax = x + column + gap;
        context.drawText(Fonts.MEDIUM.getFont(9), "Accent color", ax + 12, y + 12, palette.text.mulAlpha(alpha));
        context.drawText(Fonts.REGULAR.getFont(6.5F), "Selections and controls", ax + 12, y + 28, palette.muted.mulAlpha(alpha));
        float step = (column - 24) / ACCENTS.length;
        for (int i = 0; i < ACCENTS.length; i++) {
            this.swatches[i] = new Rect(ax + 12 + step * i, y + 51, 18, 18);
            Rect r = this.swatches[i];
            boolean selected = settings().getV2Accent().getColor().getRGB() == ACCENTS[i].getRGB();
            context.drawRoundedRect(r.x - 2, r.y - 2, 22, 22, BorderRadius.all(11),
                (selected ? palette.text : palette.surface).mulAlpha(alpha));
            context.drawRoundedRect(r.x, r.y, 18, 18, BorderRadius.all(9), ACCENTS[i].mulAlpha(alpha));
        }
        this.custom = new Rect(ax + 12, y + 85, column - 24, 26);
        boolean hover = this.custom.contains(context.getMouseX(), context.getMouseY());
        context.drawRoundedRect(custom.x, custom.y, custom.w, custom.h, BorderRadius.all(5),
            (hover ? palette.selection : palette.field).mulAlpha(alpha));
        LucideIcons.PALETTE.draw(context, custom.x + 8, custom.y + 7, 12, palette.text.mulAlpha(alpha));
        context.drawText(Fonts.MEDIUM.getFont(7), "Custom color", custom.x + 27, custom.y + 9, palette.text.mulAlpha(alpha));
        context.drawRoundedRect(ax + 12, y + 128, column - 24, 18, BorderRadius.all(5), palette.selection.mulAlpha(alpha));
        context.drawText(Fonts.MEDIUM.getFont(7), "Selected", ax + 20, y + 134, palette.text.mulAlpha(alpha));
    }

    private void preview(UIContext c, Rect r, boolean dark, boolean selected, float alpha) {
        var palette = ClickGuiV2Theme.current();
        ColorRGBA bg = dark ? new ColorRGBA(20,22,26) : new ColorRGBA(240,241,242);
        ColorRGBA card = dark ? new ColorRGBA(38,42,49) : ColorRGBA.WHITE;
        ColorRGBA ink = dark ? new ColorRGBA(239,241,245) : ColorRGBA.BLACK;
        c.drawRoundedRect(r.x - 1, r.y - 1, r.w + 2, 58, BorderRadius.all(6),
            (selected ? palette.accent : palette.border).mulAlpha(alpha));
        c.drawRoundedRect(r.x, r.y, r.w, 56, BorderRadius.all(5), bg.mulAlpha(alpha));
        c.drawRoundedRect(r.x + 5, r.y + 8, 12, 40, BorderRadius.all(3), card.mulAlpha(alpha));
        for (int i = 0; i < 3; i++) {
            c.drawRoundedRect(r.x + 22, r.y + 8 + i * 14, r.w - 27, 10, BorderRadius.all(3), card.mulAlpha(alpha));
            c.drawRoundedRect(r.x + 26, r.y + 12 + i * 14, Math.max(4, r.w - 42), 2, BorderRadius.all(1), ink.mulAlpha(.4F * alpha));
        }
        c.drawCenteredText(Fonts.MEDIUM.getFont(7), dark ? "Dark" : "Light", r.x + r.w / 2, r.y + 66,
            (selected ? palette.text : palette.muted).mulAlpha(alpha));
    }

    public boolean onClick(double x, double y, MouseButton button) {
        if (button != MouseButton.LEFT) return false;
        if (light.contains(x,y)) { settings().setV2Dark(false); return true; }
        if (dark.contains(x,y)) { settings().setV2Dark(true); return true; }
        for (int i=0; i<swatches.length; i++) {
            if (swatches[i] != null && swatches[i].contains(x,y)) {
                settings().getV2Accent().setColor(ACCENTS[i]);
                AbstractSetting.saveConfig();
                return true;
            }
        }
        if (custom.contains(x,y)) {
            this.picker = new ColorPicker((float)x, (float)y, 6, false, settings().getV2Accent().getColor(), "V2 accent");
            this.colorPickers.add(this.picker);
            return true;
        }
        return false;
    }
    public void onRelease(MouseButton button) { }
    private record Rect(float x, float y, float w, float h) {
        static final Rect EMPTY = new Rect(0,0,0,0);
        boolean contains(double mx, double my) { return GuiUtility.isHovered(x,y,w,h,mx,my); }
    }
}
