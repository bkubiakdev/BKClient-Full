package dev.bkclient.systems.modules;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.utility.concurrent.ShutdownTasks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

/** A session stop, not an uninstall. All cleanup runs on the Minecraft thread. */
public final class PanicSession {
    private PanicSession() {}

    public static void activate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isOnThread()) { mc.execute(PanicSession::activate); return; }
        BKClientInit client = BKClientInit.getInstance();
        if (client.isPanic()) return;
        ShutdownTasks.run(error -> BKClientInit.LOGGER.error("Could not save pre-panic state", error),
            () -> client.getFileManager().saveClientFiles(),
            () -> {
                ConfigFile autosave = client.getConfigManager().getAutoSaveConfig();
                if (autosave == null) autosave = new ConfigFile("autosave");
                if (!autosave.save()) throw new IllegalStateException("Could not save autosave before panic");
            },
            () -> {
                var preset = client.getSwingPresetManager().getAutoSavePreset();
                if (preset != null) preset.save();
            });
        client.setPanic(true);
        boolean suppressed = AbstractSetting.suppressSave;
        AbstractSetting.suppressSave = true;
        try {
            ShutdownTasks.run(error -> BKClientInit.LOGGER.error("Panic cleanup failed; continuing", error),
                () -> client.getModuleManager().shutdownModules(),
                () -> {
                    if (mc.currentScreen != null && mc.currentScreen.getClass().getName().startsWith("dev.bkclient."))
                        mc.setScreen(null);
                },
                () -> client.getRotationHandler().reset(),
                () -> client.getTargetManager().setCurrentTarget(null),
                () -> {
                    KeyBinding.unpressAll();
                    if (mc.player != null) mc.setCameraEntity(mc.player);
                    if (mc.currentScreen == null && mc.player != null) mc.mouse.lockCursor();
                    dev.bkclient.utility.game.cursor.CursorUtility.set(dev.bkclient.utility.game.cursor.CursorType.DEFAULT);
                },
                () -> client.getMusicTracker().shutdown(),
                () -> client.getDiscordManager().shutdown(),
                () -> client.getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.XRay.class).shutdownScanner(),
                dev.bkclient.utility.game.ClientTelemetryUtility::shutdown,
                dev.bkclient.utility.game.AutoMarketRemote::shutdown,
                dev.bkclient.utility.concurrent.ClientAsyncExecutor::shutdown);
        } finally { AbstractSetting.suppressSave = suppressed; }
        BKClientInit.LOGGER.info("Panic active until Minecraft restart; configuration and installed mod preserved");
    }
}
