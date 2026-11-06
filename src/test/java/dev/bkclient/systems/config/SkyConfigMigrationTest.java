package dev.bkclient.systems.config;
import com.google.gson.*;
public final class SkyConfigMigrationTest {
    static void check(boolean b, String m) { if (!b) throw new AssertionError(m); }
    static JsonObject module(JsonObject root, String name) {
        for (JsonElement e : root.getAsJsonArray("modules")) if (e.getAsJsonObject().get("name").getAsString().equals(name)) return e.getAsJsonObject();
        throw new AssertionError(name);
    }
    public static void main(String[] args) {
        String legacy = """
          {"modules":[{"name":"Ambience","key":75,"enabled":true,"settings":{
          "modules.settings.ambience.skybox":"Stars","Shader Speed":1.7,"theme.sync":false,
          "modules.settings.ambience.wet_world":true,"Vegetation Wind":true,"future-setting":7}}]}
          """;
        JsonObject input=JsonParser.parseString(legacy).getAsJsonObject();
        JsonObject before=input.deepCopy(), result=SkyConfigMigration.migrate(input);
        check(input.equals(before),"input remains untouched");
        JsonObject sky=module(result,"Sky"), ambience=module(result,"Ambience");
        check(sky.get("enabled").getAsBoolean(),"enabled state migrates");
        check(sky.get("key").getAsInt()!=75 && ambience.get("key").getAsInt()==75,"no duplicate bind");
        check(sky.getAsJsonObject("settings").get("modules.settings.ambience.skybox").getAsString().equals("Classic Stars"),"old Stars appearance retained");
        check(sky.getAsJsonObject("settings").get("Shader Speed").getAsFloat()==1.7f,"speed retained");
        check(!sky.getAsJsonObject("settings").get("theme.sync").getAsBoolean(),"custom colors retained");
        check(ambience.getAsJsonObject("settings").has("Vegetation Wind") && ambience.getAsJsonObject("settings").has("future-setting"),"wind and unknown settings preserved");
        check(!ambience.getAsJsonObject("settings").has("Shader Speed"),"moved settings removed");
        check(SkyConfigMigration.migrate(result).equals(result),"idempotent migration");
        JsonObject explicit=sky.deepCopy(); explicit.addProperty("enabled",false);
        explicit.getAsJsonObject("settings").addProperty("modules.settings.ambience.skybox","Stars");
        for(boolean first:new boolean[]{false,true}) {
            JsonObject mixed=before.deepCopy();
            if(first) { JsonArray entries=new JsonArray(); entries.add(explicit); entries.add(module(mixed,"Ambience")); mixed.add("modules",entries); }
            else mixed.getAsJsonArray("modules").add(explicit);
            check(module(SkyConfigMigration.migrate(mixed),"Sky").equals(explicit),"explicit Sky wins in either order");
        }
        module(input,"Ambience").addProperty("enabled",false);
        check(!module(SkyConfigMigration.migrate(input),"Sky").get("enabled").getAsBoolean(),"disabled legacy stays disabled");
        JsonObject empty=JsonParser.parseString("{\"modules\":[]}").getAsJsonObject();
        check(SkyConfigMigration.migrate(empty).equals(empty),"no manufactured settings");
        System.out.println("Sky migration: legacy appearance, settings, binds, explicit precedence, unknown fields and idempotence PASS");
    }
}
