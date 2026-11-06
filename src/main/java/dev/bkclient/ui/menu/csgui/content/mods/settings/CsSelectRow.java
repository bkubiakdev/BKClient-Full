package dev.bkclient.ui.menu.csgui.content.mods.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.ScissorUtility;

public class CsSelectRow implements CsSetting {

    private static final float V_PAD = 4.5f;
    private static final float FIELD_GAP = 4.0f;
    private static final float FIELD_H = 11.0f;
    private static final float ROW_H = 11.0f;
    private static final float DROP_PAD = 3.0f;
    private static final float SEARCH_H = 12.0f;
    private static final float SEARCH_GAP = 3.0f;
    private static final int MAX_VISIBLE = 7;
    private static final float FIELD_FONT = 6.0f;
    private static final float FIELD_RADIUS = 3.0f;

    private final SelectSetting setting;
    private final CsOverlayHost host;
    private final TextField searchField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private boolean open;
    private float scrollOffset;
    private boolean ownerEnabled;

    private final dev.bkclient.utility.animation.base.Animation openAnim =
            new dev.bkclient.utility.animation.base.Animation(220L,
                    dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT);
    private final java.util.Map<SelectSetting.Value, dev.bkclient.utility.animation.base.Animation> selAnims =
            new java.util.HashMap<>();

    private float fieldX, fieldY, fieldW;
    private float dropX, dropY, dropW, dropH;
    private float searchX, searchY, searchW;
    private final List<float[]> rowBounds = new ArrayList<>();
    private final List<SelectSetting.Value> rowValues = new ArrayList<>();

    public CsSelectRow(SelectSetting setting, CsOverlayHost host) {
        this.setting = setting;
        this.host = host;
    }

    @Override
    public Setting getSetting() {
        return this.setting;
    }

    public void setOwnerEnabled(boolean enabled) {
        this.ownerEnabled = enabled;
    }

    private List<SelectSetting.Value> visibleValues() {
        List<SelectSetting.Value> out = new ArrayList<>();
        for (SelectSetting.Value v : this.setting.getValues()) {
            if (!v.isHidden()) {
                out.add(v);
            }
        }
        return out;
    }

