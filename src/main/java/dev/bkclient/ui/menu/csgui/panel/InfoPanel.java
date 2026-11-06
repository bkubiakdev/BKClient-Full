/*
 * InfoPanel — "Informacja" (lewa), przeciągalny.
 */
package dev.bkclient.ui.menu.csgui.panel;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsColorPicker;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.interfaces.IMinecraft;

public class InfoPanel extends CsPanel implements IMinecraft {

    private static final float W = 120.0f;
    private static final float H = 165.0f;

    private static final float PADDING       = 7.5f;
    private static final float AVATAR_SIZE   = 24.0f;
    private static final float AVATAR_RADIUS = 7.5f;
    private static final float TEXT_GAP      = 5.0f;

    private static final float LINK_FONT_SIZE  = 6.0f;
    private static final String MODS_URL = "https://changeme.net/mods";
    private static final ColorRGBA SEPARATOR_COLOR = new ColorRGBA(255f, 255f, 255f, 255f * 0.06f);

    private static final float ROW_H         = 14.0f;
    private static final float ROW_RADIUS    = 4.0f;
    private static final float ROW_FONT_SIZE = 5.5f;
    private static final float VAL_FONT_SIZE = 6.0f;
    private static final float ROW_ICON_SIZE = 6.5f;
    private static final float ICON_DRAW_SZ  = 9.5f;
    private static final float ROW_PAD_X     = 4.0f;
    private static final float ROW_ICON_GAP  = 4.0f;
    private static final float ARROW_SZ      = 5.0f;
    private static final float ROW_GAP       = 4.5f;
    private static final float SWATCH_SZ     = 8.0f;

    private static final ColorRGBA ROW_HOVER = new ColorRGBA(217f, 217f, 217f, 255f * 0.05f);
    private static final ColorRGBA ROW_TEXT  = new ColorRGBA(255f, 255f, 255f, 255f * 0.25f);
    // VAL_COLOR — dynamic, use Colors.getAccentColor() at render time

    private static final char ICON_LANG   = 'z';
    private static final char ICON_MSCALE = '0';
    private static final char ICON_ESCALE = 'y';
    private static final char ICON_WSCALE = '1';
    private static final char ICON_UNITS  = '1';
    private static final char ICON_THEME  = '2';
    private static final char ICON_ARROW  = 'x';

    private static final float     DD_W      = 66.0f;
    private static final float     DD_RADIUS = 7.5f;
    private static final float     DD_PAD    = 7.0f;
    private static final float     DD_ROW_H  = 14.0f;
    private static final float     DD_FONT   = 6.0f;
    private static final ColorRGBA DD_BG     = new ColorRGBA(21f, 21f, 21f, 255f * 0.95f);
    private static final ColorRGBA DD_GLASS  = new ColorRGBA(21f, 21f, 21f, 255f * 0.60f);
    private static final ColorRGBA DD_BORDER = new ColorRGBA(255f, 255f, 255f, 255f * 0.15f);
    // DD_SEL — dynamic, use Colors.getAccentColor() at render time
    private static final ColorRGBA DD_UNSEL  = new ColorRGBA(255f, 255f, 255f, 255f * 0.25f);

    // Row definitions — last row (Theme) has null options → opens color picker
    private static final String[] LANG_OPTS  = { "English", "Polish" };
    private static final String[] SCALE_OPTS     = { "Auto", "75%", "100%", "125%", "150%" };
    private static final String[] MENUSCALE_OPTS = {
            "Auto", "60%", "70%", "80%", "90%", "100%", "110%", "120%", "130%"
    };
    private static final float[] MENUSCALE_VALS = {
            0f, 60f, 70f, 80f, 90f, 100f, 110f, 120f, 130f
    };
    private static final int ROW_COUNT    = 5;
    private static final int THEME_ROW    = 4;
    private static final int DD_COUNT     = 4;  // rows 0-3
    private static final int MENUSCALE_ROW = 1;

    private static final String[]   ROW_LABELS  = { "Language", "Menu Scale", "ESP Scale", "Windows Scale", "Theme" };
    private static final char[]     ROW_ICONS   = { ICON_LANG, ICON_MSCALE, ICON_ESCALE, ICON_WSCALE, ICON_THEME };
    private static final String[][] ROW_OPTS    = { LANG_OPTS, MENUSCALE_OPTS, SCALE_OPTS, SCALE_OPTS, null };

    private final int[]       selected = new int[DD_COUNT];
    private final boolean[]   open     = new boolean[DD_COUNT];
    private final Animation[] ddAnim   = new Animation[DD_COUNT];

