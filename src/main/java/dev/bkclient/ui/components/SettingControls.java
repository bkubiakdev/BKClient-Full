package dev.bkclient.ui.components;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

/** Shared control shapes for module settings and the market editor. */
public final class SettingControls {
    private SettingControls() {}

    public static void buttonBackground(UIContext context, float x, float y, float width, float height, float hover, float alpha) {
        context.drawRoundedRect(x, y, width, height, BorderRadius.all(6.0F),
            Colors.getBackgroundColor().withAlpha(255.0F * (0.3F + 0.2F * hover) * alpha));
    }

    public static boolean cleanOptions() {
        return ClickGuiV2Theme.current() != null && ClickGuiV2Theme.current().liquid;
    }

    /** Option controls share the old 26px hit area, with a compact checkbox aligned right. */
    public static void option(UIContext context, float x, float y, float position, float motion, float alpha) {
        if (!cleanOptions()) { toggle(context, x, y, position, motion, alpha); return; }
        float progress = Math.clamp(position, 0, 1);
        ColorRGBA ink = ClickGuiV2Theme.current().dark ? ColorRGBA.WHITE : ColorRGBA.BLACK;
        ColorRGBA inverse = ClickGuiV2Theme.current().dark ? ColorRGBA.BLACK : ColorRGBA.WHITE;
        float boxX = x + 16;
        dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(), boxX, y, 10, 10,
            BorderRadius.all(3), ink.mulAlpha((0.25F + 0.75F * progress) * alpha));
        dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(), boxX + 0.7F, y + 0.7F, 8.6F, 8.6F,
            BorderRadius.all(2.3F), inverse.mix(ink, progress).mulAlpha(alpha));
        // Fade through a short quiet midpoint so the two symbols never form a cluttered overlay.
        float checkAlpha = Math.clamp((progress - 0.35F) / 0.65F, 0, 1);
        float crossAlpha = Math.clamp((0.65F - progress) / 0.65F, 0, 1);
        optionGlyph(context, LucideIcons.CHECK, boxX, y, checkAlpha, -18 * (1-progress), inverse.mulAlpha(checkAlpha * alpha));
        optionGlyph(context, LucideIcons.CROSS, boxX, y, crossAlpha, 18 * progress, ink.mulAlpha(0.55F * crossAlpha * alpha));
    }

    private static void optionGlyph(UIContext context, LucideIcons icon, float x, float y, float visibility, float angle, ColorRGBA color) {
        if (visibility <= 0.001F) return;
        context.pushMatrix();
        try {
            context.getMatrices().translate(x + 5, y + 5, 0);
            context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            float scale = 0.78F + 0.22F * visibility;
            context.getMatrices().scale(scale, scale, 1);
            icon.draw(context, -4, -4, 8, color);
        } finally { context.popMatrix(); }
    }

    public static void cleanSlider(UIContext context, float x, float y, float width, float start, float end, boolean range, float alpha) {
        MusicSlider.draw(context, x, y, width, start, end, range, true, alpha);
    }

    public static void toggle(UIContext context, float x, float y, float position, float motion, float alpha) {
        toggleTrack(context, x, y, position, alpha);
        toggleThumb(context, x, y, position, motion, alpha);
    }

    public static void toggleTrack(UIContext context, float x, float y, float position, float alpha) {
        if (cleanOptions()) {
            var palette = ClickGuiV2Theme.current();
            ColorRGBA off = palette.dark ? new ColorRGBA(65, 69, 70) : new ColorRGBA(192, 196, 198);
            ColorRGBA on = palette.dark ? new ColorRGBA(231, 234, 233) : new ColorRGBA(39, 43, 44);
            dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(),
                x + 2, y - .5F, 22, 11, BorderRadius.all(5.5F),
                off.mix(on, Math.clamp(position, 0, 1)).mulAlpha(alpha));
            return;
        }
        if (ClickGuiV2Theme.active()) {
            var palette = ClickGuiV2Theme.current();
            ColorRGBA off = palette.dark ? new ColorRGBA(77, 79, 85) : new ColorRGBA(207, 210, 216);
            ColorRGBA track = off.mix(palette.accent, Math.clamp(position, 0, 1));
            dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(), x + 4, y + .5F, 18, 9,
                BorderRadius.all(4.5F), track.mulAlpha(alpha));
            return;
        }
        float progress = Math.clamp(position, 0.0F, 1.0F);
        ColorRGBA background = new ColorRGBA(173.0F, 173.0F, 173.0F)
            .mix(new ColorRGBA(0.0F, 216.0F, 32.0F), progress).mulAlpha(0.8F * alpha);
        context.drawSquircle(x, y, 26.0F, 10.0F, 3.0F, BorderRadius.all(4.0F), background);
    }

    public static void toggleThumb(UIContext context, float x, float y, float position, float motion, float alpha) {
        if (cleanOptions()) {
            float progress = Math.clamp(position, 0, 1);
            float diameter = 7.4F;
            float cx = x + 7.5F + 11 * progress;
            ColorRGBA off = new ColorRGBA(240, 242, 242);
            ColorRGBA on = ClickGuiV2Theme.current().dark ? new ColorRGBA(35, 39, 40) : ColorRGBA.WHITE;
            dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(),
                cx - diameter / 2, y + 5 - diameter / 2, diameter, diameter,
                BorderRadius.all(diameter / 2), off.mix(on, progress).mulAlpha(alpha));
            return;
        }
        if (ClickGuiV2Theme.active()) {
            ColorRGBA knob = ColorRGBA.WHITE;
            dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(),
                x + 5.25F + 9 * Math.clamp(position, 0, 1), y + 1.75F, 6.5F, 6.5F, BorderRadius.all(3.25F), knob.mulAlpha(alpha));
            return;
        }
        float progress = Math.clamp(position, 0.0F, 1.0F);
        float width = 14.0F + 6.0F * motion;
        float height = 7.0F + 4.0F * motion;
        float thumbX = x + 1.0F + 10.0F * progress - 3.0F * motion;
        float thumbY = y + (10.0F - height) / 2.0F;
        if (motion > 0.01F && !ClickGuiV2Theme.active()) {
            context.drawRoundedTexture(BKClientInit.id("textures/boolean/glassbutton.png"), thumbX, thumbY, width, height, BorderRadius.all(4.0F));
        } else {
            context.drawSquircle(thumbX, thumbY, width, height, 7.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE.mulAlpha(alpha));
        }
    }

    public static void slider(UIContext context, float x, float y, float width, float progress, float motion, float alpha) {
        MusicSlider.draw(context, x, y, width, 0, progress, false, true, alpha);
    }
}
