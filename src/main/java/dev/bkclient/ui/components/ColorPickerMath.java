package dev.bkclient.ui.components;

/** Normalized HSV/HSL conversion; independent of rendering and Minecraft. */
public final class ColorPickerMath {
    private ColorPickerMath() {}

    public static float[] hsvToHsl(float saturation, float value) {
        float light = value * (1 - saturation / 2);
        float hslSaturation = light <= 0 || light >= 1 ? 0 : (value - light) / Math.min(light, 1 - light);
        return new float[]{clamp(hslSaturation), clamp(light)};
    }

    public static float[] hslToHsv(float saturation, float light) {
        float value = light + saturation * Math.min(light, 1 - light);
        float hsvSaturation = value <= 0 ? 0 : 2 * (1 - light / value);
        return new float[]{clamp(hsvSaturation), clamp(value)};
    }

    public static float parseChannel(String text, float maximum) {
        float value = Float.parseFloat(text.trim().replace("%", ""));
        if (!Float.isFinite(value) || value < 0 || value > maximum) throw new IllegalArgumentException("Channel outside range");
        return value;
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
}
