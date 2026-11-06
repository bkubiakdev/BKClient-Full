package dev.bkclient.systems.config;

import com.google.gson.*;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.file.*;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.modules.modules.bkmods.config.AutoMarketConfigManager;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.utility.game.*;
import net.minecraft.client.MinecraftClient;
import java.nio.file.*;
import java.util.function.Consumer;

/** Resolving and previewing an import never writes or applies configuration data. */
public final class ConfigImport {
    public enum Kind { CLIENT("Client"), MARKET("AutoMarket"); public final String label; Kind(String label){this.label=label;} }
    public record Draft(Kind kind, JsonObject data, String name, String server, String summary) {
        public Draft { data = data.deepCopy(); }
        @Override public JsonObject data() { return data.deepCopy(); }
    }
    public static AutoMarket market() { return BKClientInit.INSTANCE.getModuleManager().getModule(AutoMarket.class); }
    public static Path directory(Kind kind) { return FileManager.DIRECTORY.toPath().resolve(kind==Kind.CLIENT?"configs":XSK.DIR_NAME); }
    public static String clean(Kind kind,String name) {
        if(name==null || name.isBlank())throw new IllegalArgumentException("Enter a configuration name.");
        return kind==Kind.CLIENT?ConfigManager.sanitizeConfigName(name):AutoMarketConfigManager.sanitizeProfileName(name);
    }
    public static Path file(Kind kind,String name) { return directory(kind).resolve(clean(kind,name)+(kind==Kind.CLIENT?".bk":".json")); }
    public static boolean exists(Kind kind,String name) {
        String clean=clean(kind,name);
        try(var paths=Files.list(directory(kind))) { return paths.anyMatch(p->p.getFileName().toString().equalsIgnoreCase(clean+(kind==Kind.CLIENT?".bk":".json"))); }
        catch(java.io.IOException e){ return Files.exists(file(kind,name)); }
    }
    public static Draft parse(String content) {
        if(content==null || content.length()>8_000_000)throw new IllegalArgumentException("Configuration is too large.");
        JsonObject json=JsonParser.parseString(content).getAsJsonObject();
        Kind kind;
        if(json.has("modules")) { if(!ConfigFile.validStructure(json))throw new IllegalArgumentException("Invalid Client configuration."); kind=Kind.CLIENT; }
        else { if(!AutoMarketConfigManager.validProfile(json))throw new IllegalArgumentException("Invalid configuration data.");kind=Kind.MARKET; }
        String key=kind==Kind.CLIENT?"meta_name":XSK.JSON_NAME;
        String name=clean(kind,json.has(key)?json.get(key).getAsString():"imported_config");
        String base=name;
        for(int i=2;exists(kind,name);i++)name=base+"_"+i;
        String summary;
        if(kind==Kind.CLIENT) {
            int binds=0;for(var item:json.getAsJsonArray("modules")){var module=item.getAsJsonObject();if(module.has("key")&&module.get("key").getAsInt()>0)binds++;}
            int count=json.getAsJsonArray("modules").size();
            summary=count+(count==1?" module / ":" modules / ")+binds+(binds==1?" keybind":" keybinds");
        } else {
            JsonObject settings=json.getAsJsonObject(XSK.JSON_SETTINGS);
            JsonElement items=settings.get(market().itemRegistry.getName());
            if(items==null)items=settings.get("item_registry");
            summary=(items!=null && items.isJsonArray()?items.getAsJsonArray().size()+" items / ":"")+"Prices & enchantments";
        }
        return new Draft(kind,json,name,json.has("server")?json.get("server").getAsString():"BKClient",summary);
    }
    public static void resolve(String input, Consumer<Draft> success, Consumer<String> failure) {
        var mc=MinecraftClient.getInstance();
        var parent=mc.currentScreen;
        Consumer<String> parsed=content->{
            if(BKClientInit.INSTANCE.isPanic() || mc.currentScreen!=parent){failure.accept("Import cancelled.");return;}
            try{success.accept(parse(content));}catch(Exception e){failure.accept("Invalid configuration data.");}
        };
        String text=input==null?"":input.trim();
        if(text.startsWith("{")){parsed.accept(text);return;}
        try{
            var code=ConfigShareCode.parse(text);
            CloudConfigUtility.downloadConfig(code.key(),code.deletionKey()).whenComplete((content,error)->mc.execute(()->{
                if(error!=null||content==null||content.isBlank())failure.accept("Invalid or unavailable BK code.");else parsed.accept(content);
            }));
        }catch(Exception e){failure.accept("Invalid BK code.");}
    }
    public static String confirm(Draft draft,String requested,boolean load) throws java.io.IOException {
        if(BKClientInit.INSTANCE.isPanic())throw new IllegalStateException("Import unavailable during Panic.");
        String name=clean(draft.kind(),requested);
        if(name.equalsIgnoreCase("autosave") || name.equalsIgnoreCase("autocfg-overrides"))throw new IllegalArgumentException("Choose another name.");
        if(exists(draft.kind(),name))throw new IllegalArgumentException("That name already exists.");
        JsonObject data=draft.data();
        data.addProperty(draft.kind()==Kind.CLIENT?"meta_name":XSK.JSON_NAME,name);
        if(draft.kind()==Kind.MARKET)data.addProperty(XSK.JSON_DATE,System.currentTimeMillis());
        Path target=file(draft.kind(),name);
        Files.createDirectories(target.getParent());
        if(!(draft.kind()==Kind.CLIENT?ConfigFile.validStructure(data):AutoMarketConfigManager.validProfile(data)))throw new IllegalArgumentException("Invalid configuration data.");
        Path temporary=Files.createTempFile(target.getParent(),"import-",".tmp");
        try { Files.writeString(temporary,FileManager.GSON.toJson(data));Files.move(temporary,target); }
        finally { Files.deleteIfExists(temporary); }
        if(draft.kind()==Kind.CLIENT){
            var manager=BKClientInit.INSTANCE.getConfigManager();manager.refresh();
            if(load && !manager.getConfig(name).load())throw new IllegalStateException("Saved, but could not load configuration.");
        }else{
            market().configManager.refresh();
            if(load && !loadMarket(name))throw new IllegalStateException("Saved, but could not load configuration.");
        }
        return name;
    }
    public static boolean loadMarket(String name) {
        if(BKClientInit.INSTANCE.isPanic())return false;
        AutoMarket market=market();
        market.configManager.save(market.getActiveProfile());
        if(!market.configManager.loadProfile(name))return false;
        boolean previous=market.isLoadingProfile();market.setLoadingProfile(true);
        try{market.setActiveProfile(name);market.configName.setText(name);}finally{market.setLoadingProfile(previous);}
        AbstractSetting.saveConfig();return true;
    }
}
