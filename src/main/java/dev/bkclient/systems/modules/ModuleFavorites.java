package dev.bkclient.systems.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.LinkedHashSet;
import java.util.Set;

/** Local UI preferences, independent of combat/config profiles. */
public final class ModuleFavorites {
    private static final Set<String> names = new LinkedHashSet<>();
    private static long revision;

    private ModuleFavorites() {}
    public static boolean contains(String name) { return names.contains(name); }
    public static long revision() { return revision; }
    public static void toggle(String name) {
        if (!names.remove(name)) names.add(name);
        revision++;
    }
    public static JsonArray save() {
        JsonArray result = new JsonArray();
        names.forEach(result::add);
        return result;
    }
    public static void load(JsonElement data) {
        names.clear();
        if (data != null && data.isJsonArray()) {
            for (JsonElement item : data.getAsJsonArray()) {
                if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString() && !item.getAsString().isBlank()) {
                    names.add(item.getAsString());
                }
            }
        }
        revision++;
    }
}
