package dev.bkclient.ui.menu.clickgui;

import dev.bkclient.ui.menu.clickgui.components.GuiSearchOverlay;

public final class GuiSearchTest {
    public static void main(String[] args) {
        check(GuiSearchOverlay.rank("KillAura", "", "kill aura"), 0);
        check(GuiSearchOverlay.rank("Target ESP", "", "target"), 1);
        check(GuiSearchOverlay.rank("Auto Crystal", "", "crystal"), 2);
        check(GuiSearchOverlay.rank("Range", "Attack distance", "distance"), 3);
        check(GuiSearchOverlay.rank("Range", "Attack distance", "xyz"), -1);
        check(GuiSearchOverlay.rank("Aura", "", "  "), -1);
        check(GuiSearchOverlay.rank("Przezroczystość", null, "przezroczystosc"), 0);
        check(GuiSearchOverlay.rank(null, null, "test"), -1);
        check(GuiSearchOverlay.rank("HUD-Glow", "", "hud glow"), 0);
        System.out.println("GUI search: ranking, spaces, casing, diacritics and empty input passed");
    }
    private static void check(int actual, int expected) {
        if (actual != expected) throw new AssertionError(actual + " != " + expected);
    }
}
