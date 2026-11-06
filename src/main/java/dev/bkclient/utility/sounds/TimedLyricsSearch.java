package dev.bkclient.utility.sounds;

import com.google.gson.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import java.io.ByteArrayOutputStream;

/** Two independent timed-text sources. Approximate matches never change the playback clock. */
public final class TimedLyricsSearch {
    public record Reply(int code, String body, long retrySeconds) { }
    public interface Transport { Reply request(String url, String form) throws Exception; }
    public record Result(SyncedLyrics lyrics, String source, boolean approximate, long recordingSeconds, String status) { }
    private record Candidate(String source, String id, String title, double seconds, String lrc) { }
    private static final ProviderBackoff BACKOFF = new ProviderBackoff();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    static final Transport NETWORK = TimedLyricsSearch::request;
    private TimedLyricsSearch() { }

    public static Result search(String artist, String title, String rawTitle, long seconds, boolean approximate) {
        return search(artist, title, rawTitle, seconds, approximate, NETWORK);
    }

    static Result search(String artist, String title, String rawTitle, long seconds, boolean approximate, Transport network) {
        title = compactSpacedLetters(title);
        List<Candidate> candidates = new ArrayList<>();
        boolean limited = false, error = false;
        try {
            Reply exact = network.request("https://lrclib.net/api/get?artist_name=" + enc(artist) + "&track_name=" + enc(title)
                + (seconds > 0 ? "&duration=" + seconds : ""), null);
            limited = exact.code == 429;
            if (exact.code == 200) addLrc(candidates, JsonParser.parseString(exact.body).getAsJsonObject(), artist, title, rawTitle);
            Result result = choose(candidates, seconds, false, network);
            if (result != null) return result;
            if (!limited && exact.code < 500) {
                Reply search = network.request("https://lrclib.net/api/search?artist_name=" + enc(artist) + "&track_name=" + enc(title), null);
                limited = search.code == 429;
                if (search.code == 200) for (JsonElement e : JsonParser.parseString(search.body).getAsJsonArray()) {
                    if (e.isJsonObject()) addLrc(candidates, e.getAsJsonObject(), artist, title, rawTitle);
                }
                error |= search.code >= 500;
            }
            error |= exact.code >= 500;
        } catch (Exception failure) { error = true; }
        try {
            Reply search = network.request("https://music.163.com/api/search/get/", "s=" + enc(artist + " " + title) + "&type=1&offset=0&limit=20");
            limited |= search.code == 429;
            if (search.code == 200) {
                JsonObject json = JsonParser.parseString(search.body).getAsJsonObject();
                if (json.has("result") && json.get("result").isJsonObject()) {
                    JsonObject results = json.getAsJsonObject("result");
                    if (results.has("songs")) for (JsonElement e : results.getAsJsonArray("songs")) {
                        try {
                            JsonObject song = e.getAsJsonObject();
                            boolean artistMatches = false;
                            for (JsonElement a : song.getAsJsonArray("artists"))
                                artistMatches |= norm(a.getAsJsonObject().get("name").getAsString()).equals(norm(artist));
                            String name = song.get("name").getAsString();
                            if (artistMatches && titleMatches(name, title, rawTitle))
                                candidates.add(new Candidate("NetEase", Long.toString(song.get("id").getAsLong()), name, song.get("duration").getAsDouble() / 1000, null));
                        } catch (RuntimeException malformed) { /* Skip one malformed entry, not the provider. */ }
                    }
                } else error = true;
            } else error |= search.code != 404;
        } catch (Exception failure) { error = true; }
        try {
            Result result = choose(candidates, seconds, approximate, network);
            if (result != null) return result;
        } catch (Exception failure) { error = true; }
        return new Result(SyncedLyrics.EMPTY, "", false, 0, limited ? "rate_limited" : error ? "unavailable" : "not_found");
    }

    private static void addLrc(List<Candidate> list, JsonObject o, String artist, String title, String rawTitle) {
        try {
            if (norm(o.get("artistName").getAsString()).equals(norm(artist)) && titleMatches(o.get("trackName").getAsString(), title, rawTitle)
                && o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull())
                list.add(new Candidate("LRCLIB", "", o.get("trackName").getAsString(), o.get("duration").getAsDouble(), o.get("syncedLyrics").getAsString()));
        } catch (RuntimeException malformed) { }
    }

