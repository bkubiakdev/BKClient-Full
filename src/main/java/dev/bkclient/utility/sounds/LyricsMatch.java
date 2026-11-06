package dev.bkclient.utility.sounds;

import com.google.gson.JsonObject;
import java.text.Normalizer;
import java.util.Locale;

/** Never borrow timing from another recording simply because it is the first search result. */
public final class LyricsMatch {
    private LyricsMatch() { }
    public static boolean matches(JsonObject item, String artist, String title, long seconds) {
        try {
            String candidateArtist = normalize(item.get("artistName").getAsString());
            String wantedArtist = normalize(artist);
            if (wantedArtist.isEmpty() || !candidateArtist.equals(wantedArtist)) return false;
            if (!normalize(item.get("trackName").getAsString()).equals(normalize(title))) return false;
            // Without a known recording length there is no safe automatic timing match.
            double duration = item.get("duration").getAsDouble();
            return seconds > 0 && Double.isFinite(duration) && Math.abs(duration - seconds) <= 2;
        } catch (RuntimeException malformed) { return false; }
    }
    private static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }
}
