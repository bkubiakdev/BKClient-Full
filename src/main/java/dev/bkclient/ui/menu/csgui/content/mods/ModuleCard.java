/*
 * ModuleCard — karta modułu w kategorii (Mods/Visuals/Player/Misc).
 * Szapka: toggle (lewa), ikona, nazwa, kwadrat bindu (prawa od nazwy), chevron.
 * Rozwinięta (domyślnie): pod szapką renderowane są wiersze ustawień (CsSetting).
 * Skala ÷2 jak całe CsGui.
 */
package dev.bkclient.ui.menu.csgui.content.mods;

import java.util.ArrayList;
import java.util.List;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsSetting;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsSettingFactory;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsRangeRow;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsSliderRow;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsBooleanRow;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsBindRow;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsSelectRow;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsModeRow;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsButtonRow;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsColorRow;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsStringRow;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.render.ScissorUtility;

public class ModuleCard {

    public static final float HEADER_H = 19.0f;
    private static final float RADIUS = 6.0f;
    private static final float PAD = 6.0f;
    private static final float NAME_FONT = 7.5f;
    private static final float CHEVRON_SIZE = 8.5f;

    private static final float TG_W = 12.0f;
    private static final float TG_H = 7.0f;
    private static final float TG_KNOB = 4.5f;

    private static final float HEADER_DIV_GAP = -2.0f;  // odstęp szapka → separator (ujemny: bliżej nazwy)
    private static final float SETTINGS_TOP = 1.0f;    // odstęp separatora pod szapką do 1. wiersza
    private static final float AFTER_ROW = 1.0f;       // odstęp od wiersza do separatora
    private static final float AFTER_DIV = 1.0f;       // odstęp od separatora do następnego wiersza

    private final Module module;
    private final List<CsSetting> rows = new ArrayList<>();

    private final Animation toggleAnim = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
    private final Animation hoverAnim = new Animation(180L, Easing.FIGMA_EASE_IN_OUT);
    private final Animation expandAnim = new Animation(240L, Easing.FIGMA_EASE_IN_OUT);
    private final Animation appearAnim = new Animation(260L, Easing.FIGMA_EASE_IN_OUT);
    private long appearStart;   // ms — moment startu animacji pojawienia (ze staggerem)

    private boolean expanded = false;    // karty zwinięte domyślnie — rozwijasz klikiem
    private boolean bindingMode;

    private float cardX, cardY, cardW;
    private float toggleX, toggleY;
    private float chipX, chipY, chipW;
    private boolean chipShown;
    private float headerRight; // x prawej krawędzi szapki (chevron region)

    private final List<float[]> renderedRowRects = new ArrayList<>(); // [x,y,w,h]
    private final List<CsSetting> renderedRows = new ArrayList<>();

    public ModuleCard(Module module,
            dev.bkclient.ui.menu.csgui.content.mods.settings.CsOverlayHost host) {
        this.module = module;
        for (Setting s : module.getDisplaySettings()) {
            CsSetting row = CsSettingFactory.create(s, host);
            if (row instanceof CsSliderRow || row instanceof CsRangeRow || row instanceof CsBooleanRow
                    || row instanceof CsBindRow || row instanceof CsSelectRow
                    || row instanceof CsModeRow || row instanceof CsButtonRow || row instanceof CsColorRow
                    || row instanceof CsStringRow) {
                this.rows.add(row);
            }
        }
    }

    /** Wyzwala animację pojawienia karty z opóźnieniem (stagger) — np. przy zmianie kategorii. */
    public void triggerAppear(long delayMs) {
        this.appearStart = System.currentTimeMillis() + delayMs;
        this.appearAnim.setValue(0.0f);
    }

    private float appearValue() {
        if (dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(260L) == 0L) return 1.0f;
        long now = System.currentTimeMillis();
        if (now < this.appearStart) {
            return 0.0f;  // jeszcze czeka na swój czas (stagger)
        }
        this.appearAnim.setDuration(dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(260L));
        return this.appearAnim.update(1.0f);
    }

