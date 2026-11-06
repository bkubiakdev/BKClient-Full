package dev.bkclient.systems.performance;

import java.util.Arrays;

/** Bounded recent sample window; lifetime totals include every measured frame. */
public final class FrameStats {
   private final double[] samples = new double[120_000];
   private int cursor, size;
   private long count, slowFrames;
   private double total, maximum;
   public void add(double milliseconds, double spikeThreshold) {
      if (!Double.isFinite(milliseconds) || milliseconds <= 0) return;
      samples[cursor] = milliseconds; cursor = (cursor+1)%samples.length; size = Math.min(size+1, samples.length);
      count++; total += milliseconds; maximum = Math.max(maximum, milliseconds);
      if (milliseconds >= spikeThreshold) slowFrames++;
   }
   public record Summary(long count, int sampledFrames, double averageMs, double maxMs, double p95Ms, double p99Ms, long slowFrames) { }
   public Summary summary() {
      double[] sorted = Arrays.copyOf(samples, size); Arrays.sort(sorted);
      return new Summary(count, size, count == 0 ? 0 : total/count, maximum, percentile(sorted, 0.95), percentile(sorted, 0.99), slowFrames);
   }
   private static double percentile(double[] sorted, double fraction) {
      return sorted.length == 0 ? 0 : sorted[Math.max(0, (int)Math.ceil(sorted.length*fraction)-1)];
   }
}
