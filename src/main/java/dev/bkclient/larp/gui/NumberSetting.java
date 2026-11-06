package dev.bkclient.larp.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import dev.bkclient.larp.gui.Setting;

public class NumberSetting
extends Setting {
    public final double min;
    public final double max;
    public final double step;
    public final String suffix;
    public double value;
    private int barX;
    private int barWidth = 1;

    public NumberSetting(String id, String name, double value, double min, double max, double step, String suffix) {
        super(id, name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.value = this.clamp(value);
    }

    @Override
    public int getHeight() {
        return 21;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int x, int y, int width, boolean hovered) {
        this.drawFrame(ctx, x, y, width, hovered);
        ctx.drawText(tr, this.name, x + 15, y + 3, -8090209, false);
        String label = this.format(this.value) + this.suffix;
        ctx.drawText(tr, label, x + width - 9 - tr.getWidth(label), y + 3, -1643790, false);
        this.barX = x + 15;
        this.barWidth = width - 24;
        int barY = y + 14;
        ctx.fill(this.barX, barY, this.barX + this.barWidth, barY + 3, -13288632);
        int filled = (int)Math.round((this.value - this.min) / (this.max - this.min) * (double)this.barWidth);
        ctx.fill(this.barX, barY, this.barX + filled, barY + 3, -11760129);
        int knob = this.barX + Math.min(this.barWidth - 2, Math.max(0, filled - 1));
        ctx.fill(knob, barY - 2, knob + 3, barY + 5, -1643790);
    }

    @Override
    public void click(double mouseX, int button) {
        this.setFromMouse(mouseX);
    }

    public void setFromMouse(double mouseX) {
        double pct = (mouseX - (double)this.barX) / (double)this.barWidth;
        pct = Math.max(0.0, Math.min(1.0, pct));
        double raw = this.min + pct * (this.max - this.min);
        this.value = this.clamp((double)Math.round(raw / this.step) * this.step);
        this.changed();
    }

    public void scroll(double amount) {
        double next = this.clamp(this.value + Math.signum(amount) * this.step);
        if (next != this.value) {
            this.value = next;
            this.changed();
        }
    }

    public void setValue(double value) { this.value = clamp(value); changed(); }
    public double minimum() { return min; }
    public double maximum() { return max; }
    public double increment() { return step; }
    private double clamp(double v) {
        double c = Math.max(this.min, Math.min(this.max, v));
        return BigDecimal.valueOf(c).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private String format(double v) {
        if (this.step >= 1.0 && v == Math.rint(v)) {
            return String.valueOf((long)v);
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(this.value);
    }

    @Override
    public void load(JsonElement el) {
        if (el != null && el.isJsonPrimitive()) {
            this.value = this.clamp(el.getAsDouble());
        }
    }
}
