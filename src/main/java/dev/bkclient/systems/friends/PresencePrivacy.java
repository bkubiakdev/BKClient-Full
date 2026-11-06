package dev.bkclient.systems.friends;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Supplier;

/** Default Friends privacy settings; saved choices override these defaults. */
public final class PresencePrivacy {
    private static volatile boolean enabled = true;
    private static volatile boolean hidden;
    private static volatile long revision;
    private PresencePrivacy() {}
    public static boolean isEnabled() { return enabled; }
    public static boolean isHidden() { return hidden; }
    public static long revision() { return revision; }
    public static synchronized void setEnabled(boolean value) { enabled = value; revision++; }
    public static synchronized void setHidden(boolean value) { hidden = value; revision++; }
    public static boolean allows(long capturedRevision) { return enabled && revision == capturedRevision; }
    public static String publishedServer(Supplier<String> currentServer) {
        return !enabled || hidden ? "Hidden" : currentServer.get();
    }
    public static synchronized void load(JsonElement value) {
        enabled = true;
        hidden = false;
        if (value != null && value.isJsonObject()) {
            JsonObject json = value.getAsJsonObject();
            // One-time migration from the incompatible v2 defaults.
            if (flag(json, "legacyPresence")) {
                enabled = flag(json, "enabled");
            }
            if (json.has("shareServer")) hidden = !flag(json, "shareServer");
        }
        revision++;
    }
    private static boolean flag(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive()
            && json.getAsJsonPrimitive(key).isBoolean() && json.get(key).getAsBoolean();
    }
    public static synchronized JsonObject save() {
        JsonObject json = new JsonObject();
        json.addProperty("legacyPresence", true);
        json.addProperty("enabled", enabled);
        json.addProperty("shareServer", !hidden);
        return json;
    }
}
