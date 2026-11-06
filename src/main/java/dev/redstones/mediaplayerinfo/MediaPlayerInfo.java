package dev.redstones.mediaplayerinfo;

import dev.redstones.mediaplayerinfo.impl.DummyMediaPlayerInfo;
import dev.redstones.mediaplayerinfo.impl.win.WindowsMediaBackend;
import java.util.List;

public interface MediaPlayerInfo {
   MediaPlayerInfo INSTANCE = MediaPlayerInfo.SystemMediaPlayerInfo.getInstance();

   List<IMediaSession> getMediaSessions();

   default void close() {}

   class SystemMediaPlayerInfo {
      private static final MediaPlayerInfo instance = System.getProperty("os.name").toLowerCase().startsWith("windows")
         ? new WindowsMediaBackend()
         : new DummyMediaPlayerInfo();

      public static MediaPlayerInfo getInstance() {
         return instance;
      }
   }
}
