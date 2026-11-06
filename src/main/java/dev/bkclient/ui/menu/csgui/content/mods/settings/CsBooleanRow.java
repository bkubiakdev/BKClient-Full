/*
 * CsBooleanRow — checkbox (kwadrat) dla BooleanSetting. OFF: ciemny kontur. ON: czerwone tło + biały X.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;

public class CsBooleanRow implements CsSetting {

    private static final float HEIGHT = 14.0f;
    private static final float BOX = 8.0f;      // Figma: 16

    private final BooleanSetting setting;
    private final Animation anim = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
    private final CsNameLabel nameLabel = new CsNameLabel();

    private float bx, by;

    public CsBooleanRow(BooleanSetting setting) {
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
        boolean on = this.setting.isEnabled();
        this.anim.setDuration(dev.bkclient.systems.modules.modules.visuals.Interface.animDuration(200L));
        this.anim.update(on ? 1.0f : 0.0f);
        float centerY = y + HEIGHT / 2.0f;

        this.bx = x + w - BOX;
        this.by = centerY - BOX / 2.0f;

        Font font = Fonts.SEMIBOLD.getFont(CsTheme.Slider.NAME_FONT + 1.0f);
        boolean hovered = context.getMouseX() >= x && context.getMouseX() <= x + w
                && context.getMouseY() >= y && context.getMouseY() <= y + HEIGHT;
        this.nameLabel.render(context, font, Localizator.translate(this.setting.getName()), x, centerY,
                this.bx - x - 5.0f, hovered, CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        BorderRadius r = BorderRadius.all(2.0f);  // Figma: 4
        float t = this.anim.getValue();  // 0 = off, 1 = on (płynnie)
        ColorRGBA bg = CsTheme.Color.SUB_BORDER.mix(CsTheme.Color.accent(), t);
        context.drawRoundedRect(this.bx, this.by, BOX, BOX, r,
                bg.withAlpha(bg.getAlpha() * alpha));
        if (t > 0.01f) {
            float ic = 5.5f * t;  // skala 0→pełna
            context.drawIcon(Fonts.ICONS, 'l', this.bx + (BOX - ic) / 2.0f, this.by + (BOX - ic) / 2.0f, ic,
                    CsTheme.Color.BLACK_75.withAlpha(255.0f * 0.75f * t * alpha));
        }
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return false;
        }
        // cały wiersz klikalny (jak w starym GUI); ModuleCard dispatchuje tylko gdy klik jest w paśmie wiersza
        this.setting.toggle();
        return true;
    }
}
