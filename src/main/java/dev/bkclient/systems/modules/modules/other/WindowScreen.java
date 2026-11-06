package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.utility.game.window.Win32Input;
import dev.bkclient.utility.game.window.WindowCapture;
import dev.bkclient.utility.game.window.WindowLookup;
import dev.bkclient.utility.render.StreamedTexture;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Renders captured window pixels on a world-space quad and forwards pointer
 * input by mapping crosshair intersections to window coordinates.
 */
@ModuleInfo(name = "Window Screen", category = ModuleCategory.OTHER, desc = "Hangs Spotify or another app in the world as a clickable 3D panel")
public class WindowScreen extends BaseModule {

    /** Set on construction so the mouse mixin can reach the module without a lookup per click. */
    private static WindowScreen instance;

    private static final Identifier TEXTURE_ID = Identifier.of("bkclient", "window_screen");

    private static final int WM_MOUSEMOVE = 0x0200;
    private static final int WM_LBUTTONDOWN = 0x0201;
    private static final int WM_LBUTTONUP = 0x0202;
    private static final int WM_RBUTTONDOWN = 0x0204;
    private static final int WM_RBUTTONUP = 0x0205;
    private static final int MK_LBUTTON = 0x0001;
    private static final int MK_RBUTTON = 0x0002;

    private final StringSetting process = new StringSetting(this, "Process").text("spotify.exe");

    private final BindSetting placeKey = new BindSetting(this, "Place Screen");

    private final SliderSetting panelWidth = new SliderSetting(this, "Width")
            .min(1.0F).max(12.0F).currentValue(4.0F).step(0.5F);
    private final SliderSetting placeDistance = new SliderSetting(this, "Place Distance")
            .min(1.0F).max(10.0F).currentValue(3.0F).step(0.5F);
    private final SliderSetting reach = new SliderSetting(this, "Click Reach")
            .min(2.0F).max(30.0F).currentValue(12.0F).step(1.0F);
    private final SliderSetting captureFps = new SliderSetting(this, "Capture FPS")
            .min(1.0F).max(60.0F).currentValue(12.0F).step(1.0F);

    private final BooleanSetting followCursor = new BooleanSetting(this, "Move Cursor").enabled(true);

    private final WindowCapture capture = new WindowCapture();
    private final StreamedTexture texture = new StreamedTexture();

    private volatile HWND target;
    private volatile HWND inputTarget;

    /** The frame currently on the GPU, so an unchanged one is not re-uploaded every frame. */
    private WindowCapture.Frame uploaded;
    private Thread captureThread;
    private boolean textureRegistered;

    /** Centre of the panel in world space, and the two axes of its surface. */
    private Vec3d anchor;
    private Vec3d normal;
    private Vec3d right;

    /** Where the crosshair last crossed the panel, in window pixels; null when not aiming at it. */
    private int[] hover;
    private int lastPostedX = -1;
    private int lastPostedY = -1;

    /** Latched so a blocked window logs once instead of on every message. */
    private boolean reportedInputFailure;

    /** When the panel was last actually drawn, so capture can back off when nobody is looking. */
    private volatile long lastDrawnMs;

    /** Where each button went down, so its release lands on the same spot and is not lost. */
    private int[] leftPressedAt;
    private int[] rightPressedAt;

    public WindowScreen() {
        instance = this;
    }

    @Override
    public void onEnable() {
        if (!WindowLookup.supported()) {
            notify(NotificationType.ERROR, "Window Screen only works on Windows");
            this.setEnabled(false, true);
            return;
        }

        String executable = normalisedProcess();
        HWND found = WindowLookup.findWindow(executable);
        if (found == null) {
            notify(NotificationType.ERROR, "No window found for " + executable);
            this.setEnabled(false, true);
            return;
        }

        this.target = found;
        this.inputTarget = resolveInputTarget(found);
        this.anchor = null;
        this.hover = null;
        this.reportedInputFailure = false;
        describeTarget(found);

        if (!this.textureRegistered) {
            mc.getTextureManager().registerTexture(TEXTURE_ID, this.texture);
            this.textureRegistered = true;
        }
        startCaptureThread();
        notify(NotificationType.SUCCESS, "Bind Place Screen, aim and place - then click it with the mouse");
    }

