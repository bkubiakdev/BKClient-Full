package dev.bkclient.systems.config;

import java.io.File;
import dev.bkclient.systems.file.SafeJsonFile;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWDropCallbackI;

public final class ConfigDropHandler implements IMinecraft {
   private static boolean initialized;

   public static void init() {
      if (!initialized) {
         initialized = true;
         long handle = mc.getWindow().getHandle();
         GLFWDropCallbackI[] previous = new GLFWDropCallbackI[1];
         GLFWDropCallbackI callback = (window, count, names) -> {
            if (previous[0] != null) {
               previous[0].invoke(window, count, names);
            }

            for (int i = 0; i < count; i++) {
               String path = GLFWDropCallback.getName(names, i);
               handleDrop(path);
            }
         };
         previous[0] = GLFW.glfwSetDropCallback(handle, callback);
      }
   }

   private static void handleDrop(String path) {
      try {
         File src = new File(path);
         if (!src.isFile()) {
            return;
         }

         if (!src.getName().endsWith(".bk") && !src.getName().endsWith(".sw")) {
            return;
         }

         File destDir = new File(FileManager.DIRECTORY, "configs");
         if (!destDir.exists() && !destDir.mkdirs()) {
            BKClientInit.LOGGER.error("Failed to create directory {}", destDir.getAbsolutePath());
            return;
         }

         String name = src.getName().substring(0, src.getName().lastIndexOf(46));
         File dest = new File(destDir, name + ".bk");
         SafeJsonFile.importFile(src.toPath(), dest.toPath(), ConfigFile::validStructure);
         ConfigManager manager = BKClientInit.getInstance().getConfigManager();
         manager.refresh();
         ConfigFile cfg = manager.getConfig(name);
         if (cfg == null) {
            cfg = new ConfigFile(name);
            manager.getConfigFiles().add(cfg);
         }

         if (!cfg.load()) return;
         MessageUtility.info(Text.of("Config " + name + " loaded"));
         BKClientInit.getInstance().getNotificationManager().addNotification(NotificationType.SUCCESS, Text.translatable("configs.loaded").getString());
      } catch (Exception var7) {
         BKClientInit.LOGGER.error("Failed to load dropped config {}", path, var7);
         MessageUtility.error(Text.of("Could not import config. Check the file format and write permissions."));
      }
   }

   @Generated
   private ConfigDropHandler() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
