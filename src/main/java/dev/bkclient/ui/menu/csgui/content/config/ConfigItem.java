/*
 * ConfigItem — pojedynczy config w panelu (Global/Market). Skala ÷2 (jak całe CsGui).
 * Rysuje: ikonę (#), nazwę, datę, ikonę ustawień, separator, toggle. Popup: Save/Share/Delete.
 */
package dev.bkclient.ui.menu.csgui.content.config;

import java.text.SimpleDateFormat;
import java.util.Date;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;

public class ConfigItem {

    public static final float CARD_HEIGHT = 26.0f;        // ~50
    private static final float CARD_RADIUS = 6.0f;         // 12
    private static final float PADDING = 4.0f;

    private static final float ICON_SIZE = 9.0f;           // 16.67
    private static final float ICON_RADIUS = 3.0f;
    private static final float NAME_FONT_SIZE = 5.5f;      // 11
    private static final float DATE_FONT_SIZE = 5.5f;      // 11
    private static final float SETTINGS_ICON_SIZE = 8.0f;
    private static final float DIVIDER_W = 1.0f;
    private static final float DIVIDER_H = 5.0f;
    private static final float TOGGLE_W = 12.0f;           // ~20
    private static final float TOGGLE_H = 7.0f;            // ~12
    private static final float KNOB_SIZE = 4.5f;           // 8

