package dev.bkclient.ui.menu.clickgui;

import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

public final class ClickGuiV2ThemeTest {
    public static void main(String[] args) throws Exception {
        require(!ClickGuiV2Theme.active(), "No palette outside V2 rendering");
        ColorRGBA accent = new ColorRGBA(36, 125, 218);
        try (var light = ClickGuiV2Theme.open(false, accent)) {
            require(Colors.getTextColor().getRGB() == ColorRGBA.BLACK.getRGB(), "Light text is black");
            require(Colors.getAccent().getRGB() == accent.getRGB(), "V2 has its own accent");
            require(contrast(light.surface, light.text) > 7, "Readable light cards");
            require(contrast(light.surface, light.muted) > 4.5, "Readable light secondary text");
            require(contrast(light.selection, light.text) > 7, "Readable selected light tab");
            Thread worker = new Thread(() -> require(!ClickGuiV2Theme.active(), "Palette does not leak across threads"));
            java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
            worker.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
            worker.start(); worker.join();
            require(failure.get() == null, "Background thread keeps global palette");
            try {
                try (var dark = ClickGuiV2Theme.open(true, accent)) {
                    require(ClickGuiV2Theme.current() == dark, "Nested dark palette is active");
                    require(Colors.getBackgroundColor() == dark.field, "Shared fields use V2 palette");
                    require(contrast(dark.surface, dark.text) > 7, "Readable dark cards");
                    require(contrast(dark.surface, dark.muted) > 4.5, "Readable dark secondary text");
                    require(contrast(dark.selection, dark.text) > 7, "Readable selected dark tab");
                    throw new IllegalStateException("simulate interrupted rendering");
                }
            } catch (IllegalStateException expected) {
                require(ClickGuiV2Theme.current() == light, "Exception restores previous palette");
            }
        }
        require(!ClickGuiV2Theme.active(), "Closing V2 scope restores global rendering");
        System.out.println("V2 theme: contrast, shared palette, thread isolation and exception restoration passed");
    }
    private static double channel(float value) {
        double v = value / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
    private static double luminance(ColorRGBA color) {
        return .2126 * channel(color.getRed()) + .7152 * channel(color.getGreen()) + .0722 * channel(color.getBlue());
    }
    private static double contrast(ColorRGBA a, ColorRGBA b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x,y) + .05) / (Math.min(x,y) + .05);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
