/*
 * CsSettingFactory — mapuje Setting na odpowiedni wiersz CsSetting (inline w karcie modułu).
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.RangeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.StringSetting;

public final class CsSettingFactory {

    private CsSettingFactory() {
    }

    public static CsSetting create(Setting setting, CsOverlayHost host) {
        if (setting instanceof SliderSetting s) {
            return new CsSliderRow(s);
        }
        if (setting instanceof RangeSetting r) {
            return new CsRangeRow(r);
        }
        if (setting instanceof BooleanSetting b) {
            return new CsBooleanRow(b);
        }
        if (setting instanceof ModeSetting m) {
            return new CsModeRow(m);
        }
        if (setting instanceof SelectSetting s) {
            return new CsSelectRow(s, host);
        }
        if (setting instanceof BindSetting b) {
            return new CsBindRow(b);
        }
        if (setting instanceof ColorSetting c) {
            return new CsColorRow(c, host);
        }
        if (setting instanceof ButtonSetting b) {
            return new CsButtonRow(b);
        }
        if (setting instanceof StringSetting s) {
            return new CsStringRow(s);
        }
        return new CsGenericRow(setting);
    }
}
