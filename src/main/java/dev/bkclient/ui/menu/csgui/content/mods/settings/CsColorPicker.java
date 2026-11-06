/**
 * Przeciągany selektor koloru zapisujący zmiany bezpośrednio do ColorSetting.
 * ColorRGBA ma historycznie zamienione gettery nasycenia i jasności;
 * lokalne sat/bri zachowują kolejność wymaganą przez fromHSB.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import java.util.Locale;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.framework.objects.gradient.Gradient;
import dev.bkclient.framework.objects.gradient.impl.HorizontalGradient;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;

public class CsColorPicker {

    private static final float W = 88.0f;
    private static final float PAD = 4.0f;
    private static final float SB_H = 42.0f;
    private static final float HUE_H = 3.0f;
    private static final float GAP = 5.0f;
    private static final float ALPHA_H = 9.0f;
    private static final float BOTTOM_H = 14.0f;
    private static final float RADIUS = 8.0f;
    private static final float H_BASE = PAD + SB_H + GAP + HUE_H + GAP + BOTTOM_H + PAD;
    private static final float H_ALPHA = H_BASE + GAP + ALPHA_H + 8.0f;

    private final ColorSetting setting;
    private final Animation showAnim = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);

    private float x, y;
    private float hue, sat, bri;
    private float alpha; // 0..1, zachowywany (bez suwaka, jak w referencji)

    private boolean dragSB, dragHue, dragAlpha, dragWindow;
    private float dragOffX, dragOffY;
    private boolean closing;

    public CsColorPicker(ColorSetting setting, float x, float y) {
        this.setting = setting;
        this.x = x;
        this.y = y;
        ColorRGBA c = setting.getColor() != null ? setting.getColor() : CsTheme.Color.accent();
        this.hue = c.getHue();
        this.sat = c.getBrightness(); // zamienione gettery — patrz nagłówek
        this.bri = c.getSaturation();
        this.alpha = c.getAlpha() / 255.0f;
    }

    public ColorSetting getSetting() {
        return this.setting;
    }

    public boolean isClosing() {
        return this.closing;
    }

    public boolean isDead() {
        return this.closing && this.showAnim.getValue() <= 0.01f;
    }

    public void close() {
        this.closing = true;
    }

    private ColorRGBA built() {
        return ColorRGBA.fromHSB(this.hue, this.sat, this.bri).withAlpha(255.0f * this.alpha);
    }

    private void apply() {
        this.setting.setColor(this.built());
    }

    public float width() {
        return W;
    }

    public float height() {
        return this.setting.isAlpha() ? H_ALPHA : H_BASE;
    }

    public void render(UIContext context, float screenAlpha) {
        this.showAnim.setDuration(dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(220L));
        this.showAnim.update(this.closing ? 0.0f : 1.0f);
        float a = this.showAnim.getValue() * screenAlpha;
        if (a <= 0.01f) {
            return;
        }

        if (this.dragWindow) {
            this.x = context.getMouseX() - this.dragOffX;
            this.y = context.getMouseY() - this.dragOffY;
        }
        if (this.dragSB) {
            this.updateSB(context.getMouseX(), context.getMouseY());
        }
        if (this.dragHue) {
            this.updateHue(context.getMouseX());
        }
        if (this.dragAlpha) {
            this.updateAlpha(context.getMouseX());
        }

        BorderRadius r = BorderRadius.all(RADIUS);
        float h = this.height();
        ColorRGBA pickerGlass = new ColorRGBA(21.0f, 21.0f, 21.0f, 255.0f * 0.6f);
        ColorRGBA pickerFill = new ColorRGBA(21.0f, 21.0f, 21.0f, 255.0f * 0.9f);
        ColorRGBA pickerBorder = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.15f);
        if (dev.bkclient.systems.modules.modules.visuals.Interface.shadow())
            context.drawShadow(this.x, this.y, W, h, 10.0f, r,
                    ColorRGBA.BLACK.withAlpha(255.0f * 0.5f * a));
        if (dev.bkclient.systems.modules.modules.visuals.Interface.blurGui())
            context.drawBlurredRect(this.x, this.y, W, h, 27.5f, 2.0f, r,
                    pickerGlass.withAlpha(pickerGlass.getAlpha() * a));
        context.drawRoundedRect(this.x, this.y, W, h, r,
                pickerFill.withAlpha(pickerFill.getAlpha() * a));
        context.drawRoundedBorder(this.x, this.y, W, h, 0.5f, r,
                pickerBorder.withAlpha(pickerBorder.getAlpha() * a));

        float sbX = this.x + PAD;
        float sbY = this.y + PAD;
        float sbW = W - PAD * 2.0f;
        ColorRGBA hueColor = ColorRGBA.fromHSB(this.hue, 1.0f, 1.0f);
        // topLeft=white, bottomLeft=black, topRight=hue, bottomRight=black
        context.drawRoundedRect(sbX, sbY, sbW, SB_H, BorderRadius.all(5.0f),
                Gradient.of(ColorRGBA.WHITE.withAlpha(255.0f * a), ColorRGBA.BLACK.withAlpha(255.0f * a),
                        hueColor.withAlpha(255.0f * a), ColorRGBA.BLACK.withAlpha(255.0f * a)));
        float cursorX = sbX + this.sat * sbW;
        float cursorY = sbY + (1.0f - this.bri) * SB_H;
        cursorX = Math.clamp(cursorX, sbX + 4.0f, sbX + sbW - 4.0f);
        cursorY = Math.clamp(cursorY, sbY + 4.0f, sbY + SB_H - 4.0f);
        float ring = 3.5f;
        context.drawRoundedRect(cursorX - ring, cursorY - ring, ring * 2.0f, ring * 2.0f, BorderRadius.all(ring),
                ColorRGBA.WHITE.withAlpha(255.0f * a));
        context.drawRoundedRect(cursorX - ring + 1.0f, cursorY - ring + 1.0f, (ring - 1.0f) * 2.0f,
                (ring - 1.0f) * 2.0f, BorderRadius.all(ring - 1.0f),
                ColorRGBA.fromHSB(this.hue, this.sat, this.bri).withAlpha(255.0f * a));

        float hueX = this.x + PAD;
        float hueY = sbY + SB_H + GAP;
        float hueW = W - PAD * 2.0f;
        int seg = 12;
        for (int i = 0; i < seg; ++i) {
            float h0 = (float) i / seg;
            float h1 = (float) (i + 1) / seg;
            ColorRGBA c0 = ColorRGBA.fromHSB(h0, 1.0f, 1.0f).withAlpha(255.0f * a);
            ColorRGBA c1 = ColorRGBA.fromHSB(h1, 1.0f, 1.0f).withAlpha(255.0f * a);
            float segX = hueX + hueW * i / seg;
            float segW = hueW / seg + 0.6f;
            BorderRadius br = i == 0 ? new BorderRadius(HUE_H / 2.0f, 0.0f, HUE_H / 2.0f, 0.0f)
                    : (i == seg - 1 ? new BorderRadius(0.0f, HUE_H / 2.0f, 0.0f, HUE_H / 2.0f) : BorderRadius.all(0.0f));
            context.drawRoundedRect(segX, hueY, segW, HUE_H, br, new HorizontalGradient(c0, c1));
        }
        float hk = hueX + Math.clamp(this.hue, 0.0f, 1.0f) * hueW;
        float hknobW = 2.0f;
        float hknobH = 6.0f;
        hk = Math.clamp(hk, hueX + hknobW / 2.0f, hueX + hueW - hknobW / 2.0f);
        context.drawRoundedRect(hk - hknobW / 2.0f, hueY + HUE_H / 2.0f - hknobH / 2.0f, hknobW, hknobH,
                BorderRadius.all(1.0f), ColorRGBA.WHITE.withAlpha(255.0f * a));

        float bottomY = hueY + HUE_H + GAP;
        if (this.setting.isAlpha()) {
            float alphaX = this.x + PAD;
            float alphaY = bottomY;
            float alphaW = W - PAD * 2.0f;
            ColorRGBA rgb = ColorRGBA.fromHSB(this.hue, this.sat, this.bri);
            context.drawRoundedRect(alphaX, alphaY, alphaW, ALPHA_H, BorderRadius.all(3.0f),
                    new HorizontalGradient(rgb.withAlpha(0.0f), rgb.withAlpha(255.0f * a)));
            context.drawRoundedBorder(alphaX, alphaY, alphaW, ALPHA_H, 0.35f, BorderRadius.all(3.0f),
                    ColorRGBA.WHITE.withAlpha(255.0f * 0.14f * a));

            float ak = alphaX + Math.clamp(this.alpha, 0.0f, 1.0f) * alphaW;
            float knob = 7.0f;
            ak = Math.clamp(ak, alphaX + knob / 2.0f, alphaX + alphaW - knob / 2.0f);
            context.drawRoundedRect(ak - knob / 2.0f, alphaY + ALPHA_H / 2.0f - knob / 2.0f, knob, knob,
                    BorderRadius.all(knob / 2.0f), ColorRGBA.WHITE.withAlpha(255.0f * a));
            context.drawRoundedRect(ak - knob / 2.0f + 1.0f, alphaY + ALPHA_H / 2.0f - knob / 2.0f + 1.0f,
                    knob - 2.0f, knob - 2.0f, BorderRadius.all((knob - 2.0f) / 2.0f),
                    this.built().withAlpha(255.0f * a));

            Font pf = Fonts.SEMIBOLD.getFont(5.5f);
            String percent = (int) (this.alpha * 100.0f) + "%";
            context.drawCenteredText(pf, percent, alphaX + alphaW / 2.0f,
                    alphaY + (ALPHA_H - pf.height()) / 2.0f,
                    ColorRGBA.WHITE.withAlpha(255.0f * 0.8f * a));
            bottomY += ALPHA_H + GAP + 8.0f;
        }

        ColorRGBA cur = this.built();
        String hex = String.format(Locale.US, "#%02X%02X%02X", (int) cur.getRed(), (int) cur.getGreen(),
                (int) cur.getBlue());
        Font hf = Fonts.SEMIBOLD.getFont(7.0f);
        float hexW = hf.width(hex);
        float hexX = this.x + W - PAD - hexW;
        context.drawText(hf, hex, hexX, bottomY + (BOTTOM_H - hf.height()) / 2.0f,
                CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * a));
        float dot = 7.0f;
        float dotCx = hexX - 5.0f - dot / 2.0f;
        context.drawRoundedRect(dotCx - dot / 2.0f, bottomY + BOTTOM_H / 2.0f - dot / 2.0f, dot, dot,
                BorderRadius.all(dot / 2.0f), cur.withAlpha(255.0f * a));
        context.drawIcon(Fonts.ICONS, 'b', this.x + PAD, bottomY + BOTTOM_H / 2.0f - 4.0f, 8.0f,
                CsTheme.Color.TEXT_25.withAlpha(255.0f * 0.25f * a));
    }

    private void updateSB(float mx, float my) {
        float sbX = this.x + PAD;
        float sbY = this.y + PAD;
        float sbW = W - PAD * 2.0f;
        this.sat = Math.clamp((mx - sbX) / sbW, 0.0f, 1.0f);
        this.bri = Math.clamp(1.0f - (my - sbY) / SB_H, 0.0f, 1.0f);
        this.apply();
    }

    private void updateHue(float mx) {
        float hueX = this.x + PAD;
        float hueW = W - PAD * 2.0f;
        this.hue = Math.clamp((mx - hueX) / hueW, 0.0f, 1.0f);
        this.apply();
    }

    private void updateAlpha(float mx) {
        float alphaX = this.x + PAD;
        float alphaW = W - PAD * 2.0f;
        this.alpha = Math.clamp((mx - alphaX) / alphaW, 0.0f, 1.0f);
        this.apply();
    }

    public boolean isHovered(double mx, double my) {
        return mx >= this.x && mx <= this.x + W && my >= this.y && my <= this.y + this.height();
    }

    /** @return true jeśli klik obsłużony przez picker. */
    public boolean onClick(double mx, double my, MouseButton button) {
        if (!this.isHovered(mx, my)) {
            return false;
        }
        if (button != MouseButton.LEFT) {
            return true;
        }
        float sbX = this.x + PAD, sbY = this.y + PAD, sbW = W - PAD * 2.0f;
        if (mx >= sbX && mx <= sbX + sbW && my >= sbY && my <= sbY + SB_H) {
            this.dragSB = true;
            this.updateSB((float) mx, (float) my);
            return true;
        }
        float hueY = sbY + SB_H + GAP;
        if (mx >= sbX && mx <= sbX + sbW && my >= hueY - 3.0f && my <= hueY + HUE_H + 3.0f) {
            this.dragHue = true;
            this.updateHue((float) mx);
            return true;
        }
        float alphaY = hueY + HUE_H + GAP;
        if (this.setting.isAlpha() && mx >= sbX && mx <= sbX + sbW
                && my >= alphaY - 2.0f && my <= alphaY + ALPHA_H + 2.0f) {
            this.dragAlpha = true;
            this.updateAlpha((float) mx);
            return true;
        }
        this.dragWindow = true;
        this.dragOffX = (float) (mx - this.x);
        this.dragOffY = (float) (my - this.y);
        return true;
    }

    public void onRelease() {
        this.dragSB = false;
        this.dragHue = false;
        this.dragAlpha = false;
        this.dragWindow = false;
    }
}
