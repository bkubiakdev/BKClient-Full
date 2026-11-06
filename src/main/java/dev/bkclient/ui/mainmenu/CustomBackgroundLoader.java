package dev.bkclient.ui.mainmenu;

import dev.bkclient.systems.file.FileManager;
import dev.bkclient.utility.render.DrawUtility;
import java.io.File;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Catalog entries are cheap; only the six most recently displayed backgrounds own native resources. */
public final class CustomBackgroundLoader {
   private static final File FOLDER = new File(FileManager.DIRECTORY, "backgrounds");
   private static final Map<String, MenuBackground> CATALOG = new LinkedHashMap<>();
   private static final LinkedHashMap<String, Resident> RESIDENT = new LinkedHashMap<>(8, .75F, true);
   private static final Map<String, Long> RETRY_AT = new HashMap<>();
   private record Resident(MenuBackground background, long modified, long length) {}
   private CustomBackgroundLoader() {}

   public static File getFolder() {
      if (!FOLDER.exists()) FOLDER.mkdirs();
      return FOLDER;
   }

   public static List<MenuBackground> scan() {
      File[] files = getFolder().listFiles(file -> file.isFile() && supported(file.getName()));
      Set<String> present = new HashSet<>();
      if (files != null) for (File file : files) {
         present.add(file.getName());
         CATALOG.computeIfAbsent(file.getName(), MenuBackground::deferred);
      }
      CATALOG.keySet().removeIf(name -> !present.contains(name));
      RETRY_AT.keySet().retainAll(present);
      var iterator = RESIDENT.entrySet().iterator();
      while (iterator.hasNext()) {
         var entry = iterator.next();
         if (!present.contains(entry.getKey())) { release(entry.getValue().background()); iterator.remove(); }
      }
      return new ArrayList<>(CATALOG.values());
   }

   public static MenuBackground findByFileName(String name) {
      // Resolve just the selected file; never rescan/decode the entire directory every frame.
      if (!new File(name).getName().equals(name) || !supported(name) || !new File(getFolder(), name).isFile()) return null;
      return CATALOG.computeIfAbsent(name, MenuBackground::deferred);
   }

   static MenuBackground resolve(String name) {
      File file = new File(FOLDER, name);
      Resident existing = RESIDENT.get(name);
      if (existing != null) {
         if (existing.modified() == file.lastModified() && existing.length() == file.length()) return existing.background();
         RESIDENT.remove(name);
         release(existing.background());
      }
      if (System.nanoTime() < RETRY_AT.getOrDefault(name, 0L)) return MenuBackground.CINEMATIC;
      // Evict before allocation so loading a seventh image does not temporarily double the budget.
      while (RESIDENT.size() >= 6) {
         var iterator = RESIDENT.entrySet().iterator();
         var oldest = iterator.next(); iterator.remove(); release(oldest.getValue().background());
      }
      MenuBackground loaded = load(file);
      if (loaded == null) {
         RETRY_AT.put(name, System.nanoTime() + 5_000_000_000L);
         return MenuBackground.CINEMATIC;
      }
      RETRY_AT.remove(name);
      RESIDENT.put(name, new Resident(loaded, file.lastModified(), file.length()));
      return loaded;
   }

   private static boolean supported(String name) {
      String lower = name.toLowerCase(Locale.ROOT);
      return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".mp4");
   }

   private static void release(MenuBackground background) {
      if (background.isVideo()) background.getVideoPlayer().close();
      else DrawUtility.releaseTexture(background.id());
   }

   public static void shutdown() {
      RESIDENT.values().forEach(entry -> release(entry.background()));
      RESIDENT.clear(); CATALOG.clear(); RETRY_AT.clear();
   }

   private static MenuBackground load(File file) {
      // A collision-free id even when two filenames differ only in punctuation/case.
      String key = UUID.nameUUIDFromBytes(file.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
      Identifier id = Identifier.of("bkclient", "dynamic/menu_background/" + key);
      if (file.getName().toLowerCase(Locale.ROOT).endsWith(".mp4")) {
         return MenuBackground.video(file.getName(), new VideoBackgroundPlayer(file, id));
      }
      NativeImage image = null;
      NativeImageBackedTexture texture = null;
      boolean transferred = false;
      try {
         var decoded = dev.bkclient.utility.render.BoundedImageReader.read(file, 2048);
         try {
            image = new NativeImage(decoded.getWidth(), decoded.getHeight(), false);
            for (int y = 0; y < decoded.getHeight(); y++) for (int x = 0; x < decoded.getWidth(); x++)
               image.setColorArgb(x, y, decoded.getRGB(x, y));
         } finally { decoded.flush(); }
         float aspect = image.getWidth() / (float) image.getHeight();
         texture = new NativeImageBackedTexture(image);
         MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
         transferred = true;
         return MenuBackground.custom(file.getName(), id, aspect);
      } catch (IOException | RuntimeException failure) {
         dev.bkclient.BKClientInit.LOGGER.warn("Failed to load menu background: {}", file.getName(), failure);
         return null;
      } finally {
         if (!transferred) {
            if (texture != null) { DrawUtility.releaseTexture(id); texture.close(); }
            else if (image != null) image.close();
         }
      }
   }
}
