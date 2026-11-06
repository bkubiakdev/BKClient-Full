package dev.bkclient.utility.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bkclient.BKClientInit;
import dev.bkclient.utility.game.server.ServerUtility;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ClientTelemetryUtility {
    private static volatile HttpURLConnection activeConnection;
    private static volatile boolean privacyServiceReady;
    public static boolean isPrivacyServiceReady() { return privacyServiceReady; }
    public static void privacyChanged() {
        friendPresence.clear();
        privacyServiceReady = false;
        HttpURLConnection current = activeConnection;
        if (current != null) current.disconnect();
        if (dev.bkclient.systems.friends.PresencePrivacy.isEnabled()) refreshNow();
    }
    private static final String HEARTBEAT_URL = "https://changeme.net/mods/api/client/heartbeat";
    private static final String CLIENT_ID_HEADER = "x-bk-client-id";
    private static final String CLIENT_VARIANT_HEADER = "x-bk-client-variant";
    private static final String CLIENT_MINECRAFT_VERSION_HEADER = "x-bk-minecraft-version";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String USER_AGENT_VALUE = "BK-Client/";
    private static final String CLIENT_VARIANT = "full";
    private static final String TELEMETRY_SALT = "BK-Client-Telemetry-Salt-2026";
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30L;
    private static final long FRIEND_PRESENCE_TTL_MS = TimeUnit.SECONDS.toMillis(120L);
    private static ScheduledExecutorService scheduler;
    private static volatile String cachedClientId;
    private static final Map<String, FriendPresence> friendPresence = new ConcurrentHashMap<>();

    public record FriendPresence(String name, String server, long seenAt) {
        public boolean online() {
            return server != null && !server.isBlank()
                    && System.currentTimeMillis() - seenAt <= FRIEND_PRESENCE_TTL_MS;
        }
    }

    public static synchronized void start() {
        if (scheduler != null) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor((task) -> {
            Thread thread = new Thread(task, "bkclient-telemetry");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.execute(ClientTelemetryUtility::sendHeartbeat);
        scheduler.scheduleAtFixedRate(ClientTelemetryUtility::sendHeartbeat,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public static synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public static void refreshNow() {
        ScheduledExecutorService current = scheduler;
        if (current == null || current.isShutdown()) {
            return;
        }

        current.execute(ClientTelemetryUtility::sendHeartbeat);
    }

    public static void applyClientHeaders(HttpURLConnection connection) {
        connection.setRequestProperty(CLIENT_ID_HEADER, getClientId());
        connection.setRequestProperty(CLIENT_VARIANT_HEADER, CLIENT_VARIANT);
        connection.setRequestProperty(CLIENT_MINECRAFT_VERSION_HEADER, getMinecraftVersion());
    }

    public static String getClientId() {
        String current = cachedClientId;
        if (current != null) {
            return current;
        }

        current = sha256(getIdentitySeed() + ":" + TELEMETRY_SALT);
        cachedClientId = current;
        return current;
    }

    public static FriendPresence getFriendPresence(String name) {
        if (!dev.bkclient.systems.friends.PresencePrivacy.isEnabled() || name == null) {
            return null;
        }

        FriendPresence presence = friendPresence.get(normalizeName(name));
        if (presence == null || !presence.online()) {
            return null;
        }

        return presence;
    }

    public static void forgetFriendPresence(String name) {
        if (name != null) {
            friendPresence.remove(normalizeName(name));
        }
    }

    private static void sendHeartbeat() {
        if (!dev.bkclient.systems.friends.PresencePrivacy.isEnabled()) return;
        long privacyRevision = dev.bkclient.systems.friends.PresencePrivacy.revision();
        HttpURLConnection connection = null;
        try {
            java.net.URI uri = java.net.URI.create(HEARTBEAT_URL);
            if (!dev.bkclient.systems.friends.PresencePrivacy.allows(privacyRevision)) return;
            connection = (HttpURLConnection) uri.toURL().openConnection();
            activeConnection = connection;
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE);
            connection.setRequestProperty("X-BK-Presence-Version", "1");
            connection.setRequestProperty(USER_AGENT_HEADER, USER_AGENT_VALUE + BKClientInit.VERSION);
            applyClientHeaders(connection);
            connection.setDoOutput(true);

            // Snapshot game state on the client thread, then perform only I/O here.
            String snapshot = MinecraftClient.getInstance().submit(ClientTelemetryUtility::createHeartbeatBody)
                    .get(5, TimeUnit.SECONDS);
            if (snapshot == null || !dev.bkclient.systems.friends.PresencePrivacy.allows(privacyRevision)) return;
            byte[] body = snapshot.getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            if (dev.bkclient.systems.friends.PresencePrivacy.allows(privacyRevision)) readPresenceResponse(connection, privacyRevision);
        } catch (Exception e) {
            privacyServiceReady = false;
            BKClientInit.LOGGER.debug("Failed to send client heartbeat", e);
        } finally {
            if (activeConnection == connection) activeConnection = null;
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String createHeartbeatBody() {
        if (!dev.bkclient.systems.friends.PresencePrivacy.isEnabled()) return null;
        JsonObject body = new JsonObject();
        body.addProperty("presenceEnabled", true);
        body.addProperty("username", getUsername());
        body.addProperty("server", dev.bkclient.systems.friends.PresencePrivacy.publishedServer(ClientTelemetryUtility::getCurrentServer));
        body.addProperty("hidden", dev.bkclient.systems.friends.PresencePrivacy.isHidden());
        body.addProperty("version", BKClientInit.VERSION);

        JsonArray friends = new JsonArray();
        for (String friend : BKClientInit.getInstance().getFriendManager().listFriends()) {
            friends.add(friend);
        }
        body.add("friends", friends);

        return body.toString();
    }

    private static void readPresenceResponse(HttpURLConnection connection, long privacyRevision) {
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) { privacyServiceReady = false; return; }
            InputStream input = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (input == null) {
                return;
            }

            String response;
            try (input) {
                response = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
            }

            if (response.isEmpty()) {
                return;
            }

            JsonElement json = JsonParser.parseString(response);
            if (dev.bkclient.systems.friends.PresencePrivacy.allows(privacyRevision)) {
                parsePresencePayload(json);
                privacyServiceReady = true;
            }
        } catch (Exception e) {
            BKClientInit.LOGGER.debug("Failed to read friend presence", e);
        }
    }

    private static void parsePresencePayload(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return;
        }

        if (json.isJsonArray()) {
            parsePresenceArray(json.getAsJsonArray());
            return;
        }

        if (!json.isJsonObject()) {
            return;
        }

        JsonObject object = json.getAsJsonObject();
        JsonElement friends = firstPresent(object, "friends", "presence", "online");
        if (friends == null) {
            parsePresenceObject(object);
        } else if (friends.isJsonArray()) {
            parsePresenceArray(friends.getAsJsonArray());
        } else if (friends.isJsonObject()) {
            parsePresenceObject(friends.getAsJsonObject());
        }
    }

    private static void parsePresenceArray(JsonArray array) {
        long now = System.currentTimeMillis();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject object = element.getAsJsonObject();
            String name = getString(object, "name", "username", "nick");
            String server = readPublishedServer(object);
            boolean online = !object.has("online") || object.get("online").getAsBoolean();
            updateFriendPresence(name, online ? server : null, now);
        }
    }

    private static void parsePresenceObject(JsonObject object) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String name = entry.getKey();
            if (isMetaPresenceKey(name)) {
                continue;
            }

            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull()) {
                updateFriendPresence(name, null, now);
            } else if (value.isJsonPrimitive()) {
                updateFriendPresence(name, value.getAsString(), now);
            } else if (value.isJsonObject()) {
                JsonObject presence = value.getAsJsonObject();
                String server = readPublishedServer(presence);
                boolean online = !presence.has("online") || presence.get("online").getAsBoolean();
                updateFriendPresence(name, online ? server : null, now);
            }
        }
    }

    private static void updateFriendPresence(String name, String server, long seenAt) {
        if (name == null || name.isBlank()) {
            return;
        }

        String key = normalizeName(name);
        if (server == null || server.isBlank()) {
            friendPresence.remove(key);
            return;
        }

        friendPresence.put(key, new FriendPresence(name, server, seenAt));
    }

    private static String readPublishedServer(JsonObject presence) {
        if (presence.has("hidden") && presence.get("hidden").isJsonPrimitive()
                && presence.getAsJsonPrimitive("hidden").isBoolean() && presence.get("hidden").getAsBoolean()) {
            return "Hidden";
        }
        return getString(presence, "server", "serverAddress", "address");
    }

    private static JsonElement firstPresent(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name)) {
                return object.get(name);
            }
        }

        return null;
    }

    private static String getString(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name) && !object.get(name).isJsonNull()) {
                String value = object.get(name).getAsString();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }

        return null;
    }

    private static boolean isMetaPresenceKey(String name) {
        return "ok".equalsIgnoreCase(name)
                || "error".equalsIgnoreCase(name)
                || "message".equalsIgnoreCase(name)
                || "onlineWindowSeconds".equalsIgnoreCase(name);
    }

    private static String getUsername() {
        try {
            String username = MinecraftClient.getInstance().getSession().getUsername();
            return username == null || username.isBlank() ? "unknown" : username;
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private static String getCurrentServer() {
        try {
            return ServerUtility.getCurrentServerAddress("Offline");
        } catch (Exception ignored) {
            return "Offline";
        }
    }

    private static String getIdentitySeed() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            UUID uuid = client.getSession().getUuidOrNull();
            if (uuid != null) {
                return uuid.toString();
            }

            String username = client.getSession().getUsername();
            if (username != null && !username.isBlank()) {
                return username;
            }
        } catch (Exception ignored) {
        }

        return "unknown";
    }

    private static String getMinecraftVersion() {
        try {
            return SharedConstants.getGameVersion().getName();
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private ClientTelemetryUtility() {
    }
}