    private static Result choose(List<Candidate> candidates, long seconds, boolean approximate, Transport network) throws Exception {
        if (seconds <= 0) return null;
        candidates.sort(Comparator.comparingDouble(c -> Math.abs(c.seconds - seconds)));
        int downloads = 0;
        for (Candidate c : candidates) {
            double delta = Math.abs(c.seconds - seconds);
            if (!Double.isFinite(c.seconds) || c.seconds <= 0 || delta > (approximate ? Math.min(15, seconds * .08) : 2)) continue;
            String lrc = c.lrc;
            if (lrc == null) {
                if (++downloads > 3) continue;
                try {
                    Reply r = network.request("https://music.163.com/api/song/lyric?id=" + c.id + "&lv=1", null);
                    if (r.code != 200) continue;
                    JsonObject json = JsonParser.parseString(r.body).getAsJsonObject();
                    lrc = json.getAsJsonObject("lrc").get("lyric").getAsString();
                } catch (Exception invalid) { continue; }
            }
            SyncedLyrics parsed = SyncedLyrics.parseLrc(lrc);
            long actualLines = parsed.getLines().stream().filter(l -> !l.text().isBlank()).count();
            if (parsed.isSynced() && actualLines >= 2)
                return new Result(parsed, c.source, delta > 2, Math.round(c.seconds), delta > 2 ? "approximate" : "ready");
        }
        return null;
    }

    static boolean titleMatches(String candidate, String requested, String rawTitle) {
        if (norm(candidate).equals(norm(requested))) return true;
        // Presentation labels are harmless; recording labels require evidence in the actual media title.
        String base = candidate.replaceAll("(?i)\\s*[\\[(](official (?:audio|video)|lyrics|audio|video)[\\])]", "").strip();
        if (norm(base).equals(norm(requested))) return true;
        String qualifier = candidate.replaceFirst("^.*?[\\[(]", "").replaceAll("[\\])]", "");
        base = candidate.replaceAll("\\s*[\\[(][^\\])]*[\\])]", "").strip();
        return !base.equals(candidate) && norm(base).equals(norm(requested))
            && norm(qualifier).replace(" live", "").length() > 2
            && norm(rawTitle).contains(norm(qualifier).replace(" live", ""));
    }

    static String norm(String s) {
        return Normalizer.normalize(compactSpacedLetters(s), Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }
    private static final java.util.regex.Pattern SPACED_LETTERS = java.util.regex.Pattern.compile(
        "(?U)(?<![\\p{L}\\p{N}])(?:\\p{L}\\h+){2,}\\p{L}(?![\\p{L}\\p{N}])");
    static String compactSpacedLetters(String text) {
        return SPACED_LETTERS.matcher(text == null ? "" : text).replaceAll(match -> match.group().replaceAll("\\h+", ""));
    }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static Reply request(String url, String form) throws Exception {
        URI uri = URI.create(url);
        long now = System.currentTimeMillis();
        Reply waiting = BACKOFF.remaining(uri.getHost(), now);
        if (waiting != null) return waiting;
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
            .header("User-Agent", "BKClient/3.0.8 (lyrics)")
            .header("Accept", "application/json");
        if (form != null) builder.header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form));
        else builder.GET();
        var future = HTTP.sendAsync(builder.build(), info -> new LimitedBody());
        HttpResponse<byte[]> response;
        try { response = future.get(6, TimeUnit.SECONDS); }
        catch (Exception e) { future.cancel(true); throw e; }
        long retry = 60;
        try { retry = Math.clamp(Long.parseLong(response.headers().firstValue("Retry-After").orElse("60")), 30, 600); }
        catch (NumberFormatException ignored) { }
        BACKOFF.record(uri.getHost(), response.statusCode(), retry, System.currentTimeMillis());
        return new Reply(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8), retry);
    }

    /** A service outage stays 503 during backoff; waiting is not evidence of a rate limit. */
    static final class ProviderBackoff {
        private record Pause(int code, long until) { }
        private final Map<String, Pause> pauses = new ConcurrentHashMap<>();
        Reply remaining(String host, long now) {
            Pause pause = pauses.get(host);
            if (pause == null) return null;
            if (now >= pause.until) { pauses.remove(host, pause); return null; }
            return new Reply(pause.code, "", (pause.until - now + 999) / 1000);
        }
        void record(String host, int code, long retrySeconds, long now) {
            if (code == 429 || code == 503)
                pauses.put(host, new Pause(code, now + Math.clamp(retrySeconds, 30, 600) * 1000));
        }
    }

    /** Bounded subscriber: cancellation also stops a response that never finishes. */
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();
        private Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody() { return result; }
        public void onSubscribe(Flow.Subscription s) { subscription = s; s.request(1); }
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer b : buffers) {
                if (data.size() + b.remaining() > 1024 * 1024) {
                    subscription.cancel(); result.completeExceptionally(new IllegalStateException("Lyrics response exceeds 1 MiB")); return;
                }
                byte[] chunk = new byte[b.remaining()]; b.get(chunk); data.writeBytes(chunk);
            }
            subscription.request(1);
        }
        public void onError(Throwable t) { result.completeExceptionally(t); }
        public void onComplete() { result.complete(data.toByteArray()); }
    }
}
