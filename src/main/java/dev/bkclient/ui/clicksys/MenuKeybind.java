package dev.bkclient.ui.clicksys;

/** Shared modifier rules for the Settings popup and the module key handler. */
public final class MenuKeybind {
    private MenuKeybind() {}
    public static int normalizeModifiers(int key, int modifiers) {
        if (key < 0) return 0;
        if (key == 340 || key == 344) modifiers &= ~1;
        if (key == 341 || key == 345) modifiers &= ~2;
        if (key == 342 || key == 346) modifiers &= ~4;
        return modifiers & 7;
    }
    public static boolean accepts(int required, int held) { return (held & required) == required; }
    public static String format(String label, boolean symbols) {
        return symbols ? label.replace("CTRL + ", "^ ").replace("ALT + ", "/ ").replace("SHIFT + ", "+ ") : label;
    }
}
