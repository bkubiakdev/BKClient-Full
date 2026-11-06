/*
 * CsSetting — wspólny interfejs wiersza ustawienia wewnątrz rozwiniętej karty modułu.
 * Każdy typ Setting ma swoją implementację (slider, checkbox, mode, select, bind, color, button, string).
 * Konwencja jak w CsGui: render(x,y,w,alpha) + bounds zapamiętywane przy renderze; handleClick zwraca consumed.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.setting.Setting;

public interface CsSetting {

    Setting getSetting();

    /** Widoczność (live — zależy od hideCondition). */
    default boolean isVisible() {
        return this.getSetting().isVisible();
    }

    /** Wysokość wiersza (dynamiczna, liczona co klatkę). */
    float getHeight();

    void render(UIContext context, float x, float y, float w, float alpha);

    /** @return true jeśli kliknięcie obsłużone. */
    default boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        return false;
    }

    default void handleRelease() {
    }

    /** @return true jeśli klawisz pochłonięty (np. edycja text-boxa / przechwyt bindu). */
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(char chr) {
        return false;
    }

    /** Czy ten wiersz właśnie przechwytuje klawiaturę (text-box focus / bind listening). */
    default boolean isCapturingInput() {
        return false;
    }

    default void unfocus() {
    }
}
