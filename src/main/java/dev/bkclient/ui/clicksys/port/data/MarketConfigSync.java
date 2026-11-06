package dev.bkclient.ui.clicksys.port.data;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.config.*;
import dev.bkclient.utility.game.*;
import dev.bkclient.utility.game.server.ServerUtility;
import com.google.gson.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;

/** Same config cards, backed by the existing market profile directory. */
public final class MarketConfigSync extends ConfigSync {
    private static final MarketConfigSync INSTANCE=new MarketConfigSync();
    public static MarketConfigSync instance(){return INSTANCE;}
    private final Set<String> sharing=new HashSet<>();
    private void report(boolean ok,String message){BKClientInit.INSTANCE.getNotificationManager().addNotificationOther(ok?dev.bkclient.systems.notifications.NotificationType.INFO:dev.bkclient.systems.notifications.NotificationType.ERROR,"AutoMarket configs",message);}
    @Override public List<ConfigEntry> getList(){
        var market=ConfigImport.market();market.configManager.refresh();var list=new ArrayList<ConfigEntry>();
        for(var profile:market.configManager.getConfigs()){
            var entry=new ConfigEntry();entry.setText(profile.filename);entry.setText2(profile.server.equals(XSK.DEFAULT_SERVER)?"Local profile":profile.server);
            entry.setText4(java.time.Instant.ofEpochMilli(profile.date).toString());
            entry.setFlag(profile.filename.equalsIgnoreCase(market.getActiveProfile()));
            entry.setFlag3(entry.isFlag());list.add(entry);
        }
        list.sort(Comparator.comparing(ConfigEntry::getText,String.CASE_INSENSITIVE_ORDER));return list;
    }
    @Override public void onRunnableString2(Runnable done,String name){boolean ok=ConfigImport.loadMarket(name);report(ok,ok?"Profile loaded.":"Could not load profile.");done.run();}
    @Override public void save(String name){if(BKClientInit.INSTANCE.isPanic())return;ConfigImport.market().configManager.save(name);}
    @Override public void onRunnableString(Runnable done,String name){
        if(!name.equalsIgnoreCase(ConfigImport.market().getActiveProfile()))ConfigImport.market().configManager.delete(name);
        done.run();
    }
    @Override public void onStringStringRunnable2(String oldName,String newName,Runnable done){
        if(BKClientInit.INSTANCE.isPanic())return;
        report(ConfigImport.market().configManager.rename(oldName,newName),"Profile rename finished.");done.run();
    }
    @Override public void onStringStringRunnable(String description,String name,Runnable done){done.run();}
    @Override public void onStringStringConsumer2(String description,String name,Consumer<Object> done){
        try{
            JsonObject json=new JsonObject(),settings=new JsonObject();
            for(var setting:ConfigImport.market().getMarketSettings())settings.add(setting.getName(),setting.save());
            json.add(XSK.JSON_SETTINGS,settings);json.addProperty(XSK.JSON_SERVER,ServerUtility.getCurrentServerAddress("Singleplayer"));
            done.accept(ConfigImport.confirm(new ConfigImport.Draft(ConfigImport.Kind.MARKET,json,name,"",""),name,false));
        }catch(Exception e){report(false,e.getMessage());done.accept(null);}
    }
    @Override public boolean isSharing(String name){return sharing.contains(name);}
    @Override public void shareCode(String name,boolean privateMode,Consumer<Boolean> done){
        if(!sharing.add(name))return;
        try{
            String content=Files.readString(ConfigImport.file(ConfigImport.Kind.MARKET,name));
            CloudConfigUtility.uploadConfig(name,content).whenComplete((result,error)->MinecraftClient.getInstance().execute(()->{
                sharing.remove(name);
                if(BKClientInit.INSTANCE.isPanic()){done.accept(false);return;}
                if(error!=null||result==null||privateMode&&(result.deletionKey==null||result.deletionKey.isBlank())){report(false,"Could not share profile.");done.accept(false);return;}
                MinecraftClient.getInstance().keyboard.setClipboard(new ConfigShareCode(result.key,privateMode?result.deletionKey:null).formatted());done.accept(true);
            }));
        }catch(Exception e){sharing.remove(name);report(false,"Could not share profile.");done.accept(false);}
    }
}
