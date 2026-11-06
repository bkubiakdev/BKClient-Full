package dev.bkclient.systems.modules;

import com.google.gson.JsonParser;

public final class ModuleFavoritesTest {
    public static void main(String[] args) {
        ModuleFavorites.load(null);
        check(!ModuleFavorites.contains("KillAura"), "old client data");
        long revision = ModuleFavorites.revision();
        ModuleFavorites.toggle("KillAura");
        check(ModuleFavorites.contains("KillAura") && ModuleFavorites.revision() > revision, "toggle invalidates sort cache");
        var saved = ModuleFavorites.save();
        ModuleFavorites.load(null);
        ModuleFavorites.load(saved);
        check(ModuleFavorites.contains("KillAura"), "round trip");
        ModuleFavorites.toggle("KillAura");
        check(!ModuleFavorites.contains("KillAura"), "unfavorite");
        ModuleFavorites.load(JsonParser.parseString("[null,3,{},true,\"\",\"World\",\"World\"]"));
        check(ModuleFavorites.contains("World") && ModuleFavorites.save().size() == 1, "invalid entries and duplicates");
        ModuleFavorites.load(null);
        System.out.println("Module favorites: persistence, legacy data, invalid entries and cache revision passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
