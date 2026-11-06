/*
 * CsSliderRow — slider dla SliderSetting (nazwa, czerwony chip z wartością, tor + wypełnienie + knob).
 * Wizualnie 1:1 z CsSlider, ale czyta/zapisuje realny SliderSetting. Drag bez zapisu, zapis na release.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import java.util.Locale;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;

public class CsSliderRow implements CsSetting {

    private static final float HEIGHT = 14.0f;      // Figma chip h:12 → 6, z odstępami
    private static final float TRACK_H = 2.0f;      // Figma: 2
    private static final float KNOB = 5.0f;
    private static final float CHIP_H = 8.0f;       // Figma: 12
    private static final float CHIP_MIN_W = 14.0f;  // Figma: 21
    private static final float GAP = 6.0f;          // odstęp tor↔chip
    private static final float NAME_GAP = 12.0f;    // odstęp nazwa↔tor (większy)

    private final SliderSetting setting;
    private float trackX, trackY, trackW;
    private boolean dragging;
    // Focused = this slider listens to ←/→ arrows for fine-tuning. Set on click, cleared on unfocus.
    private boolean focused;

    public CsSliderRow(SliderSetting setting) {
        this.setting = setting;
    }

    @Override
    public Setting getSetting() {
        return this.setting;
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    private String valueString() {
        float v = this.setting.getCurrentValue();
        String num = this.setting.getStep() >= 1.0f
                ? String.valueOf((int) v)
                : String.format(Locale.US, "%." + decimals(this.setting.getStep()) + "f", v);
        return num + this.setting.getSuffix();
    }

    /** One decimal as before; finer steps (0.01, 0.0001) show their own precision. */
    private static int decimals(float step) {
        if (!(step > 0.0f)) return 1;
        int scale = new java.math.BigDecimal(Float.toString(step)).stripTrailingZeros().scale();
        return Math.max(1, Math.min(4, scale));
    }

    @Override
    public void render(UIContext context, float x, float y, float w, float alpha) {
        Font nameFont = Fonts.SEMIBOLD.getFont(CsTheme.Slider.NAME_FONT + 1.0f); // Figma 11 → 6
        float centerY = y + HEIGHT / 2.0f;

        String name = Localizator.translate(this.setting.getName());
        context.drawText(nameFont, name, x, centerY - nameFont.height() / 2.0f ,
                CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        String valStr = this.valueString();
        Font chipFont = Fonts.SEMIBOLD.getFont(6.0f);
        float chipH = 7.0f;
        float chipW = Math.max(CHIP_MIN_W, chipFont.width(valStr) + 8.0f);
        float chipX = x + w - chipW + 3.3f;
        float chipY = centerY - chipH / 2.0f;
        context.drawRoundedRect(chipX, chipY, chipW - 4 , chipH, BorderRadius.all(2f),
                CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * alpha));
        context.drawCenteredText(chipFont, valStr , chipX + chipW / 2.0f - 1.5f, chipY + (chipH - chipFont.height()) / 2.0f,
                CsTheme.Color.BLACK_75.withAlpha(255.0f * 0.75f * alpha));

        this.trackX = x + nameFont.width(name) + NAME_GAP;
        this.trackY = centerY - TRACK_H / 2.0f + 0.5f;
        this.trackW = chipX - GAP - this.trackX;
        if (this.trackW < 4.0f) {
            this.trackW = 4.0f;
        }
        float min = this.setting.getMin();
        float max = this.setting.getMax();
        float progress = max > min ? (this.setting.getCurrentValue() - min) / (max - min) : 0.0f;
        progress = Math.clamp(progress, 0.0f, 1.0f);

        dev.bkclient.ui.components.SettingControls.slider(context, this.trackX, this.trackY, this.trackW, progress, 0, alpha);

    }

    private void updateValue(float mouseX) {
        float p = Math.clamp((mouseX - this.trackX) / this.trackW, 0.0f, 1.0f);
        float min = this.setting.getMin();
        float max = this.setting.getMax();
        this.setting.setCurrentValue(min + p * (max - min));
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return false;
        }
        if (mouseX >= this.trackX && mouseX <= this.trackX + this.trackW
                && mouseY >= this.trackY - 6.0f && mouseY <= this.trackY + 6.0f) {
            this.dragging = true;
            this.focused = true; // grab keyboard focus so ←/→ can fine-tune after clicking
            this.updateValue((float) mouseX);
            return true;
        }
        return false;
    }

    @Override
    public void handleRelease() {
        if (this.dragging) {
            this.dragging = false;
            this.setting.setCurrentValueAndSave(this.setting.getCurrentValue());
        }
    }

    /** Reports keyboard capture so ModuleCard routes ←/→ here while this slider is focused. */
    @Override
    public boolean isCapturingInput() {
        return this.focused;
    }

    @Override
    public void unfocus() {
        this.focused = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Right (262) / Left (263) nudge by exactly one step, snapped to the step grid (50 -> 51,
        // not 50.7) and clamped to min/max. Returns true so the key is consumed.
        if (!this.focused || (keyCode != 262 && keyCode != 263)) {
            return false;
        }
        float step = this.setting.getStep();
        if (step <= 0.0f) {
            step = 1.0f;
        }
        float dir = (keyCode == 262) ? 1.0f : -1.0f;
        float next = Math.round((this.setting.getCurrentValue() + step * dir) / step) * step;
        next = Math.clamp(next, this.setting.getMin(), this.setting.getMax());
        this.setting.setCurrentValueAndSave(next);
        return true;
    }
}
