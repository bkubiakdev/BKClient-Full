package dev.bkclient.utility.game.window;

import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.ptr.IntByReference;
import java.util.ArrayList;
import java.util.List;

/** Read-only window discovery for WindowScreen capture. */
public final class WindowLookup {
    private WindowLookup() {}
    public static boolean supported() {
        return System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).startsWith("windows");
    }

    public static HWND findWindow(String executable) {
        if (!supported() || executable == null || executable.isBlank()) {
            return null;
        }
        String needle = executable.toLowerCase(java.util.Locale.ROOT);
        List<HWND> matches = new ArrayList<>();

        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd) || User32.INSTANCE.GetWindowTextLength(hwnd) == 0) {
                return true;
            }
            IntByReference pid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
            if (pid.getValue() == 0) {
                return true;
            }
            try {
                String image = Kernel32Util.QueryFullProcessImageName(pid.getValue(), 0);
                if (image != null && image.toLowerCase(java.util.Locale.ROOT).endsWith(needle)) {
                    matches.add(hwnd);
                }
            } catch (Throwable ignored) {
                // Protected or already-exited process; nothing we can do with it either way.
            }
            return true;
        }, null);

        HWND best = null;
        long bestArea = -1L;
        for (HWND candidate : matches) {
            RECT rect = new RECT();
            if (!User32.INSTANCE.GetWindowRect(candidate, rect)) {
                continue;
            }
            long area = (long) (rect.right - rect.left) * (rect.bottom - rect.top);
            if (area > bestArea) {
                bestArea = area;
                best = candidate;
            }
        }
        return bestArea > 0L ? best : null;
    }

}
