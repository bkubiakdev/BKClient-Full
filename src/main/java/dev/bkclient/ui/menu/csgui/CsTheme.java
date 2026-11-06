/**
 * Wspólne kolory i wymiary CsGui.
 * Wymiary odpowiadają połowie wartości projektu Figma 1920×1080 (GUI scale 2).
 */
package dev.bkclient.ui.menu.csgui;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.utility.colors.ColorRGBA;

public final class CsTheme {

    private CsTheme() {
    }

    public static final class Color {
        /** Główny akcent — dynamiczny, czyta z Interface.accentColor. */
        public static ColorRGBA accent() {
            return BKClientInit.getInstance().getModuleManager()
                    .getModule(Interface.class).getAccentColor().getColor();
        }
        /** Stałe fallbacki (używaj accent() zamiast ACCENT w renderze). */
        public static final ColorRGBA ACCENT      = rgba(255, 65, 65, 1.00f);
        public static final ColorRGBA ACCENT_GLOW = rgba(255, 65, 65, 0.50f);
        public static final ColorRGBA ACCENT_SOFT = rgba(255, 65, 65, 0.18f);

        /** Tekst — różne poziomy jasności. */
        public static final ColorRGBA TEXT = rgba(255, 255, 255, 1.00f);
        public static final ColorRGBA TEXT_75 = rgba(255, 255, 255, 0.75f);
        public static final ColorRGBA TEXT_50 = rgba(255, 255, 255, 0.50f);
        public static final ColorRGBA TEXT_25 = rgba(255, 255, 255, 0.25f);

        public static final ColorRGBA BLACK_75 = rgba(0, 0, 0, 0.75f);
        public static final ColorRGBA SHADOW = rgba(0, 0, 0, 0.50f);

        public static final ColorRGBA WINDOW_FILL = rgba(8, 8, 8, 0.97f);
        public static final ColorRGBA WINDOW_GLASS = rgba(8, 8, 8, 0.70f);
        public static final ColorRGBA WINDOW_BORDER = rgba(255, 255, 255, 0.10f);
        /** Górna/dolna nakładka. */
        public static final ColorRGBA BAR_FILL = rgba(12, 12, 12, 1.00f);
        public static final ColorRGBA BAR_BORDER = rgba(255, 255, 255, 0.08f);

        public static final ColorRGBA PANEL_FILL = rgba(10, 10, 10, 0.60f);
        public static final ColorRGBA PANEL_TINT = rgba(255, 255, 255, 0.02f);  // białe podbicie
        public static final ColorRGBA PANEL_BORDER = rgba(255, 255, 255, 0.08f);
        public static final ColorRGBA PANEL_GLASS = rgba(10, 10, 10, 0.50f);

        public static final ColorRGBA SUB_FILL = rgba(10, 10, 10, 0.60f);
        public static final ColorRGBA SUB_BORDER = rgba(255, 255, 255, 0.05f);

        public static final ColorRGBA SEPARATOR = rgba(255, 255, 255, 0.06f);
        public static final ColorRGBA DIVIDER = rgba(255, 255, 255, 0.10f);
        public static final ColorRGBA HEADER_LINE = rgba(217, 217, 217, 0.15f);

        public static final ColorRGBA TOGGLE_OFF = rgba(255, 255, 255, 0.15f);
        public static final ColorRGBA KNOB_OFF = rgba(255, 255, 255, 0.25f);
        public static final ColorRGBA HOVER = rgba(217, 217, 217, 0.05f);
    }

    public static final class Window {
        private static final float BASE_WIDTH  = 420.0f; // Figma: 840
        private static final float BASE_HEIGHT = 310.0f; // Figma: 620
        public static float WIDTH()  { return BASE_WIDTH; }
        public static float HEIGHT() { return BASE_HEIGHT; }
        public static final float RADIUS = 12.5f;        // Figma: 25
        public static final float BORDER = 0.05f;        // grubość obwódki
        public static final float BLUR = 27.5f;          // Figma blur: 55
        public static final float SHADOW_SOFT = 5.0f;
        public static final float TOP_BAR_H = 30.0f;     // Figma: 60
        public static final float BOTTOM_BAR_H = 35.0f;  // Figma: 70
        public static final float PADDING = 8.0f;        // wewnętrzny odstęp barów
    }

    public static final class Tabs {
        public static final float FONT = 6.0f;
        public static final float ICON = 9.5f;
        public static final float ICON_GAP = 3.0f;
        public static final float PAD_X = 6.0f;
        public static final float HEIGHT = 17.0f;
        public static final float GAP = 4.0f;
        public static final float RADIUS = 6.0f;
        /** Tło aktywnego/hoverowanego taba. */
        public static final ColorRGBA BG = rgba(255, 65, 65, 0.15f);
    }

    public static final class Logo {
        public static final char GLYPH = 'D';
        public static final float W = 17.5f;   // Figma: 35
        public static final float H = 9.0f;    // Figma: 18
        public static final float RADIUS = 2.5f;
        public static final float ICON = 8.0f;
    }

    public static final class Panel {
        public static final float RADIUS = 8.0f;         // Figma: 16
        public static final float BORDER = 0.05f;
        public static final float GAP = 8.0f;
        public static final float BLUR = 12.0f;
    }

    public static final class Slider {
        public static final float NAME_FONT = 6.0f;
        public static final float VALUE_FONT = 5.5f;
        public static final float TRACK_H = 2.5f;
        public static final float KNOB = 6.0f;
        public static final float CHIP_H = 8.5f;
        public static final float CHIP_PAD = 4.0f;
        public static final float TRACK_OFFSET = 11.0f;  // od nazwy do toru
        public static final float ROW_GAP = 19.0f;       // odstęp między sliderami
    }

    public static final class Toggle {
        public static final float W = 13.0f;
        public static final float H = 7.0f;
        public static final float KNOB = 5.0f;
        public static final float INSET = 1.0f;
    }

    public static final class ConfigCard {
        public static final float HEIGHT = 26.0f;
        public static final float RADIUS = 6.0f;
        public static final float PADDING = 4.0f;
        public static final float ICON = 9.0f;
        public static final float ICON_RADIUS = 3.0f;
        public static final float NAME_FONT = 5.5f;
        public static final float DATE_FONT = 5.5f;
        public static final char ICON_GLYPH = '#';
        public static final char SETTINGS_GLYPH = '"';
        /** Podbicie jasności gdy config włączony. */
        public static final ColorRGBA FILL_ON = rgba(255, 255, 255, 0.03f);
    }

    public static ColorRGBA rgba(float r, float g, float b, float a) {
        return new ColorRGBA(r, g, b, 255.0f * a);
    }
}
