package dev.bkclient.utility.sounds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsFetcher {
   private static final HttpClient HTTP_CLIENT = createHttpClient();
   private static final String USER_AGENT = "BK-Client/1.0 (Windows NT 10.0; Win64; x64)";

   private static HttpClient createHttpClient() {
      try {
         return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4L)).build();
      } catch (RuntimeException ignored) {
         // Lyrics and HD artwork are optional. A restricted or unavailable network must not
         // prevent the main menu (and therefore the whole client) from loading.
         return null;
      }
   }

   public static SyncedLyrics fetchSyncedLyrics(String rawArtist, String rawTitle, long durationSeconds) {
      return fetchLyricsResult(rawArtist, rawTitle, durationSeconds, false).lyrics();
   }

   public static TimedLyricsSearch.Result fetchLyricsResult(String rawArtist, String rawTitle, long durationSeconds, boolean approximate) {
      if (rawTitle == null || rawTitle.isBlank())
         return new TimedLyricsSearch.Result(SyncedLyrics.EMPTY, "", false, 0, "not_found");
      ParsedTrack parsed = parseTrackInfo(fixEncoding(rawArtist), fixEncoding(rawTitle));
      var result = TimedLyricsSearch.search(parsed.artist(), parsed.title(), rawTitle, durationSeconds, approximate);
      if (!result.lyrics().isEmpty()) return result;
      // Gap filler for tracks LRCLIB/NetEase miss. Synced only — Lyrics 3D needs real timing, never invented timing.
      SyncedLyrics musixmatch = MusixmatchLyricsProvider.fetch(parsed.artist(), parsed.title(), durationSeconds);
      if (musixmatch.isSynced() && !musixmatch.isEmpty())
         return new TimedLyricsSearch.Result(musixmatch, "Musixmatch", false, durationSeconds, "ready");
      // Untimed lyrics remain useful to the music panel, but never invent timing for Lyrics 3D.
      String plain = fetchFromGenius(parsed.artist(), parsed.title());
      if (plain != null && !plain.isBlank())
         return new TimedLyricsSearch.Result(SyncedLyrics.parsePlain(plain), "Genius", false, 0, "plain_only");
      return result;
   }

   public static byte[] fetchHdArtwork(String rawArtist, String rawTitle) {
      if (rawTitle == null || rawTitle.isBlank()) {
         return null;
      }
      String fixedArtist = fixEncoding(rawArtist);
      String fixedTitle = fixEncoding(rawTitle);

      ParsedTrack parsed = parseTrackInfo(fixedArtist, fixedTitle);
      Set<String> searchQueries = generateSearchQueries(parsed, fixedArtist, fixedTitle);

      for (String q : searchQueries) {
         if (q.isBlank()) continue;

         // 1. Try Deezer API (1000x1000 Ultra HD Cover)
         try {
            String deezerUrl = "https://api.deezer.com/search?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&limit=1";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(deezerUrl)).header("User-Agent", USER_AGENT).timeout(Duration.ofSeconds(3L)).GET().build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200) {
               JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
               if (json.has("data")) {
                  JsonArray data = json.getAsJsonArray("data");
                  if (!data.isEmpty()) {
                     JsonObject first = data.get(0).getAsJsonObject();
                     String imgUrl = null;
                     if (first.has("album") && first.getAsJsonObject("album").has("cover_xl")) {
                        imgUrl = first.getAsJsonObject("album").get("cover_xl").getAsString();
                     } else if (first.has("album") && first.getAsJsonObject("album").has("cover_big")) {
                        imgUrl = first.getAsJsonObject("album").get("cover_big").getAsString();
                     }
                     if (imgUrl != null && !imgUrl.isBlank()) {
                        byte[] bytes = downloadImage(imgUrl);
                        if (bytes != null && bytes.length > 0) {
                           return bytes;
                        }
                     }
                  }
               }
            }
         } catch (Exception ignored) {
         }

         // 2. Try iTunes Search API (600x600 HD Cover)
         try {
            String itunesUrl = "https://itunes.apple.com/search?term=" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&entity=song&limit=1";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(itunesUrl)).header("User-Agent", USER_AGENT).timeout(Duration.ofSeconds(3L)).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
               JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
               if (json.has("results")) {
                  JsonArray results = json.getAsJsonArray("results");
                  if (!results.isEmpty()) {
                     JsonObject first = results.get(0).getAsJsonObject();
                     if (first.has("artworkUrl100")) {
                        String artUrl = first.get("artworkUrl100").getAsString();
                        String hdUrl = artUrl.replace("100x100bb.jpg", "600x600bb.jpg");
                        byte[] bytes = downloadImage(hdUrl);
                        if (bytes != null && bytes.length > 0) {
                           return bytes;
                        }
                     }
                  }
               }
            }
         } catch (Exception ignored) {
         }
      }
      return null;
   }

   private static byte[] downloadImage(String url) {
      try {
         HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5L)).GET().build();
         HttpResponse<byte[]> resp = HTTP_CLIENT.send(req, BodyHandlers.ofByteArray());
         if (resp.statusCode() == 200 && resp.body() != null && resp.body().length > 0) {
            return resp.body();
         }
      } catch (Exception ignored) {
      }
      return null;
   }

   private static Set<String> generateSearchQueries(ParsedTrack parsed, String rawArtist, String rawTitle) {
      Set<String> queries = new LinkedHashSet<>();

      if (!parsed.artist().isEmpty() && !parsed.title().isEmpty()) {
         queries.add((parsed.artist() + " " + parsed.title()).trim());
      }
      if (!parsed.extractedArtist().isEmpty() && !parsed.extractedTitle().isEmpty()) {
         queries.add((parsed.extractedArtist() + " " + parsed.extractedTitle()).trim());
      }
      if (!parsed.title().isEmpty()) {
         queries.add(parsed.title().trim());
      }
      if (!parsed.extractedTitle().isEmpty()) {
         queries.add(parsed.extractedTitle().trim());
      }

      String cleanRaw = cleanTitleString(rawTitle);
      if (!cleanRaw.isBlank()) {
         queries.add(cleanRaw.trim());
      }
      return queries;
   }

   private static String geniusToken() {
      try {
         String token = System.getenv("BKCLIENT_GENIUS_TOKEN");
         if (token == null || token.isBlank()) return null;
         token = token.trim();
         return token.indexOf('\r') >= 0 || token.indexOf('\n') >= 0 ? null : token;
      } catch (SecurityException ignored) {
         return null;
      }
   }

   public static String fetchFromGenius(String artist, String title) {
      if (title == null || title.isBlank()) {
         return null;
      }
      String token = geniusToken();
      if (token == null) return null;
      try {
         SongInfo info = searchGeniusSong(artist, title, token);
         if (info == null) {
            return null;
         }
         String lyrics = fetchGeniusLyricsFromApi(info.id, token);
         if (lyrics == null && info.url != null) {
            lyrics = fetchGeniusLyricsFromPage(info.url);
         }
         return lyrics;
      } catch (Exception ignored) {
         return null;
      }
   }

   private static SongInfo searchGeniusSong(String artist, String title, String token) throws IOException, InterruptedException {
      String query = URLEncoder.encode((artist.isEmpty() ? "" : artist + " ") + title, StandardCharsets.UTF_8);
      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create("https://api.genius.com/search?q=" + query))
         .header("Authorization", "Bearer " + token)
         .header("Accept", "application/json; charset=utf-8")
         .timeout(Duration.ofSeconds(5L))
         .GET()
         .build();
      HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200) {
         return null;
      }
      JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
      JsonObject responseObj = jsonResponse.getAsJsonObject("response");
      if (responseObj != null && responseObj.has("hits")) {
         for (JsonElement hit : responseObj.getAsJsonArray("hits")) {
            JsonObject result = hit.getAsJsonObject().getAsJsonObject("result");
            if (result != null && result.has("url") && result.has("id")) {
               return new SongInfo(result.get("url").getAsString(), result.get("id").getAsInt());
            }
         }
      }
      return null;
   }

   private static String fetchGeniusLyricsFromApi(int id, String token) throws IOException, InterruptedException {
      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create("https://api.genius.com/songs/" + id + "?text_format=plain"))
         .header("Authorization", "Bearer " + token)
         .header("Accept", "application/json; charset=utf-8")
         .timeout(Duration.ofSeconds(5L))
         .GET()
         .build();
      HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200) {
         return null;
      }
      JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
      JsonObject song = Optional.ofNullable(jsonResponse.getAsJsonObject("response")).map(obj -> obj.getAsJsonObject("song")).orElse(null);
      if (song != null && song.has("lyrics")) {
         JsonObject lyricsObj = song.getAsJsonObject("lyrics");
         if (lyricsObj.has("plain")) {
            return lyricsObj.get("plain").getAsString();
         }
      }
      return song != null && song.has("lyrics_body") ? song.get("lyrics_body").getAsString() : null;
   }

   private static String fetchGeniusLyricsFromPage(String url) throws IOException, InterruptedException {
      HttpRequest request = HttpRequest.newBuilder()
         .uri(URI.create(url))
         .header("User-Agent", USER_AGENT)
         .header("Accept-Language", "en-US,en;q=0.9,pl;q=0.8")
         .timeout(Duration.ofSeconds(5L))
         .GET()
         .build();
      HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
      Pattern pattern = Pattern.compile("(<div[^>]*class=\"[^\"]*Lyrics__Container[^\"]*\"[^>]*>.*?</div>)", 32);
      Matcher matcher = pattern.matcher(response.body());
      StringBuilder lyrics = new StringBuilder();
      while (matcher.find()) {
         String snippet = matcher.group(1).replaceAll("<br\\s*/?>", "\n").replaceAll("<.*?>", "").replaceAll("&quot;", "\"").trim();
         if (!snippet.isEmpty()) {
            lyrics.append(snippet).append("\n\n");
         }
      }
      return lyrics.isEmpty() ? null : lyrics.toString().trim();
   }

   // HUD elements parse the same track on every frame; the regex pipeline below is only re-run when the
   // raw strings change. Render-thread use only, so one slot is enough.
   private static String memoArtist, memoTitle;
   private static ParsedTrack memoResult;

   public static ParsedTrack parseTrackInfo(String rawArtist, String rawTitle) {
      if (rawTitle == null) {
         return new ParsedTrack("", "", "", "");
      }
      ParsedTrack memo = memoResult;
      if (memo != null && rawTitle.equals(memoTitle) && java.util.Objects.equals(rawArtist, memoArtist)) {
         return memo;
      }
      ParsedTrack parsed = parseTrackInfoUncached(rawArtist, rawTitle);
      memoArtist = rawArtist; memoTitle = rawTitle; memoResult = parsed;
      return parsed;
   }

   private static ParsedTrack parseTrackInfoUncached(String rawArtist, String rawTitle) {
      String fixedArtist = fixEncoding(rawArtist);
      String fixedTitle = fixEncoding(rawTitle);

      String cleanRawTitle = fixedTitle.replaceAll("^\\(\\d+\\)\\s*", "").replaceAll("^\\[\\d+\\]\\s*", "").trim();
      String artist = fixedArtist != null ? fixedArtist.trim() : "";
      String extractedArtist = "";
      String extractedTitle = "";

      // Remove generic series/episode tags: e.g. [S02 / E02], [EP.01], (Odcinek 1)
      String processedTitle = cleanRawTitle.replaceAll("(?i)\\[s\\d+\\s*/\\s*e\\d+\\]", "")
                                          .replaceAll("(?i)\\[ep\\.?\\s*\\d+\\]", "")
                                          .replaceAll("(?i)\\(odcinek\\s*\\d+\\)", "")
                                          .trim();

      // Check if title has colon: e.g. "SANAH: ITEPE ITEDE" or "ENKLAWA - SANAH: ITEPE ITEDE"
      if (processedTitle.contains(" - ") || processedTitle.contains(" — ") || processedTitle.contains(" – ")) {
         String[] parts = processedTitle.split("\\s+[-—–]\\s+", 2);
         if (parts.length == 2) {
            String left = parts[0].trim();
            String right = parts[1].trim();

            if (right.contains(":")) {
               String[] colonParts = right.split(":\\s*", 2);
               extractedArtist = cleanArtistString(colonParts[0]);
               extractedTitle = cleanTitleString(colonParts[1]);
            } else if (left.contains(":")) {
               String[] colonParts = left.split(":\\s*", 2);
               extractedArtist = cleanArtistString(colonParts[1]);
               extractedTitle = cleanTitleString(right);
            } else {
               extractedArtist = cleanArtistString(left);
               extractedTitle = cleanTitleString(right);
            }
            artist = extractedArtist;
         }
      } else if (processedTitle.contains(":")) {
         String[] colonParts = processedTitle.split(":\\s*", 2);
         extractedArtist = cleanArtistString(colonParts[0]);
         extractedTitle = cleanTitleString(colonParts[1]);
         artist = extractedArtist;
      }

      String title = !extractedTitle.isEmpty() ? extractedTitle : cleanTitleString(processedTitle);
      String cleanArt = cleanArtistString(artist);

      return new ParsedTrack(cleanArt, title, extractedArtist, extractedTitle);
   }

   public static String fixEncoding(String s) {
      if (s == null || s.isBlank()) {
         return "";
      }
      if (s.contains("Ã") || s.contains("Å") || s.contains("Ä") || s.contains("Â") || s.contains("â€") || s.contains("╨") || s.contains("╤")) {
         try {
            byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
            String candidate = new String(bytes, StandardCharsets.UTF_8);
            if (!candidate.contains("\uFFFD")) {
               return candidate.trim();
            }
         } catch (Exception ignored) {
         }
         try {
            byte[] bytes = s.getBytes(Charset.forName("windows-1250"));
            String candidate = new String(bytes, StandardCharsets.UTF_8);
            if (!candidate.contains("\uFFFD")) {
               return candidate.trim();
            }
         } catch (Exception ignored) {
         }
      }
      return s.trim();
   }

   private static String cleanArtistString(String s) {
      if (s == null) return "";
      return s.replaceAll("(?i)\\b(feat|ft|featuring)\\.?\\s+.*", "")
              .replaceAll("[\\[\\(].*?[\\]\\)]", "")
              .replaceAll("(?i)\\b(official|vevo|topic|records|music)\\b", "")
              .replaceAll("(?i)\\b(prod\\.?|produced by)\\b.*", "")
              .trim();
   }

   private static String cleanTitleString(String s) {
      if (s == null) return "";
      return s.replaceAll("(?i)\\b(official\\s*video|official\\s*audio|lyrics|lyric\\s*video|audio|visualizer|music\\s*video|clip\\s*officiel|video\\s*clip|teledysk)\\b", "")
              .replaceAll("[\\[\\(](remastered|slowed|reverb|sped\\s*up|slowed\\s*\\+\\s*reverb|audio|video|hd|hq|4k|official|prod\\.[^\\]\\)]*)[^\\]\\)]*[\\]\\)]", "")
              .replaceAll("(?i)\\b(feat|ft|featuring)\\.?\\s+.*", "")
              .replaceAll("(?i)\\b(prod\\.?|produced by)\\b.*", "")
              .trim();
   }

   public record ParsedTrack(String artist, String title, String extractedArtist, String extractedTitle) {
   }

   record SongInfo(String url, int id) {
   }
}
