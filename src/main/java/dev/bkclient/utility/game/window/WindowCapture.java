package dev.bkclient.utility.game.window;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.PointerByReference;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

/**
 * Captures a window into a DIB and copies frames into buffers for OpenGL.
 * GPU-rendered windows may reject ordinary BitBlt capture; capture(HWND)
 * tries fallback methods and reuses the last successful method.
 */
public final class WindowCapture implements AutoCloseable {

    /** Ask the window to redraw itself fully, including GPU-composited content. */
    private static final int PW_RENDERFULLCONTENT = 0x00000002;

    private static final int DIB_RGB_COLORS = 0;
    private static final int SRCCOPY = 0x00CC0020;
    private static final int BI_RGB = 0;

    /** Capture strategies, tried in this order until one returns a non-empty frame. */
    private enum Method {
        PRINT_FULL_CONTENT,
        PRINT_PLAIN,
        BIT_BLT
    }

    /**
     * Publishes pixels and dimensions together so a resize cannot mix frame sizes.
     */
    public record Frame(ByteBuffer pixels, int width, int height) {
    }

    private HDC memoryDc;
    private HBITMAP bitmap;
    private HANDLE previousBitmap;
    private Pointer pixels;
    private ByteBuffer dib;

    private int width;
    private int height;
    private Method method;

    /**
     * Alternates between two direct buffers owned by the capture thread.
     * Copies pixels out of the DIB before publishing, since resizing frees the DIB.
     */
    private final ByteBuffer[] published = new ByteBuffer[2];
    private int publishIndex;

    private volatile Frame frame;

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    /**
     * Returns the most recently published frame, or null before the first capture.
     */
    public Frame frame() {
        return this.frame;
    }

    public boolean isReady() {
        return this.dib != null && this.width > 0 && this.height > 0;
    }

    /** Which strategy last worked, for logging. Null until the first successful capture. */
    public String methodName() {
        return this.method == null ? "none" : this.method.name();
    }

    /**
     * Makes sure the backing bitmap matches the window's current client size.
     *
     * @return false if the window is gone or has no drawable area
     */
    public boolean prepare(HWND target) {
        if (target == null) {
            return false;
        }
        RECT client = new RECT();
        if (!User32.INSTANCE.GetClientRect(target, client)) {
            return false;
        }
        int newWidth = client.right - client.left;
        int newHeight = client.bottom - client.top;
        if (newWidth <= 0 || newHeight <= 0) {
            return false;
        }
        if (newWidth == this.width && newHeight == this.height && this.dib != null) {
            return true;
        }

        close();

        HDC screenDc = User32.INSTANCE.GetDC(null);
        if (screenDc == null) {
            return false;
        }
        try {
            BITMAPINFO info = new BITMAPINFO();
            info.bmiHeader.biSize = info.bmiHeader.size();
            info.bmiHeader.biWidth = newWidth;
            // Negative height asks for a top-down bitmap. Without it GDI hands back the rows
            // bottom-up and the panel renders upside down.
            info.bmiHeader.biHeight = -newHeight;
            info.bmiHeader.biPlanes = 1;
            info.bmiHeader.biBitCount = 32;
            info.bmiHeader.biCompression = BI_RGB;

            PointerByReference pixelPointer = new PointerByReference();
            HBITMAP newBitmap = GDI32.INSTANCE.CreateDIBSection(screenDc, info, DIB_RGB_COLORS, pixelPointer, null, 0);
            if (newBitmap == null || pixelPointer.getValue() == null) {
                return false;
            }

            HDC newDc = GDI32.INSTANCE.CreateCompatibleDC(screenDc);
            if (newDc == null) {
                GDI32.INSTANCE.DeleteObject(newBitmap);
                return false;
            }

            this.memoryDc = newDc;
            this.bitmap = newBitmap;
            this.previousBitmap = GDI32.INSTANCE.SelectObject(newDc, newBitmap);
            this.pixels = pixelPointer.getValue();
            this.dib = this.pixels.getByteBuffer(0, (long) newWidth * newHeight * 4);
            this.width = newWidth;
            this.height = newHeight;
            return true;
        } finally {
            User32.INSTANCE.ReleaseDC(null, screenDc);
        }
    }

