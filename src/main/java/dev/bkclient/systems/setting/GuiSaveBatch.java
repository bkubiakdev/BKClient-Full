package dev.bkclient.systems.setting;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Preserve live setting previews; serialize after release, focus loss, screen close or shutdown. */
public final class GuiSaveBatch {
    private static final PendingGuiWrites pending = new PendingGuiWrites();
    private static boolean registered;
    private static boolean flushing;
    private GuiSaveBatch() {}
    public static boolean isEditing() {
        if (flushing) return false;
        var mc = MinecraftClient.getInstance();
        return mc != null && mc.isOnThread() && mc.currentScreen != null && mc.isWindowFocused()
            && mc.getWindow() != null && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }
    public static boolean defer(Object key, Runnable write) {
        boolean editing = isEditing();
        if (editing && !registered) {
            registered = true;
            ClientTickEvents.END_CLIENT_TICK.register(client -> { if (!isEditing()) flush(); });
            ClientLifecycleEvents.CLIENT_STOPPING.register(client -> flush());
        }
        return pending.defer(key,write,editing);
    }
    private static void flush() {
        flushing = true;
        try {
            for (var write : pending.drain()) {
                try { write.run(); }
                catch (Exception error) { dev.bkclient.BKClientInit.LOGGER.warn("Failed to save GUI setting", error); }
            }
        } finally { flushing = false; }
    }
}
