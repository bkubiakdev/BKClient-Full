package dev.redstones.mediaplayerinfo.impl.win;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

public class WindowsMediaPlayerInfo implements MediaPlayerInfo {
   private static Function cleanupFunction;

   /** Bundled x64 DLL: C++ export, not JNI; both unused JNI arguments may be null. */
   public static void cleanup() {
      if (cleanupFunction != null) cleanupFunction.invokeVoid(new Object[]{Pointer.NULL, Pointer.NULL});
   }
   @Override
   public native List<IMediaSession> getMediaSessions();

   static {
      try {
         Path tempDir = Files.createTempDirectory("mediaplayerinfo-");
         Path dllFile = tempDir.resolve("MediaPlayerInfo.dll");

         try (InputStream inputStream = WindowsMediaPlayerInfo.class.getResourceAsStream("/mediaplayerinfo/natives/win/MediaPlayerInfo.dll")) {
            if (inputStream == null) {
               throw new IOException("Resource not found: /mediaplayerinfo/natives/win/MediaPlayerInfo.dll");
            }

            Files.write(dllFile, inputStream.readAllBytes());
         }

         System.load(dllFile.toAbsolutePath().toString());
         cleanupFunction = NativeLibrary.getInstance(dllFile.toAbsolutePath().toString()).getFunction(
            "?Java_dev_redstones_mediaplayerinfo_impl_win_WindowsMediaPlayerInfo_cleanup@@YAXPEAUJNIEnv_@@PEAV_jclass@@@Z");
         System.out.println("[WindowsMediaPlayerInfo] Successfully loaded native MediaPlayerInfo.dll from " + dllFile);

         try {
            Files.deleteIfExists(dllFile);
            Files.deleteIfExists(tempDir);
         } catch (IOException var6) {
            dllFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
         }
      } catch (Throwable var8) {
         System.err.println("[WindowsMediaPlayerInfo] CRITICAL: Failed to load native MediaPlayerInfo.dll: " + var8.getMessage());
         var8.printStackTrace();
      }
   }
}
