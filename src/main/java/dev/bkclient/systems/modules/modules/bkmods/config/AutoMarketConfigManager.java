package dev.bkclient.systems.modules.modules.bkmods.config;

import com.google.gson.JsonObject;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.config.ConfigManager;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.file.SafeJsonFile;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.utility.game.XSK;
import dev.bkclient.utility.game.server.ServerUtility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AutoMarketConfigManager {
    private static final File DIRECTORY = new File(FileManager.DIRECTORY, XSK.DIR_NAME);
    private static final int CLOUD_KEY_LENGTH = 12;
    private final List<ConfigInfo> configs = new ArrayList<>();

    public static class ConfigInfo {
        public String name;
        public String filename;
        public long date;
        public String server;
        public String favicon;

        public ConfigInfo(String name, String filename, long date, String server, String favicon) {
            this.name = name;
            this.filename = filename;
            this.date = date;
            this.server = server;
            this.favicon = favicon;
        }
    }

    private final AutoMarket autoMarket;

    public AutoMarketConfigManager(AutoMarket autoMarket) {
        this.autoMarket = autoMarket;
        if (!DIRECTORY.exists()) {
            DIRECTORY.mkdir();
        }
        refresh(false);
    }

    public static String sanitizeProfileName(String value) {
        return ConfigManager.sanitizeConfigName(value);
    }

    private static File getProfileFile(String name) {
        if (name != null && !name.isBlank()) {
            File exactFile = new File(DIRECTORY, name + ".json");
            if (exactFile.exists()) {
                return exactFile;
            }
        }
        return new File(DIRECTORY, sanitizeProfileName(name) + ".json");
    }

    public boolean profileExists(String name) {
        return getProfileFile(name).exists();
    }

    private File normalizeConfigFile(File file) {
        try { SafeJsonFile.read(file.toPath(), AutoMarketConfigManager::validProfile); }
        catch (IOException error) {
            BKClientInit.LOGGER.warn("Preserving unreadable profile {}", file.getName(), error);
            return file;
        }
        String filename = file.getName().substring(0, file.getName().length() - ".json".length());
        String safeBase = sanitizeProfileName(filename);
        String finalName = safeBase;
        File targetFile = new File(DIRECTORY, finalName + ".json");
        int i = 1;

        while (!file.getName().equals(targetFile.getName()) && targetFile.exists()) {
            finalName = safeBase + "_" + i;
            targetFile = new File(DIRECTORY, finalName + ".json");
            i++;
        }

        File normalizedFile = file;
        if (!file.getName().equals(targetFile.getName())) {
            if (file.renameTo(targetFile)) {
                normalizedFile = targetFile;
            } else {
                finalName = filename;
            }
        }

        patchConfigName(normalizedFile, finalName);
        syncActiveProfileName(filename, finalName);
        return normalizedFile;
    }

    private void patchConfigName(File file, String name) {
        try {
            JsonObject json = SafeJsonFile.read(file.toPath(), AutoMarketConfigManager::validProfile);
            if (json.has(XSK.JSON_NAME) && name.equals(json.get(XSK.JSON_NAME).getAsString())) {
                return;
            }
            json.addProperty(XSK.JSON_NAME, name);
            SafeJsonFile.write(file.toPath(), json, AutoMarketConfigManager::validProfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void syncActiveProfileName(String oldName, String newName) {
        if (autoMarket == null || oldName.equals(newName)) {
            return;
        }
        String activeProfile = autoMarket.getActiveProfile();
        String configuredProfile = autoMarket.configName.getText();
        if ((activeProfile != null && oldName.equalsIgnoreCase(activeProfile))
                || (configuredProfile != null && oldName.equalsIgnoreCase(configuredProfile))) {
            autoMarket.setActiveProfile(newName);
            autoMarket.setLoadingProfile(true);
            try {
                autoMarket.configName.setText(newName);
            } finally {
                autoMarket.setLoadingProfile(false);
            }
        }
    }

    private static String sanitizeServer(String value) {
        if (value == null || value.isBlank() || value.replace("\u00A7", "").isBlank()) {
            return XSK.DEFAULT_SERVER;
        }
        return value;
    }

    public void refresh() {
        refresh(true);
    }

    private void refresh(boolean normalizeNames) {
        configs.clear();
        File[] files = DIRECTORY.listFiles((dir, name) -> name.endsWith(".json")
                && !name.equals("autocfg-overrides.json"));
        if (files != null) {
            for (File file : files) {
                if (normalizeNames) {
                    file = normalizeConfigFile(file);
                }
                String filename = file.getName().replace(".json", "");
                try {
                    JsonObject json = SafeJsonFile.read(file.toPath(), AutoMarketConfigManager::validProfile);
                    String name = filename;
                    long date = json.has(XSK.JSON_DATE) ? json.get(XSK.JSON_DATE).getAsLong() : 0;
                    String server = json.has(XSK.JSON_SERVER) ? sanitizeServer(json.get(XSK.JSON_SERVER).getAsString())
                            : XSK.DEFAULT_SERVER;
                    String favicon = json.has(XSK.JSON_FAVICON) ? json.get(XSK.JSON_FAVICON).getAsString() : null;
                    configs.add(new ConfigInfo(name, filename, date, server, favicon));
                } catch (Exception e) {
                    e.printStackTrace();
                    configs.add(new ConfigInfo(filename, filename, 0, XSK.DEFAULT_SERVER, null));
                }
            }
        }
    }

    public void save(String name) {
        if (autoMarket == null || autoMarket.isLoading())
            return;
        String finalName = sanitizeProfileName(name);

        File file = new File(DIRECTORY, finalName + ".json");
        JsonObject json;
        try {
            json = file.exists() ? SafeJsonFile.read(file.toPath(), AutoMarketConfigManager::validProfile) : new JsonObject();
        } catch (IOException error) {
            BKClientInit.LOGGER.error("Refusing to overwrite unreadable market profile {}", finalName, error);
            return;
        }
        json.addProperty(XSK.JSON_NAME, finalName);
        json.addProperty(XSK.JSON_DATE, System.currentTimeMillis());
        String currentServer = ServerUtility.getCurrentServerAddress(XSK.DEFAULT_SERVER);
        if (!currentServer.equals(XSK.DEFAULT_SERVER)) {
            json.addProperty(XSK.JSON_SERVER, currentServer);
            String favicon = ServerUtility.getCurrentServerFaviconBase64();
            if (favicon != null) {
                json.addProperty(XSK.JSON_FAVICON, favicon);
            }
        }

        JsonObject settings = json.has(XSK.JSON_SETTINGS) ? json.getAsJsonObject(XSK.JSON_SETTINGS).deepCopy() : new JsonObject();
        for (Setting setting : autoMarket.getMarketSettings()) {
            settings.add(setting.getName(), setting.save());
        }
        json.add(XSK.JSON_SETTINGS, settings);

        if (!DIRECTORY.exists())
            DIRECTORY.mkdirs();
        try {
            SafeJsonFile.write(file.toPath(), json, AutoMarketConfigManager::validProfile);
        } catch (IOException e) {
            BKClientInit.LOGGER.error("Failed to save market profile {}", finalName, e);
            return;
        }

    }

    private void resetToDefaults() {
        autoMarket.setLoading(true);
        try {
            autoMarket.refreshRate.setCurrentValue(77.0f);
            autoMarket.confirmDelay.setCurrentValue(173.0f);
            autoMarket.restartDelay.setCurrentValue(1500.0f);
            autoMarket.aggressiveMode.setEnabled(false);
            autoMarket.autoJump.setEnabled(false);
            autoMarket.autoSellEnabled.setEnabled(false);
            autoMarket.showHistory.setEnabled(true);
            autoMarket.sellCommand.text(XSK.DEFAULT_SELL_CMD);
            autoMarket.marketCommand.text(XSK.DEFAULT_MARKET_CMD);
            autoMarket.customMarketTitle.text("");
            autoMarket.confirmSlotSetting.setCurrentValue(11.0f);
            autoMarket.sortingSlotSetting.setCurrentValue(53.0f);
            autoMarket.refreshSlotSetting.setCurrentValue(0.0f);
            autoMarket.nextPageSlotSetting.setCurrentValue(26.0f);
            autoMarket.sortingKeyword.text(XSK.DEFAULT_SORT_KW);
            autoMarket.confirmItemName.text("");
            autoMarket.loreRegex.text("(?i).*Koszt.*?([\\d.,]+(?:mld|[km])?).*");
            autoMarket.loreSellerRegex.text("");
            autoMarket.itemRegistry.getValue().clear();
            autoMarket.itemRegistry.notifyChanged();
        } finally {
            autoMarket.setLoading(false);
        }
    }

    public static boolean validProfile(JsonObject json) {
        return json.has(XSK.JSON_SETTINGS) && json.get(XSK.JSON_SETTINGS).isJsonObject();
    }

    public void load(String name) { loadProfile(name); }
    public boolean loadProfile(String name) {
        if (autoMarket == null)
            return false;
        File file = getProfileFile(name);
        if (!file.exists() && !java.nio.file.Files.exists(SafeJsonFile.backup(file.toPath()))) {
            BKClientInit.LOGGER.warn("Market profile not found: {}", name);
            return false;
        }

        try {
            JsonObject json = SafeJsonFile.read(file.toPath(), AutoMarketConfigManager::validProfile);
            JsonObject settings = json.getAsJsonObject(XSK.JSON_SETTINGS);

            if (settings != null) {
                boolean previousLoading = autoMarket.isLoading();
                boolean previousSuppress = dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave;
                java.util.Map<Setting, com.google.gson.JsonElement> previous = new java.util.LinkedHashMap<>();
                for (Setting setting : autoMarket.getMarketSettings()) previous.put(setting, setting.save().deepCopy());
                autoMarket.setLoading(true);
                dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave = true;
                try {
                    for (Setting setting : autoMarket.getMarketSettings()) {
                        String fullName = setting.getName();
                        if (settings.has(fullName)) {
                            setting.load(settings.get(fullName));
                        } else {
                            String shortName = fullName.substring(fullName.lastIndexOf('.') + 1);
                            if (settings.has(shortName)) {
                                setting.load(settings.get(shortName));
                            }
                        }
                    }
                } catch (RuntimeException invalid) {
                    for (var entry : previous.entrySet()) {
                        try { entry.getKey().load(entry.getValue()); }
                        catch (RuntimeException rollback) { invalid.addSuppressed(rollback); }
                    }
                    throw invalid;
                } finally {
                    autoMarket.setLoading(previousLoading);
                    dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave = previousSuppress;
                }
            }

            if (autoMarket != null) {
                autoMarket.syncControllersConfig();
            }
            // Dynamic Island: animacja "Config <nazwa> Loaded" + dzwiek przy zaladowaniu profilu rynku.
            // Tylko gdy to swiadoma zmiana usera (nie startowe/wewnetrzne wczytanie profilu).
            if (!autoMarket.isLoading()) {
                dev.bkclient.ui.hud.impl.island.impl.ConfigStatus.show(name);
                if (net.minecraft.client.MinecraftClient.getInstance() != null
                        && net.minecraft.client.MinecraftClient.getInstance().getSoundManager() != null) {
                    try {
                        dev.bkclient.utility.sounds.ClientSounds.MODULE.play(BKClientInit.getInstance()
                                .getModuleManager()
                                .getModule(dev.bkclient.systems.modules.modules.other.Sounds.class)
                                .getVolume().getCurrentValue(), 1.0f);
                    } catch (Exception ignored) {
                    }
                }
            }
            return true;
        } catch (Exception e) {
            BKClientInit.getInstance().getNotificationManager().addNotificationOther(
                    dev.bkclient.systems.notifications.NotificationType.ERROR,
                    "Auto-Market", Localizator.translate("modules.notifications.auto_market.config_load_error", name));
            return false;
        }
    }

    public void delete(String name) {
        File file = new File(DIRECTORY, name + ".json");
        if (!file.exists()) {
            file = new File(DIRECTORY, sanitizeProfileName(name) + ".json");
        }
        if (file.exists() && file.delete() && autoMarket != null) {
            try { java.nio.file.Files.deleteIfExists(SafeJsonFile.backup(file.toPath())); }
            catch (IOException error) { BKClientInit.LOGGER.warn("Could not remove market backup", error); }
            String deletedProfile = file.getName().replace(".json", "");
            String activeProfile = autoMarket.getActiveProfile();
            String configuredProfile = autoMarket.configName.getText();
            if ((activeProfile != null && deletedProfile.equalsIgnoreCase(activeProfile))
                    || (configuredProfile != null && deletedProfile.equalsIgnoreCase(configuredProfile))) {
                autoMarket.setActiveProfile("default");
                autoMarket.setLoadingProfile(true);
                try {
                    autoMarket.configName.setText("default");
                } finally {
                    autoMarket.setLoadingProfile(false);
                }
            }
        }
        refresh();
    }

    public boolean rename(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty())
            return false;
        final String trimmedName = sanitizeProfileName(newName);
        File oldFile = getProfileFile(oldName);
        File newFile = new File(DIRECTORY, trimmedName + ".json");
        if (!oldFile.exists() || newFile.exists())
            return false;
        try {
            JsonObject json = SafeJsonFile.read(oldFile.toPath(), AutoMarketConfigManager::validProfile);
            json.addProperty(XSK.JSON_NAME, trimmedName);
            SafeJsonFile.write(newFile.toPath(), json, AutoMarketConfigManager::validProfile);
            java.nio.file.Files.delete(oldFile.toPath());
            java.nio.file.Files.deleteIfExists(SafeJsonFile.backup(oldFile.toPath()));
        } catch (Exception e) {
            BKClientInit.LOGGER.error("Failed to rename market profile", e);
            return false;
        }
        // If this was the active profile, update activeProfile + configName without
        // triggering save/load
        if (autoMarket != null && oldName.equalsIgnoreCase(autoMarket.getActiveProfile())) {
            autoMarket.setActiveProfile(trimmedName);
            autoMarket.setLoadingProfile(true);
            try {
                autoMarket.configName.setText(trimmedName);
            } finally {
                autoMarket.setLoadingProfile(false);
            }
        }
        refresh();
        return true;
    }

    public void exportToCloud(String name, boolean privateMode) {
        String finalName = sanitizeProfileName(name);
        save(finalName);
        File file = new File(DIRECTORY, finalName + ".json");
        if (!file.exists()) {
            BKClientInit.getInstance().getNotificationManager().addNotificationOther(
                    dev.bkclient.systems.notifications.NotificationType.ERROR, "Auto-Market",
                    Localizator.translate("modules.notifications.auto_market.config_file_missing"));
            return;
        }

        try {
            String content = java.nio.file.Files.readString(file.toPath());
            BKClientInit.getInstance().getNotificationManager().addNotificationOther(
                    dev.bkclient.systems.notifications.NotificationType.INFO,
                    "Auto-Market", Localizator.translate("modules.notifications.auto_market.config_sharing"));
            dev.bkclient.utility.game.CloudConfigUtility.uploadConfig(finalName, content).thenAccept(result -> {
                if (result != null) {
                    String code = XSK.CLOUD_PREFIX + result.key;
                    if (privateMode && result.deletionKey != null) {
                        code += result.deletionKey;
                    }
                    BKClientInit.mc.keyboard.setClipboard(code);
                    BKClientInit.getInstance().getNotificationManager().addNotificationOther(
                            dev.bkclient.systems.notifications.NotificationType.SUCCESS,
                            "Auto-Market", Localizator.translate(privateMode
                                    ? "modules.notifications.auto_market.config_private_shared"
                                    : "modules.notifications.auto_market.config_shared"));
                } else {
                    BKClientInit.getInstance().getNotificationManager().addNotificationOther(
                            dev.bkclient.systems.notifications.NotificationType.ERROR, "Auto-Market",
                            Localizator.translate("modules.notifications.auto_market.config_cloud_error"));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void importFromCloud(String bkCode) {
        dev.bkclient.ui.clicksys.port.data.ConfigSync.getInstance().onConsumerString2(result -> {}, bkCode);
    }
    public List<ConfigInfo> getConfigs() {
        return configs;
    }
}
