package dev.bkclient.systems.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Resolves the old/new module names before settings and enabled state are applied. */
final class FreecamConfigMigration {
    private static final String VERSION = "freecamGhostDefaultsVersion";

    static JsonObject migrate(JsonObject input) {
        JsonObject output = input.deepCopy();
        if (!output.has("modules") || !output.get("modules").isJsonArray()) return output;
        JsonObject canonical = null;
        JsonObject legacy = null;
        JsonArray modules = new JsonArray();
        for (JsonElement element : output.getAsJsonArray("modules")) {
            if (element.isJsonObject() && element.getAsJsonObject().has("name")) {
                JsonObject module = element.getAsJsonObject();
                String name = module.get("name").getAsString().replace(" ", "");
                if (name.equalsIgnoreCase("FreeCam")) { canonical = module; continue; }
                if (name.equalsIgnoreCase("FreeCamera")) { legacy = module; continue; }
            }
            modules.add(element);
        }
        JsonObject freecam = canonical != null ? canonical : legacy;
        if (freecam != null) {
            freecam.addProperty("name", "FreeCam");
            JsonObject settings = freecam.has("settings") && freecam.get("settings").isJsonObject()
                    ? freecam.getAsJsonObject("settings") : new JsonObject();
            if (settings.has("Display Coordinates")) {
                if (!settings.has("Display coordinates")) settings.add("Display coordinates", settings.get("Display Coordinates"));
                settings.remove("Display Coordinates");
            }
            // Repair existing profiles once. The original switch remains user-configurable afterwards.
            if (!output.has(VERSION)) settings.addProperty("Cancel Move", true);
            freecam.add("settings", settings);
            modules.add(freecam);
        }
        output.add("modules", modules);
        output.addProperty(VERSION, 1);
        return output;
    }
}
