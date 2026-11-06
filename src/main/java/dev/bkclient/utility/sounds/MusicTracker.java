package dev.bkclient.utility.sounds;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.imageio.ImageIO;
import lombok.Generated;
import dev.bkclient.BKClientInit;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class MusicTracker implements IMinecraft {
   private final Thread thread;
   private volatile boolean running = true;
   private volatile IMediaSession session;
   private volatile ColorRGBA mediaColor = ColorRGBA.WHITE;
   private record Artwork(Identifier texture, ColorRGBA color) { }
   // Only accessed on the client/render thread.
   private final dev.bkclient.utility.concurrent.ResourceCache<String, Artwork> artworkCache =
      new dev.bkclient.utility.concurrent.ResourceCache<>(10, 64L * 1024 * 1024,
         art -> DrawUtility.releaseTexture(art.texture()));
   private final java.util.concurrent.ThreadPoolExecutor fetchExecutor = new java.util.concurrent.ThreadPoolExecutor(
      1, 1, 30, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.ArrayBlockingQueue<>(1), task -> {
         Thread worker = new Thread(task, "BK-Client Media Fetcher"); worker.setDaemon(true); return worker;
      }, new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
   private volatile long hdArtworkGeneration = -1;
   private static final Random RANDOM = new Random();
   private volatile String lyrics = "";
   private volatile SyncedLyrics syncedLyrics = SyncedLyrics.EMPTY;
   private volatile boolean fetchingLyrics = false;
   private volatile boolean approximateLyrics = true;
   private volatile boolean lyricsRefresh;
   private volatile String lyricsStatus = "idle";
   private volatile String lyricsSource = "";
   private volatile long lyricsRecordingSeconds;

   public synchronized void setApproximateLyrics(boolean allowed) {
      if (approximateLyrics == allowed) return;
      approximateLyrics = allowed;
      ++trackGeneration; // Any in-flight result for the old matching policy is stale.
      syncedLyrics = SyncedLyrics.EMPTY;
      lyricsStatus = "loading";
      lyricsRefresh = true;
   }
   public String getLyricsStatus() { return lyricsStatus; }
   public String getLyricsSource() { return lyricsSource; }
   public long getLyricsRecordingSeconds() { return lyricsRecordingSeconds; }

   private volatile String lastTrack = "";
   private volatile String artworkTrack = "";
   private volatile Identifier currentArtwork;
   private volatile long lastPositionSeconds = -1L;
   private volatile long lastPositionTimestamp = 0L;
   private volatile boolean wasPlaying = false;
   private volatile long trackGeneration;

   public MusicTracker() {
      this.thread = new Thread(() -> {
         try {
            while (this.running) {
               try {
                  Thread.sleep(500L);
                  if (this.running) this.onScheduleTask();
               } catch (InterruptedException var2) {
                  if (!this.running) {
                     break;
                  }
               }
            }
         } finally {
            try {
               MediaPlayerInfo.INSTANCE.close();
               BKClientInit.LOGGER.info("[MusicTracker] Native media manager released");
            } catch (Throwable failure) {
               BKClientInit.LOGGER.error("[MusicTracker] Native media cleanup failed", failure);
            }
         }
      }, "BK-Client Music Tracker");
      this.thread.setDaemon(true);
      this.thread.start();
   }

   private volatile long lastFetchTime = 0L;
   private volatile long lastSessionLog = 0L;

   private void onScheduleTask() {
      try {
         List<IMediaSession> sessions = MediaPlayerInfo.INSTANCE.getMediaSessions();
         if (sessions == null) {
            return;
         }

         long now = System.currentTimeMillis();
         if (BKClientInit.LOGGER.isDebugEnabled() && now - lastSessionLog > 4000L) {
            lastSessionLog = now;
            if (sessions.isEmpty()) {
               BKClientInit.LOGGER.debug("[MusicTracker] Polling Windows media sessions... (0 active sessions found)");
            } else {
               BKClientInit.LOGGER.debug("[MusicTracker] Found {} active Windows session(s):", sessions.size());
               for (IMediaSession s : sessions) {
                  MediaInfo m = s != null ? s.getMedia() : null;
                  BKClientInit.LOGGER.debug("   -> [{}] Title='{}', Artist='{}', Playing={}",
                     (s != null ? s.getOwner() : "null"),
                     (m != null ? m.getTitle() : "null"),
                     (m != null ? m.getArtist() : "null"),
                     (m != null && m.isPlaying()));
               }
            }
         }

         IMediaSession playing = sessions.stream()
            .filter(s -> s != null && s.getMedia() != null && s.getMedia().getTitle() != null && !s.getMedia().getTitle().isEmpty() && s.getMedia().isPlaying())
            .findFirst()
            .orElse(null);

         IMediaSession found = playing != null ? playing : sessions.stream()
            .filter(s -> s != null && s.getMedia() != null && s.getMedia().getTitle() != null && !s.getMedia().getTitle().isEmpty())
            .findFirst()
            .orElse(null);

         if (found != null && found.getMedia() != null) {
            this.session = found;
            String artist = nullToEmpty(found.getMedia().getArtist());
            String title = nullToEmpty(found.getMedia().getTitle());
            String trackId = artist + " - " + title;
            boolean changed = !trackId.equals(this.lastTrack);
            if (changed) {
               synchronized (this) {
                  this.lastTrack = trackId;
                  ++this.trackGeneration;
                  this.syncedLyrics = SyncedLyrics.EMPTY;
                  this.lyricsStatus = "loading";
                  this.lastPositionSeconds = -1L;
                  this.currentArtwork = null;
                  this.artworkTrack = trackId;
               }
               // ImageIO, resampling and average-color calculation stay off the render thread.
               publishArtwork(found.getMedia().getArtworkPng(),
                  found.getOwner().toLowerCase(java.util.Locale.ROOT).contains("spotify"),
                  trackGeneration, false);
            }
            // A network failure is retried after 10 s; "not found" and rate limits keep the 60 s pause.
            long retryMs = "unavailable".equals(lyricsStatus) ? 10000L : 60000L;
            if (changed || lyricsRefresh || ((!this.syncedLyrics.isSynced() || this.syncedLyrics.isEmpty()) && !this.fetchingLyrics && now - lastFetchTime > retryMs)) {
               lyricsRefresh = false;
               final boolean allowApproximate = approximateLyrics;
               final long generation = trackGeneration;
               final long duration = found.getMedia().getDuration();
               fetchingLyrics = true;
               lastFetchTime = now;
               // At most one running request and one newest pending track. No native images in this queue.
               fetchExecutor.execute(() -> {
                  try {
                     if (!running || generation != trackGeneration) return;
                     var result = LyricsFetcher.fetchLyricsResult(artist, title, duration, allowApproximate);
                     synchronized (this) {
                        if (running && generation == trackGeneration && result != null) {
                           syncedLyrics = result.lyrics();
                           if (!lyricsStatus.equals(result.status()) || !lyricsSource.equals(result.source()))
                              BKClientInit.LOGGER.info("[Lyrics] status={}, source={}, recording={}s, playback={}s", result.status(), result.source(), result.recordingSeconds(), duration);
                           lyricsSource = result.source();
                           lyricsRecordingSeconds = result.recordingSeconds();
                           lyricsStatus = result.status();
                        }
                     }
                     if (running && generation == trackGeneration && hdArtworkGeneration != generation) {
                        publishArtwork(LyricsFetcher.fetchHdArtwork(artist, title), false, generation, true);
                     }
                  } catch (Exception failure) {
                     BKClientInit.LOGGER.warn("Media metadata request failed: {}", failure.getClass().getSimpleName());
                     if (generation == trackGeneration) lyricsStatus = "unavailable";
                  } finally {
                     synchronized (this) {
                        if (generation == trackGeneration) fetchingLyrics = false;
                     }
                  }
               });
            }
         } else {
            synchronized (this) {
               if (this.session != null) ++this.trackGeneration;
               this.session = null;
               this.lastTrack = "";
               this.lyricsStatus = "idle";
               this.currentArtwork = null;
               this.syncedLyrics = SyncedLyrics.EMPTY;
               this.fetchingLyrics = false;
               this.lastPositionSeconds = -1L;
            }
         }
      } catch (Throwable t) {
         // Silently ignore or print once
      }
   }

   public Identifier getImage() {
      return session == null ? null : currentArtwork;
   }

   private void publishArtwork(byte[] data, boolean spotify, long generation, boolean hd) {
      if (data == null || data.length == 0 || !running || generation != trackGeneration) return;
      NativeImage image = processAndUpscaleImage(data, spotify);
      if (image == null) return;
      try {
         String key = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data)) + spotify;
         ColorRGBA color = getAverageColor(image, 1);
         synchronized (this) {
            if (!running || generation != trackGeneration) { image.close(); return; }
            // A single slot owns pending native memory, even if the client thread stalls.
            PendingArtwork previous = pendingArtwork;
            pendingArtwork = new PendingArtwork(key, image, color, generation, hd);
            if (previous != null) previous.image().close();
            if (!artworkUploadQueued) {
               artworkUploadQueued = true;
               mc.execute(this::uploadArtwork);
            }
         }
      } catch (Exception failure) {
         image.close();
         BKClientInit.LOGGER.debug("Artwork processing failed", failure);
      }
   }

   private record PendingArtwork(String key, NativeImage image, ColorRGBA color, long generation, boolean hd) { }
   private PendingArtwork pendingArtwork;
   private boolean artworkUploadQueued;

   private void uploadArtwork() {
      synchronized (this) {
         PendingArtwork pending = pendingArtwork;
         pendingArtwork = null;
         artworkUploadQueued = false;
         if (pending == null) return;
         NativeImage image = pending.image();
         if (!running || pending.generation() != trackGeneration || (!pending.hd() && hdArtworkGeneration == trackGeneration)) {
            image.close(); return;
         }
         Artwork art = artworkCache.get(pending.key());
         if (art != null) {
            image.close();
         } else {
            Identifier id = BKClientInit.id("temp/" + randomString());
            NativeImageBackedTexture texture = null;
            try {
               long bytes = (long)image.getWidth() * image.getHeight() * 4;
               if (bytes > 64L * 1024 * 1024) { image.close(); return; }
               texture = new NativeImageBackedTexture(image);
               texture.setFilter(true, false);
               mc.getTextureManager().registerTexture(id, texture);
               art = new Artwork(id, pending.color());
               artworkCache.put(pending.key(), art, bytes);
            } catch (RuntimeException failure) {
               if (texture != null) { DrawUtility.releaseTexture(id); texture.close(); }
               else image.close();
               BKClientInit.LOGGER.debug("Artwork upload failed", failure);
               return;
            }
         }
         currentArtwork = art.texture();
         mediaColor = art.color();
         if (pending.hd()) hdArtworkGeneration = pending.generation();
      }
   }

   private static NativeImage processAndUpscaleImage(byte[] imageData, boolean spotify) {
      if (imageData == null || imageData.length == 0) {
         return null;
      }
      try {
         ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
         BufferedImage bImg = ImageIO.read(bais);
         if (bImg == null) {
            return NativeImage.read(imageData);
         }

         int w = bImg.getWidth();
         int h = bImg.getHeight();

         if (spotify) {
            int leftCut = (int)(w * 0.11);
            int rightCut = (int)(w * 0.11);
            int bottomCut = (int)(h * 0.22);
            int cropW = w - leftCut - rightCut;
            int cropH = h - bottomCut;
            if (cropW > 0 && cropH > 0) {
               bImg = bImg.getSubimage(leftCut, 0, cropW, cropH);
               w = bImg.getWidth();
               h = bImg.getHeight();
            }
         } else if (w != h) {
            int squareSize = Math.min(w, h);
            int cropX = (w - squareSize) / 2;
            int cropY = (h - squareSize) / 2;
            bImg = bImg.getSubimage(cropX, cropY, squareSize, squareSize);
            w = squareSize;
            h = squareSize;
         }

         int targetSize = Math.min(2048, Math.max(512, Math.max(w, h)));
         BufferedImage scaled = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
         Graphics2D g2d = scaled.createGraphics();
         g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
         g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
         g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
         g2d.drawImage(bImg, 0, 0, targetSize, targetSize, null);
         g2d.dispose();

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ImageIO.write(scaled, "png", baos);
         return NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
      } catch (Exception e) {
         try {
            return NativeImage.read(imageData);
         } catch (Exception ignored) {
            return null;
         }
      }
   }

   private void clearTextureCache() {
      Runnable release = () -> { artworkCache.clear(); currentArtwork = null; artworkTrack = ""; };
      if (mc.isOnThread()) release.run(); else mc.execute(release);
   }

   public void shutdown() {
      this.running = false;
      this.thread.interrupt();
      this.fetchExecutor.shutdownNow();
       synchronized (this) {
          ++trackGeneration;
          if (pendingArtwork != null) { pendingArtwork.image().close(); pendingArtwork = null; }
       }
       this.clearTextureCache();
      this.session = null;
      try {
         this.thread.join(2000L);
         if (this.thread.isAlive()) BKClientInit.LOGGER.warn("[MusicTracker] Native media call still pending during shutdown");
      } catch (InterruptedException interrupted) {
         Thread.currentThread().interrupt();
      }
   }

   public ColorRGBA getAverageColor(NativeImage image, int step) {
      int width = image.getWidth();
      int height = image.getHeight();
      long totalA = 0L;
      long totalR = 0L;
      long totalG = 0L;
      long totalB = 0L;
      int sampledPixels = 0;
      int y = 0;

      while (y < height) {
         for (int x = 0; x < width; x += step) {
            int argb = image.getColorArgb(x, y);
            int a = argb >> 24 & 0xFF;
            if (a != 0) {
               totalA += a;
               totalR += argb >> 16 & 0xFF;
               totalG += argb >> 8 & 0xFF;
               totalB += argb & 0xFF;
               sampledPixels++;
            }
         }

         y += step;
      }

      if (sampledPixels == 0) {
         return ColorRGBA.WHITE;
      }

      float additional = 50.0F;
      return new ColorRGBA((float)totalR / sampledPixels + additional, (float)totalG / sampledPixels + additional, (float)totalB / sampledPixels + additional);
   }

   private static void logDebugOnce(String message) {
      System.out.println(message);
   }

   private static String nullToEmpty(String value) {
      return value == null ? "" : value;
   }

   private static String randomString() {
      StringBuilder sb = new StringBuilder(32);

      for (int i = 0; i < 32; i++) {
         char c = (char)(97 + RANDOM.nextInt(26));
         sb.append(c);
      }

      return sb.toString();
   }

   public boolean haveActiveSession() {
      return this.session != null;
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         MusicTracker that = (MusicTracker)o;
         return Objects.equals(this.thread, that.thread)
            && Objects.equals(this.session, that.session)
            && Objects.equals(this.mediaColor, that.mediaColor)
            && Objects.equals(this.artworkCache, that.artworkCache)
            && Objects.equals(this.lyrics, that.lyrics)
            && Objects.equals(this.lastTrack, that.lastTrack);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.thread, this.session, this.mediaColor, this.artworkCache, this.lyrics, this.lastTrack);
   }

   @Generated
   public Thread getThread() {
      return this.thread;
   }

   @Generated
   public IMediaSession getSession() {
      return this.session;
   }

   @Generated
   public ColorRGBA getMediaColor() {
      return this.mediaColor;
   }

   @Generated
   public String getLyrics() {
      return this.lyrics;
   }

   @Generated
   public String getLastTrack() {
      return this.lastTrack;
   }

   public SyncedLyrics getSyncedLyrics() {
      return this.syncedLyrics;
   }

   public boolean isFetchingLyrics() {
      return this.fetchingLyrics;
   }

   public record LyricsPlayback(String track, SyncedLyrics lyrics, long positionMs, long durationMs, boolean playing) { }

   /** One track-consistent snapshot for world lyrics; native polling and downloads stay on their workers. */
   public synchronized LyricsPlayback getLyricsPlayback() {
      IMediaSession current = this.session;
      MediaInfo media = current == null ? null : current.getMedia();
      if (media == null) return new LyricsPlayback("", SyncedLyrics.EMPTY, 0, 0, false);
      String key = nullToEmpty(media.getArtist()) + " - " + nullToEmpty(media.getTitle());
      if (!key.equals(this.lastTrack)) return new LyricsPlayback("", SyncedLyrics.EMPTY, 0, 0, false);
      return new LyricsPlayback(key, this.syncedLyrics, smoothPositionMs(media),
          Math.max(0L, media.getDuration() * 1000L), media.isPlaying());
   }

   public synchronized long getSmoothPositionMs() {
      IMediaSession s = this.session;
      if (s == null || s.getMedia() == null) {
         return 0L;
      }
      return smoothPositionMs(s.getMedia());
   }

   private long smoothPositionMs(MediaInfo media) {
      long posSec = Math.max(0L, media.getPosition());
      boolean playing = media.isPlaying();
      long now = System.currentTimeMillis();

      if (posSec != this.lastPositionSeconds || playing != this.wasPlaying) {
         this.lastPositionSeconds = posSec;
         this.lastPositionTimestamp = now;
         this.wasPlaying = playing;
      }

      long durationMs = Math.max(0L, media.getDuration() * 1000L);
      if (playing) {
         long elapsed = Math.min(1000L, Math.max(0L, now - this.lastPositionTimestamp));
         long calculated = posSec * 1000L + elapsed;
         return durationMs > 0L ? Math.min(durationMs, calculated) : calculated;
      } else {
         return posSec * 1000L;
      }
   }
}
