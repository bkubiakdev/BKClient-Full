package dev.bkclient.utility.sounds;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

public class ClientSoundInstance extends PositionedSoundInstance {
   private static final float DEFAULT_PITCH = 1.0F;
   private static volatile boolean playbackReady;
   private static boolean lifecycleRegistered;
   private final String fileName;

   public static void initializePlayback() {
      if (lifecycleRegistered) return;
      lifecycleRegistered = true;
      ClientTickEvents.END_CLIENT_TICK.register(client -> {
         // Module defaults/configs are restored inside Minecraft's constructor.
         // Lunar's sound hooks and resource loading are not ready at that point.
         playbackReady = client.getOverlay() == null && client.getSoundManager() != null;
      });
   }

   public ClientSoundInstance(String fileName, float volume) {
      super(
         Identifier.of(BKClientInit.MOD_ID + ":" + fileName),
         SoundCategory.MASTER,
         volume,
         1.0F,
         SoundInstance.createRandom(),
         false,
         0,
         AttenuationType.NONE,
         0.0,
         0.0,
         0.0,
         true
      );
      this.fileName = fileName;
   }

   public ClientSoundInstance(String fileName, float volume, float pitch) {
      super(
         Identifier.of(BKClientInit.MOD_ID + ":" + fileName),
         SoundCategory.MASTER,
         volume,
         pitch,
         SoundInstance.createRandom(),
         false,
         0,
         AttenuationType.NONE,
         0.0,
         0.0,
         0.0,
         true
      );
      this.fileName = fileName;
   }

   public void play(float volume) {
      this.playOnClientThread(new ClientSoundInstance(this.fileName, volume));
   }

   public void play(float volume, float pitch) {
      this.playOnClientThread(new ClientSoundInstance(this.fileName, volume, pitch));
   }

   private void playOnClientThread(ClientSoundInstance sound) {
      // Drop startup notifications rather than replaying a backlog after loading.
      if (!playbackReady) return;
      MinecraftClient client = MinecraftClient.getInstance();
      Runnable playback = () -> {
         // Recheck on the render thread: a resource reload may have started meanwhile.
         if (playbackReady && client.getOverlay() == null && client.getSoundManager() != null) {
            client.getSoundManager().play(sound);
         }
      };
      if (client.isOnThread()) {
         playback.run();
      } else {
         client.execute(playback);
      }
   }

   @Generated
   public String getFileName() {
      return this.fileName;
   }
}
