package dev.bkclient.utility.sounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Represents a parsed set of synchronized lyrics with timestamps. */
public class SyncedLyrics {
   private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[\\.:](\\d{1,3}))?\\]");
   public static final SyncedLyrics EMPTY = new SyncedLyrics(Collections.emptyList(), false);

   private final List<LyricLine> lines;
   private final boolean synced;

   public SyncedLyrics(List<LyricLine> lines, boolean synced) {
      this.lines = lines != null ? List.copyOf(lines) : Collections.emptyList();
      this.synced = synced;
   }

   public static SyncedLyrics parseLrc(String lrc) {
      if (lrc == null || lrc.isBlank()) {
         return EMPTY;
      }
      List<LyricLine> parsedLines = new ArrayList<>();
      String[] rawLines = lrc.split("\\r?\\n");

      long offset = 0;
      for (String line : rawLines) {
         Matcher metadata = Pattern.compile("\\[offset:([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE).matcher(line.trim());
         if (metadata.matches()) {
            try { offset = Math.clamp(Long.parseLong(metadata.group(1)), -60000L, 60000L); }
            catch (NumberFormatException ignored) { }
         }
      }
      for (String line : rawLines) {
         String trimmed = line.trim();
         if (!trimmed.startsWith("[")) continue;
         Matcher matcher = LRC_PATTERN.matcher(trimmed);
         List<Long> timestamps = new ArrayList<>();
         int end = 0;
         while (matcher.find() && matcher.start() == end) {
            end = matcher.end();
            try {
               long seconds = Long.parseLong(matcher.group(2));
               if (seconds >= 60) continue;
               String fraction = matcher.group(3);
               long ms = fraction == null ? 0 : Long.parseLong((fraction + "000").substring(0, 3));
               timestamps.add(Math.max(0L, (Long.parseLong(matcher.group(1)) * 60L + seconds) * 1000L + ms + offset));
            } catch (NumberFormatException ignored) { }
         }
         String text = cleanText(trimmed.substring(end));
         // An empty timed verse deliberately clears the previous lyric during instrumental sections.
         for (long timestamp : timestamps) parsedLines.add(new LyricLine(timestamp, text));
      }

      if (parsedLines.isEmpty()) {
         return parsePlain(lrc);
      }

      parsedLines.sort(Comparator.comparingLong(LyricLine::timeMs));
      return new SyncedLyrics(parsedLines, true);
   }

   public static SyncedLyrics parsePlain(String plain) {
      if (plain == null || plain.isBlank()) {
         return EMPTY;
      }
      List<LyricLine> parsedLines = new ArrayList<>();
      String[] rawLines = plain.split("\\r?\\n");
      long stepMs = 4000L;
      long currentMs = 0L;

      for (String line : rawLines) {
         String cleaned = cleanText(line);
         if (!cleaned.isEmpty()) {
            parsedLines.add(new LyricLine(currentMs, cleaned));
            currentMs += stepMs;
         }
      }

      return new SyncedLyrics(parsedLines, false);
   }

   static String cleanText(String s) {
      if (s == null) return "";
      return s.replace('\u2013', '-')
              .replace('\u2014', '-')
              .replace('\u2212', '-')
              .replace('\u2018', '\'')
              .replace('\u2019', '\'')
              .replace('\u201C', '"')
              .replace('\u201D', '"')
              .replace('\u201E', '"')
              .replace('\u201F', '"')
              .replace('\u2026', '.')
              .replace('\u00A0', ' ')
              .trim();
   }

   public int getActiveIndex(long positionMs) {
      if (this.lines.isEmpty()) {
         return -1;
      }
      int active = -1;
      for (int i = 0; i < this.lines.size(); i++) {
         if (positionMs >= this.lines.get(i).timeMs()) {
            active = i;
         } else {
            break;
         }
      }
      return active;
   }

   public boolean isLineActive(int index, long positionMs) {
      if (index < 0 || index >= this.lines.size()) {
         return false;
      }
      long start = this.lines.get(index).timeMs();
      if (positionMs < start) {
         return false;
      }
      long end;
      if (index + 1 < this.lines.size()) {
         long nextStart = this.lines.get(index + 1).timeMs();
         end = Math.min(nextStart, start + 8000L);
      } else {
         end = start + 8000L;
      }
      return positionMs <= end;
   }

   public List<LyricLine> getLines() {
      return this.lines;
   }

   public boolean isSynced() {
      return this.synced;
   }

   public boolean isEmpty() {
      return this.lines.isEmpty();
   }

   /** {@code wordMs}: song time of every whitespace-separated word of {@code text}, or null when only the line is timed. */
   public record LyricLine(long timeMs, String text, long[] wordMs) {
      public LyricLine(long timeMs, String text) { this(timeMs, text, null); }
   }
}
