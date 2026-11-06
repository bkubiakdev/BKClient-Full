package dev.bkclient.larp;

import com.google.gson.JsonObject;
import dev.bkclient.larp.feature.*;
import dev.bkclient.larp.gui.*;
import dev.bkclient.systems.file.FileManager;
import net.minecraft.client.MinecraftClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LarpCatalog {
    private static List<ModOption> entries = List.of();
    public static void init() {
        Panels.init(); Presets.init();
        entries = Panels.CATEGORIES.stream().flatMap(category -> category.options.stream()).toList();
        try {
            if (Files.exists(path())) {
                var data = com.google.gson.JsonParser.parseString(Files.readString(path())).getAsJsonObject();
                for (var entry : entries) if (data.has(entry.id)) {
                    var values = data.getAsJsonObject(entry.id);
                    for (var setting : entry.settings) if (values.has(setting.id)) setting.load(values.get(setting.id));
                }
            }
        } catch (Exception e) { dev.bkclient.BKClientInit.LOGGER.warn("Could not load Larp Ana settings", e); }
    }
    public static List<ModOption> entries() { return entries; }
    public static Path path() { return FileManager.DIRECTORY.toPath().resolve("larp-ana.json"); }
    public static void save() {
        if (dev.bkclient.BKClientInit.INSTANCE.isPanic()) return;
        var data = new JsonObject();
        for (var entry : entries) {
            var values = new JsonObject();
            for (var setting : entry.settings) values.add(setting.id, setting.save());
            data.add(entry.id, values);
        }
        try {
            Files.createDirectories(path().getParent());
            Path temp = path().resolveSibling("larp-ana.json.tmp");
            Files.writeString(temp, FileManager.GSON.toJson(data));
            try { Files.move(temp, path(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temp, path(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception e) { dev.bkclient.BKClientInit.LOGGER.warn("Could not save Larp Ana settings", e); }
    }
    public static boolean assign(ModOption option) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.getMainHandStack().isEmpty()) { Status.error("Hold an item in your main hand first."); return false; }
        if (dev.bkclient.BKClientInit.INSTANCE.isPanic()) return false;
        var module = dev.bkclient.systems.modules.modules.visuals.LarpAna.instance;
        if (!module.isEnabled()) module.setEnabled(true, true);
        String fingerprint = Disguise.fingerprint(mc.player.getMainHandStack());
        LarpRuntime.resetEffects();
        for (var entry : entries) if (fingerprint.equals(entry.item.fingerprint())) entry.item.disable();
        boolean success = option.item.enable();
        if (success) Status.info("Assigned to held item. Only visible to you.");
        return success;
    }
    public static void remove(ModOption option) { LarpRuntime.resetEffects(); option.item.disable(); Status.info("Appearance removed."); }
    public static void clearAssignments() { for (var entry : entries) { entry.item.disable(); entry.enabled = false; } }
}