    private final float[] rowY = new float[ROW_COUNT];
    private float rowX, rowW;

    private CsColorPicker picker;
    private CsScalePicker scalePicker;

    public InfoPanel(float x, float y) {
        super(x, y, W, H);
        for (int i = 0; i < DD_COUNT; i++) {
            this.ddAnim[i] = new Animation(200L, Easing.BAKEK_SMALLER);
        }
    }

    private Interface iface() {
        return BKClientInit.getInstance().getModuleManager().getModule(Interface.class);
    }

    private void syncLanguage() {
        String cur = this.iface().getLanguage().getValue().getName();
        this.selected[0] = cur.equalsIgnoreCase("Polish") ? 1 : 0;
    }

    private void syncMenuScale() {
        float cur = this.iface().getMenuScale().getCurrentValue();
        for (int i = 0; i < MENUSCALE_VALS.length; i++) {
            if (MENUSCALE_VALS[i] == cur) { this.selected[MENUSCALE_ROW] = i; return; }
        }
    }

    private String menuScaleLabel() {
        float value = this.iface().getMenuScale().getCurrentValue();
        return value <= 0.0f ? Localizator.translate("gui.scale_picker.auto") : Math.round(value) + "%";
    }

    @Override
    protected void renderContent(UIContext context, float alpha) {
        this.syncLanguage();
        this.syncMenuScale();
        Font linkFont  = Fonts.SEMIBOLD.getFont(LINK_FONT_SIZE);
        float avX = this.x + PADDING;
        float avY = this.y + PADDING;
        if (mc.player != null) {
            context.drawHead(mc.player, avX, avY, AVATAR_SIZE, BorderRadius.all(AVATAR_RADIUS),
                    ColorRGBA.WHITE.withAlpha(255f * alpha));
        }

        String playerName = mc.player != null ? mc.player.getName().getString() : "Player";
        float tx    = avX + AVATAR_SIZE + TEXT_GAP;
        float linkY = avY + 2.0f;
        context.drawText(linkFont,  playerName,   tx, linkY,
                ColorRGBA.WHITE.withAlpha(255f * 0.75f * alpha));
        context.drawText(linkFont,  MODS_URL,   tx, linkY + 8.0f,
                Colors.getAccentColor().withAlpha(255f * alpha));

        float sepY = avY + AVATAR_SIZE + PADDING;
        context.drawRect(this.x + PADDING, sepY, this.width - PADDING * 2f, 1f,
                SEPARATOR_COLOR.withAlpha(255f * 0.06f * alpha));

        this.rowX = this.x + PADDING;
        this.rowW = this.width - PADDING * 2f;
        float curY = sepY + PADDING;

        for (int i = 0; i < ROW_COUNT; i++) {
            this.rowY[i] = curY;
            if (i == THEME_ROW) {
                this.themeRow(context, alpha, curY);
            } else {
                String value = i == MENUSCALE_ROW
                        ? this.menuScaleLabel()
                        : ROW_OPTS[i][this.selected[i]];
                this.settingRow(context, alpha, curY, ROW_ICONS[i], ROW_LABELS[i], value, i);
            }
            curY += ROW_H + ROW_GAP;
        }
    }

    /** Overlay — dropdowny + picker rysowane na wierzchu wszystkiego. */
    public void renderOverlay(UIContext context, float alpha) {
        for (int i = 0; i < DD_COUNT; i++) {
            if (i == MENUSCALE_ROW) {
                continue;
            }
            this.ddAnim[i].setDuration(dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(200L));
            this.ddAnim[i].update(this.open[i] ? 1.0f : 0.0f);
            if (this.ddAnim[i].getValue() > 0.0f) {
                this.drawDropdown(context, alpha, i);
            }
        }
        if (this.picker != null) {
            this.picker.render(context, alpha);
            if (this.picker.isDead()) {
                this.picker = null;
            }
        }
    }

    public void renderScalePicker(UIContext context, float alpha) {
        if (this.scalePicker == null) {
            return;
        }
        this.scalePicker.render(context, alpha);
        if (this.scalePicker.isDead()) {
            this.scalePicker = null;
        }
    }

    public boolean hasActivePicker() {
        return this.picker != null && !this.picker.isDead();
    }

    /** @return true jeśli picker pochłonął klik */
    public boolean pickerClick(double mx, double my, MouseButton button) {
        if (this.picker == null) return false;
        if (this.picker.isHovered(mx, my)) {
            this.picker.onClick(mx, my, button);
            return true;
        }
        this.picker.close();
        return false;
    }

