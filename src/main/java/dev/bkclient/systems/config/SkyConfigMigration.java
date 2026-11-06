package dev.bkclient.systems.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;

/** Splits legacy Ambience sky settings without mutating the input or overriding an explicit Sky module. */
final class SkyConfigMigration {
    private static final Set<String> KEYS = Set.of(
        "theme.sync", "color.source", "modules.settings.ambience.change_color",
        "modules.settings.ambience.sky_color", "modules.settings.ambience.cloud_color", "modules.settings.ambience.stars_color",
        "modules.settings.ambience.skybox", "modules.settings.ambience.shader", "modules.settings.ambience.shader_type",
        "modules.settings.ambience.shader_opacity", "Shader Speed", "Sky Effect Color", "Sky Second Color", "Sky Brightness",
        "Sky Scale", "Sky Intensity", "Neon Palette", "Neon Color 1", "Neon Color 2", "Neon Color 3",
        "Storm Lightning", "Strike Interval", "Bolt Power", "World Flash");
    static JsonObject migrate(JsonObject input) {
        JsonObject output = input.deepCopy();
        if (!output.has("modules") || !output.get("modules").isJsonArray()) return output;
        JsonObject ambience = null, sky = null;
        for (JsonElement entry : output.getAsJsonArray("modules")) {
            if (!entry.isJsonObject() || !entry.getAsJsonObject().has("name")) continue;
            JsonObject module = entry.getAsJsonObject();
            if ("Ambience".equalsIgnoreCase(module.get("name").getAsString())) ambience = module;
            if ("Sky".equalsIgnoreCase(module.get("name").getAsString())) sky = module;
        }
        if (ambience == null || !ambience.has("settings")) return output;
        JsonObject old = ambience.getAsJsonObject("settings");
        JsonObject moved = new JsonObject();
        for (String key : KEYS) if (old.has(key)) moved.add(key, old.remove(key));
        if (sky == null && !moved.isEmpty()) {
            String modeKey = "modules.settings.ambience.skybox";
            if (moved.has(modeKey) && "Stars".equals(moved.get(modeKey).getAsString()))
                moved.addProperty(modeKey, "Classic Stars");
            sky = new JsonObject();
            sky.addProperty("name", "Sky");
            sky.addProperty("enabled", ambience.has("enabled") && ambience.get("enabled").getAsBoolean());
            // Keep the original Ambience binding; sharing it would toggle two independent modules.
            sky.addProperty("key", -1);
            sky.add("settings", moved);
            output.getAsJsonArray("modules").add(sky);
        }
        return output;
    }
}
