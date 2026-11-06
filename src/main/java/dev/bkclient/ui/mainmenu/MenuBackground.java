package dev.bkclient.ui.mainmenu;

import dev.bkclient.BKClientInit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;

public final class MenuBackground {
   private static final Map<String, MenuBackground> BUILTINS = new LinkedHashMap<>();

   public static final MenuBackground CINEMATIC = builtin("CINEMATIC", "image/mainmenu/cinematic.png", "Cinematic", true, 1672.0F / 941.0F);
   public static final MenuBackground DARK = builtin("DARK", "image/mainmenu/dark.png", "Dark", false, 4480.0F / 3088.0F);
   public static final MenuBackground LIGHT = builtin("LIGHT", "image/mainmenu/light.png", "Light", false, 4480.0F / 3088.0F);

   private final String uniqueId;
   private final Identifier textureId;
   private final String displayName;
   private final boolean animated;
   private final float aspect;
   private final boolean custom;
   private final VideoBackgroundPlayer videoPlayer;

   private MenuBackground(
      String uniqueId, Identifier textureId, String displayName, boolean animated, float aspect, boolean custom, VideoBackgroundPlayer videoPlayer
   ) {
      this.uniqueId = uniqueId;
      this.textureId = textureId;
      this.displayName = displayName;
      this.animated = animated;
      this.aspect = aspect;
      this.custom = custom;
      this.videoPlayer = videoPlayer;
   }

   private static MenuBackground builtin(String id, String path, String displayName, boolean animated, float aspect) {
      MenuBackground background = new MenuBackground(id, BKClientInit.id(path), displayName, animated, aspect, false, null);
      BUILTINS.put(id, background);
      return background;
   }

   public static MenuBackground custom(String fileName, Identifier textureId, float aspect) {
      return new MenuBackground("file:" + fileName, textureId, fileName, false, aspect, true, null);
   }

   public static MenuBackground video(String fileName, VideoBackgroundPlayer player) {
      return new MenuBackground("file:" + fileName, null, fileName, true, player.getAspect(), true, player);
   }

   static MenuBackground deferred(String name) {
      return new MenuBackground("file:" + name, null, name, false, 16F / 9F, true, null);
   }

   private boolean deferred() { return custom && textureId == null && videoPlayer == null; }

   public Identifier id() {
      if (deferred()) return CustomBackgroundLoader.resolve(displayName).id();
      return this.videoPlayer != null ? this.videoPlayer.getTextureId() : this.textureId;
   }

   public String getUniqueId() {
      return this.uniqueId;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public boolean isAnimated() {
      if (deferred()) return CustomBackgroundLoader.resolve(displayName).isAnimated();
      return this.animated;
   }

   public float getAspect() {
      if (deferred()) return CustomBackgroundLoader.resolve(displayName).getAspect();
      return this.videoPlayer != null ? this.videoPlayer.getAspect() : this.aspect;
   }

   public boolean isCustom() {
      return this.custom;
   }

   public boolean isVideo() {
      if (deferred()) return CustomBackgroundLoader.resolve(displayName).isVideo();
      return this.videoPlayer != null;
   }

   public VideoBackgroundPlayer getVideoPlayer() {
      if (deferred()) return CustomBackgroundLoader.resolve(displayName).getVideoPlayer();
      return this.videoPlayer;
   }

   public static List<MenuBackground> builtins() {
      return new ArrayList<>(BUILTINS.values());
   }

   public static MenuBackground fromUniqueId(String uniqueId, MenuBackground fallback) {
      if (uniqueId == null) {
         return fallback;
      }
      MenuBackground builtin = BUILTINS.get(uniqueId);
      if (builtin != null) {
         return builtin;
      }
      if (uniqueId.startsWith("file:")) {
         MenuBackground custom = CustomBackgroundLoader.findByFileName(uniqueId.substring(5));
         if (custom != null) {
            return custom;
         }
      }
      return fallback;
   }
}
