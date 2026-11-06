package dev.bkclient.systems.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;

public class ConfigManager {
   private final List<ConfigFile> configFiles = new ArrayList<>();
   private ConfigFile current;
   private boolean initialized = false;

   public static String sanitizeConfigName(String value) {
      return sanitizeConfigName(value, "imported_config");
   }

   public static String sanitizeConfigName(String value, String fallback) {
      String source = value;
      if (source == null || source.isBlank()) source = fallback;
      if (source == null || source.isBlank()) source = "imported_config";
      String sanitized = source.trim()
         .replaceAll("\\s+", "_")
         .replaceAll("[^a-zA-Z0-9_-]", "_")
         .replaceAll("_+", "_")
         .replaceAll("^_+|_+$", "");
      return sanitized.isBlank() ? "imported_config" : sanitized;
   }

   public void handle() {
      if (this.getAutoSaveConfig() == null) {
         this.createConfig("autosave");
      }

      if (!this.initialized) {
         this.scanConfigDirectory();
         this.initialized = true;
      }
   }

   public void directionConfig() {
      try {
         File configDir = new File(FileManager.DIRECTORY, "configs");
         String[] commands = new String[]{"explorer", configDir.getAbsolutePath()};
         Runtime.getRuntime().exec(commands);
      } catch (Exception var3) {
         BKClientInit.LOGGER.error("Failed to open the config folder: {}", var3.getMessage());
      }
   }

   public boolean createConfig(String name) {
      if (name != null) {
         this.refresh();
         ConfigFile config = new ConfigFile(name);
         if (name.equals("autosave") && (config.getFile().isFile()
            || Files.exists(dev.bkclient.systems.file.SafeJsonFile.backup(config.getFile().toPath())))) {
            if (!config.load()) return false;
         }

         if (!config.save()) return false;
         this.configFiles.removeIf(existing -> existing.getFileName().equalsIgnoreCase(name));
         this.configFiles.add(config);
         return true;
      }
      return false;
   }

   public void listConfigs() {
      this.refresh();
      MessageUtility.info(Text.of("Config list:"));

      for (ConfigFile configFile : this.configFiles) {
         int idx = this.configFiles.indexOf(configFile) + 1;
         MessageUtility.info(Text.of("[" + idx + "] " + configFile.getFileName()));
      }
   }

   private void scanConfigDirectory() {
      this.configFiles.clear();
      Path configPath = Paths.get(FileManager.DIRECTORY.getPath(), "configs");
      if (!Files.exists(configPath)) {
         try {
            Files.createDirectories(configPath);
         } catch (IOException var5) {
            BKClientInit.LOGGER.error("Failed to create the config directory: {}", var5.getMessage());
         }
      } else {
         try (Stream<Path> stream = Files.list(configPath)) {
             stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".bk")).forEach(path -> {
               String fileName = path.getFileName().toString();
               String name = fileName.substring(0, fileName.lastIndexOf(46));
               ConfigFile configFile = new ConfigFile(name);
               this.configFiles.add(configFile);
            });
         } catch (IOException var8) {
            BKClientInit.LOGGER.error("Failed to scan the config directory: {}", var8.getMessage());
         }
      }
   }

   public ConfigFile getConfig(String name, boolean rescan) {
      if (rescan) {
         this.scanConfigDirectory();
      }

      return this.configFiles.stream().filter(configFile -> configFile.getFileName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public ConfigFile getConfig(String name) {
      return this.getConfig(name, false);
   }

   public ConfigFile getAutoSaveConfig() {
      return this.current != null ? this.current : this.getConfig("autosave", false);
   }

   public void refresh() {
      this.scanConfigDirectory();
   }

   public void renameConfig(ConfigFile config, String newName) throws IOException {
      String oldName = config.getFileName();
      boolean active = this.current != null && this.current.getFileName().equalsIgnoreCase(oldName);
      config.renameTo(sanitizeConfigName(newName));
      if (active) {
         this.current = config;
         BKClientInit.getInstance().getFileManager().writeFile("client");
      }
      this.refresh();
   }

   public boolean importConfig(String name, String content) {
      if (BKClientInit.getInstance().isPanic()) return false;
      if (content == null || content.isBlank()) return false;
      try {
         String finalName = sanitizeConfigName(name);
         try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            if (json.has("meta_name") && !json.get("meta_name").isJsonNull()) {
               finalName = sanitizeConfigName(json.get("meta_name").getAsString(), finalName);
            }
         } catch (RuntimeException ignored) {
            // Older configs do not contain metadata; keep the requested fallback name.
         }

         File configDir = new File(FileManager.DIRECTORY, "configs");
         Files.createDirectories(configDir.toPath());
         com.google.gson.JsonObject importedJson = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
         dev.bkclient.systems.file.SafeJsonFile.write(new File(configDir, finalName + ".bk").toPath(), importedJson, ConfigFile::validStructure);
         this.refresh();
         ConfigFile imported = this.getConfig(finalName);
         return imported != null && imported.load();
      } catch (IOException | RuntimeException error) {
         BKClientInit.LOGGER.error("Failed to import config {}", name, error);
         MessageUtility.error(Text.of("Could not import config: " + name));
         return false;
      }
   }

   @Generated
   public List<ConfigFile> getConfigFiles() {
      return this.configFiles;
   }

   @Generated
   public ConfigFile getCurrent() {
      return this.current;
   }

   @Generated
   public boolean isInitialized() {
      return this.initialized;
   }

   @Generated
   public void setCurrent(ConfigFile current) {
      this.current = current;
   }
}
