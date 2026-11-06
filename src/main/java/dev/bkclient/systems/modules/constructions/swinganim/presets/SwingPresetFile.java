package dev.bkclient.systems.modules.constructions.swinganim.presets;

import dev.bkclient.BKClientInit;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import lombok.Generated;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;

public class SwingPresetFile implements IMinecraft {
   private final File file;
   private final String fileName;
   private final Animation hoverAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation activeAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);

   public SwingPresetFile(String fileName) {
      this.fileName = fileName;
      File configsFolder = new File(FileManager.DIRECTORY + "/presets", "swing");
      if (!configsFolder.exists()) {
         configsFolder.mkdirs();
      }

      this.file = new File(configsFolder, fileName + "." + FileManager.DEFAULT_FILE_TYPE);
   }

   public void load() {
      if (!this.file.exists()) {
         BKClientInit.LOGGER.warn("Config file not found: {}", this.file.getAbsolutePath());
      } else {
         try {
            JsonObject jsonObject = dev.bkclient.systems.file.SafeJsonFile.read(this.file.toPath());
            JsonObject animation = jsonObject.getAsJsonObject("animation");

            for (Setting setting : BKClientInit.getInstance().getSwingManager().getSharedSettings().getSettings()) {
               if (animation.has(setting.getName())) {
                  setting.load(animation.get(setting.getName()));
               }
            }

            JsonObject startPhase = jsonObject.getAsJsonObject("startPhase");

            for (Setting settingx : BKClientInit.getInstance().getSwingManager().getStartPhase().getSettings()) {
               if (startPhase.has(settingx.getName())) {
                  settingx.load(startPhase.get(settingx.getName()));
               }
            }

            JsonObject endPhase = jsonObject.getAsJsonObject("endPhase");

            for (Setting settingxx : BKClientInit.getInstance().getSwingManager().getEndPhase().getSettings()) {
               if (endPhase.has(settingxx.getName())) {
                  settingxx.load(endPhase.get(settingxx.getName()));
               }
            }

            if (!this.fileName.equals("autosave")) {
               BKClientInit.getInstance().getSwingPresetManager().setCurrent(this);
            }
         } catch (Exception var10) {
            BKClientInit.LOGGER.error("Failed to load config file {}: {}", this.fileName, var10.getMessage());
         }
      }
   }

   public void save() {
      try {
JsonObject json = new JsonObject();
         JsonObject animation = new JsonObject();

         for (Setting setting : BKClientInit.getInstance().getSwingManager().getSharedSettings().getSettings()) {
            animation.add(setting.getName(), setting.save());
         }

         json.add("animation", animation);
         JsonObject startPhase = new JsonObject();

         for (Setting setting : BKClientInit.getInstance().getSwingManager().getStartPhase().getSettings()) {
            startPhase.add(setting.getName(), setting.save());
         }

         json.add("startPhase", startPhase);
         JsonObject endPhase = new JsonObject();

         for (Setting setting : BKClientInit.getInstance().getSwingManager().getEndPhase().getSettings()) {
            endPhase.add(setting.getName(), setting.save());
         }

         json.add("endPhase", endPhase);
         dev.bkclient.systems.file.SafeJsonFile.write(this.file.toPath(), json);
         System.out.println("saved");
         if (!this.fileName.equals("autosave")) {
            BKClientInit.getInstance().getSwingPresetManager().setCurrent(this);
         }
      } catch (IOException var10) {
         BKClientInit.LOGGER.error("Failed to save config file", var10);
      }
   }

   public void delete() {
      Path filePath = this.file.toPath();

      try {
         Files.delete(filePath);
         SwingPresetManager manager = BKClientInit.getInstance().getSwingPresetManager();
         manager.getSwingPresetFiles().remove(this);
         if (manager.getCurrent() != null && manager.getCurrent().getFileName().equals(this.fileName)) {
            manager.setCurrent(null);
            BKClientInit.getInstance().getSwingManager().setCurrent("autosave");
         }
         BKClientInit.LOGGER.info("Config file deleted: {}", filePath);
      } catch (NoSuchFileException var3) {
         BKClientInit.LOGGER.warn("Tried to delete a file that does not exist: {}", filePath);
      } catch (IOException var4) {
         MessageUtility.error(Text.of("An error occurred while deleting"));
         BKClientInit.LOGGER.warn("Failed to delete config file: {}. Reason: {}", filePath, var4.getMessage());
      }
   }

   private JsonObject getSettingsJsonObject(Module module) {
      JsonObject settingsObject = new JsonObject();

      for (Setting setting : module.getSettings()) {
         settingsObject.add(setting.getName(), setting.save());
      }

      return settingsObject;
   }

   @Generated
   public File getFile() {
      return this.file;
   }

   @Generated
   public String getFileName() {
      return this.fileName;
   }

   @Generated
   public Animation getHoverAnimation() {
      return this.hoverAnimation;
   }

   @Generated
   public Animation getActiveAnimation() {
      return this.activeAnimation;
   }
}