    public void pickerRelease() {
        if (this.picker != null) this.picker.onRelease();
    }

    public boolean hasActiveScalePicker() {
        return this.scalePicker != null && !this.scalePicker.isDead();
    }

    public boolean scalePickerClick(double mx, double my, MouseButton button) {
        return this.scalePicker != null && this.scalePicker.onClick(mx, my, button);
    }

    public boolean scalePickerScroll(double amount) {
        return this.scalePicker != null && this.scalePicker.onScroll(amount);
    }

    public boolean scalePickerKeyPressed(int keyCode) {
        return this.scalePicker != null && this.scalePicker.onKeyPressed(keyCode);
    }

    public void closeTransientOverlays() {
        this.closeAll();
        if (this.picker != null) {
            this.picker.close();
        }
        if (this.scalePicker != null) {
            this.scalePicker.close();
        }
    }

    public boolean handleClick(double mx, double my) {
        if (this.scalePicker != null && !this.scalePicker.isDead()) {
            return this.scalePicker.onClick(mx, my, MouseButton.LEFT);
        }

        if (this.picker != null && !this.picker.isDead() && this.picker.isHovered(mx, my)) {
            return false; // pickerClick() wywoła CsGuiScreen
        }

        for (int i = 0; i < DD_COUNT; i++) {
            if (i == MENUSCALE_ROW) continue;
            if (!this.open[i] && this.ddAnim[i].getValue() <= 0f) continue;
            float dx = this.ddX();
            float dy = this.rowY[i];
            float dh = this.ddHeight(i);
            if (mx >= dx && mx <= dx + DD_W && my >= dy && my <= dy + dh) {
                int idx = (int) ((my - dy - DD_PAD) / DD_ROW_H);
                if (idx >= 0 && idx < ROW_OPTS[i].length) {
                    this.selected[i] = idx;
                    if (i == 0) {
                        this.iface().getLanguage().setValue(this.iface().getLanguage().getValues().get(idx));
                    }
                }
                this.closeAll();
                return true;
            }
            this.open[i] = false;
        }

        if (mx >= this.rowX && mx <= this.rowX + this.rowW
                && my >= this.rowY[THEME_ROW] && my <= this.rowY[THEME_ROW] + ROW_H) {
            this.closeAll();
            if (this.scalePicker != null) this.scalePicker.close();
            if (this.picker != null && !this.picker.isDead()) {
                this.picker.close();
            } else {
                float px = this.rowX + this.rowW + 6.0f;
                float py = this.rowY[THEME_ROW] - 60.0f;
                this.picker = new CsColorPicker(this.iface().getAccentColor(), px, py);
            }
            return true;
        }

        for (int i = 0; i < DD_COUNT; i++) {
            if (mx >= this.rowX && mx <= this.rowX + this.rowW
                    && my >= this.rowY[i] && my <= this.rowY[i] + ROW_H) {
                if (i == MENUSCALE_ROW) {
                    this.closeAll();
                    if (this.picker != null) this.picker.close();
                    if (this.scalePicker != null && !this.scalePicker.isDead()) {
                        this.scalePicker.close();
                    } else {
                        this.scalePicker = new CsScalePicker(this.iface().getMenuScale());
                    }
                    return true;
                }
                boolean was = this.open[i];
                this.closeAll();
                if (this.picker != null) this.picker.close();
                this.open[i] = !was;
                return true;
            }
        }
        return false;
    }

    private void closeAll() {
        for (int i = 0; i < DD_COUNT; i++) this.open[i] = false;
    }

    private float ddX() {
        return this.rowX + this.rowW + 6.0f;
    }

    private float ddHeight(int row) {
        return DD_PAD * 2.0f + ROW_OPTS[row].length * DD_ROW_H;
    }

