package dev.bkclient.systems.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class FreecamConfigMigrationTest {
    private static final String CURRENT = "{\"name\":\"FreeCam\",\"enabled\":true,\"key\":71,\"settings\":{\"Speed X\":1.3,\"Cancel Move\":false}}";
    private static final String LEGACY = "{\"name\":\"Free Camera\",\"enabled\":false,\"settings\":{\"Speed X\":0.05,\"Cancel Move\":true,\"Display Coordinates\":true}}";
    private static JsonObject read(String modules) { return JsonParser.parseString("{\"modules\":[" + modules + "]}").getAsJsonObject(); }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        for (String order : new String[]{CURRENT + "," + LEGACY, LEGACY + "," + CURRENT}) {
            JsonObject input = read(order);
            JsonObject output = FreecamConfigMigration.migrate(input);
            check(input.getAsJsonArray("modules").size() == 2);
            check(output.getAsJsonArray("modules").size() == 1);
            JsonObject module = output.getAsJsonArray("modules").get(0).getAsJsonObject();
            check(module.get("enabled").getAsBoolean() && module.get("key").getAsInt() == 71);
            check(module.getAsJsonObject("settings").get("Speed X").getAsDouble() == 1.3);
            check(module.getAsJsonObject("settings").get("Cancel Move").getAsBoolean());
            check(FreecamConfigMigration.migrate(output).equals(output));
            module.getAsJsonObject("settings").addProperty("Cancel Move", false);
            check(!FreecamConfigMigration.migrate(output).getAsJsonArray("modules").get(0).getAsJsonObject().getAsJsonObject("settings").get("Cancel Move").getAsBoolean());
        }
        JsonObject legacy = FreecamConfigMigration.migrate(read(LEGACY)).getAsJsonArray("modules").get(0).getAsJsonObject();
        check(legacy.get("name").getAsString().equals("FreeCam"));
        check(legacy.getAsJsonObject("settings").get("Display coordinates").getAsBoolean());
        System.out.println("FreeCam migration: duplicate ordering, ghost default, saved key/speed, idempotence and legacy settings passed");
    }
}
