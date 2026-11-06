package dev.bkclient.ui.menu.clickgui;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.utility.colors.ColorRGBA;
import java.nio.file.Files;
import java.util.Locale;

/** Opt-in A/B profiler. CPU submission time is not GPU execution time. */
public final class GuiProfiler {
    public enum Mode { ALL, NO_TEXT, NO_ICONS, NO_TOGGLES, NO_BACKGROUNDS, NO_CARDS, NO_FRAME, NO_BLUR, NO_GUI }
    private static Mode mode = Mode.ALL;
    private static boolean enabled, automatic;
    private static long phaseStart, previousFrame, frameStart, samples, frameNanos, cpuNanos;
    private static final long WARMUP = 2_000_000_000L, PHASE = 8_000_000_000L;
    private static final StringBuilder report = new StringBuilder();
    private static String label = "";
    private static long labelAt;
    private GuiProfiler() { }

    public static void stop() { enabled = automatic = false; mode = Mode.ALL; }
    public static boolean hides(Mode layer) { return enabled && mode == layer; }
    public static boolean key(int key) {
        if (key == 299) { // F10: normal rendering immediately
            stop();
            return true;
        }
        if (key == 298) { // F9: automatic sweep
            report.setLength(0);
            report.append("mode,frames,fps,frame_ms,gui_cpu_ms\n");
            enabled = automatic = true;
            start(Mode.ALL);
            return true;
        }
        if (key == 297) { // F8: manual layer selection
            automatic = false;
            Mode next = enabled ? Mode.values()[(mode.ordinal() + 1) % Mode.values().length] : Mode.ALL;
            enabled = true;
            if (report.isEmpty()) report.append("mode,frames,fps,frame_ms,gui_cpu_ms\n");
            start(next);
            return true;
        }
        return false;
    }
    private static void start(Mode next) {
        mode = next;
        phaseStart = System.nanoTime(); previousFrame = 0; labelAt = 0;
        samples = frameNanos = cpuNanos = 0;
        BKClientInit.LOGGER.info("[GUI-PROFILE] {}: 2s warmup + 6s sampling; CPU is submission time", mode);
    }
    public static void begin() {
        if (!enabled) return;
        long now = System.nanoTime();
        // Closing/reopening the screen or a long stall invalidates this phase.
        if (previousFrame != 0 && now - previousFrame > 1_000_000_000L) start(mode);
        frameStart = now;
    }
    public static void end(UIContext context) {
        if (!enabled) return;
        long end = System.nanoTime();
        long age = frameStart - phaseStart;
        if (previousFrame >= phaseStart + WARMUP) {
            samples++;
            frameNanos += frameStart - previousFrame;
            cpuNanos += end - frameStart;
        }
        previousFrame = frameStart;
        if (end - labelAt > 500_000_000L) {
            labelAt = end;
            label = String.format(Locale.ROOT, "GUI DEBUG %s | %.0f FPS | CPU %.3f ms | %ds | F8 next / F9 auto / F10 off",
                mode, fps(), samples == 0 ? 0 : cpuNanos / (samples * 1e6), Math.max(0, (PHASE-age)/1_000_000_000L));
        }
        // Same small overlay in every phase; excluded from CPU timing, included in frame timing.
        context.drawText(Fonts.MEDIUM.getFont(7), label, 8, 8, ColorRGBA.WHITE);
        if (age >= PHASE && samples > 0) {
            String row = String.format(Locale.ROOT, "%s,%d,%.2f,%.5f,%.5f%n", mode, samples, fps(),
                frameNanos / (samples * 1e6), cpuNanos / (samples * 1e6));
            report.append(row);
            BKClientInit.LOGGER.info("[GUI-PROFILE] {}", row.trim());
            try {
                var path = FileManager.DIRECTORY.toPath().resolve("gui-profile.csv");
                Files.createDirectories(path.getParent());
                Files.writeString(path, report);
            } catch (Exception error) { BKClientInit.LOGGER.warn("Cannot save GUI profile", error); }
            if (automatic && mode == Mode.NO_GUI) {
                enabled = automatic = false;
                mode = Mode.ALL;
                BKClientInit.LOGGER.info("[GUI-PROFILE] Complete. Normal GUI restored. Results: BK-Client/gui-profile.csv");
            } else start(automatic ? Mode.values()[mode.ordinal()+1] : mode);
        }
    }
    private static double fps() { return frameNanos == 0 ? 0 : samples * 1e9 / frameNanos; }
}