    private float settingsBlockHeight() {
        java.util.List<CsSetting> vis = new ArrayList<>();
        for (CsSetting row : this.rows) {
            if (row.isVisible()) {
                vis.add(row);
            }
        }
        if (vis.isEmpty()) {
            return 0.0f;
        }
        float h = HEADER_DIV_GAP + 1.0f + SETTINGS_TOP;  // odstęp + separator + odstęp do 1. wiersza
        for (int i = 0; i < vis.size(); ++i) {
            h += vis.get(i).getHeight();
            if (i < vis.size() - 1) {
                // separator tylko MIĘDZY wierszami (nie po ostatnim)
                h += AFTER_ROW + 1.0f + AFTER_DIV;
            }
        }
        h += 3.0f;  // dolny margines po ostatnim wierszu
        return h;
    }

    public float getHeight() {
        float extra = this.settingsBlockHeight() * this.expandAnim.getValue();
        return HEADER_H + extra;
    }

    public void render(UIContext context, float x, float y, float w, float alpha) {
        float appear = this.appearValue();
        alpha *= appear;
        float slideY = (1.0f - appear) * 6.0f;
        boolean sliding = appear < 0.999f;
        if (sliding) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0f, slideY, 0.0f);
        }

        this.cardX = x;
        this.cardY = y;
        this.cardW = w;

        boolean on = this.module.isEnabled();
        this.toggleAnim.setDuration(dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(220L));
        this.toggleAnim.update(on ? 1.0f : 0.0f);
        boolean hover = this.isHeaderHovered(context.getMouseX(), context.getMouseY());
        this.hoverAnim.setDuration(dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(180L));
        this.hoverAnim.update(hover ? 1.0f : 0.0f);
        this.expandAnim.setDuration(dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(240L));
        this.expandAnim.update(this.expanded ? 1.0f : 0.0f);

        float h = this.getHeight();
        BorderRadius r = BorderRadius.all(RADIUS);
        context.drawRoundedRect(x, y, w, h, r, CsTheme.Color.SUB_FILL
                .withAlpha(CsTheme.Color.SUB_FILL.getAlpha() * alpha));
        float tintA = (on ? 0.03f : 0.0f) + 0.025f * this.hoverAnim.getValue();
        if (tintA > 0.0f) {
            context.drawRoundedRect(x, y, w, h, r,
                    new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * tintA).withAlpha(255.0f * tintA * alpha));
        }
        context.drawRoundedBorder(x, y, w, h, 0.05f, r,
                CsTheme.Color.SUB_BORDER.withAlpha(CsTheme.Color.SUB_BORDER.getAlpha() * alpha));

        float centerY = y + HEADER_H / 2.0f;

        this.toggleX = x + PAD;
        this.toggleY = centerY - TG_H / 2.0f;
        this.miniToggle(context, this.toggleX, centerY, this.toggleAnim.getValue(), alpha);

        Font nameFont = Fonts.SEMIBOLD.getFont(NAME_FONT);
        String name = this.module.getName();
        float nameX = this.toggleX + TG_W + 6.0f;
        ColorRGBA nameColor = on ? ColorRGBA.WHITE : CsTheme.Color.TEXT_75;
        context.drawText(nameFont, name, nameX, centerY - nameFont.height() / 2.0f,
                nameColor.withAlpha(nameColor.getAlpha() * alpha));

        float chevSize = CHEVRON_SIZE;
        this.headerRight = x + w - PAD - chevSize;
        char chev = this.expanded ? 'h' : 'h';
        ColorRGBA chevColor;
        if (on) {
            chevColor = CsTheme.Color.accent().withAlpha(255.0f * alpha);
        } else {
            chevColor = CsTheme.Color.TEXT_25.withAlpha(255.0f * (0.25f + 0.25f * this.hoverAnim.getValue()) * alpha);
        }
        context.drawIcon(Fonts.ICONS, chev, this.headerRight, centerY - chevSize / 2.0f, chevSize, chevColor);

        int key = this.module.getKey();
        this.chipShown = key != -1 || this.bindingMode;
        if (this.chipShown) {
            Font keyFont = Fonts.SEMIBOLD.getFont(5.5f);
            String label = this.bindingMode ? "..." : TextUtility.getKeyName(key);
            float chipH = 8.0f;  // Figma: 16
            this.chipW = Math.max(chipH, keyFont.width(label) + 6.0f);
            this.chipX = nameX + nameFont.width(name) + 5.0f;
            this.chipY = centerY - chipH / 2.0f + 0.3f;
            BorderRadius cr = BorderRadius.all(2.0f);  // Figma: 4
            context.drawRoundedRect(this.chipX, this.chipY, this.chipW, chipH, cr,
                    CsTheme.Color.SUB_FILL.withAlpha(CsTheme.Color.SUB_FILL.getAlpha() * alpha));
            context.drawRoundedBorder(this.chipX, this.chipY, this.chipW, chipH, 0.05f, cr,
                    CsTheme.Color.SUB_BORDER.withAlpha(255.0f * 0.05f * alpha));
            ColorRGBA col = this.bindingMode ? CsTheme.Color.accent() : CsTheme.Color.TEXT_25;
            context.drawCenteredText(keyFont, label, this.chipX + this.chipW / 2.0f + 0.5f,
                    this.chipY + (chipH - keyFont.height()) / 2.0f,
                    col.withAlpha((this.bindingMode ? 255.0f : 255.0f * 0.25f) * alpha));
        }
        this.renderedRowRects.clear();
        this.renderedRows.clear();
        if (this.expandAnim.getValue() > 0.01f && !this.rows.isEmpty()) {
            float rowsAlpha = alpha * this.expandAnim.getValue();
            float cx = x + PAD;
            float cw = w - PAD * 2.0f;
            float divY = y + HEADER_H + HEADER_DIV_GAP;
            float clipTop = Math.min(y + HEADER_H, divY) - 1.0f;
            ScissorUtility.push(context.getMatrices(), x, clipTop, w, Math.max(0.0f, y + h - clipTop));
            context.drawRect(cx, divY, cw, 1.0f, CsTheme.Color.SEPARATOR.withAlpha(255.0f * 0.06f * rowsAlpha));

            float cy = divY + 1.0f + SETTINGS_TOP;
            java.util.List<CsSetting> vis = new ArrayList<>();
            for (CsSetting row : this.rows) {
                if (row.isVisible()) {
                    vis.add(row);
                }
            }
            for (int i = 0; i < vis.size(); ++i) {
                CsSetting row = vis.get(i);
                float rh = row.getHeight();
                if (row instanceof CsSelectRow sr) {
                    sr.setOwnerEnabled(on);
                }
                row.render(context, cx, cy, cw, rowsAlpha);
                this.renderedRowRects.add(new float[] { cx, cy, cw, rh });
                this.renderedRows.add(row);
                cy += rh + AFTER_ROW;
                if (i < vis.size() - 1) {
                    // separator tylko MIĘDZY wierszami (nie po ostatnim)
                    context.drawRect(cx, cy, cw, 1.0f, CsTheme.Color.SEPARATOR.withAlpha(255.0f * 0.06f * rowsAlpha));
                    cy += 1.0f + AFTER_DIV;
                }
            }
            ScissorUtility.pop();
        }

        if (sliding) {
            context.getMatrices().pop();
        }
    }

    private void miniToggle(UIContext context, float x, float centerY, float t, float alpha) {
        float y = centerY - TG_H / 2.0f + 0.4f;
        boolean on = t > 0.5f;
        ColorRGBA bg = on ? CsTheme.Color.accent() : CsTheme.Color.TOGGLE_OFF;
        context.drawRoundedRect(x, y, TG_W, TG_H, BorderRadius.all(TG_H / 3.0f),
                bg.withAlpha(bg.getAlpha() * alpha));
        float knobX = x + 1.5f + (TG_W - TG_KNOB - 3.0f) * t;
        float knobY = y + (TG_H - TG_KNOB) / 2.0f;
        ColorRGBA knobColor = on ? ColorRGBA.WHITE : CsTheme.Color.KNOB_OFF;
        context.drawRoundedRect(knobX, knobY, TG_KNOB, TG_KNOB, BorderRadius.all(TG_KNOB / 2.0f),
                knobColor.withAlpha(knobColor.getAlpha() * alpha));
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= this.cardX && mouseX <= this.cardX + this.cardW
                && mouseY >= this.cardY && mouseY <= this.cardY + this.getHeight();
    }

    private boolean isHeaderHovered(double mouseX, double mouseY) {
        return mouseX >= this.cardX && mouseX <= this.cardX + this.cardW
                && mouseY >= this.cardY && mouseY <= this.cardY + HEADER_H;
    }

    /** @return true jeśli kliknięcie obsłużone. */
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (this.isHeaderHovered(mouseX, mouseY)) {
            if (button == MouseButton.MIDDLE) {
                this.bindingMode = true;
                return true;
            }
            if (button == MouseButton.LEFT) {
                if (mouseX >= this.toggleX && mouseX <= this.toggleX + TG_W
                        && mouseY >= this.toggleY - 2.0f && mouseY <= this.toggleY + TG_H + 2.0f) {
                    this.module.toggle();
                    return true;
                }
                if (this.chipShown && mouseX >= this.chipX && mouseX <= this.chipX + this.chipW) {
                    this.bindingMode = !this.bindingMode;
                    return true;
                }
                this.expanded = !this.expanded;
                return true;
            }
            if (button == MouseButton.RIGHT) {
                this.expanded = true;
                return true;
            }
            return false;
        }
        if (this.expanded) {
            for (int i = 0; i < this.renderedRowRects.size(); ++i) {
                float[] b = this.renderedRowRects.get(i);
                if (mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                    CsSetting clicked = this.renderedRows.get(i);
                    // Move keyboard focus to the clicked row so only it receives ←/→ arrows.
                    for (CsSetting row : this.rows) {
                        if (row != clicked) {
                            row.unfocus();
                        }
                    }
                    return clicked.handleClick(mouseX, mouseY, button);
                }
            }
        }
        return false;
    }

    public void handleRelease() {
        for (CsSetting row : this.rows) {
            row.handleRelease();
        }
    }

    public boolean isBindingMode() {
        return this.bindingMode;
    }

    public boolean isCapturing() {
        if (this.bindingMode) {
            return true;
        }
        for (CsSetting row : this.rows) {
            if (row.isCapturingInput()) {
                return true;
            }
        }
        return false;
    }

    public void clearCapture() {
        this.bindingMode = false;
        for (CsSetting row : this.rows) {
            row.unfocus();
        }
    }

    public void applyMouseBind(MouseButton button) {
        if (this.bindingMode) {
            this.module.setKey(button.getButtonIndex());
            this.bindingMode = false;
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.bindingMode) {
            if (keyCode == 256 || keyCode == 261) {
                this.module.setKey(-1);
            } else {
                this.module.setKey(keyCode);
            }
            this.bindingMode = false;
            return true;
        }
        for (CsSetting row : this.rows) {
            if (row.isCapturingInput() && row.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char chr) {
        for (CsSetting row : this.rows) {
            if (row.isCapturingInput() && row.charTyped(chr)) {
                return true;
            }
        }
        return false;
    }

    public Module getModule() {
        return this.module;
    }
}
