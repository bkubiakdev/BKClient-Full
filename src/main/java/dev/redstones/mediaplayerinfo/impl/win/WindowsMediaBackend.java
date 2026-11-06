package dev.redstones.mediaplayerinfo.impl.win;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.util.List;

/** Serializes the shared native manager and releases it before JVM teardown. */
public final class WindowsMediaBackend implements MediaPlayerInfo {
   private final WindowsMediaPlayerInfo nativeApi = new WindowsMediaPlayerInfo();
   private final Object lock = new Object();
   private volatile boolean closed;
   private boolean released;

   @Override public List<IMediaSession> getMediaSessions() {
      synchronized (lock) {
         if (closed) return List.of();
         List<IMediaSession> sessions = nativeApi.getMediaSessions();
         return sessions == null ? List.of() : sessions.stream()
            .filter(java.util.Objects::nonNull).map(s -> (IMediaSession)new Session(s)).toList();
      }
   }

   @Override public void close() {
      closed = true;
      synchronized (lock) {
         if (released) return;
         WindowsMediaPlayerInfo.cleanup();
         released = true;
      }
   }

   private final class Session implements IMediaSession {
      private final IMediaSession delegate;
      Session(IMediaSession delegate) { this.delegate = delegate; }
      public String getOwner() { return delegate.getOwner(); }
      public MediaInfo getMedia() { return delegate.getMedia(); }
      private void command(Runnable action) {
         synchronized (lock) { if (!closed) action.run(); }
      }
      public void play() { command(delegate::play); }
      public void pause() { command(delegate::pause); }
      public void playPause() { command(delegate::playPause); }
      public void stop() { command(delegate::stop); }
      public void next() { command(delegate::next); }
      public void previous() { command(delegate::previous); }
      public void swapCycle() { command(delegate::swapCycle); }
      public int getCycleType() {
         synchronized (lock) { return closed ? -1 : delegate.getCycleType(); }
      }
   }
}
