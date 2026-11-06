package dev.bkclient.systems.config;

import dev.bkclient.BKClientInit;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.file.Files;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import java.io.IOException;
import java.util.List;
import lombok.Generated;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.file.SafeJsonFile;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.exception.UnknownModuleException;
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.ui.hud.impl.island.impl.ConfigStatus;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.sounds.ClientSounds;
import net.minecraft.text.Text;

public class ConfigFile implements IMinecraft {
   private List<Module> modules = BKClientInit.getInstance().getModuleManager().getModules();
   private File file;
   private String fileName;
   private JsonObject storedJson = new JsonObject();

   public ConfigFile(String fileName) {
      this.fileName = fileName;
      File configsFolder = new File(FileManager.DIRECTORY, "configs");
      if (!configsFolder.exists()) {
         configsFolder.mkdir();
      }

      this.file = new File(configsFolder, fileName + "." + FileManager.DEFAULT_FILE_TYPE);
      if (this.file.isFile() || Files.exists(SafeJsonFile.backup(this.file.toPath()))) {
         try {
            this.storedJson = SafeJsonFile.read(this.file.toPath(), ConfigFile::validStructure);
         } catch (Exception error) {
            BKClientInit.LOGGER.warn("Could not read config metadata: {}", this.fileName);
         }
      }
   }

   public boolean load() {
      if (BKClientInit.getInstance().isPanic()) return false;
      if (!this.file.isFile() && !Files.exists(SafeJsonFile.backup(this.file.toPath()))) {
         BKClientInit.LOGGER.warn("Config file not found: {}", this.file.getAbsolutePath());
         MessageUtility.error(Text.of("Config not found: " + this.fileName));
         return false;
      }
      try {
         JsonObject json = SafeJsonFile.read(this.file.toPath(), ConfigFile::validStructure);
         if (!json.has("modules") || !json.get("modules").isJsonArray()) {
            BKClientInit.LOGGER.warn("Invalid config format: missing modules in {}", this.fileName);
            return false;
         }
         this.loadFromJson(json);
         BKClientInit.getInstance().getConfigManager().setCurrent(this.fileName.equals("autosave") ? null : this);
         if (!this.fileName.equals("autosave")) {
            if (mc.getSoundManager() != null) {
               ClientSounds.MODULE.play(BKClientInit.getInstance().getModuleManager().getModule(Sounds.class).getVolume().getCurrentValue(), 1.0F);
            }
            BKClientInit.getInstance().getNotificationManager().addNotification(NotificationType.SUCCESS, Localizator.translate("configs.loaded"));
            ConfigStatus.show(this.fileName);
         }
         BKClientInit.LOGGER.info("Loaded config {}", this.fileName);
         return true;
      } catch (Exception error) {
         BKClientInit.LOGGER.error("Failed to load config {}", this.fileName, error);
         MessageUtility.error(Text.of("Could not load config: " + this.fileName));
         return false;
      }
   }
   public void loadFromJson(JsonObject jsonObject) {
      if (!validStructure(jsonObject)) throw new IllegalArgumentException("Invalid config structure");
      jsonObject = SkyConfigMigration.migrate(RetiredModulesMigration.migrate(FreecamConfigMigration.migrate(AuraConfigMigration.migrate(jsonObject))));
      boolean previousSuppress = AbstractSetting.suppressSave;
      AbstractSetting.suppressSave = true;
      java.util.Map<Setting, JsonElement> before = new java.util.LinkedHashMap<>();
      java.util.Map<Module, Integer> keys = new java.util.LinkedHashMap<>();
      java.util.Map<Module, Boolean> enabled = new java.util.LinkedHashMap<>();
      try {
         // Snapshot before applying anything, including changes caused by module callbacks.
         for (Module module : this.modules) {
            if (!fileName.equals("autosave") && module instanceof dev.bkclient.systems.modules.modules.bkmods.AutoMarket) continue;
            keys.put(module, module.getKey());
            enabled.put(module, module.isEnabled());
            for (Setting setting : module.getSettings()) before.put(setting, setting.save().deepCopy());
         }
         java.util.Map<Module, JsonObject> loaded = new java.util.LinkedHashMap<>();
         for (JsonElement entry : jsonObject.getAsJsonArray("modules")) {
            JsonObject object = entry.getAsJsonObject();
            try {
               Module module = BKClientInit.getInstance().getModuleManager().getModule(object.get("name").getAsString());
               if (!fileName.equals("autosave") && module instanceof dev.bkclient.systems.modules.modules.bkmods.AutoMarket) continue;
               loaded.put(module, object);
               if (object.has("settings")) {
                  JsonObject settings = object.getAsJsonObject("settings");
                  if (module instanceof dev.bkclient.systems.modules.modules.visuals.HandsShader) {
                     settings = dev.bkclient.systems.modules.modules.visuals.HandsShader.migrateSettings(settings);
                     object.add("settings", settings);
                  }
                  if (module instanceof dev.bkclient.systems.modules.modules.visuals.Lyrics3D lyrics) settings = lyrics.options.withDefaults(settings);
                  for (Setting setting : module.getSettings()) {
                     if (settings.has(setting.getName())) setting.load(settings.get(setting.getName()));
                  }
               }
            } catch (UnknownModuleException unknown) {
               BKClientInit.LOGGER.warn("Preserving unknown module: {}", object.get("name"));
            }
         }
         // Enable modules only after every setting has loaded successfully.
         for (var entry : loaded.entrySet()) {
            Module module = entry.getKey();
            JsonObject object = entry.getValue();
            if (!(module instanceof MenuModule)) {
               module.setKey(object.has("key") ? object.get("key").getAsInt() : module.getKey());
               boolean on = object.has("enabled") && object.get("enabled").getAsBoolean();
               module.setEnabled(on && !isSessionOnly(module), true);
            }
         }
         this.storedJson = jsonObject.deepCopy();
      } catch (RuntimeException failure) {
         for (var entry : before.entrySet()) {
            try { entry.getKey().load(entry.getValue()); }
            catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
         }
         for (Module module : keys.keySet()) {
            try {
               module.setKey(keys.get(module));
               module.setEnabled(enabled.get(module), true);
            } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
         }
         throw failure;
      } finally {
         AbstractSetting.suppressSave = previousSuppress;
      }
   }
   public String saveToString() {
      this.storedJson = SkyConfigMigration.migrate(RetiredModulesMigration.migrate(FreecamConfigMigration.migrate(AuraConfigMigration.migrate(this.storedJson))));
      JsonObject json = this.storedJson.deepCopy();
      JsonArray modulesJsonArray = this.getModulesJsonArray();
      json.add("modules", modulesJsonArray);
      json.addProperty("meta_name", this.fileName);
      if (!json.has("server")) {
         json.addProperty("server", dev.bkclient.utility.game.server.ServerUtility.getCurrentServerAddress("Singleplayer"));
      }
      if (!json.has("favicon")) {
         String favicon = dev.bkclient.utility.game.server.ServerUtility.getCurrentServerFaviconBase64();
         if (favicon != null) json.addProperty("favicon", favicon);
      }
      return FileManager.GSON.toJson(json);
   }

