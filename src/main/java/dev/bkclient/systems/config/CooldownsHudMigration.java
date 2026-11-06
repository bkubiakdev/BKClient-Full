package dev.bkclient.systems.config;
import com.google.gson.*;

/** Consolidates the optional Cooldowns2 layout into the canonical Cooldowns widget. */
public final class CooldownsHudMigration {
    private CooldownsHudMigration() {}
    public static JsonObject migrate(JsonObject input) {
        JsonObject result = input.deepCopy();
        if (!result.has("hudElements")) return result;
        JsonArray elements = new JsonArray();
        JsonObject original = null, replacement = null;
        for (JsonElement entry : result.getAsJsonArray("hudElements")) {
            JsonObject hud = entry.getAsJsonObject();
            String name = hud.get("name").getAsString();
            if (name.equalsIgnoreCase("hud.cooldowns")) original = hud;
            else if (name.equalsIgnoreCase("hud.cooldowns2")) replacement = hud;
            else elements.add(hud);
        }
        if (replacement == null) return result;
        JsonObject selected = original == null || showing(replacement) ? replacement.deepCopy() : original.deepCopy();
        JsonObject settings = original != null && original.has("settings") ? original.getAsJsonObject("settings").deepCopy() : new JsonObject();
        if (replacement.has("settings")) replacement.getAsJsonObject("settings").entrySet().forEach(e -> settings.add(e.getKey(), e.getValue().deepCopy()));
        selected.addProperty("name", "hud.cooldowns");
        selected.addProperty("showing", showing(original) || showing(replacement));
        selected.add("settings", settings);
        elements.add(selected);
        result.add("hudElements", elements);
        return result;
    }
    private static boolean showing(JsonObject hud) { return hud != null && hud.has("showing") && hud.get("showing").getAsBoolean(); }
}
