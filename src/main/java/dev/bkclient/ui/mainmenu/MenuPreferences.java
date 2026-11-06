package dev.bkclient.ui.mainmenu;
import com.google.gson.*;
/** Menu-only preferences; never modifies HUD settings. */
public final class MenuPreferences {
    private MenuPreferences() {}
    public enum Option {
        DATE("Data","Date"), CLOCK("Zegar","Clock"), USERNAME("Nick","Username"),
        MUSIC("Karta muzyki","Music card"), MOTION("Ruch tła","Background motion"), DIM("Przyciemnienie","Dim background"),
        SERVER_CAROUSEL("Karuzela serwerów","Server carousel"), WORLD_CAROUSEL("Karuzela światów","World carousel",false);
        final String pl,en;final boolean defaultEnabled;public boolean enabled;
        Option(String pl,String en){this(pl,en,true);}
        Option(String pl,String en,boolean defaultEnabled){this.pl=pl;this.en=en;this.defaultEnabled=defaultEnabled;this.enabled=defaultEnabled;}
    }
    private static JsonObject retained=new JsonObject();
    public static JsonObject save(){var out=retained.deepCopy();for(var o:Option.values())out.addProperty(o.name(),o.enabled);LastPlayedServer.save(out);return out;}
    public static void load(JsonElement data){
        retained=data!=null&&data.isJsonObject()?data.getAsJsonObject().deepCopy():new JsonObject();
        LastPlayedServer.load(retained);
        for(var o:Option.values()){
            var value=retained.get(o.name());
            o.enabled=value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean()?value.getAsBoolean():o.defaultEnabled;
        }
    }
}
