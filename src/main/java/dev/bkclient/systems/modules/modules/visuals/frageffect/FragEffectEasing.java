package dev.bkclient.systems.modules.modules.visuals.frageffect;

import net.minecraft.util.math.MathHelper;

public final class FragEffectEasing {
    private FragEffectEasing() {
    }

    public static float quintOut(float value) {
        float t = 1.0f - MathHelper.clamp(value, 0.0f, 1.0f);
        return 1.0f - t * t * t * t * t;
    }

    public static float sineOut(float value) {
        return (float) Math.sin(MathHelper.clamp(value, 0.0f, 1.0f) * Math.PI / 2.0D);
    }

    public static float sineInOut(float value) {
        return -(float) (Math.cos(Math.PI * MathHelper.clamp(value, 0.0f, 1.0f)) - 1.0D) / 2.0f;
    }

    public static float expoInOut(float value) {
        float t = MathHelper.clamp(value, 0.0f, 1.0f);
        if (t <= 0.0f) return 0.0f;
        if (t >= 1.0f) return 1.0f;
        return t < 0.5f
                ? (float) Math.pow(2.0D, 20.0f * t - 10.0f) / 2.0f
                : (2.0f - (float) Math.pow(2.0D, -20.0f * t + 10.0f)) / 2.0f;
    }
}
