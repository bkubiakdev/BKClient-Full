package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.modules.api.*;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.performance.FrameStats;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import java.lang.management.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ModuleInfo(name = "Profiler", category = ModuleCategory.OTHER,
   desc = "Record frame times, stutters, heap and GC; disable to save a local report")
public final class PerformanceProfiler extends BaseModule implements IMinecraft {
   private static PerformanceProfiler instance;
   private final SliderSetting threshold = new SliderSetting(this, "Stutter threshold (ms)").min(20).max(250).step(5).currentValue(50);
   private final ButtonSetting save = new ButtonSetting(this, "Save report").action(this::saveReport);
   private FrameStats frames;
   private List<GarbageCollectorMXBean> collectors;
   private final List<String> memory = new ArrayList<>();
   private final List<String> spikes = new ArrayList<>();
   private Instant started;
   private long startedNanos, previousFrame, sampledAt, initialGcMs, initialGcCount, peakHeap;
   private long latestGcMs;
   private String context = "";
   private int memoryDropped;
   private double recordedThreshold;
   public PerformanceProfiler() { instance = this; }
   @Override protected boolean shouldAutoSaveOnToggle() { return false; }
   @Override public void onEnable() {
      collectors = ManagementFactory.getGarbageCollectorMXBeans();
      frames = new FrameStats(); memory.clear(); spikes.clear(); memoryDropped = 0; peakHeap = 0;
      started = Instant.now(); startedNanos = System.nanoTime(); sampledAt = startedNanos;
      previousFrame = 0; context = ""; initialGcMs = gcTime(); latestGcMs = initialGcMs; initialGcCount = gcCount();
      recordedThreshold = threshold.getCurrentValue(); sampleMemory(startedNanos);
   }
   @Override public void onDisable() { if (frames != null) saveReport(); previousFrame = 0; }
   /** Called once per GameRenderer.render, including menus; does no file IO or GL queries. */
   public static void frame() {
      if (instance != null && instance.isEnabled() && instance.frames != null) instance.record();
   }
   private void record() {
      long now = System.nanoTime();
      String current = mc.world == null ? "menu" : mc.currentScreen == null ? "world" : "world+GUI";
      if (previousFrame != 0 && context.equals(current)) {
         double ms = (now-previousFrame)/1_000_000.0;
         frames.add(ms, recordedThreshold);
         if (ms >= recordedThreshold) {
            if (spikes.size() == 200) spikes.removeFirst();
            spikes.add(String.format(Locale.ROOT, "%.2fs | %.2f ms | %s | heap %.1f MiB",
               (now-startedNanos)/1e9, ms, current, heap()/1048576.0));
         }
      }
      previousFrame = now; context = current;
      if (now-sampledAt >= 1_000_000_000L) { sampledAt = now; sampleMemory(now); }
   }
   private static long heap() { Runtime r = Runtime.getRuntime(); return r.totalMemory()-r.freeMemory(); }
   private long gcTime() { return collectors.stream().mapToLong(b -> Math.max(0, b.getCollectionTime())).sum(); }
   private long gcCount() { return collectors.stream().mapToLong(b -> Math.max(0, b.getCollectionCount())).sum(); }
   private void sampleMemory(long now) {
      long used = heap(), gc = gcTime(); peakHeap = Math.max(peakHeap, used);
      if (memory.size() == 3600) { memory.removeFirst(); memoryDropped++; }
      memory.add(String.format(Locale.ROOT, "%.1f,%.2f,%d", (now-startedNanos)/1e9, used/1048576.0, Math.max(0, gc-latestGcMs)));
      latestGcMs = gc;
   }
   private void saveReport() {
      if (frames == null) { if (mc.player != null) MessageUtility.info(Text.literal("Enable Profiler to start recording.")); return; }
      sampleMemory(System.nanoTime());
      var summary = frames.summary();
      var text = new StringBuilder("BKClient performance report\n");
      text.append("Started: ").append(started).append("\nEnded: ").append(Instant.now()).append('\n');
      text.append(String.format(Locale.ROOT,
         "Frames: %d\nAverage frame: %.3f ms\nEffective FPS: %.1f\nMax frame: %.3f ms\nP95: %.3f ms\nP99: %.3f ms\nPercentile window: last %d frames (max 120000)\nStutters >= %.0f ms: %d\n",
         summary.count(), summary.averageMs(), summary.averageMs() > 0 ? 1000/summary.averageMs() : 0,
         summary.maxMs(), summary.p95Ms(), summary.p99Ms(), summary.sampledFrames(), recordedThreshold, summary.slowFrames()));
      text.append(String.format(Locale.ROOT, "Peak sampled heap: %.1f MiB\nHeap limit: %.1f MiB\nGC collection time: %d ms\nGC collections: %d\n",
         peakHeap/1048576.0, Runtime.getRuntime().maxMemory()/1048576.0, gcTime()-initialGcMs, gcCount()-initialGcCount));
      text.append("Java: ").append(System.getProperty("java.version")).append("\n");
      text.append("Window: ").append(mc.getWindow().getFramebufferWidth()).append('x').append(mc.getWindow().getFramebufferHeight()).append('\n');
      text.append("Enabled modules at report time: ");
      BKClientInit.getInstance().getModuleManager().getModules().stream().filter(m -> m.isEnabled()).forEach(m -> text.append(m.getName()).append(", "));
      text.append("\n\nMeasured intervals include CPU, GPU waiting, VSync and FPS limiting. They do not isolate the cause of a stutter.\n");
      text.append("GC values are JVM collection-time counters, not precise stop-the-world pause measurements. Context changes (menu/world/GUI) skip one interval.\n");
      text.append("\nLast 200 stutters\n").append(String.join("\n", spikes));
      text.append("\n\nMemory samples (1 second; last 3600; dropped ").append(memoryDropped).append(")\nseconds,heap_MiB,gc_collection_ms_since_sample\n");
      text.append(String.join("\n", memory));
      try {
         Path dir = FileManager.DIRECTORY.toPath().resolve("profiler"); Files.createDirectories(dir);
         String stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now());
         Path file = Files.createTempFile(dir, "profile-"+stamp+"-", ".txt");
         Files.writeString(file, text, StandardCharsets.UTF_8);
         BKClientInit.LOGGER.info("Profiler report: {}", file);
         if (mc.player != null) MessageUtility.info(Text.literal("Profiler: " + file.toAbsolutePath()));
      } catch (Exception failure) {
         BKClientInit.LOGGER.error("Could not save profiler report", failure);
         if (mc.player != null) MessageUtility.error(Text.literal("Could not save profiler report."));
      } finally {
         // Report generation itself is not counted as a gameplay stutter.
         previousFrame = 0;
      }
   }
}
