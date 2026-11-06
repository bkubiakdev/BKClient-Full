package dev.bkclient.systems.performance;

public final class FrameStatsTest {
   public static void main(String[] args) {
      FrameStats stats = new FrameStats();
      check(stats.summary().count() == 0 && stats.summary().p99Ms() == 0, "empty session");
      stats.add(Double.NaN, 50); stats.add(-1, 50); stats.add(Double.POSITIVE_INFINITY, 50);
      for (int i=1; i<=100; i++) stats.add(i, 50);
      var summary = stats.summary();
      check(summary.count() == 100 && summary.averageMs() == 50.5, "valid-frame mean");
      check(summary.p95Ms() == 95 && summary.p99Ms() == 99 && summary.slowFrames() == 51, "tail latency and inclusive stutter threshold");
      for (int i=0; i<120000; i++) stats.add(10, 50);
      summary = stats.summary();
      check(summary.count() == 120100 && summary.sampledFrames() == 120000, "bounded percentile window with lifetime count");
      check(summary.p99Ms() == 10 && summary.maxMs() == 100 && summary.slowFrames() == 51, "rolling percentiles preserve lifetime extremes");
      System.out.println("FrameStats tests passed");
   }
   private static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