    /**
     * Grabs one frame.
     *
     * <p>Once a strategy has worked it is reused without retrying the others; only if it stops
     * producing pixels does the chain get walked again.
     *
     * @return false when nothing drew anything, which for a Chromium window usually means the
     *         program is minimised to the tray rather than merely hidden
     */
    public boolean capture(HWND target) {
        if (target == null || !isReady()) {
            return false;
        }

        if (this.method != null && attempt(target, this.method)) {
            publish();
            return true;
        }
        for (Method candidate : Method.values()) {
            if (candidate == this.method) {
                continue;
            }
            if (attempt(target, candidate)) {
                this.method = candidate;
                publish();
                return true;
            }
        }
        return false;
    }

    /**
     * Copies DIB pixels into a published buffer. The render thread must not retain
     * a DIB pointer because resizing frees its memory.
     */
    private void publish() {
        int bytes = this.width * this.height * 4;
        ByteBuffer destination = this.published[this.publishIndex];
        if (destination == null || destination.capacity() != bytes) {
            destination = ByteBuffer.allocateDirect(bytes);
            this.published[this.publishIndex] = destination;
        }

        MemoryUtil.memCopy(Pointer.nativeValue(this.pixels), MemoryUtil.memAddress(destination), bytes);
        destination.rewind();

        this.frame = new Frame(destination, this.width, this.height);
        // Alternate, so the next capture fills the buffer the render thread is not reading.
        this.publishIndex = 1 - this.publishIndex;
    }

    private boolean attempt(HWND target, Method candidate) {
        boolean drew = switch (candidate) {
            case PRINT_FULL_CONTENT -> User32.INSTANCE.PrintWindow(target, this.memoryDc, PW_RENDERFULLCONTENT);
            case PRINT_PLAIN -> User32.INSTANCE.PrintWindow(target, this.memoryDc, 0);
            case BIT_BLT -> blit(target);
        };
        return drew && !isBlank();
    }

    private boolean blit(HWND target) {
        HDC windowDc = User32.INSTANCE.GetDC(target);
        if (windowDc == null) {
            return false;
        }
        try {
            return GDI32.INSTANCE.BitBlt(this.memoryDc, 0, 0, this.width, this.height, windowDc, 0, 0, SRCCOPY);
        } finally {
            User32.INSTANCE.ReleaseDC(target, windowDc);
        }
    }

    /**
     * Samples a grid of pixels to detect an all-black capture without scanning the full frame.
     */
    private boolean isBlank() {
        int stepX = Math.max(1, this.width / 10);
        int stepY = Math.max(1, this.height / 10);
        for (int y = 0; y < this.height; y += stepY) {
            for (int x = 0; x < this.width; x += stepX) {
                int index = (y * this.width + x) * 4;
                if (index + 2 >= this.dib.capacity()) {
                    continue;
                }
                if (this.dib.get(index) != 0 || this.dib.get(index + 1) != 0 || this.dib.get(index + 2) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void close() {
        if (this.memoryDc != null) {
            if (this.previousBitmap != null) {
                GDI32.INSTANCE.SelectObject(this.memoryDc, this.previousBitmap);
            }
            GDI32.INSTANCE.DeleteDC(this.memoryDc);
            this.memoryDc = null;
        }
        if (this.bitmap != null) {
            GDI32.INSTANCE.DeleteObject(this.bitmap);
            this.bitmap = null;
        }
        this.previousBitmap = null;
        this.pixels = null;
        this.dib = null;
        this.width = 0;
        this.height = 0;
    }
}
