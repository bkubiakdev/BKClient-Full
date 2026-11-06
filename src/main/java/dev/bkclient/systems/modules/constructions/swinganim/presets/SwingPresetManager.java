package dev.bkclient.systems.modules.constructions.swinganim.presets;

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

public class SwingPresetManager {
   private final List<SwingPresetFile> swingPresetFiles = new ArrayList<>();
   private SwingPresetFile current;
   private boolean initialized = false;

   public void handle() {
      if (this.initialized) return;
      this.scanPresetDirectory();
      SwingPresetFile autosave = this.getPreset("autosave");
      if (autosave == null) this.createPreset("autosave");
      else {
         autosave.load();
         BKClientInit.getInstance().getSwingManager().setCurrent("autosave");
      }
      this.initialized = true;
   }

   public void directionPreset() {
      String[] commands = new String[]{"explorer", new File(FileManager.DIRECTORY + "/presets", "swing").getAbsolutePath()};

      try {
         Runtime.getRuntime().exec(commands);
      } catch (Exception var3) {
         BKClientInit.LOGGER.error("Failed to access the config directory: {}", var3.getMessage());
      }
   }

   public void createPreset(String name) {
      if (name != null) {
         if (this.getPreset(name, false) != null) {
            BKClientInit.LOGGER.warn("Preset {} already exists", name);
         } else {
            SwingPresetFile preset = new SwingPresetFile(name);
            if (name.equals("autosave")) {
               preset.load();
            }

            preset.save();
            this.swingPresetFiles.add(preset);
         }
      }
   }

   public void listPresets() {
      MessageUtility.info(Text.of("Config list:"));

      for (SwingPresetFile swingPresetFile : this.swingPresetFiles) {
         int idx = this.swingPresetFiles.indexOf(swingPresetFile) + 1;
         MessageUtility.info(Text.of("[" + idx + "] " + swingPresetFile.getFileName()));
      }
   }

   private void scanPresetDirectory() {
      this.swingPresetFiles.clear();
      Path presetPath = Paths.get(FileManager.DIRECTORY + "/presets", "swing");
      if (!Files.exists(presetPath)) {
         try {
            Files.createDirectories(presetPath);
         } catch (IOException var5) {
            BKClientInit.LOGGER.error("Could not create the preset directory: {}", var5.getMessage());
         }
      } else {
         try (Stream<Path> stream = Files.list(presetPath)) {
             stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".bk")).forEach(path -> {
               String fileName = path.getFileName().toString();
               String name = fileName.substring(0, fileName.lastIndexOf(46));
               SwingPresetFile swingPresetFile = new SwingPresetFile(name);
               this.swingPresetFiles.add(swingPresetFile);
            });
         } catch (IOException var8) {
            BKClientInit.LOGGER.error("Could not scan the config directory: {}", var8.getMessage());
         }
      }
   }

   public SwingPresetFile getPreset(String name, boolean rescan) {
      if (rescan) {
         this.scanPresetDirectory();
      }

      return this.swingPresetFiles.stream().filter(swingPresetFile -> swingPresetFile.getFileName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public SwingPresetFile getPreset(String name) {
      return this.getPreset(name, false);
   }

   public SwingPresetFile getAutoSavePreset() {
      return this.getPreset("autosave", true);
   }

   public void refresh() {
      this.scanPresetDirectory();
   }

   @Generated
   public List<SwingPresetFile> getSwingPresetFiles() {
      return this.swingPresetFiles;
   }

   @Generated
   public SwingPresetFile getCurrent() {
      return this.current;
   }

   @Generated
   public boolean isInitialized() {
      return this.initialized;
   }

   @Generated
   public void setCurrent(SwingPresetFile current) {
      this.current = current;
   }
}
