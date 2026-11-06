package dev.bkclient.utility.game.window;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * PostMessage binding that preserves its success result.
 * The result distinguishes rejected input (for example, ERROR_ACCESS_DENIED)
 * from messages accepted by Windows but ignored by the target application.
 */
public interface Win32Input extends StdCallLibrary {

    Win32Input INSTANCE = Native.load("user32", Win32Input.class, W32APIOptions.DEFAULT_OPTIONS);

    /** UIPI refused the message: the target runs at a privilege this process cannot reach. */
    int ERROR_ACCESS_DENIED = 5;

    /**
     * The handle no longer names a window. Chromium destroys and recreates its child input
     * window during normal operation, so this is routine rather than exceptional.
     */
    int ERROR_INVALID_WINDOW_HANDLE = 1400;

    boolean PostMessage(HWND window, int message, WPARAM wParam, LPARAM lParam);
}