    private void drawDropdown(UIContext context, float alpha, int row) {
        float t    = this.ddAnim[row].getValue();
        float dx   = this.ddX();
        float fullH = this.ddHeight(row);
        float dy   = this.rowY[row] + (1.0f - t) * 6.0f;
        float dh   = fullH * t;
        float a    = alpha * t;
        BorderRadius r = BorderRadius.all(DD_RADIUS);

        if (dev.bkclient.systems.modules.modules.visuals.Interface.blurGui())
            context.drawBlurredRect(dx, dy, DD_W, dh, 12f, 2f, r, DD_GLASS.withAlpha(DD_GLASS.getAlpha() * a));
        context.drawRoundedRect(dx, dy, DD_W, dh, r, DD_BG.withAlpha(DD_BG.getAlpha() * a));
        context.drawRoundedBorder(dx, dy, DD_W, dh, BORDER_THICKNESS, r, DD_BORDER.withAlpha(DD_BORDER.getAlpha() * a));

        Font font = Fonts.MEDIUM.getFont(DD_FONT);
        ScissorUtility.push(context.getMatrices(), dx, dy, DD_W, dh);
        for (int i = 0; i < ROW_OPTS[row].length; i++) {
            float ry  = this.rowY[row] + DD_PAD + i * DD_ROW_H;
            ColorRGBA c = (i == this.selected[row]) ? Colors.getAccentColor() : DD_UNSEL;
            context.drawText(font, ROW_OPTS[row][i], dx + DD_PAD,
                    ry + (DD_ROW_H - font.height()) / 2.0f, c.withAlpha(c.getAlpha() * a));
        }
        ScissorUtility.pop();
    }

    private void themeRow(UIContext context, float alpha, float rY) {
        Font nameFont = Fonts.MEDIUM.getFont(ROW_FONT_SIZE);
        boolean hover = context.getMouseX() >= this.rowX && context.getMouseX() <= this.rowX + this.rowW
                && context.getMouseY() >= rY && context.getMouseY() <= rY + ROW_H;
        if (hover) {
            context.drawRoundedRect(this.rowX, rY, this.rowW, ROW_H, BorderRadius.all(ROW_RADIUS),
                    ROW_HOVER.withAlpha(255f * 0.05f * alpha));
        }
        float cy = rY + ROW_H / 2.0f;
        context.drawIcon(Fonts.ICONS, ICON_THEME,
                this.rowX + ROW_PAD_X + ROW_ICON_SIZE / 2.0f - ICON_DRAW_SZ / 2.0f,
                cy - ICON_DRAW_SZ / 2.0f, ICON_DRAW_SZ, ROW_TEXT.withAlpha(255f * 0.25f * alpha));
        context.drawText(nameFont, "Theme",
                this.rowX + ROW_PAD_X + ROW_ICON_SIZE + ROW_ICON_GAP,
                cy - nameFont.height() / 2.0f, ROW_TEXT.withAlpha(255f * 0.25f * alpha));

        ColorRGBA accent = Colors.getAccentColor();
        context.drawRoundedRect(this.rowX + this.rowW - ROW_PAD_X - SWATCH_SZ,
                cy - SWATCH_SZ / 2.0f, SWATCH_SZ, SWATCH_SZ,
                BorderRadius.all(SWATCH_SZ / 2.0f), accent.withAlpha(accent.getAlpha() * alpha));
    }

    private void settingRow(UIContext context, float alpha, float rY,
            char icon, String name, String value, int idx) {
        Font nameFont  = Fonts.MEDIUM.getFont(ROW_FONT_SIZE);
        Font valueFont = Fonts.MEDIUM.getFont(VAL_FONT_SIZE);
        boolean hover = context.getMouseX() >= this.rowX && context.getMouseX() <= this.rowX + this.rowW
                && context.getMouseY() >= rY && context.getMouseY() <= rY + ROW_H;
        if (hover) {
            context.drawRoundedRect(this.rowX, rY, this.rowW, ROW_H, BorderRadius.all(ROW_RADIUS),
                    ROW_HOVER.withAlpha(255f * 0.05f * alpha));
        }
        float cy = rY + ROW_H / 2.0f;
        context.drawIcon(Fonts.ICONS, icon,
                this.rowX + ROW_PAD_X + ROW_ICON_SIZE / 2.0f - ICON_DRAW_SZ / 2.0f,
                cy - ICON_DRAW_SZ / 2.0f, ICON_DRAW_SZ, ROW_TEXT.withAlpha(255f * 0.25f * alpha));
        context.drawText(nameFont, name,
                this.rowX + ROW_PAD_X + ROW_ICON_SIZE + ROW_ICON_GAP,
                cy - nameFont.height() / 2.0f, ROW_TEXT.withAlpha(255f * 0.25f * alpha));

        float arrowX = this.rowX + this.rowW - ROW_PAD_X - ARROW_SZ;
        context.drawIcon(Fonts.ICONS, ICON_ARROW, arrowX, cy - ARROW_SZ / 2.0f, ARROW_SZ,
                ROW_TEXT.withAlpha(255f * 0.25f * alpha));
        context.drawRightText(valueFont, value, arrowX - 3.0f,
                cy - valueFont.height() / 2.0f, Colors.getAccentColor().withAlpha(255f * alpha));
    }
}