    private static final ColorRGBA CARD_FILL = new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.6f);
    private static final ColorRGBA CARD_FILL_ON = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.015f);
    private static final ColorRGBA CARD_BORDER = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.05f);
    private static dev.bkclient.utility.colors.ColorRGBA ICON_COLOR() { return dev.bkclient.ui.menu.csgui.CsTheme.Color.accent(); }
    private static final ColorRGBA NAME_COLOR = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.75f);
    private static final ColorRGBA DATE_COLOR = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);
    private static final ColorRGBA SETTINGS_COLOR = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.5f);
    private static final ColorRGBA DIVIDER_COLOR = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.1f);
    private static dev.bkclient.utility.colors.ColorRGBA TOGGLE_GLOW() { return dev.bkclient.ui.menu.csgui.CsTheme.Color.accent().withAlpha(255.0f * 0.5f); }
    private static final ColorRGBA TOGGLE_OFF_BG = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.15f);
    private static final ColorRGBA KNOB_OFF = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);

    private static final float POPUP_W = 60.0f;            // 120
    private static final float POPUP_PAD = 6.0f;
    private static final float POPUP_ROW_H = 13.0f;
    // 5 zwykłych wierszy (Rename, Load, Save, Public, Private) + separator + Delete
    private static final float POPUP_H = POPUP_PAD + POPUP_ROW_H * 5.0f + 1.0f + POPUP_ROW_H + POPUP_PAD;
    private static final float POPUP_RADIUS = 6.0f;
    private static final ColorRGBA POPUP_BG = new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.94f);
    private static final ColorRGBA POPUP_BORDER = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.05f);
    private static final ColorRGBA POPUP_GLASS = new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.55f);
    private static final ColorRGBA ITEM_COLOR = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.5f);
    private static dev.bkclient.utility.colors.ColorRGBA DELETE_COLOR() { return dev.bkclient.ui.menu.csgui.CsTheme.Color.accent(); }
    private static final ColorRGBA POPUP_SEP = new ColorRGBA(217.0f, 217.0f, 217.0f, 255.0f * 0.05f);

    private final String name;
    private final long date;
    private boolean enabled;

    private final Animation toggleAnim = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
    private final Animation popupAnim = new Animation(150L, Easing.FIGMA_EASE_IN_OUT);
    private final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");

    private boolean showPopup;

    private float cardX, cardY, cardW;
    private float settingsX, settingsY;
    private float toggleX, toggleY;

    private final Runnable onToggle, onSave, onShare, onSharePrivate, onDelete, onRename, onLoad;

    public ConfigItem(String name, long date, boolean enabled,
            Runnable onToggle, Runnable onSave, Runnable onShare, Runnable onDelete) {
        this(name, date, enabled, onToggle, onSave, onShare, onShare, onDelete, null, onToggle);
    }

    public ConfigItem(String name, long date, boolean enabled,
            Runnable onToggle, Runnable onSave, Runnable onShare, Runnable onSharePrivate, Runnable onDelete) {
        this(name, date, enabled, onToggle, onSave, onShare, onSharePrivate, onDelete, null, onToggle);
    }

    public ConfigItem(String name, long date, boolean enabled,
            Runnable onToggle, Runnable onSave, Runnable onShare, Runnable onSharePrivate, Runnable onDelete,
            Runnable onRename, Runnable onLoad) {
        this.name = name;
        this.date = date;
        this.enabled = enabled;
        this.onToggle = onToggle;
        this.onSave = onSave;
        this.onShare = onShare;
        this.onSharePrivate = onSharePrivate;
        this.onDelete = onDelete;
        this.onRename = onRename;
        this.onLoad = onLoad;
    }

    private boolean clickSysStyle;
    public ConfigItem useClickSysStyle() { clickSysStyle=true; return this; }
    private ColorRGBA color(ColorRGBA fallback, int paletteColor) { return clickSysStyle ? ColorRGBA.fromInt(paletteColor) : fallback; }
    public void render(UIContext context, float x, float y, float width, float alpha) {
        var theme=dev.bkclient.ui.clicksys.port.data.ThemeConfig.getPalette();
        ColorRGBA CARD_FILL=color(ConfigItem.CARD_FILL,theme.getValue2());
        ColorRGBA CARD_FILL_ON=clickSysStyle ? ColorRGBA.fromInt(theme.getValue3()).mulAlpha(.4f) : ConfigItem.CARD_FILL_ON;
        ColorRGBA CARD_BORDER=color(ConfigItem.CARD_BORDER,dev.bkclient.ui.clicksys.ClickSysMaterial.panelBorder(theme.getValue2(),theme.getValue4()));
        ColorRGBA NAME_COLOR=color(ConfigItem.NAME_COLOR,theme.getValue7());
        ColorRGBA DATE_COLOR=color(ConfigItem.DATE_COLOR,theme.getValue5());
        ColorRGBA SETTINGS_COLOR=color(ConfigItem.SETTINGS_COLOR,theme.getValue5());
        ColorRGBA DIVIDER_COLOR=color(ConfigItem.DIVIDER_COLOR,theme.getValue4());
        ColorRGBA TOGGLE_OFF_BG=color(ConfigItem.TOGGLE_OFF_BG,theme.getValue3());
        ColorRGBA KNOB_OFF=color(ConfigItem.KNOB_OFF,theme.getValue5());
        this.cardX = x;
        this.cardY = y;
        this.cardW = width;

        BorderRadius radius = BorderRadius.all(CARD_RADIUS);
        context.drawRoundedRect(x, y, width, CARD_HEIGHT, radius, CARD_FILL.withAlpha(CARD_FILL.getAlpha() * alpha));
        if (this.enabled) {
            context.drawRoundedRect(x, y, width, CARD_HEIGHT, radius,
                    CARD_FILL_ON.withAlpha(CARD_FILL_ON.getAlpha() * alpha));
        }
        context.drawRoundedBorder(x, y, width, CARD_HEIGHT, 0.05f, radius,
                CARD_BORDER.withAlpha(CARD_BORDER.getAlpha() * alpha));

        float centerY = y + CARD_HEIGHT / 2.0f;

        float iconX = x + PADDING;
        float iconY = centerY - ICON_SIZE / 2.0f;

        context.drawIcon(Fonts.ICONS, '#', iconX, iconY - 1, ICON_SIZE + 2.5f, ICON_COLOR().withAlpha(ICON_COLOR().getAlpha() * alpha));

        Font nameFont = Fonts.SEMIBOLD.getFont(NAME_FONT_SIZE);
        Font dateFont = Fonts.SEMIBOLD.getFont(DATE_FONT_SIZE);
        float textX = iconX + ICON_SIZE + 4.0f;
        context.drawText(nameFont, this.name, textX + 3, centerY - nameFont.height() - 1.0f,
                NAME_COLOR.withAlpha(NAME_COLOR.getAlpha() * alpha));
        context.drawText(dateFont, this.df.format(new Date(this.date)), textX + 3, centerY + 1.5f,
                DATE_COLOR.withAlpha(DATE_COLOR.getAlpha() * alpha));

        this.toggleX = x + width - PADDING - TOGGLE_W;
        this.toggleY = centerY - TOGGLE_H / 2.0f;
        this.toggleAnim.update(this.enabled ? 1.0f : 0.0f);
        float t = this.toggleAnim.getValue();
        if (this.enabled) {
            context.drawRoundedRect(this.toggleX, this.toggleY, TOGGLE_W, TOGGLE_H, BorderRadius.all(TOGGLE_H / 3),
                    ICON_COLOR().withAlpha(ICON_COLOR().getAlpha() * alpha));
        } else {
            context.drawRoundedRect(this.toggleX, this.toggleY, TOGGLE_W, TOGGLE_H, BorderRadius.all(TOGGLE_H / 3f),
                    TOGGLE_OFF_BG.withAlpha(TOGGLE_OFF_BG.getAlpha() * alpha));
        }
        float knobX = this.toggleX + 1.5f + (TOGGLE_W - KNOB_SIZE - 3.0f) * t;
        float knobY = this.toggleY + (TOGGLE_H - KNOB_SIZE) / 2.0f;
        ColorRGBA knobColor = this.enabled ? color(ColorRGBA.WHITE,theme.getValue()) : KNOB_OFF;
        context.drawRoundedRect(knobX, knobY, KNOB_SIZE, KNOB_SIZE, BorderRadius.all(KNOB_SIZE / 2.0f),
                knobColor.withAlpha(knobColor.getAlpha() * alpha));

        float divX = this.toggleX - 4.0f;
        context.drawRoundedRect(divX - 1, centerY - DIVIDER_H / 2.0f, DIVIDER_W, DIVIDER_H,
                BorderRadius.all(0), DIVIDER_COLOR.withAlpha(DIVIDER_COLOR.getAlpha() * alpha));

        this.settingsX = divX - 4.0f - SETTINGS_ICON_SIZE;
        this.settingsY = centerY - SETTINGS_ICON_SIZE / 2.0f;
        context.drawIcon(Fonts.ICONS, '"', this.settingsX - 2, this.settingsY - 1, SETTINGS_ICON_SIZE + 2,
                SETTINGS_COLOR.withAlpha(SETTINGS_COLOR.getAlpha() * alpha));

        this.popupAnim.update(this.showPopup ? 1.0f : 0.0f);
    }

    // Dolna granica obszaru listy — poniżej niej popup ma się flipnąć nad kartę.
    private float panelBottom = Float.MAX_VALUE;

    public void setPanelBottom(float bottom) {
        this.panelBottom = bottom;
    }

    // Y popupu: domyślnie pod kartą; jeśli wyszedłby poza dół panelu — nad kartą.
    private float popupY() {
        float below = this.cardY + CARD_HEIGHT + 2.0f;
        if (below + POPUP_H > this.panelBottom) {
            return this.cardY - POPUP_H - 2.0f;
        }
        return below;
    }

    public void renderPopup(UIContext context, float alpha) {
        var theme=dev.bkclient.ui.clicksys.port.data.ThemeConfig.getPalette();
        ColorRGBA POPUP_BG=color(ConfigItem.POPUP_BG,theme.getValue2());
        ColorRGBA POPUP_BORDER=color(ConfigItem.POPUP_BORDER,dev.bkclient.ui.clicksys.ClickSysMaterial.panelBorder(theme.getValue2(),theme.getValue4()));
        ColorRGBA ITEM_COLOR=color(ConfigItem.ITEM_COLOR,theme.getValue7());
        ColorRGBA POPUP_SEP=color(ConfigItem.POPUP_SEP,theme.getValue4());
        float pa = this.popupAnim.getValue() * alpha;
        if (pa <= 0.0f) {
            return;
        }
        // Popup nad ikoną ustawień, wyrównany prawą krawędzią do toggle
        float px = this.cardX + this.cardW - POPUP_W;
        float py = this.popupY();
        BorderRadius r = BorderRadius.all(POPUP_RADIUS);
        if (!clickSysStyle && dev.bkclient.systems.modules.modules.visuals.Interface.blurGui())
            context.drawBlurredRect(px, py, POPUP_W, POPUP_H, 12.0f, 2.0f, r, POPUP_GLASS.withAlpha(POPUP_GLASS.getAlpha() * pa));
        context.drawRoundedRect(px, py, POPUP_W, POPUP_H, r, POPUP_BG.withAlpha(POPUP_BG.getAlpha() * pa));
        context.drawRoundedBorder(px, py, POPUP_W, POPUP_H, 0.05f, r, POPUP_BORDER.withAlpha(POPUP_BORDER.getAlpha() * pa));

        Font font = Fonts.SEMIBOLD.getFont(6.5f);
        float iconSize = 6.0f;
        float rowBase = py + POPUP_PAD;
        ColorRGBA item = ITEM_COLOR.withAlpha(ITEM_COLOR.getAlpha() * pa);

        String[] labels = { "Rename", "Load", "Save", "Public", "Private" };
        char[] icons = { 'e', 'm', 'S', '1', '1' };
        for (int i = 0; i < labels.length; i++) {
            float rowY = rowBase + POPUP_ROW_H * i;
            context.drawText(font, labels[i], px + POPUP_PAD, rowY + (POPUP_ROW_H - font.height()) / 2.0f - 2, item);
            context.drawIcon(Fonts.ICONS, icons[i], px + POPUP_W - POPUP_PAD - iconSize - 2,
                    rowY + (POPUP_ROW_H - iconSize) / 2.0f - 3, iconSize + 3, item);
        }

        float sepY = rowBase + POPUP_ROW_H * 5.0f;
        context.drawRect(px + POPUP_PAD, sepY, POPUP_W - POPUP_PAD * 2.0f, 0.5f,
                POPUP_SEP.withAlpha(POPUP_SEP.getAlpha() * pa));

        float rowDel = sepY + 1.0f;
        context.drawText(font, "Delete", px + POPUP_PAD, rowDel + (POPUP_ROW_H - font.height()) / 2.0f + 1,
                DELETE_COLOR().withAlpha(DELETE_COLOR().getAlpha() * pa));
        context.drawIcon(Fonts.ICONS, '(', px + POPUP_W - POPUP_PAD - iconSize - 2, rowDel + (POPUP_ROW_H - iconSize) / 2.0f - 1,
                iconSize + 4, DELETE_COLOR().withAlpha(DELETE_COLOR().getAlpha() * pa));
    }

    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT && button != MouseButton.RIGHT) {
            return false;
        }
        float mx = (float) mouseX;
        float my = (float) mouseY;

        if (this.showPopup) {
            float px = this.cardX + this.cardW - POPUP_W;
            float py = this.popupY();
            if (mx >= px && mx <= px + POPUP_W && my >= py && my <= py + POPUP_H) {
                float rel = my - (py + POPUP_PAD);
                int row = (int) (rel / POPUP_ROW_H);
                // 0=Rename 1=Load 2=Save 3=Public 4=Private  (po separatorze) Delete
                switch (row) {
                    case 0 -> { if (this.onRename != null) this.onRename.run(); }
                    case 1 -> { if (this.onLoad != null) this.onLoad.run(); }
                    case 2 -> { if (this.onSave != null) this.onSave.run(); }
                    case 3 -> { if (this.onShare != null) this.onShare.run(); }
                    case 4 -> { if (this.onSharePrivate != null) this.onSharePrivate.run(); }
                    default -> { if (this.onDelete != null) this.onDelete.run(); } // row >= 5
                }
                this.showPopup = false;
                return true;
            }
            this.showPopup = false;
            return true;
        }

        if (button == MouseButton.RIGHT && mx >= this.cardX && mx <= this.cardX + this.cardW
                && my >= this.cardY && my <= this.cardY + CARD_HEIGHT) {
            this.showPopup = true;
            return true;
        }

        if (button == MouseButton.LEFT && mx >= this.settingsX && mx <= this.settingsX + SETTINGS_ICON_SIZE
                && my >= this.settingsY && my <= this.settingsY + SETTINGS_ICON_SIZE) {
            this.showPopup = true;
            return true;
        }
        if (button == MouseButton.LEFT && mx >= this.toggleX && mx <= this.toggleX + TOGGLE_W
                && my >= this.toggleY && my <= this.toggleY + TOGGLE_H) {
            this.enabled = !this.enabled;
            if (this.onToggle != null) this.onToggle.run();
            return true;
        }
        return false;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void closePopup() {
        this.showPopup = false;
    }

    public String getName() {
        return this.name;
    }

    public boolean isPopupOpen() {
        return this.showPopup;
    }

    public boolean shouldRenderPopup() {
        return this.showPopup || this.popupAnim.getValue() > 0.0f;
    }

    public void setPopupOpen(boolean open) {
        this.showPopup = open;
    }
}
