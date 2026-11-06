package dev.bkclient.systems.config;
import com.google.gson.*;
public final class RetiredModulesMigrationTest {
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    static JsonObject migration(String name,String settings){return RetiredModulesMigration.migrate(JsonParser.parseString(
        "{\"modules\":[{\"name\":\""+name+"\",\"settings\":"+settings+"}]}").getAsJsonObject());}
    static JsonObject settings(JsonObject root){return root.getAsJsonArray("modules").get(0).getAsJsonObject().getAsJsonObject("settings");}
    public static void main(String[] args){
        for(String name:new String[]{"AuraSprint","Aim Bot","Auto Explosion","Elytra Target","Criticals","NoClip","Super Firework","Elytra Strafe","SoundESP","ItemHighlight","Item Pickup","MediaControls","Window Embed","Auction","Auto Resell"})
            check(migration(name,"{}").getAsJsonArray("modules").isEmpty(),"removed "+name);
        JsonObject old=JsonParser.parseString("{\"modules\":[{\"name\":\"Brewing Overlay\",\"enabled\":true,\"key\":80}]}").getAsJsonObject();
        JsonObject result=RetiredModulesMigration.migrate(old);
        check(!result.getAsJsonArray("modules").get(0).getAsJsonObject().get("enabled").getAsBoolean(),"Brewing disabled");
        check(old.getAsJsonArray("modules").get(0).getAsJsonObject().get("enabled").getAsBoolean(),"input unmodified");
        result=migration("Back Track","{\"Mode\":\"Target Hold\",\"Nemesis Range\":7.2,\"Range\":2,\"Delay\":1000,\"Nemesis Delay\":350}");
        check(Math.abs(settings(result).get("Range").getAsDouble()-7.2)<.001,"preserve Nemesis Range");
        check(!settings(result).has("Delay") && !settings(result).has("Nemesis Range"),"old settings discarded");
        check(settings(result).get("Nemesis Delay").getAsInt()==350,"delay retained");
        check(settings(result).get("Mode").getAsString().equals("Distance"),"canonical mode");
        var hit=settings(migration("BackTrack","{\"Mode\":\"Hit\",\"Range\":4.5}"));
        check(hit.get("Mode").getAsString().equals("Hit") && hit.get("Range").getAsDouble()==4.5,"saved Hit and range preserved");
        check(result.equals(RetiredModulesMigration.migrate(result)),"idempotence");
        check(settings(migration("BackTrack","{\"Mode\":\"Target Hold\",\"Range\":2}")).get("Range").getAsInt()==6,"target range cannot become default range");
        check(settings(migration("BackTrack","{\"Mode\":\"Default\",\"Range\":8}")).get("Range").getAsInt()==8,"new range preserved");
        result=migration("Trails","{\"modules.settings.trails.style\":\"Ribbon\",\"modules.settings.trails.color2\":12,\"modules.settings.trails.color1\":34,\"modules.settings.trails.dash_length\":5}");
        check(settings(result).get("modules.settings.trails.style").getAsString().equals("Dash"),"Ribbon falls back");
        check(!settings(result).has("modules.settings.trails.color2") && settings(result).get("modules.settings.trails.color1").getAsInt()==34,"only Ribbon colors removed");
        for(String name:new String[]{"AutoSprint","Aura","CustomSwords","AutoMarket","WindowScreen","FutureModule"})
            check(migration(name,"{}").getAsJsonArray("modules").size()==1,"retain "+name);
        result=migration("Ambience","{\"modules.settings.ambience.end_sky\":true,\"modules.settings.ambience.skybox\":\"Neon\"}");
        check(!settings(result).has("modules.settings.ambience.end_sky"),"retired Change sky removed");
        check(settings(result).get("modules.settings.ambience.skybox").getAsString().equals("Neon"),"Sky selection retained");
        System.out.println("Retired modules, Brewing, BackTrack, Trails and Ambience config checks passed");
    }
}
