package dev.bkclient.systems.cosmetics;

import com.google.gson.JsonObject;
import dev.bkclient.systems.file.SafeJsonFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

/** Local selections, independent of module configs and other players. */
public final class CosmeticPreferences {
    private final Path file;
    private final EnumMap<CosmeticSlot, CosmeticEntry> equipped = new EnumMap<>(CosmeticSlot.class);
    private String persistenceError;

    public CosmeticPreferences(Path file) {
        this.file = file;
        if (!Files.exists(file) && !Files.exists(SafeJsonFile.backup(file))) return;
        try {
            JsonObject saved = SafeJsonFile.read(file);
            for (var slot : CosmeticSlot.values()) {
                var value = saved.get(slot.name());
                if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) continue;
                var entry = CosmeticCatalog.find(slot, value.getAsString());
                if (entry != null) equipped.put(slot, entry);
            }
        } catch (Exception failure) { report(failure); }
    }

    public synchronized CosmeticEntry equipped(CosmeticSlot slot) { return equipped.get(slot); }
    public synchronized String persistenceError() { return persistenceError; }

    public synchronized void equip(CosmeticEntry entry) {
        if (entry == null) return;
        var canonical = CosmeticCatalog.find(entry.slot(), entry.id());
        if (canonical == null) throw new IllegalArgumentException("Unknown cosmetic");
        if (equipped.put(entry.slot(), canonical) != canonical) save();
    }

    public synchronized void unequip(CosmeticSlot slot) {
        if (equipped.remove(slot) != null) save();
    }

    private void save() {
        var saved = new JsonObject();
        saved.addProperty("version", 1);
        equipped.forEach((slot, entry) -> saved.addProperty(slot.name(), entry.id()));
        try { SafeJsonFile.write(file, saved); persistenceError = null; }
        catch (Exception failure) { report(failure); }
    }

    private void report(Exception failure) {
        persistenceError = "Could not save or restore cosmetics";
        System.err.println("[BKClient Cosmetics] " + persistenceError + ": " + failure);
    }
}
