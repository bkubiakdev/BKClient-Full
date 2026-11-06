package dev.bkclient.ui.clicksys.port.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.config.ConfigManager;
import dev.bkclient.systems.config.ConfigShareCode;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.file.SafeJsonFile;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.utility.game.CloudConfigUtility;
import net.minecraft.client.MinecraftClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/** Config cards use the same local files and BK cloud format as ClickGUI. */
public class ConfigSync {
    public record Imported(dev.bkclient.systems.config.ConfigImport.Kind kind,String name) {}
    private static final ConfigSync INSTANCE = new ConfigSync();
    private final Map<String, String> descriptions = new HashMap<>();
    private final Set<String> sharing = new HashSet<>();
    private final Path path = FileManager.DIRECTORY.toPath().resolve("clicksys-config-labels.json");
    public static ConfigSync getInstance() { return INSTANCE; }
    private ConfigManager manager() { return BKClientInit.getInstance().getConfigManager(); }

    protected ConfigSync() {
        try {
            if (Files.exists(path)) JsonParser.parseString(Files.readString(path)).getAsJsonObject()
                .entrySet().forEach(e -> descriptions.put(e.getKey(), e.getValue().getAsString()));
        } catch (Exception e) { BKClientInit.LOGGER.warn("Could not read config descriptions", e); }
    }

    public List<ConfigEntry> getList() {
        manager().refresh();
        List<ConfigEntry> result = new ArrayList<>();
        for (ConfigFile file : manager().getConfigFiles()) {
            ConfigEntry entry = new ConfigEntry();
            entry.setText(file.getFileName());
            entry.setText2(descriptions.getOrDefault(file.getFileName(), "Local configuration"));
            entry.setText4(java.time.Instant.ofEpochMilli(file.getFile().lastModified()).toString());
            entry.setFlag(manager().getCurrent() == null ? "autosave".equalsIgnoreCase(file.getFileName())
                : manager().getCurrent().getFileName().equals(file.getFileName()));
            entry.setFlag3("autosave".equalsIgnoreCase(file.getFileName()));
            result.add(entry);
        }
        result.sort(Comparator.comparing(ConfigEntry::getText, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private void done(Runnable action) { if (action != null) action.run(); }
    public void setRunnable2(Runnable action) { done(action); }
    public boolean isFlag() { return false; }
    public void onRunnableString(Runnable action, String name) {
        if ("autosave".equalsIgnoreCase(name)) return;
        ConfigFile file = manager().getConfig(name);
        if (file != null) {
            if (manager().getCurrent() != null && manager().getCurrent().getFileName().equals(name)) {
                ConfigFile autosave = manager().getConfig("autosave");
                if (autosave == null || !autosave.load()) { notify(false, "Could not switch to autosave."); return; }
            }
            file.delete();
            if (!file.getFile().exists()) { descriptions.remove(name); saveMetadata(); }
        }
        done(action);
    }
    public void onRunnableString2(Runnable action, String name) {
        ConfigFile file = manager().getConfig(name);
        if (file != null && file.load()) notify(true, "Config loaded.");
        done(action);
    }
    public void save(String name) {
        ConfigFile file = manager().getConfig(name);
        if (file != null) { boolean ok = file.save(); notify(ok, ok ? "Config saved." : "Could not save config."); }
    }
    public void onRunnable(Runnable action) {
        if (manager().getCurrent() != null) save(manager().getCurrent().getFileName());
        done(action);
    }
    public void onStringStringRunnable(String description, String name, Runnable action) {
        descriptions.put(name, description); saveMetadata(); done(action);
    }
    public void onStringStringRunnable2(String oldName, String newName, Runnable action) {
        if ("autosave".equalsIgnoreCase(oldName)) return;
        try {
            ConfigFile file = manager().getConfig(oldName);
            String clean = ConfigManager.sanitizeConfigName(newName);
            if (file != null && !clean.equalsIgnoreCase(oldName)) {
                if (manager().getConfig(clean, true) != null) throw new IllegalArgumentException("That name already exists.");
                manager().renameConfig(file, clean);
                String description = descriptions.remove(oldName);
                if (description != null) descriptions.put(clean, description);
                saveMetadata(); notify(true, "Config renamed.");
            }
        } catch (Exception e) { error(e); }
        done(action);
    }

    public boolean isSharing(String name) { return sharing.contains(name); }
    public void shareCode(String name, boolean privateMode, Consumer<Boolean> callback) {
        if (!sharing.add(name)) return;
        try {
            ConfigFile file = Objects.requireNonNull(manager().getConfig(name), "Config not found.");
            JsonObject json = JsonParser.parseString(Files.readString(file.getFile().toPath())).getAsJsonObject();
            json.addProperty("meta_name", ConfigManager.sanitizeConfigName(name));
            notify(true, "Uploading config...");
            CloudConfigUtility.uploadConfig(name, FileManager.GSON.toJson(json)).whenComplete((result, failure) ->
                MinecraftClient.getInstance().execute(() -> {
                    sharing.remove(name);
                    if (failure != null || result == null || privateMode && (result.deletionKey == null || result.deletionKey.isBlank())) {
                        notify(false, "Could not create a sharing code. Try again."); callback.accept(false); return;
                    }
                    String code = new ConfigShareCode(result.key, privateMode ? result.deletionKey : null).formatted();
                    MinecraftClient.getInstance().keyboard.setClipboard(code);
                    notify(true, privateMode ? "Private BK code copied." : "Public BK code copied.");
                    callback.accept(true);
                }));
        } catch (Exception e) { sharing.remove(name); error(e); callback.accept(false); }
    }

    /** Accept old JSON exports as well as the public/private codes used by ClickGUI. */
    public void onConsumerString2(Consumer<Object> callback, String input) {
        dev.bkclient.systems.config.ConfigImport.resolve(input, draft -> {
            var mc = MinecraftClient.getInstance();
            dev.bkclient.ui.clicksys.ConfigImportScreen.open(mc.currentScreen, draft,
                name -> callback.accept(name == null ? Boolean.FALSE : new Imported(draft.kind(),name)));
        }, error -> { notify(false,error);callback.accept(null); });
    }
    public void onStringStringConsumer2(String description, String name, Consumer<Object> callback) {
        if (name == null || name.isBlank()) { notify(false, "Enter a config name."); callback.accept(null); return; }
        String clean = ConfigManager.sanitizeConfigName(name);
        if (manager().getConfig(clean, true) != null) { notify(false, "That name already exists."); callback.accept(null); return; }
        boolean ok = manager().createConfig(clean);
        if (ok) { descriptions.put(clean, description); saveMetadata(); notify(true, "Config created."); }
        callback.accept(ok ? clean : null);
    }
    private void error(Exception e) {
        BKClientInit.LOGGER.warn("ClickSys config operation failed", e);
        notify(false, e.getMessage() == null ? "Config operation failed." : e.getMessage());
    }
    private void notify(boolean success, String message) {
        BKClientInit.getInstance().getNotificationManager().addNotificationOther(
            success ? NotificationType.INFO : NotificationType.ERROR, dev.bkclient.systems.localization.Localizator.translate("Configs"), dev.bkclient.systems.localization.Localizator.translate(message));
    }
    private void saveMetadata() {
        try { SafeJsonFile.write(path, FileManager.GSON.toJsonTree(descriptions).getAsJsonObject(), json -> true); }
        catch (Exception e) { error(e); }
    }
}