    private List<SelectSetting.Value> filteredValues() {
        String query = this.searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return visibleValues();
        }
        List<SelectSetting.Value> out = new ArrayList<>();
        for (SelectSetting.Value v : visibleValues()) {
            String raw = v.getName().toLowerCase(Locale.ROOT);
            String translated = Localizator.translate(v.getName()).toLowerCase(Locale.ROOT);
            if (raw.contains(query) || translated.contains(query)) {
                out.add(v);
            }
        }
        return out;
    }

    private String selectedText() {
        List<SelectSetting.Value> sel = this.setting.getSelectedValues();
        if (sel.isEmpty()) {
            return "None";
        }
        String first = Localizator.translate(sel.get(0).getName());
        if (sel.size() == 1) {
            return first;
        }
        String second = Localizator.translate(sel.get(1).getName());
        String two = first + ", " + second;
        if (two.length() > 16) {
            return first + "...";
        }
        return sel.size() > 2 ? two + "..." : two;
    }

    @Override
    public float getHeight() {
        Font label = Fonts.SEMIBOLD.getFont(CsTheme.Slider.NAME_FONT + 1.0f);
        return V_PAD + label.height() + FIELD_GAP + FIELD_H + V_PAD;
    }

    @Override
    public void render(UIContext context, float x, float y, float w, float alpha) {
        float ny = y + V_PAD;
        Font label = Fonts.SEMIBOLD.getFont(CsTheme.Slider.NAME_FONT + 1.0f);
        context.drawText(label, Localizator.translate(this.setting.getName()), x, ny,
                CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        int selected = this.setting.getSelectedValues().size();
        int total = visibleValues().size();
        String count = selected + " / " + total;
        Font cf = Fonts.SEMIBOLD.getFont(6.0f);
        float chipW = cf.width(count) + 8.0f;
        float chipH = 7.0f;
        float chipX = x + w - chipW;
        float chipY = ny + label.height() / 2.0f - chipH / 2.0f;
        context.drawRoundedRect(chipX + 1.5f, chipY, chipW - 4.0f, chipH, BorderRadius.all(2.0f),
                CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * alpha));
        context.drawCenteredText(cf, count, chipX + chipW / 2.0f, chipY + (chipH - cf.height()) / 2.0f,
                CsTheme.Color.BLACK_75.withAlpha(255.0f * 0.75f * alpha));

        this.fieldX = x - 1.5f;
        this.fieldY = ny + label.height() + FIELD_GAP;
        this.fieldW = w + 1.0f;
        if (!this.open) {
            BorderRadius fr = BorderRadius.all(FIELD_RADIUS);
            context.drawRoundedRect(this.fieldX, this.fieldY, this.fieldW, FIELD_H, fr,
                    new ColorRGBA(0.0f, 0.0f, 0.0f, 255.0f * 0.05f).withAlpha(255.0f * 0.05f * alpha));
            context.drawRoundedBorder(this.fieldX, this.fieldY, this.fieldW, FIELD_H, 0.05f, fr,
                    CsTheme.Color.SUB_BORDER.withAlpha(255.0f * 0.05f * alpha));
            this.drawFieldContent(context, alpha);
        }
    }

    private void drawFieldContent(UIContext context, float alpha) {
        Font ff = Fonts.SEMIBOLD.getFont(FIELD_FONT);
        context.drawText(ff, this.selectedText(), this.fieldX + 4.0f, this.fieldY + (FIELD_H - ff.height()) / 2.0f,
                CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));
        context.drawIcon(Fonts.ICONS, 'm', this.fieldX + this.fieldW - 8.0f,
                this.fieldY + FIELD_H / 2.0f - 2.5f, 5.5f,
                CsTheme.Color.TEXT_50.withAlpha(255.0f * 0.5f * alpha));
    }

    private void drawSearchField(UIContext context, float alpha) {
        this.searchX = this.dropX + DROP_PAD;
        this.searchY = this.fieldY + FIELD_H + DROP_PAD;
        this.searchW = this.dropW - DROP_PAD * 2.0f;

        context.drawRoundedRect(this.searchX, this.searchY, this.searchW, SEARCH_H, BorderRadius.all(4.0f),
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.05f).withAlpha(255.0f * 0.05f * alpha));
        context.drawRoundedBorder(this.searchX, this.searchY, this.searchW, SEARCH_H, 0.05f, BorderRadius.all(4.0f),
                CsTheme.Color.SUB_BORDER.withAlpha(255.0f * 0.05f * alpha));

        this.searchField.setAlpha(alpha);
        this.searchField.setTextColor(CsTheme.Color.TEXT_75);
        this.searchField.setPreview("Search..");
        this.searchField.set(this.searchX + 2.0f, this.searchY, this.searchW - 13.0f, SEARCH_H);
        this.searchField.render(context);
        context.drawIcon(Fonts.ICONS, 'v', this.searchX + this.searchW - 9.0f,
                this.searchY + (SEARCH_H - 5.5f) / 2.0f - 1.25f, 7.5f,
                CsTheme.Color.TEXT_25.withAlpha(255.0f * 0.25f * alpha));
    }

    public void renderDropdown(UIContext context, float alpha) {
        this.openAnim.setDuration(dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(220L));
        this.openAnim.update(this.open ? 1.0f : 0.0f);
        float openV = this.openAnim.getValue();
        if (!this.open && openV <= 0.01f) {
            return;
        }

        List<SelectSetting.Value> values = filteredValues();
        int total = values.size();
        int visible = Math.min(total, MAX_VISIBLE);
        float fullListH = visible * ROW_H + DROP_PAD * 2.0f;
        float listH = fullListH * openV;
        float searchBlockH = SEARCH_H + SEARCH_GAP + DROP_PAD;

        float panelX = this.fieldX;
        float panelY = this.fieldY;
        float panelW = this.fieldW;
        float panelH = FIELD_H + (searchBlockH + listH) * openV;
        BorderRadius r = BorderRadius.all(FIELD_RADIUS);

        if (dev.bkclient.systems.modules.modules.visuals.Interface.shadow()) {
            context.drawShadow(panelX - 3.0f, panelY - 3.0f, panelW + 6.0f, panelH + 6.0f, 12.0f, r,
                    ColorRGBA.BLACK.withAlpha(255.0f * 0.35f * alpha));
        }
        context.drawRoundedRect(panelX, panelY, panelW, panelH, r,
                CsTheme.Color.WINDOW_FILL.withAlpha(CsTheme.Color.WINDOW_FILL.getAlpha() * alpha));
        if (this.ownerEnabled) {
            context.drawRoundedRect(panelX, panelY, panelW, panelH, r,
                    new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.03f).withAlpha(255.0f * 0.03f * alpha));
        }
        context.drawRoundedBorder(panelX, panelY, panelW, panelH, 0.05f, r,
                CsTheme.Color.WINDOW_BORDER.withAlpha(CsTheme.Color.WINDOW_BORDER.getAlpha() * alpha));

        this.drawFieldContent(context, alpha);

        this.dropX = panelX;
        this.dropW = panelW;
        if (openV > 0.05f) {
            this.drawSearchField(context, alpha * openV);
        }

        this.dropY = this.fieldY + FIELD_H + searchBlockH;
        this.dropH = listH;

        float maxOff = Math.max(0.0f, total * ROW_H - visible * ROW_H);
        this.scrollOffset = Math.clamp(this.scrollOffset, 0.0f, maxOff);

        ScissorUtility.push(context.getMatrices(), this.dropX, this.dropY, this.dropW, this.dropH);
        Font rf = Fonts.SEMIBOLD.getFont(FIELD_FONT);
        this.rowBounds.clear();
        this.rowValues.clear();
        float listAlpha = alpha * openV;
        float ry = this.dropY + DROP_PAD - this.scrollOffset;
        for (SelectSetting.Value v : values) {
            boolean selected = v.isSelected();
            dev.bkclient.utility.animation.base.Animation sa = this.selAnims.computeIfAbsent(v,
                    k -> new dev.bkclient.utility.animation.base.Animation(180L,
                            dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT));
            sa.setDuration(dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(180L));
            sa.update(selected ? 1.0f : 0.0f);
            float sv = sa.getValue();
            float rr = 255.0f + (CsTheme.Color.accent().getRed() - 255.0f) * sv;
            float gg = 255.0f + (CsTheme.Color.accent().getGreen() - 255.0f) * sv;
            float bb = 255.0f + (CsTheme.Color.accent().getBlue() - 255.0f) * sv;
            float aFrac = 0.5f + 0.5f * sv;
            ColorRGBA tc = new ColorRGBA(rr, gg, bb, 255.0f * aFrac);
            context.drawText(rf, Localizator.translate(v.getName()), this.dropX + 5.0f,
                    ry + (ROW_H - rf.height()) / 2.0f, tc.withAlpha(255.0f * aFrac * listAlpha));
            this.rowBounds.add(new float[] { this.dropX, ry, this.dropW, ROW_H });
            this.rowValues.add(v);
            ry += ROW_H;
        }
        ScissorUtility.pop();

        if (total > visible) {
            float sbW = 1.5f;
            float sbX = this.dropX + this.dropW - sbW - 2.0f;
            float trackH = this.dropH - DROP_PAD * 2.0f;
            float thumbH = Math.max(12.0f, trackH * visible / total);
            float thumbY = this.dropY + DROP_PAD + (trackH - thumbH) * (maxOff <= 0.0f ? 0.0f : this.scrollOffset / maxOff);
            context.drawRoundedRect(sbX - 2.2f, this.dropY + DROP_PAD, sbW, trackH, BorderRadius.all(0.0f),
                    new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.06f).withAlpha(255.0f * 0.06f * alpha));
            context.drawRoundedRect(sbX - 2.2f, thumbY, sbW, thumbH, BorderRadius.all(0.0f),
                    CsTheme.Color.TEXT_25.withAlpha(255.0f * 0.25f * alpha));
        }
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return false;
        }
        if (this.host != null) {
            this.host.openDropdown(this);
        }
        return true;
    }

    public boolean handleDropdownClick(double mouseX, double mouseY, MouseButton button) {
        if (!this.open) {
            return false;
        }
        boolean inPanel = mouseX >= this.fieldX && mouseX <= this.fieldX + this.fieldW
                && mouseY >= this.fieldY && mouseY <= this.fieldY + this.openPanelHeight();
        if (!inPanel) {
            return false;
        }
        if (button == MouseButton.LEFT) {
            boolean inSearch = mouseX >= this.searchX && mouseX <= this.searchX + this.searchW
                    && mouseY >= this.searchY && mouseY <= this.searchY + SEARCH_H;
            if (inSearch) {
                this.searchField.onMouseClicked(mouseX, mouseY, button);
                this.searchField.setFocused(true);
                return true;
            }
            this.searchField.setFocused(false);
            for (int i = 0; i < this.rowBounds.size(); ++i) {
                float[] b = this.rowBounds.get(i);
                if (mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                    this.rowValues.get(i).toggle();
                    break;
                }
            }
        }
        return true;
    }

    public void onScroll(double amount) {
        this.scrollOffset -= (float) amount * ROW_H;
    }

    @Override
    public void handleRelease() {
        this.searchField.onMouseReleased(0.0, 0.0, MouseButton.LEFT);
    }

    private float openPanelHeight() {
        return FIELD_H + SEARCH_H + SEARCH_GAP + DROP_PAD + this.dropH;
    }

    public boolean isFieldHovered(double mouseX, double mouseY) {
        return mouseX >= this.fieldX && mouseX <= this.fieldX + this.fieldW
                && mouseY >= this.fieldY && mouseY <= this.fieldY + FIELD_H;
    }

    public boolean isOpen() {
        return this.open;
    }

    @Override
    public boolean isCapturingInput() {
        return this.searchField.isFocused();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.searchField.isFocused()) {
            return false;
        }
        if (keyCode == 256) {
            this.searchField.setFocused(false);
            return true;
        }
        this.searchField.onKeyPressed(keyCode, scanCode, modifiers);
        this.scrollOffset = 0.0f;
        return true;
    }

    @Override
    public boolean charTyped(char chr) {
        if (!this.searchField.isFocused()) {
            return false;
        }
        boolean handled = this.searchField.charTyped(chr, 0);
        if (handled) {
            this.scrollOffset = 0.0f;
        }
        return handled;
    }

    public boolean isAnimating() {
        return this.openAnim.getValue() > 0.01f;
    }

    public void setOpen(boolean open) {
        this.open = open;
        if (!open) {
            this.scrollOffset = 0.0f;
            this.searchField.setFocused(false);
            this.searchField.clear();
        }
    }

    @Override
    public void unfocus() {
        if (this.open && this.host != null) {
            this.host.closeDropdown(this);
        }
    }
}