   public boolean save() {
      if (AbstractSetting.suppressSave || BKClientInit.getInstance().isPanic()) return false;
      try {
         String data = saveToString();
         SafeJsonFile.write(this.file.toPath(), JsonParser.parseString(data).getAsJsonObject(), ConfigFile::validStructure);
         this.storedJson = JsonParser.parseString(data).getAsJsonObject();
         if (!this.fileName.equals("autosave")) {
            BKClientInit.getInstance().getConfigManager().setCurrent(this);
         }

         BKClientInit.LOGGER.info("Successfully saved config " + this.fileName);
         return true;
      } catch (IOException | RuntimeException var8) {
         BKClientInit.LOGGER.error("Failed to save config file", var8);
         MessageUtility.error(Text.of("Could not save config: " + this.fileName));
         return false;
      }
   }

   static boolean validStructure(JsonObject json) {
      if (!json.has("modules") || !json.get("modules").isJsonArray()) return false;
      for (JsonElement entry : json.getAsJsonArray("modules")) {
         if (!entry.isJsonObject()) return false;
         JsonObject module = entry.getAsJsonObject();
         if (!module.has("name") || !module.get("name").isJsonPrimitive()) return false;
         if (module.has("enabled") && (!module.get("enabled").isJsonPrimitive() || !module.getAsJsonPrimitive("enabled").isBoolean())) return false;
         if (module.has("key") && (!module.get("key").isJsonPrimitive() || !module.getAsJsonPrimitive("key").isNumber())) return false;
         if (module.has("settings") && !module.get("settings").isJsonObject()) return false;
      }
      return true;
   }

