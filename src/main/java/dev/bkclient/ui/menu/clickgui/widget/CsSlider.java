package dev.bkclient.ui.menu.clickgui.widget;

import java.util.function.Function;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

/** Compact slider shared by the AutoMarket editor and styled by the active Liquid theme. */
public final class CsSlider {
    private static final float TRACK_H = 2.0f;
    private static final float KNOB_W = 11.0f;
    private static final float KNOB_H = 7.0f;
    private static final float TRACK_OFFSET = 11.0f;

    private final Animation position = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final String name;
    private final double min;
    private final double max;
    private final Function<Double, String> formatter;
    private double value;
    private float trackX;
    private float trackY;
    private float trackW;
    private boolean dragging;
    private boolean focused;
    private final Animation moving = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

    public CsSlider(String name, double min, double max, double value, Function<Double, String> formatter) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.value = value;
        this.position.setValue((float)((value - min) / Math.max(0.00001, max - min)));
        this.formatter = formatter;
    }

    public float render(UIContext context, float x, float y, float width, float alpha) {
        return this.render(context, x, y, width, alpha, true);
    }

    public float render(UIContext context, float x, float y, float width, float alpha, boolean visible) {
        this.trackX = x;
        this.trackY = y + TRACK_OFFSET;
        this.trackW = width;
        if (this.dragging) this.updateValue(context.getMouseX());
        float progress = (float)((this.value - this.min) / Math.max(0.00001, this.max - this.min));
        if (this.dragging) this.position.setValue(Math.clamp(progress, 0.0F, 1.0F));
        else this.position.update(Math.clamp(progress, 0.0F, 1.0F));
        this.moving.update(this.dragging ? 1.0F : 0.0F);
        if (!visible) return this.trackY + TRACK_H;
        Font nameFont = Fonts.REGULAR.getFont(7.0f);
        Font valueFont = Fonts.REGULAR.getFont(7.0f);
        ColorRGBA text = Colors.getTextColor();
        ColorRGBA accent = Colors.getAccent();
        context.drawText(nameFont, dev.bkclient.systems.localization.Localizator.translate(this.name), x, y + 1.0f, text.mulAlpha(0.75f * alpha));

        String valueText = this.formatter.apply(this.value);
        context.drawRightText(valueFont, valueText, x + width, y + 1.0f,
                text.mulAlpha(0.75f * alpha));

        this.trackX = x;
        this.trackY = y + TRACK_OFFSET;
        this.trackW = width;
        dev.bkclient.ui.components.SettingControls.slider(context, this.trackX, this.trackY, width,
            this.position.getValue(), this.moving.getValue(), alpha);
        return this.trackY + TRACK_H;
    }

    /** Styling-style single row; retains this slider's existing input/value handling. */
    public float renderRow(UIContext context, float x, float y, float width, float alpha, boolean visible) {
        float valueWidth = Math.max(26, Fonts.REGULAR.getFont(6).width(this.formatter.apply(this.value)) + 8);
        this.trackW = Math.max(24, Math.min(58, width * .27F));
        this.trackX = x + width - valueWidth - this.trackW;
        this.trackY = y + 8;
        if (this.dragging) this.updateValue(context.getMouseX());
        float progress = (float)((this.value - this.min) / Math.max(.00001, this.max - this.min));
        if (this.dragging) this.position.setValue(Math.clamp(progress, 0, 1));
        else this.position.update(Math.clamp(progress, 0, 1));
        if (visible) {
            MarketSettingRow.draw(context, this.name, x, y, width, this.trackX - x - 22, alpha);
            dev.bkclient.ui.components.SettingControls.cleanSlider(context, this.trackX, this.trackY,
                    this.trackW, 0, this.position.getValue(), false, alpha);
            context.drawRightText(Fonts.REGULAR.getFont(6), this.formatter.apply(this.value), x + width - 5,
                    y + 6, Colors.getTextColor().mulAlpha(.6F * alpha));
        }
        return y + 19;
    }

    private void updateValue(float mouseX) {
        float progress = Math.clamp((mouseX - this.trackX) / Math.max(1.0f, this.trackW), 0.0f, 1.0f);
        this.value = this.min + progress * (this.max - this.min);
    }

    public boolean onClick(double mouseX, double mouseY) {
        if (mouseX >= this.trackX && mouseX <= this.trackX + this.trackW
                && mouseY >= this.trackY - 5.0f && mouseY <= this.trackY + 5.0f) {
            this.dragging = true;
            this.focused = true;
            this.updateValue((float) mouseX);
            return true;
        }
        return false;
    }

    public void onRelease() {
        this.dragging = false;
    }

    public boolean onDrag(double mouseX) {
        if (!this.dragging) return false;
        this.updateValue((float) mouseX);
        return true;
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        if (!this.dragging) this.value = Math.clamp(value, this.min, this.max);
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
