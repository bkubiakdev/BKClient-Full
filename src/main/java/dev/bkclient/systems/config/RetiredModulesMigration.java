package dev.bkclient.systems.config;

import com.google.gson.*;
import java.util.Locale;
import java.util.Set;

/** Retired modules cannot be resurrected by an older profile. Unknown third-party data is preserved. */
final class RetiredModulesMigration {
    private static final Set<String> REMOVED = Set.of("aurasprint", "aimbot", "autoexplosion",
            "elytratarget", "criticals", "noclip", "superfirework", "elytrastrafe", "soundesp",
            "itemhighlight", "itempickup", "mediacontrols", "windowembed", "auction", "autoresell");

    static JsonObject migrate(JsonObject input) {
        JsonObject output = input.deepCopy();
        if (!output.has("modules") || !output.get("modules").isJsonArray()) return output;
        JsonArray kept = new JsonArray();
        for (JsonElement element : output.getAsJsonArray("modules")) {
            if (element.isJsonObject()) {
                JsonObject module = element.getAsJsonObject();
                String name = module.has("name") ? module.get("name").getAsString()
                        .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") : "";
                if (REMOVED.contains(name)) continue;
                if (name.equals("brewingoverlay")) module.addProperty("enabled", false);
                if (module.has("settings") && module.get("settings").isJsonObject()) {
                    JsonObject settings = module.getAsJsonObject("settings");
                    if (name.equals("ambience")) settings.remove("modules.settings.ambience.end_sky");
                    if (name.equals("backtrack")) {
                        // The old Range belonged to Target Hold. Never let it overwrite Nemesis Range.
                        if (settings.has("Nemesis Range")) settings.add("Range", settings.remove("Nemesis Range"));
                        else if (!settings.has("Mode") || !Set.of("Default", "Distance", "Hit").contains(settings.get("Mode").getAsString()))
                            settings.addProperty("Range", 6);
                        if (!settings.has("Mode") || !Set.of("Distance", "Hit").contains(settings.get("Mode").getAsString()))
                            settings.addProperty("Mode", "Distance");
                        settings.remove("Delay");
                    }
                    if (name.equals("trails")) {
                        settings.addProperty("modules.settings.trails.style", "Dash");
                        for (String key : new String[]{"custom_color", "color2", "faded_style", "alpha", "height", "duration", "down"})
                            settings.remove("modules.settings.trails." + key);
                        settings.remove("color.source.ribbon");
                    }
                }
            }
            kept.add(element);
        }
        output.add("modules", kept);
        return output;
    }
}