    @Override
    public void onDisable() {
        // The capture thread owns the GDI objects and frees them itself on the way out. Closing
        // them from here would pull the bitmap out from under a PrintWindow already in progress.
        this.target = null;
        stopCaptureThread();
        this.inputTarget = null;
        this.anchor = null;
        this.hover = null;
        this.leftPressedAt = null;
        this.rightPressedAt = null;
    }

    private String normalisedProcess() {
        String value = this.process.getText();
        if (value == null || value.isBlank()) {
            return "spotify.exe";
        }
        value = value.trim().toLowerCase();
        return value.endsWith(".exe") ? value : value + ".exe";
    }

    private void notify(NotificationType type, String text) {
        BKClientInit.getInstance().getNotificationManager().addNotification(type, text);
    }

    /**
     * Logs the selected window and capture size to diagnose incorrect window
     * selection and expensive PrintWindow calls.
     */
    private void describeTarget(HWND window) {
        char[] title = new char[512];
        User32.INSTANCE.GetWindowText(window, title, title.length);
        RECT client = new RECT();
        User32.INSTANCE.GetClientRect(window, client);
        BKClientInit.LOGGER.info("[WindowScreen] Capturing \"{}\" at {}x{}",
                Native.toString(title), client.right - client.left, client.bottom - client.top);
    }

    /**
     * Compositing surfaces, which look like the obvious target because they cover the whole
     * window, and are the one thing that will never act on a click.
     */
    private static final List<String> NON_INPUT_CLASSES = List.of("Intermediate D3D Window");

    /**
     * The last choice reported, so re-resolving after Chromium recycles a window does not repeat
     * the same three log lines every time it happens.
     */
    private static String lastLoggedTarget;
    private static String lastLoggedChildren;

    private static void logTargetOnce(String description) {
        if (!description.equals(lastLoggedTarget)) {
            lastLoggedTarget = description;
            BKClientInit.LOGGER.info("[WindowScreen] Sending input to {}", description);
        }
    }

    /**
     * Selects the Chromium input child when present, otherwise the top-level window.
     * Intermediate D3D Window is a compositing surface and does not handle mouse input.
     */
    private static HWND resolveInputTarget(HWND frame) {
        List<HWND> candidates = new ArrayList<>();
        StringBuilder seen = new StringBuilder();

        User32.INSTANCE.EnumChildWindows(frame, (child, data) -> {
            char[] className = new char[256];
            User32.INSTANCE.GetClassName(child, className, className.length);
            String name = Native.toString(className);
            seen.append(name).append(' ');
            candidates.add(child);
            return true;
        }, null);

        String children = seen.toString().trim();
        if (!children.equals(lastLoggedChildren)) {
            lastLoggedChildren = children;
            BKClientInit.LOGGER.info("[WindowScreen] Child windows: {}", children);
        }

        HWND renderWidget = User32.INSTANCE.FindWindowEx(frame, null, "Chrome_RenderWidgetHostHWND", null);
        if (renderWidget != null) {
            logTargetOnce("Chrome_RenderWidgetHostHWND");
            return renderWidget;
        }

        // Any other real child is worth a try, as long as it is not a compositing surface.
        for (HWND candidate : candidates) {
            char[] className = new char[256];
            User32.INSTANCE.GetClassName(candidate, className, className.length);
            String name = Native.toString(className);
            if (NON_INPUT_CLASSES.contains(name)) {
                continue;
            }
            logTargetOnce("child " + name);
            return candidate;
        }

        logTargetOnce("the frame window");
        return frame;
    }


