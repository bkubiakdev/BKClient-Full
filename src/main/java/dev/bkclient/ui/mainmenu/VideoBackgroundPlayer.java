package dev.bkclient.ui.mainmenu;

import dev.bkclient.mixin.accessors.NativeImageAccessor;
import dev.bkclient.utility.interfaces.IMinecraft;
import java.io.File;
import java.nio.ByteBuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.lwjgl.system.MemoryUtil;

/** Lazily decodes a displayed video. Idle players release their decoder and native frame storage. */
public final class VideoBackgroundPlayer implements IMinecraft {
   private static final int MAX_DIMENSION = 1440;
   private static final long IDLE_NANOS = 750_000_000L;
   private final File file;
   private final Identifier textureId;
   private final Object frameLock = new Object();
   private volatile boolean closed;
   private volatile long requestedAt;
   private volatile float aspect = 16F / 9F;
   private Thread worker;
   // Both threads hold frameLock for the complete copy, including position/limit changes.
   private ByteBuffer frameBytes;
   private boolean pending;
   private int width, height;
   private NativeImage sharedImage;
   private NativeImageBackedTexture texture;

   public VideoBackgroundPlayer(File file, Identifier textureId) {
      this.file = file;
      this.textureId = textureId;
   }

   private synchronized void requestDecode() {
      if (closed) return;
      requestedAt = System.nanoTime();
      if (worker == null) {
         worker = new Thread(this::decodeLoop, "VideoBackground-" + file.getName());
         worker.setDaemon(true);
         worker.start();
      }
   }

   private boolean requested() {
      return !closed && !Thread.currentThread().isInterrupted() && System.nanoTime() - requestedAt < IDLE_NANOS;
   }

   private int[] probeAndScale() throws Exception {
      try (FFmpegFrameGrabber probe = new FFmpegFrameGrabber(file)) {
         probe.start();
         int w = probe.getImageWidth(), h = probe.getImageHeight();
         if (w <= 0 || h <= 0) throw new IllegalArgumentException("Invalid video dimensions");
         float scale = Math.min(1F, (float)MAX_DIMENSION / Math.max(w, h));
         return new int[]{Math.max(2, Math.round(w * scale) / 2 * 2), Math.max(2, Math.round(h * scale) / 2 * 2)};
      }
   }

   private void decodeLoop() {
      try {
         int[] size = probeAndScale();
         while (requested()) {
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
               grabber.setImageMode(FrameGrabber.ImageMode.COLOR);
               grabber.setPixelFormat(avutil.AV_PIX_FMT_RGBA);
               grabber.setImageWidth(size[0]);
               grabber.setImageHeight(size[1]);
               grabber.start();
               int w = grabber.getImageWidth(), h = grabber.getImageHeight();
               int byteCount = Math.multiplyExact(Math.multiplyExact(w, h), 4);
               if (w <= 0 || h <= 0 || w > MAX_DIMENSION || h > MAX_DIMENSION) throw new IllegalArgumentException("Invalid decoded dimensions");
               synchronized (frameLock) {
                  if (frameBytes == null || frameBytes.capacity() != byteCount) {
                     if (frameBytes != null) MemoryUtil.memFree(frameBytes);
                     frameBytes = null;
                     frameBytes = MemoryUtil.memAlloc(byteCount);
                  }
                  width = w; height = h; pending = false;
                  aspect = (float)w / h;
               }
               double fps = grabber.getFrameRate();
               if (!Double.isFinite(fps) || fps <= 0) fps = 30;
               long delay = (long)(1_000_000_000.0 / fps);
               while (requested()) {
                  long start = System.nanoTime();
                  Frame frame = grabber.grabImage();
                  if (frame == null) break;
                  if (frame.image != null && frame.image.length > 0) {
                     ByteBuffer source = ((ByteBuffer)frame.image[0]).duplicate();
                     int rowBytes = w * 4;
                     int stride = frame.imageStride > 0 ? frame.imageStride : rowBytes;
                     synchronized (frameLock) {
                        frameBytes.clear();
                        for (int y = 0; y < h; y++) {
                           source.clear().position(y * stride).limit(y * stride + rowBytes);
                           frameBytes.put(source);
                        }
                        frameBytes.flip();
                        pending = true;
                     }
                  }
                  long remaining = delay - (System.nanoTime() - start);
                  if (remaining > 0) Thread.sleep(remaining / 1_000_000, (int)(remaining % 1_000_000));
               }
            }
         }
      } catch (InterruptedException interrupted) {
         Thread.currentThread().interrupt();
      } catch (Exception failure) {
         dev.bkclient.BKClientInit.LOGGER.debug("Video background decode failed: {}", file.getName(), failure);
         try { Thread.sleep(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
      } finally {
         synchronized (frameLock) {
            if (frameBytes != null) MemoryUtil.memFree(frameBytes);
            frameBytes = null;
            pending = false;
         }
         synchronized (this) { worker = null; }
      }
   }

   /** Called only while displayed, on the render thread. */
   public void update() {
      if (closed) return;
      requestDecode();
      synchronized (frameLock) {
         if (!pending || frameBytes == null) return;
         if (texture == null || sharedImage.getWidth() != width || sharedImage.getHeight() != height) {
            releaseTexture();
            sharedImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            texture = new NativeImageBackedTexture(sharedImage);
            mc.getTextureManager().registerTexture(textureId, texture);
         }
         long pointer = ((NativeImageAccessor)(Object)sharedImage).getPointer();
         MemoryUtil.memByteBuffer(pointer, width * height * 4).put(frameBytes.duplicate());
         pending = false;
      }
      texture.upload();
   }

   public boolean hasFrame() { return texture != null; }

   public Identifier getTextureId() { return textureId; }
   public float getAspect() { return aspect; }

   private void releaseTexture() {
      if (texture != null) dev.bkclient.utility.render.DrawUtility.releaseTexture(textureId);
      texture = null;
      sharedImage = null;
   }

   public void close() {
      synchronized (this) {
         closed = true;
         if (worker != null) worker.interrupt();
      }
      releaseTexture();
   }
}