   public void delete() {
      ConfigManager manager = BKClientInit.getInstance().getConfigManager();
      if (manager.getCurrent() != null
         && manager.getCurrent().getFileName().equalsIgnoreCase(this.fileName)) {
         ConfigFile autosave = manager.getConfig("autosave", true);
         if (autosave == null || !autosave.load()) {
            MessageUtility.error(Text.of("Could not switch to autosave; config was not deleted"));
            BKClientInit.LOGGER.warn("Refusing to delete active config without a valid autosave: {}", this.fileName);
            return;
         }
      }
      if (this.file.exists() && this.file.delete()) {
         try { Files.deleteIfExists(SafeJsonFile.backup(this.file.toPath())); }
         catch (IOException error) { BKClientInit.LOGGER.warn("Could not remove config backup", error); }
         manager.getConfigFiles().removeIf(config -> config.getFileName().equalsIgnoreCase(this.fileName));
         if (manager.getCurrent() != null && manager.getCurrent().getFileName().equalsIgnoreCase(this.fileName)) {
            manager.setCurrent(null);
            BKClientInit.getInstance().getFileManager().writeFile("client");
         }
         MessageUtility.info(Text.of("Config " + this.fileName + " deleted successfully"));
         BKClientInit.LOGGER.info("Config file deleted: {}", this.file.getAbsolutePath());
      } else {
         MessageUtility.error(Text.of("An error occurred while deleting the config"));
         BKClientInit.LOGGER.warn("Failed to delete config file: {}", this.file.getAbsolutePath());
      }
   }

   private JsonArray getModulesJsonArray() {
      JsonArray modulesJsonArray = new JsonArray();
      java.util.Map<String, JsonObject> storedModules = new java.util.LinkedHashMap<>();
      if (this.storedJson.has("modules") && this.storedJson.get("modules").isJsonArray()) {
         for (JsonElement element : this.storedJson.getAsJsonArray("modules")) {
            if (element.isJsonObject() && element.getAsJsonObject().has("name")) {
               JsonObject object = element.getAsJsonObject();
               String storedName = object.get("name").getAsString();
               if ("ShaderESP".equalsIgnoreCase(storedName.replace(" ", ""))) storedName = "Chams";
               if ("Auto-Zmianka".equalsIgnoreCase(storedName)) storedName = "Auto Zmianka";
               storedModules.put(storedName, object.deepCopy());
            }
         }
      }

      for (Module module : this.modules) {
         JsonObject moduleObject = storedModules.remove(module.getName());
         if (!fileName.equals("autosave") && module instanceof dev.bkclient.systems.modules.modules.bkmods.AutoMarket) continue;
         if (moduleObject == null) moduleObject = new JsonObject();
         moduleObject.addProperty("name", module.getName());
         moduleObject.addProperty("enabled", module.isEnabled() && !isSessionOnly(module));
         moduleObject.addProperty("key", module.getKey());
         JsonObject settings = moduleObject.has("settings") && moduleObject.get("settings").isJsonObject()
            ? moduleObject.getAsJsonObject("settings") : new JsonObject();
         for (var entry : this.getSettingsJsonObject(module).entrySet()) settings.add(entry.getKey(), entry.getValue());
         moduleObject.add("settings", settings);
         modulesJsonArray.add(moduleObject);
      }
      storedModules.values().forEach(modulesJsonArray::add);

      return modulesJsonArray;
   }

   private JsonObject getSettingsJsonObject(Module module) {
      JsonObject settingsObject = new JsonObject();

      for (Setting setting : module.getSettings()) {
         try {
            JsonElement value = setting.save();
            if (value != null) {
               settingsObject.add(setting.getName(), value);
            }
         } catch (RuntimeException error) {
            BKClientInit.LOGGER.warn("Failed to save setting {} for module {}", setting.getName(), module.getName(), error);
         }
      }

      return settingsObject;
   }

   private static boolean isSessionOnly(Module module) {
      String normalized = module.getName().replace(" ", "").toLowerCase(java.util.Locale.ROOT);
      return normalized.equals("airdrop") || normalized.equals("panic") || normalized.equals("automarket")
         || normalized.equals("minehelper")
         || normalized.equals("chatspammer");
   }

   @Generated
   public String getFileName() {
      return this.fileName;
   }

   public String getDisplayName() {
      return this.storedJson.has("meta_name") && !this.storedJson.get("meta_name").isJsonNull()
         ? this.storedJson.get("meta_name").getAsString() : this.fileName;
   }

   public void renameTo(String name) throws IOException {
      this.storedJson = SafeJsonFile.read(this.file.toPath(), ConfigFile::validStructure);
      File target = new File(this.file.getParentFile(), name + "." + FileManager.DEFAULT_FILE_TYPE);
      if (target.exists()) throw new IOException("Config already exists: " + name);
      JsonObject renamed = this.storedJson.deepCopy();
      renamed.addProperty("meta_name", name);
      SafeJsonFile.write(target.toPath(), renamed, ConfigFile::validStructure);
      Files.delete(this.file.toPath());
      Files.deleteIfExists(SafeJsonFile.backup(this.file.toPath()));
      this.file = target;
      this.fileName = name;
      this.storedJson = renamed;
   }

   public File getFile() {
      return this.file;
   }
}
