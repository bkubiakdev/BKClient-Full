package dev.bkclient.ui.menu.csgui.content.mods.settings;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.systems.setting.settings.RangeSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.game.TextUtility;

public class CsRangeRow implements CsSetting {

    private static final float HEIGHT = 28.0f;
    private static final float TRACK_H = 2.0f;
    private static final float KNOB = 6.0f;

    private final RangeSetting setting;
    private float trackX, trackY, trackW;
    private boolean dragFirst, dragSecond;

    public CsRangeRow(RangeSetting setting) {
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
        Font nameFont = Fonts.SEMIBOLD.getFont(7.0f);
        Font valueFont = Fonts.SEMIBOLD.getFont(6.5f);

        float first = this.setting.getFirstValue();
        float second = this.setting.getSecondValue();
        float left = Math.min(first, second);
        float right = Math.max(first, second);
        String value = "from " + TextUtility.formatNumber(left) + " to " + TextUtility.formatNumber(right);

        context.drawText(nameFont, Localizator.translate(this.setting.getName()), x, y + 2.0f,
                CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));
        context.drawRightText(valueFont, value, x + w, y + 2.5f,
                CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        this.trackX = x + 2.0f;
        this.trackY = y + 20.0f;
        this.trackW = w - 4.0f;

        float firstP = this.percent(first);
        float secondP = this.percent(second);
        float startP = Math.min(firstP, secondP);
        float endP = Math.max(firstP, secondP);
        dev.bkclient.ui.components.SettingControls.cleanSlider(context, this.trackX, this.trackY, this.trackW,
                startP, endP, true, alpha);

        if (this.dragFirst) {
            this.setting.setFirstValue(this.valueFromMouse(context.getMouseX()));
        } else if (this.dragSecond) {
            this.setting.setSecondValue(this.valueFromMouse(context.getMouseX()));
        }
    }

    private float percent(float value) {
        float min = this.setting.getMin();
        float max = this.setting.getMax();
        return max > min ? Math.clamp((value - min) / (max - min), 0.0f, 1.0f) : 0.0f;
    }

    private float valueFromMouse(float mouseX) {
        float p = Math.clamp((mouseX - this.trackX) / this.trackW, 0.0f, 1.0f);
        return this.setting.getMin() + p * (this.setting.getMax() - this.setting.getMin());
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return false;
        }
        if (mouseX < this.trackX || mouseX > this.trackX + this.trackW
                || mouseY < this.trackY - 7.0f || mouseY > this.trackY + 7.0f) {
            return false;
        }
        float firstX = this.trackX + this.trackW * this.percent(this.setting.getFirstValue());
        float secondX = this.trackX + this.trackW * this.percent(this.setting.getSecondValue());
        this.dragFirst = Math.abs(mouseX - firstX) <= Math.abs(mouseX - secondX);
        this.dragSecond = !this.dragFirst;
        if (this.dragFirst) {
            this.setting.setFirstValue(this.valueFromMouse((float) mouseX));
        } else {
            this.setting.setSecondValue(this.valueFromMouse((float) mouseX));
        }
        return true;
    }

    @Override
    public void handleRelease() {
        if (this.dragFirst || this.dragSecond) {
            AbstractSetting.saveConfig();
        }
        this.dragFirst = false;
        this.dragSecond = false;
    }
}
