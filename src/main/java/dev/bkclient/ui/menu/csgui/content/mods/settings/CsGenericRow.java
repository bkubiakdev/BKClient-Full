/*
 * CsGenericRow — fallback dla typów jeszcze nie obsłużonych inline (Number/Range/Bezier/ItemList).
 * Pokazuje nazwę oraz wartość NumberSetting; ten wiersz nie obsługuje edycji.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import java.util.Locale;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.NumberSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;

public class CsGenericRow implements CsSetting {

    private static final float HEIGHT = 12.0f;
    private static final float FONT = 6.5f;

    private final Setting setting;

    public CsGenericRow(Setting setting) {
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
        Font font = Fonts.SEMIBOLD.getFont(FONT);
        context.drawText(font, Localizator.translate(this.setting.getName()), x,
                centerY - font.height() / 2.0f, CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        String right;
        if (this.setting instanceof NumberSetting n) {
            right = n.isInteger() ? String.valueOf(n.getIntValue()) : String.format(Locale.US, "%.2f", n.getValue());
        } else {
            right = "—";
        }
        context.drawRightText(font, right, x + w, centerY - font.height() / 2.0f,
                CsTheme.Color.TEXT_25.withAlpha(255.0f * 0.25f * alpha));
    }
}
