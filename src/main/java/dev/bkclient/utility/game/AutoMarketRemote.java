package dev.bkclient.utility.game;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Opt-in, device-specific remote settings. Credentials never enter exported configs. */
public final class AutoMarketRemote {
    private static final URI ENDPOINT = URI.create("https://changeme.net/mods/api/client/automarket");
    private static final Path FILE = FileManager.DIRECTORY.toPath().resolve("market-link-private.json");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private record Link(String token, String installation) {}
    private static volatile Link link;
    private static volatile String status = "Not linked";
    private static boolean started;
    private static volatile boolean stopped;
    private static java.util.concurrent.ScheduledExecutorService scheduler;
    private static final dev.bkclient.utility.concurrent.ClientTaskGate CLIENT_TASK = new dev.bkclient.utility.concurrent.ClientTaskGate();
    private static long applied;
    private static String error = "";
    private static Link activeLink;
    private static Link notifiedLink;

    public static synchronized void start() {
        if (started) return;
        started = true;
        try {
            if (Files.exists(FILE)) {
                JsonObject saved = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
                String token = saved.get("token").getAsString();
                String id = saved.get("installation").getAsString();
                if (token.matches("[a-f0-9]{64}") && id.matches("[a-f0-9-]{36}")) link = new Link(token, id);
            }
        } catch (Exception ignored) { status = "Link could not be loaded; link again"; }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "bkclient-market-link"); t.setDaemon(true); return t;
        });
        scheduler.scheduleWithFixedDelay(AutoMarketRemote::poll, 1, 1, TimeUnit.SECONDS);
    }

    public static synchronized void shutdown() {
        stopped = true;
        CLIENT_TASK.cancel();
        if (scheduler != null) scheduler.shutdownNow();
        scheduler = null;
        HTTP.shutdownNow();
        activeLink = notifiedLink = link = null;
    }

    private static <T> T onClient(java.util.function.Supplier<T> work) throws Exception {
        var task = CLIENT_TASK.submit(MinecraftClient.getInstance()::execute, () -> {
            if (stopped) throw new java.util.concurrent.CancellationException();
            return work.get();
        });
        try { return task.get(5, TimeUnit.SECONDS); }
        catch (Exception error) { task.cancel(false); throw error; }
    }

    public static void command(String value) {
        try {
            if (value.equalsIgnoreCase("status")) { MessageUtility.info(Text.of("AutoMarket: " + status)); return; }
            if (value.equalsIgnoreCase("off")) {
                link = null;
                Files.deleteIfExists(FILE);
                status = "Disconnected";
                MessageUtility.info(Text.of("AutoMarket website control disconnected.")); return;
            }
            if (!value.matches("[a-f0-9]{64}")) {
                MessageUtility.error(Text.of("Use .marketlink <key from website>, status or off.")); return;
            }
            Link next = new Link(value, UUID.randomUUID().toString());
            // Re-pasting the same key must keep its bound installation identity.
            if (link != null && link.token.equals(value)) next = link;
            JsonObject saved = new JsonObject(); saved.addProperty("token", next.token); saved.addProperty("installation", next.installation);
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(tmp, saved.toString());
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            link = next; status = "Connecting";
            MessageUtility.info(Text.of("Connecting AutoMarket to the website…"));
        } catch (Exception ignored) { MessageUtility.error(Text.of("Could not save the AutoMarket link.")); }
    }

    private static Map<String, SliderSetting> settings(AutoMarket m) {
        return Map.of("refreshRate", m.refreshRate, "confirmDelay", m.confirmDelay,
            "restartDelay", m.restartDelay, "sellInterval", m.sellInterval,
            "sellTax", m.sellTax, "reconnectDelay", m.reconnectDelay);
    }

    private static void poll() {
        Link current = link;
        if (current == null || stopped) return;
        if (activeLink != current) { activeLink = current; applied = 0; error = ""; }
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            JsonObject body = onClient(() -> {
                AutoMarket m = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
                if (link != current || m == null || m.isLoading() || m.isLoadingProfile()) return null;
                JsonObject b = new JsonObject(), values = new JsonObject();
                settings(m).forEach((key, setting) -> values.addProperty(key, Math.round(setting.getCurrentValue() * 1000.0) / 1000.0));
                b.add("state", values); b.addProperty("profile", m.getActiveProfile());
                b.addProperty("installation", current.installation); b.addProperty("applied", applied); b.addProperty("error", error);
                return b;
            });
            if (body == null || link != current) return;
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + current.token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            HttpResponse<String> response = HTTP.send(request, info -> new dev.bkclient.utility.network.LimitedStringBodySubscriber(8192));
            if (link != current) return;
            if (response.statusCode() == 401) {
                link = null; status = "Key revoked or already bound; create a new link";
                mc.execute(() -> MessageUtility.error(Text.of("AutoMarket: " + status))); return;
            }
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP failure");
            if (response.body().length() > 8192) throw new IllegalStateException("Response too large");
            JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
            long revision = result.get("revision").getAsLong();
            String profile = result.get("profile").getAsString();
            JsonObject values = result.getAsJsonObject("values");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
            Boolean done = onClient(() -> {
                AutoMarket m = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
                if (link != current || System.nanoTime() > deadline || m.isLoading() || m.isLoadingProfile()
                    || !profile.equals(m.getActiveProfile()) || !profile.equals(body.get("profile").getAsString())) return false;
                var allowed = settings(m);
                // Validate the whole response before mutating any setting.
                for (var entry : values.entrySet()) {
                    SliderSetting setting = allowed.get(entry.getKey());
                    if (setting == null || !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber())
                        throw new IllegalArgumentException("Unknown setting");
                    float v = entry.getValue().getAsFloat();
                    if (!Float.isFinite(v) || v < setting.getMin() || v > setting.getMax()) throw new IllegalArgumentException("Invalid range");
                }
                if (revision > applied && !values.isEmpty()) {
                    values.entrySet().forEach(entry -> allowed.get(entry.getKey()).setCurrentValue(entry.getValue().getAsFloat()));
                    m.syncControllersConfig();
                    m.getConfigManager().save(m.getActiveProfile());
                }
                return true;
            });
            if (done) { applied = revision; error = ""; }
            if (notifiedLink != current) {
                notifiedLink = current;
                mc.execute(() -> MessageUtility.info(Text.of("AutoMarket connected to the website.")));
            }
            status = "Connected";
        } catch (Exception ignored) { status = "Connection interrupted; retrying"; error = "Client could not confirm settings"; }
    }
}
