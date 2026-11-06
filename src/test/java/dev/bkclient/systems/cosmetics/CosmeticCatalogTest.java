package dev.bkclient.systems.cosmetics;

import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import dev.bkclient.systems.file.SafeJsonFile;

public final class CosmeticCatalogTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        int[] expected = {8, 14, 33, 32};
        for (CosmeticSlot slot : CosmeticSlot.values()) {
            var entries = CosmeticCatalog.entries(slot);
            check(entries.size() == expected[slot.ordinal()], "Catalog size " + slot);
            var ids = new HashSet<String>();
            for (var entry : entries) {
                check(ids.add(entry.id()), "Duplicate id " + entry.id());
                check(entry.slot() == slot, "Wrong slot");
                check(CosmeticCatalog.find(slot, entry.id()) == entry, "Stable lookup");
                for (String resource : new String[]{entry.model(), entry.texture(), entry.animation()}) {
                    if (resource == null) continue;
                    try (var stream = CosmeticCatalog.class.getResourceAsStream("/assets/bkclient/" + resource)) {
                        check(stream != null && stream.read() >= 0, "Missing resource " + resource);
                    }
                }
            }
            check(CosmeticCatalog.find(slot, "../../bad") == null, "Unknown ids ignored");
            try { entries.clear(); throw new AssertionError("Mutable catalog"); } catch (UnsupportedOperationException expectedError) { }
        }
        Path file = Files.createTempDirectory("bk-cosmetics-test-").resolve("cosmetics.json");
        var selection = new CosmeticPreferences(file);
        check(selection.equipped(CosmeticSlot.WINGS) == null, "Defaults must be off");
        selection.equip(CosmeticCatalog.find(CosmeticSlot.WINGS, "angel_white"));
        selection.equip(CosmeticCatalog.find(CosmeticSlot.BACKPACK, "labubu"));
        var restored = new CosmeticPreferences(file);
        check(restored.equipped(CosmeticSlot.WINGS).id().equals("angel_white"), "Selection survives restart");
        check(restored.equipped(CosmeticSlot.BACKPACK).id().equals("labubu"), "Independent slots");
        check(restored.equipped(CosmeticSlot.CHARACTER) == null, "Old preferences keep the normal skin");
        restored.equip(CosmeticCatalog.find(CosmeticSlot.CHARACTER, "tralalerotralala"));
        restored = new CosmeticPreferences(file);
        check(restored.equipped(CosmeticSlot.CHARACTER).id().equals("tralalerotralala"), "Character survives restart");
        check(restored.equipped(CosmeticSlot.BACKPACK).id().equals("labubu"), "Character keeps existing accessories saved");
        restored.unequip(CosmeticSlot.CHARACTER);
        check(new CosmeticPreferences(file).equipped(CosmeticSlot.CHARACTER) == null, "Normal skin restored persistently");
        restored.unequip(CosmeticSlot.WINGS);
        check(new CosmeticPreferences(file).equipped(CosmeticSlot.WINGS) == null, "Unequip persists");
        var future = new JsonObject(); future.addProperty("WINGS", "removed_item"); future.addProperty("MELEE", "karambit");
        future.addProperty("BACKPACK", 42); future.addProperty("future_slot", "future");
        Files.writeString(file, future.toString());
        restored = new CosmeticPreferences(file);
        check(restored.equipped(CosmeticSlot.WINGS) == null, "Unknown cosmetic safely ignored");
        check(restored.equipped(CosmeticSlot.BACKPACK) == null, "Malformed slot safely ignored");
        check(restored.equipped(CosmeticSlot.MELEE).id().equals("karambit"), "Valid selection retained");
        future.addProperty("MELEE", "rgx_blade_yellow");
        Files.writeString(file, future.toString());
        check(new CosmeticPreferences(file).equipped(CosmeticSlot.MELEE) == null, "Removed RGX selection safely ignored");
        Files.writeString(SafeJsonFile.backup(file), "{\"MELEE\":\"butterfly\"}");
        Files.writeString(file, "{broken");
        check(new CosmeticPreferences(file).equipped(CosmeticSlot.MELEE).id().equals("butterfly"), "Backup recovers corruption");
        System.out.println("PASS: 87 cosmetic assets, character selection, slot validation, persistence, unequip and corrupt-file recovery");
    }
}
