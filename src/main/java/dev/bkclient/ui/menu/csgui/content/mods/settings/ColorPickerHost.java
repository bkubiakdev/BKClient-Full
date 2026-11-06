/*
 * ColorPickerHost — kontrakt pozwalający wierszowi koloru poprosić o otwarcie
 * pływającego okna color-pickera (rysowanego na wierzchu przez ModsContent/CsGuiScreen).
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import dev.bkclient.systems.setting.settings.ColorSetting;

public interface ColorPickerHost {
    void openColorPicker(ColorSetting setting, float anchorX, float anchorY);
}