    /**
     * Captures frames on a worker thread to keep PrintWindow off the render thread.
     * Rendering uploads the most recently completed frame.
     */
    private void startCaptureThread() {
        stopCaptureThread();
        Thread thread = new Thread(() -> {
            boolean warnedBlank = false;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    HWND current = refreshHandles();
                    if (current == null) {
                        // The program is gone or restarting. Idle instead of giving up, so it
                        // reconnects on its own when the window comes back.
                        sleep(500L);
                        continue;
                    }
                    if (this.capture.prepare(current) && this.capture.capture(current)) {
                        warnedBlank = false;
                    } else if (!warnedBlank) {
                        warnedBlank = true;
                        BKClientInit.LOGGER.warn("[WindowScreen] Capture produced nothing for {} (method: {})",
                                normalisedProcess(), this.capture.methodName());
                    }

                    sleep(currentIntervalMs());
                }
            } finally {
                this.capture.close();
            }
        }, "BK-Client Window Screen Capture");
        thread.setDaemon(true);
        thread.start();
        this.captureThread = thread;
    }

    /**
     * Refreshes capture and input handles, which Chromium may recreate during use.
     *
     * @return the window to capture, or null while no valid window is available
     */
    private HWND refreshHandles() {
        HWND current = this.target;
        if (current == null || !User32.INSTANCE.IsWindow(current)) {
            current = WindowLookup.findWindow(normalisedProcess());
            this.target = current;
            this.inputTarget = null;
            if (current == null) {
                return null;
            }
            // A new frame means a new bitmap size to discover.
            this.capture.close();
        }

        HWND input = this.inputTarget;
        if (input == null || !User32.INSTANCE.IsWindow(input)) {
            this.inputTarget = resolveInputTarget(current);
        }
        return current;
    }

    /**
     * Reduces capture frequency when the panel has not been rendered recently.
     * PrintWindow asks the target application to redraw on its UI thread.
     */
    private long currentIntervalMs() {
        long sinceDrawn = System.currentTimeMillis() - this.lastDrawnMs;
        if (sinceDrawn > 1000L) {
            return 500L;
        }
        return (long) (1000.0F / Math.max(1.0F, this.captureFps.getCurrentValue()));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void stopCaptureThread() {
        Thread thread = this.captureThread;
        this.captureThread = null;
        if (thread != null) {
            thread.interrupt();
        }
    }


    private final EventListener<KeyPressEvent> keyPressEvent = event -> {
        if (event.getAction() != 1 || mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (this.placeKey.isKey(event.getKey())) {
            place();
        }
    };

    /**
     * Forwards press and release events before Minecraft handles them, allowing drags
     * without triggering an in-game attack.
     *
     * @return true when the panel consumed the click
     */
    public static boolean handleMouseButton(int button, int action) {
        // Nothing about a panel in the world can justify eating a click aimed at a menu. This
        // has to be the very first test, releases included: the client's own screens sit behind
        // this same callback, and swallowing one press there costs the player their interface.
        if (mc.currentScreen != null) {
            return false;
        }
        if (button != 0 && button != 1) {
            return false;
        }
        WindowScreen module = instance;
        if (module == null || !module.isEnabled() || module.anchor == null) {
            return false;
        }

        boolean left = button == 0;
        int up = left ? WM_LBUTTONUP : WM_RBUTTONUP;

        // A press we took has to have its release taken too, wherever the crosshair has wandered
        // to by then. Letting the release through would leave the button held down inside the
        // other program and hand the game half a click it never saw the start of.
        if (action == 0) {
            int[] releasePoint = left ? module.leftPressedAt : module.rightPressedAt;
            if (releasePoint == null) {
                return false;
            }
            if (left) {
                module.leftPressedAt = null;
            } else {
                module.rightPressedAt = null;
            }
            module.post(up, 0, releasePoint[0], releasePoint[1]);
            return true;
        }

        if (action != 1) {
            return false;
        }
        int[] point = module.hover;
        if (point == null) {
            return false;
        }

        int down = left ? WM_LBUTTONDOWN : WM_RBUTTONDOWN;
        int flag = left ? MK_LBUTTON : MK_RBUTTON;
        if (left) {
            module.leftPressedAt = point;
        } else {
            module.rightPressedAt = point;
        }
        module.post(WM_MOUSEMOVE, flag, point[0], point[1]);
        module.post(down, flag, point[0], point[1]);
        return true;
    }

    /**
     * Places the panel facing the player, using horizontal look direction to keep it upright.
     */
    private void place() {
        Vec3d look = mc.player.getRotationVec(1.0F);
        Vec3d flat = new Vec3d(look.x, 0.0, look.z);
        if (flat.lengthSquared() < 1.0E-6) {
            flat = new Vec3d(0.0, 0.0, 1.0);
        }
        flat = flat.normalize();

        Vec3d eye = mc.player.getEyePos();
        this.anchor = eye.add(flat.multiply(this.placeDistance.getCurrentValue()));
        this.normal = flat.multiply(-1.0);
        this.right = new Vec3d(this.normal.z, 0.0, -this.normal.x);
        this.hover = null;
        this.lastPostedX = -1;
        this.lastPostedY = -1;
    }

    /**
     * Uses the displayed frame aspect ratio so hit testing matches the rendered panel after a resize.
     */
    private float panelHeight(WindowCapture.Frame frame) {
        if (frame == null || frame.width() <= 0 || frame.height() <= 0) {
            return this.panelWidth.getCurrentValue() * 0.6F;
        }
        return this.panelWidth.getCurrentValue() * frame.height() / frame.width();
    }


    /**
     * Works out where the crosshair crosses the panel.
     *
     * <p>Standard ray against a plane, then the hit point is resolved along the panel's own two
     * axes to get a fraction across and down, which maps straight onto window pixels.
     *
     * @return {@code {pixelX, pixelY}} or null when the player is not looking at the panel
     */
    private int[] traceHover(WindowCapture.Frame frame) {
        if (this.anchor == null || mc.player == null || frame == null) {
            return null;
        }
        Vec3d origin = mc.gameRenderer.getCamera().getPos();
        Vec3d direction = mc.player.getRotationVec(1.0F);

        double denominator = direction.dotProduct(this.normal);
        if (Math.abs(denominator) < 1.0E-6) {
            return null;
        }
        double distance = this.anchor.subtract(origin).dotProduct(this.normal) / denominator;
        if (distance <= 0.0 || distance > this.reach.getCurrentValue()) {
            return null;
        }

        Vec3d hit = origin.add(direction.multiply(distance));
        Vec3d local = hit.subtract(this.anchor);

        float width = this.panelWidth.getCurrentValue();
        float height = panelHeight(frame);

        double acrossFraction = local.dotProduct(this.right) / width + 0.5;
        double downFraction = 0.5 - local.y / height;
        if (acrossFraction < 0.0 || acrossFraction > 1.0 || downFraction < 0.0 || downFraction > 1.0) {
            return null;
        }

        int pixelX = (int) (acrossFraction * frame.width());
        int pixelY = (int) (downFraction * frame.height());
        return new int[]{
                Math.max(0, Math.min(frame.width() - 1, pixelX)),
                Math.max(0, Math.min(frame.height() - 1, pixelY))
        };
    }

    private void sendMouseMove(int x, int y) {
        // Chromium redoes hit-testing on every move message, so only send when the pixel changed.
        if (x == this.lastPostedX && y == this.lastPostedY) {
            return;
        }
        this.lastPostedX = x;
        this.lastPostedY = y;
        post(WM_MOUSEMOVE, 0, x, y);
    }

    /**
     * Forwards a mouse message and logs rejected input once to avoid repeated errors.
     */
    private void post(int message, int buttons, int x, int y) {
        HWND destination = this.inputTarget;
        if (destination == null) {
            return;
        }
        // lParam packs the position as two signed 16-bit halves, y in the high word.
        long packed = ((long) (y & 0xFFFF) << 16) | (x & 0xFFFF);
        boolean accepted = Win32Input.INSTANCE.PostMessage(destination, message, new WPARAM(buttons), new LPARAM(packed));

        if (accepted) {
            return;
        }

        int error = Native.getLastError();
        if (error == Win32Input.ERROR_INVALID_WINDOW_HANDLE) {
            // Chromium creates and destroys its child input window as it pleases, so a handle
            // resolved when the module was switched on goes stale under us. Drop it and let the
            // capture thread work out the new one rather than posting into nothing forever.
            this.inputTarget = null;
            return;
        }
        if (!this.reportedInputFailure) {
            this.reportedInputFailure = true;
            BKClientInit.LOGGER.warn("[WindowScreen] PostMessage to {} failed with error {}",
                    normalisedProcess(), error);
        }
    }


    /**
     * Culls panels outside the view so hidden panels do not keep the capture rate high.
     */
    private boolean panelInView() {
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        Vec3d toPanel = this.anchor.subtract(camera);
        double distance = toPanel.length();
        if (distance > this.reach.getCurrentValue() * 3.0) {
            return false;
        }
        if (distance < 0.01) {
            return true;
        }
        // A generous cone rather than a strict frustum: the panel is wide, so its edge can be on
        // screen while its centre is not, and clipping it early would be worse than capturing.
        return toPanel.multiply(1.0 / distance).dotProduct(mc.player.getRotationVec(1.0F)) > 0.1;
    }

    private final EventListener<Render3DEvent> render3DEvent = event -> {
        // Cleared first, before any of the bail-outs below can skip past it. The click handler
        // treats a non-null hover as "the crosshair is on the panel, swallow this button", so
        // leaving a stale one behind eats every click in the game — including on the HUD.
        this.hover = null;

        if (this.anchor == null || mc.player == null || !panelInView()) {
            return;
        }

        // One read of the volatile, then everything below works off that same snapshot. Reading
        // it again per use is what let the size and the pixels disagree.
        WindowCapture.Frame frame = this.capture.frame();
        if (frame == null) {
            return;
        }
        if (frame != this.uploaded) {
            this.uploaded = frame;
            this.texture.upload(frame.pixels(), frame.width(), frame.height());
        }
        if (!this.texture.hasContent()) {
            return;
        }

        this.hover = traceHover(frame);
        if (this.hover != null && this.followCursor.isEnabled()) {
            sendMouseMove(this.hover[0], this.hover[1]);
        }

        this.lastDrawnMs = System.currentTimeMillis();
        drawPanel(event.getMatrices(), frame);
    };

    private void drawPanel(MatrixStack matrices, WindowCapture.Frame frame) {
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        float width = this.panelWidth.getCurrentValue();
        float height = panelHeight(frame);

        Vec3d centre = this.anchor.subtract(camera);
        Vec3d across = this.right.multiply(width * 0.5);
        double half = height * 0.5;

        // Corners, anticlockwise from the top left as seen from the front.
        Vec3d topLeft = centre.subtract(across).add(0.0, half, 0.0);
        Vec3d topRight = centre.add(across).add(0.0, half, 0.0);
        Vec3d bottomRight = centre.add(across).add(0.0, -half, 0.0);
        Vec3d bottomLeft = centre.subtract(across).add(0.0, -half, 0.0);

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer consumer = immediate.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE_ID));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Full block light: a screen emits its own light, and shading it by the world would make
        // it unreadable at night, which is when you would actually be looking at it.
        int light = 0xF000F0;

        // Both faces are emitted. The entity translucent layer culls back faces, and which way
        // this quad ends up wound depends on where the player happened to stand when they placed
        // it — get that wrong and the panel is silently invisible with no GL error to show for it.
        // Two faces cost four extra vertices and remove the guesswork.
        vertex(consumer, matrix, topLeft, 0.0F, 0.0F, light, this.normal);
        vertex(consumer, matrix, bottomLeft, 0.0F, 1.0F, light, this.normal);
        vertex(consumer, matrix, bottomRight, 1.0F, 1.0F, light, this.normal);
        vertex(consumer, matrix, topRight, 1.0F, 0.0F, light, this.normal);

        Vec3d back = this.normal.multiply(-1.0);
        vertex(consumer, matrix, topRight, 1.0F, 0.0F, light, back);
        vertex(consumer, matrix, bottomRight, 1.0F, 1.0F, light, back);
        vertex(consumer, matrix, bottomLeft, 0.0F, 1.0F, light, back);
        vertex(consumer, matrix, topLeft, 0.0F, 0.0F, light, back);

        immediate.draw();
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3d position, float u, float v, int light, Vec3d facing) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal((float) facing.x, (float) facing.y, (float) facing.z);
    }
}
