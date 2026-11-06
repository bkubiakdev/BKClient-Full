package dev.bkclient.ui.clicksys;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.clicksys.port.concurrent.SystemClient;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import net.minecraft.client.MinecraftClient;

/** One startup preparation step per tick, after the resource loading overlay has finished. */
public final class GuiWarmup {
    private static boolean done;
    private GuiWarmup() {}
    public static void tick(MinecraftClient client) {
        if (done || client.getOverlay() != null || client.currentScreen == null) return;
        try {
            if (!TextShader.preloadNextFont()) return;
            SystemClient.getInstance().getModuleRegistry().getList32();
            done = true;
        } catch (RuntimeException failure) {
            done = true;
            BKClientInit.LOGGER.warn("GUI warmup failed; regular initialization remains available", failure);
        }
    }
}
