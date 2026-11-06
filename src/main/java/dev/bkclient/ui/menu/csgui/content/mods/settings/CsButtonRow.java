/*
 * CsButtonRow — ButtonSetting: nazwa po lewej, kompaktowy czerwony przycisk "Click" po prawej.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;

public class CsButtonRow implements CsSetting {

    private static final float HEIGHT = 14.0f;
    private static final float BTN_W = 34.0f;   // Figma: 68
    private static final float BTN_H = 8.0f;    // Figma: 16
    private static final String BTN_LABEL = "Click";

    private final ButtonSetting setting;
    private final CsNameLabel nameLabel = new CsNameLabel();
    private float bx, by, bw;

    public CsButtonRow(ButtonSetting setting) {
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

    @Override
    public void render(UIContext context, float x, float y, float w, float alpha) {
        float centerY = y + HEIGHT / 2.0f;
        Font font = Fonts.SEMIBOLD.getFont(CsTheme.Slider.NAME_FONT + 1.0f);

        Font bf = Fonts.SEMIBOLD.getFont(6.0f);
        this.bw = BTN_W;
        this.bx = x + w - this.bw;
        this.by = centerY - BTN_H / 2.0f;

        boolean hovered = context.getMouseX() >= x && context.getMouseX() <= x + w
                && context.getMouseY() >= y && context.getMouseY() <= y + HEIGHT;
        this.nameLabel.render(context, font, Localizator.translate(this.setting.getName()), x, centerY,
                this.bx - x - 5.0f, hovered, CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        context.drawRoundedRect(this.bx, this.by, this.bw, BTN_H, BorderRadius.all(2.0f),
                CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * alpha));
        context.drawCenteredText(bf, BTN_LABEL, this.bx + this.bw / 2.0f, this.by + (BTN_H - bf.height()) / 2.0f,
                CsTheme.Color.BLACK_75.withAlpha(255.0f * 0.75f * alpha));
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return false;
        }
        if (mouseX >= this.bx && mouseX <= this.bx + this.bw
                && mouseY >= this.by - 2.0f && mouseY <= this.by + BTN_H + 2.0f) {
            if (this.setting.getAction() != null) {
                this.setting.getAction().run();
            }
            return true;
        }
        return false;
    }
}
