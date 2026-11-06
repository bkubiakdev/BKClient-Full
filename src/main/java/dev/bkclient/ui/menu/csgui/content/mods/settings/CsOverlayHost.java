/*
 * CsOverlayHost — kontrakt dla elementów, które rysują pływające nakładki na wierzchu GUI
 * (color-picker oraz dropdown SelectSetting). Implementuje go ModsContent.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

public interface CsOverlayHost extends ColorPickerHost {
    void openDropdown(CsSelectRow row);

    void closeDropdown(CsSelectRow row);
}
