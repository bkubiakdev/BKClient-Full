package dev.redstones.mediaplayerinfo;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Opt-in Windows integration test: native cleanup and a second JVM reading sessions. */
public final class MediaCleanupTest {
   public static void main(String[] args) throws Exception {
      if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
         System.out.println("SKIP: Windows media integration");
         return;
      }
      if (args.length > 0) {
         MediaPlayerInfo backend = MediaPlayerInfo.INSTANCE;
         try {
            for (int i = 0; i < 5; i++) {
               System.out.println("Native media sessions: " + backend.getMediaSessions().size());
               Thread.sleep(100);
            }
         } finally {
            backend.close();
            backend.close();
         }
         if (!backend.getMediaSessions().isEmpty()) throw new AssertionError("Sessions after close");
         return;
      }
      for (int i = 0; i < 2; i++) {
         Process child = new ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
            "-cp", System.getProperty("java.class.path"), MediaCleanupTest.class.getName(), "child")
            .inheritIO().start();
         if (!child.waitFor(20, TimeUnit.SECONDS)) {
            child.destroyForcibly();
            throw new AssertionError("Native media JVM did not exit within 20 seconds, PID " + child.pid());
         }
         if (child.exitValue() != 0) throw new AssertionError("Media test JVM failed: " + child.exitValue());
      }
      System.out.println("PASS: media cleanup, repeated close, no sessions after close, two JVMs exit normally");
   }
}
