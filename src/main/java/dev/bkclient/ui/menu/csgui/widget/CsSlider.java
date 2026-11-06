/*
 * CsSlider — przeciągalny slider w stylu CsGui (do użycia w różnych miejscach).
 * Nazwa po lewej, czerwony chip z wartością po prawej, tor + wypełnienie + knob.
 */
package dev.bkclient.ui.menu.csgui.widget;

import java.util.function.Function;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.colors.ColorRGBA;

public class CsSlider {

    private static final float NAME_FONT_SIZE = CsTheme.Slider.NAME_FONT;
    private static final float VALUE_FONT_SIZE = CsTheme.Slider.VALUE_FONT;
    private static final float TRACK_H = CsTheme.Slider.TRACK_H;
    private static final float KNOB_SIZE = CsTheme.Slider.KNOB;
    private static final float CHIP_H = CsTheme.Slider.CHIP_H;
    private static final float CHIP_PAD = CsTheme.Slider.CHIP_PAD;
    private static final float TRACK_OFFSET = CsTheme.Slider.TRACK_OFFSET;

    private static final ColorRGBA NAME_COLOR = CsTheme.Color.TEXT_75;
    private static final ColorRGBA TRACK_COLOR = CsTheme.Color.DIVIDER;
    private static final ColorRGBA KNOB_COLOR = CsTheme.Color.TEXT;
    private static final ColorRGBA CHIP_TEXT = CsTheme.Color.BLACK_75;

    private final String name;
    private final double min;
    private final double max;
    private double value;
    private final Function<Double, String> formatter;

    private float trackX, trackY, trackW;
    private boolean dragging;
    private boolean focused;
    private double displayedValue;
    private long lastFrame;
    private boolean clickSysSounds;

    public CsSlider(String name, double min, double max, double value, Function<Double, String> formatter) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.value = value;
        this.formatter = formatter;
    }

    public float render(UIContext context, float x, float y, float w, float alpha) {
        var theme = dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.current();
        boolean flat = dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.active();
        this.clickSysSounds = dev.bkclient.ui.clicksys.ClickSysMaterial.context() != null;
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 1 : (now - lastFrame) / 1_000_000_000.0;
        displayedValue = dt > .25 ? value : displayedValue + (value - displayedValue) * (1 - Math.exp(-14 * dt));
        lastFrame = now;
        ColorRGBA nameColor = flat ? theme.text : NAME_COLOR;
        ColorRGBA accent = flat ? theme.accent : CsTheme.Color.accent();
        ColorRGBA chipText = flat ? theme.muted : CHIP_TEXT;
        Font nameFont = Fonts.SEMIBOLD.getFont(NAME_FONT_SIZE + 1);
        Font valFont = Fonts.SEMIBOLD.getFont(VALUE_FONT_SIZE + 0.5f);

        if (flat) dev.bkclient.ui.clicksys.AutoMarketControls.icon(context,
                dev.bkclient.ui.clicksys.port.module.CategoryType.SETTING_SLIDER, x, y + 1, 7, theme.muted.getRGB(), alpha);
        context.drawText(nameFont, dev.bkclient.systems.localization.Localizator.translate(this.name), x + (flat ? 10 : 0), y + 1, nameColor.withAlpha(255.0f * 0.75f * alpha));

        String valStr = this.formatter.apply(this.value);
        float chipW = valFont.width(valStr) + CHIP_PAD * 2.0f - 5;
        float chipX = x + w - chipW;
        float chipY = y - 1.0f;

        if (!flat) context.drawRoundedRect(chipX, chipY, chipW, CHIP_H, BorderRadius.all(2.5f),
                accent.mulAlpha(alpha));
        context.drawCenteredText(valFont, valStr, chipX + chipW / 2.0f + 0.5f, chipY + (CHIP_H - valFont.height()) / 2.0f,
                chipText.withAlpha(255.0f * 0.75f * alpha));

        this.trackX = x;
        this.trackY = y + TRACK_OFFSET;
        this.trackW = w;
        float progress = (float) ((this.displayedValue - this.min) / Math.max(0.000001, this.max - this.min));
        progress = Math.clamp(progress, 0.0f, 1.0f);

        if (flat) {
            context.draw();
            dev.bkclient.ui.clicksys.ClickSysMaterial.slider(dev.bkclient.ui.clicksys.AutoMarketControls.matrix(context),
                this.trackX * 2, this.trackY * 2, this.trackW * 2, progress, alpha);
        } else {
            dev.bkclient.ui.components.SettingControls.slider(context, this.trackX, this.trackY, this.trackW, progress, 0, alpha);
        }

        return this.trackY + TRACK_H;
    }

    private void updateValue(double mouseX) {
        if (this.trackW <= 0 || !Double.isFinite(mouseX)) return;
        double p = Math.clamp((mouseX - this.trackX) / this.trackW, 0.0, 1.0);
        double previous = this.value;
        this.value = this.min + p * (this.max - this.min);
        if (clickSysSounds && previous != this.value) dev.bkclient.ui.clicksys.ClickSysSounds.slider();
    }

    public boolean onClick(double mouseX, double mouseY) {
        if (mouseX >= this.trackX && mouseX <= this.trackX + this.trackW
                && mouseY >= this.trackY - 5.0f && mouseY <= this.trackY + 5.0f) {
            this.dragging = true;
            this.focused = true;
            this.updateValue(mouseX);
            return true;
        }
        return false;
    }

    public void onRelease() {
        this.dragging = false;
    }

    public boolean onDrag(double mouseX) {
        if (!this.dragging) {
            return false;
        }
        this.updateValue(mouseX);
        return true;
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        if (!this.dragging) {
            this.value = Math.clamp(value, this.min, this.max);
        }
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public boolean isFocused() {
        return this.focused;
    }

    public void unfocus() {
        this.focused = false;
        this.dragging = false;
    }
}
