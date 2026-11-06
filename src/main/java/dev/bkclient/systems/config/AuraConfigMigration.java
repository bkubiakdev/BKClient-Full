package dev.bkclient.systems.config;

import com.google.gson.*;
import java.util.Map;

/** Migrates only the retired combat modules; other module data is preserved. */
final class AuraConfigMigration {
    private static final Map<String, String> LABELS = Map.ofEntries(
        Map.entry("Игроков", "Players"), Map.entry("Голых игроков", "Unarmored players"),
        Map.entry("Мобов", "Mobs"), Map.entry("Друзей", "Friends"),
        Map.entry("Сфокусированная", "Focused"), Map.entry("Свободная", "Free"),
        Map.entry("Умные криты", "Smart criticals"), Map.entry("Бить через блоки", "Through walls"),
        Map.entry("Используешь еду", "Using an item"), Map.entry("Открыт контейнер", "Screen open"),
        Map.entry("HvH", "Ana"), Map.entry("Fov", "FOV"));

    static JsonObject migrate(JsonObject input) {
        JsonObject result = input.deepCopy();
        if (!result.has("modules") || !result.get("modules").isJsonArray()) return result;
        JsonArray modules = new JsonArray();
        JsonObject bkengine = null;
        JsonObject main = null;
        for (JsonElement element : result.getAsJsonArray("modules")) {
            JsonObject module = element.getAsJsonObject();
            String name = module.get("name").getAsString();
            if (legacyLabel(name, "b30f0cc85b4329a329807945850cc92188b7308c7c61ff60ab3db5e7f3f1f4f2")) {
                bkengine = module;
            } else if (name.equalsIgnoreCase("Aura")) {
                // Old Aura has a different settings schema and must not enable the new engine.
                if (module.has("settings") && module.get("settings").isJsonObject()
                    && module.getAsJsonObject("settings").has("Movement correction")
                    && module.getAsJsonObject("settings").has("Snap mode")) main = module;
            } else if (!name.equalsIgnoreCase("Aura2")) {
                modules.add(module);
            }
        }
        JsonObject selected = main != null ? main : bkengine;
        if (selected != null) {
            selected.addProperty("name", "Aura");
            if (selected.has("settings")) selected.add("settings", translate(selected.get("settings")));
            if (!result.has("auraCriticalChecksVersion")) migrateCriticalCheck(selected.getAsJsonObject("settings"));
            modules.add(selected);
        }
        result.addProperty("auraCriticalChecksVersion", 1);
        result.add("modules", modules);
        return result;
    }

    private static void migrateCriticalCheck(JsonObject settings) {
        if (settings == null) return;
        JsonElement old = settings.remove("Only critical hits");
        boolean enabled = old == null || old.getAsBoolean();
        JsonElement checks = settings.get("Checks");
        if (checks == null) { checks = new JsonObject(); settings.add("Checks", checks); }
        JsonArray selected;
        if (checks.isJsonArray()) selected = checks.getAsJsonArray();
        else {
            JsonObject object = checks.getAsJsonObject();
            if (!object.has("selected")) object.add("selected", new JsonArray());
            selected = object.getAsJsonArray("selected");
        }
        JsonPrimitive label = new JsonPrimitive("Only critical hits");
        selected.remove(label);
        if (enabled) selected.add(label);
    }

    // Fingerprints retain compatibility with retired configuration labels without
    // shipping their obsolete branding in the client.
    private static boolean legacyLabel(String value, String fingerprint) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(value.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).equals(fingerprint);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String translateLabel(String label) {
        if (legacyLabel(label, "107289f5913b03bedbcda5c64ca646f608abef0dfdd79c528ea34bc163fc367f")) return "Anti-bot";
        return LABELS.getOrDefault(label, label);
    }

    private static JsonElement translate(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject result = new JsonObject();
            value.getAsJsonObject().entrySet().forEach(entry ->
                result.add(translateLabel(entry.getKey()), translate(entry.getValue())));
            return result;
        }
        if (value.isJsonArray()) {
            JsonArray result = new JsonArray();
            value.getAsJsonArray().forEach(entry -> result.add(translate(entry)));
            return result;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String label = value.getAsString();
            return new JsonPrimitive(translateLabel(label));
        }
        return value.deepCopy();
    }
}
