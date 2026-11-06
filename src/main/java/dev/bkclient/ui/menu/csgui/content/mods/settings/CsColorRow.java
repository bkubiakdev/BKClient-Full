/*
 * CsColorRow — ColorSetting: nazwa po lewej, swatch (tinted pill) z hexem po prawej.
 * Klik → otwiera pływające okno color-pickera (CsColorPicker) przez ColorPickerHost.
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
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.colors.ColorRGBA;

public class CsColorRow implements CsSetting {

    private static final float HEIGHT = 17.0f;
    private static final float SW_H = 10.5f;
    private static final float SW_PAD_X = 5.0f;
    private static final float SW_RADIUS = 3.0f;
    private static final float FONT = 6.5f;
    private static final float HEX_FONT = 6.5f;

    private final ColorSetting setting;
    private final ColorPickerHost host;
    private float rowX, rowY, rowW;
    private float swX, swY, swW;

    public CsColorRow(ColorSetting setting, ColorPickerHost host) {
        this.setting = setting;
        this.host = host;
    }

    @Override
    public Setting getSetting() {
        return this.setting;
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    private ColorRGBA color() {
        ColorRGBA c = this.setting.getColor();
        return c != null ? c : CsTheme.Color.accent();
    }

    @Override
    public void render(UIContext context, float x, float y, float w, float alpha) {
        this.rowX = x;
        this.rowY = y;
        this.rowW = w;

        float centerY = y + HEIGHT / 2.0f;
        Font font = Fonts.SEMIBOLD.getFont(FONT);
        context.drawText(font, Localizator.translate(this.setting.getName()), x,
                centerY - font.height() / 2.0f, CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        ColorRGBA c = this.color();
        String hex = String.format(Locale.US, "#%02X%02X%02X", (int) c.getRed(), (int) c.getGreen(), (int) c.getBlue());
        Font hf = Fonts.SEMIBOLD.getFont(HEX_FONT);
        this.swW = Math.max(36.5f, hf.width(hex) + SW_PAD_X * 2.0f);
        this.swX = x + w - this.swW;
        this.swY = centerY - SW_H / 2.0f;
        context.drawRoundedRect(this.swX, this.swY, this.swW, SW_H, BorderRadius.all(SW_RADIUS),
                new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), 255.0f * alpha));
        context.drawCenteredText(hf, hex, this.swX + this.swW / 2.0f, this.swY + (SW_H - hf.height()) / 2.0f,
                CsTheme.Color.BLACK_75.withAlpha(255.0f * 0.75f * alpha));
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT && button != MouseButton.RIGHT) {
            return false;
        }
        boolean swatchHovered = mouseX >= this.swX && mouseX <= this.swX + this.swW
                && mouseY >= this.swY - 2.0f && mouseY <= this.swY + SW_H + 2.0f;
        boolean rowRightClicked = button == MouseButton.RIGHT
                && mouseX >= this.rowX && mouseX <= this.rowX + this.rowW
                && mouseY >= this.rowY && mouseY <= this.rowY + HEIGHT;
        if (swatchHovered || rowRightClicked) {
            if (this.host != null) {
                this.host.openColorPicker(this.setting, (float) mouseX, (float) mouseY);
            }
            return true;
        }
        return false;
    }
}
