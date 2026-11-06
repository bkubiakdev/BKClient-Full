package dev.bkclient.utility.sounds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Anonymous Musixmatch subtitles — a very large synced-lyrics database reached with an app token obtained without
 * any login, so it needs no personal credentials. Used to fill the tracks LRCLIB/NetEase miss. Any failure returns
 * EMPTY and the caller falls through to the next provider.
 */
public final class MusixmatchLyricsProvider {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private static final String BASE = "https://apic-desktop.musixmatch.com/ws/1.1/";
    private static final String APP_ID = "web-desktop-app-v1.0";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) BKClient/3.0 (lyrics)";
    private static volatile String userToken;

    private MusixmatchLyricsProvider() { }

    public static SyncedLyrics fetch(String artist, String title, long seconds) {
        if (title == null || title.isBlank()) return SyncedLyrics.EMPTY;
        try {
            String token = token();
            if (token == null) return SyncedLyrics.EMPTY;
            String url = BASE + "macro.subtitles.get?format=json&namespace=lyrics_richsynched&subtitle_format=lrc"
                + "&app_id=" + APP_ID + "&usertoken=" + enc(token)
                + "&q_artist=" + enc(artist == null ? "" : artist) + "&q_track=" + enc(title)
                + (seconds > 0 ? "&q_duration=" + seconds + "&f_subtitle_length=" + seconds : "");
            JsonObject body = call(url);
            if (body == null || !body.has("macro_calls")) return SyncedLyrics.EMPTY;
            JsonObject macro = body.getAsJsonObject("macro_calls");
            // Confirm Musixmatch matched the playing song, not a loose foreign result (it fuzzy-matches server-side).
            if (!matches(macro, artist, title, seconds)) return SyncedLyrics.EMPTY;
            JsonObject subtitleCall = macro.getAsJsonObject("track.subtitles.get");
            if (subtitleCall == null || !subtitleCall.has("message")) return SyncedLyrics.EMPTY;
            JsonObject message = subtitleCall.getAsJsonObject("message");
            if (!message.has("body") || !message.get("body").isJsonObject()) return SyncedLyrics.EMPTY;
            JsonArray list = message.getAsJsonObject("body").getAsJsonArray("subtitle_list");
            if (list == null || list.isEmpty()) return SyncedLyrics.EMPTY;
            JsonObject subtitle = list.get(0).getAsJsonObject().getAsJsonObject("subtitle");
            if (subtitle == null || !subtitle.has("subtitle_body")) return SyncedLyrics.EMPTY;
            return SyncedLyrics.parseLrc(subtitle.get("subtitle_body").getAsString());
        } catch (Exception ignored) {
            return SyncedLyrics.EMPTY;
        }
    }

    /** The track Musixmatch actually matched must share the title, and the artist or duration, with what is playing. */
    private static boolean matches(JsonObject macro, String artist, String title, long seconds) {
        try {
            JsonObject track = macro.getAsJsonObject("matcher.track.get")
                .getAsJsonObject("message").getAsJsonObject("body").getAsJsonObject("track");
            if (!TimedLyricsSearch.titleMatches(track.get("track_name").getAsString(), title, title)) return false;
            if (artist != null && !artist.isBlank()
                && TimedLyricsSearch.norm(track.get("artist_name").getAsString()).equals(TimedLyricsSearch.norm(artist))) return true;
            if (seconds > 0 && track.has("track_length")
                && Math.abs(track.get("track_length").getAsLong() - seconds) <= 3) return true;
            return artist == null || artist.isBlank();
        } catch (RuntimeException missing) {
            return false;
        }
    }

    private static synchronized String token() throws Exception {
        if (userToken != null) return userToken;
        JsonObject body = call(BASE + "token.get?app_id=" + APP_ID + "&format=json");
        if (body == null || !body.has("user_token")) return null;
        String value = body.get("user_token").getAsString();
        // Musixmatch hands out this placeholder when it refuses the request; it is not a usable token.
        if (value.isBlank() || value.startsWith("UpgradeOnly")) return null;
        userToken = value;
        return userToken;
    }

    /** Returns {@code message.body} of a Musixmatch response as an object, or null on any failure. */
    private static JsonObject call(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
            .header("User-Agent", UA)
            .header("Cookie", "x-mxm-token-guid=")
            .GET().build();
        var response = HTTP.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) return null;
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!root.has("message") || !root.get("message").isJsonObject()) return null;
        JsonObject message = root.getAsJsonObject("message");
        JsonObject header = message.has("header") && message.get("header").isJsonObject() ? message.getAsJsonObject("header") : null;
        if (header != null && header.has("status_code") && header.get("status_code").getAsInt() != 200) return null;
        return message.has("body") && message.get("body").isJsonObject() ? message.getAsJsonObject("body") : null;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
