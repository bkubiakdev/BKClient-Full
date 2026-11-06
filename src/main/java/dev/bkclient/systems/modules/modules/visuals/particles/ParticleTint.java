package dev.bkclient.systems.modules.modules.visuals.particles;

import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

/**
 * Shared colour resolution for the particle modules.
 *
 * <p>Hit Particles and World Particles offer the same three colour modes and are expected to look
 * like the same effect in two places, so the maths lives here rather than being written twice and
 * drifting apart.
 */
public final class ParticleTint {

    /** Declaration order must match the modules' colour mode settings, which are matched by index. */
    public enum Mode {
        CLIENT,
        RAINBOW,
        CUSTOM
    }

    /** Length of one full colour cycle. Slow enough to read as drift rather than flicker. */
    private static final long CYCLE_MS = 4000L;

    /** How far the client accent is allowed to travel towards white across a group. */
    private static final float CLIENT_SPREAD = 0.35F;

    private ParticleTint() {
    }

    public static int argb(Mode mode, ColorRGBA custom, ColorRGBA second, boolean useSecond,
                           float gradient, float alpha) {
        ColorRGBA base = switch (mode) {
            case RAINBOW -> ColorRGBA.fromHSB((cycle() + gradient) % 1.0F, 0.85F, 1.0F);
            case CUSTOM -> useSecond ? custom.mix(second, wave(gradient)) : custom;
            // The accent alone across a whole group looks flat, so each particle sits at a
            // slightly different point between the accent and white.
            case CLIENT -> Colors.getAccentColor().mix(Colors.WHITE, wave(gradient) * CLIENT_SPREAD);
        };
        return base.mulAlpha(alpha).getRGB();
    }

    /** Resolves a mode setting's selected index to a mode, defaulting to the client accent. */
    public static Mode byIndex(int index) {
        Mode[] modes = Mode.values();
        return index < 0 || index >= modes.length ? Mode.CLIENT : modes[index];
    }

    /** Position in the colour cycle, so a group drifts instead of sitting on one colour. */
    public static float cycle() {
        return (System.currentTimeMillis() % CYCLE_MS) / (float) CYCLE_MS;
    }

    /** Triangle wave over a group, offset in time so even a settled group keeps shimmering. */
    public static float wave(float gradient) {
        float t = (gradient + cycle()) % 1.0F;
        return t < 0.5F ? t * 2.0F : 2.0F - t * 2.0F;
    }
}
