package dev.bkclient.ui.clicksys.port.concurrent;
import java.util.*;
import java.util.function.Supplier;
import dev.bkclient.ui.clicksys.port.data.ConfigData;
import dev.bkclient.systems.modules.ModuleFavorites;
import com.google.gson.*;
import java.nio.file.*;
public final class ConfigManager {
 private JsonObject retained=new JsonObject();
 private ConfigData data=new ConfigData();private List<String> collapsed=new ArrayList<>();private List<Integer> colors=new ArrayList<>();
 private boolean defaultCollapsed = true;
 private Supplier<ConfigData> supplier; private Runnable restore;
 private final Path path=dev.bkclient.systems.file.FileManager.DIRECTORY.toPath().resolve("clicksys.json");
 private final ModuleRegistry modules;
 public ConfigManager(ModuleRegistry modules){this.modules=modules;try{if(Files.isRegularFile(path)){JsonObject j=JsonParser.parseString(Files.readString(path)).getAsJsonObject();retained=j.deepCopy();
 if(j.has("state"))data=new Gson().fromJson(j.get("state"),ConfigData.class);
 // Migrate older expanded layouts once; subsequent explicit choices remain saved.
 if(j.has("cardLayoutVersion") && j.get("cardLayoutVersion").getAsInt() >= 2 && j.has("collapsed")) {
 defaultCollapsed=false;
 for(var v:j.getAsJsonArray("collapsed"))collapsed.add(v.getAsString());
 }
 if(j.has("colors"))for(var v:j.getAsJsonArray("colors"))colors.add(v.getAsInt());
 if(j.has("theme"))dev.bkclient.ui.clicksys.port.data.ThemeConfig.setString(j.get("theme").getAsString());
 if(j.has("appearance")){var appearance=j.getAsJsonObject("appearance");var theme=modules.getModuleByClass(dev.bkclient.ui.clicksys.port.module.client.ThemeModule.class);theme.loadSettings(appearance);if(j.has("appearanceEnabled"))theme.restoreEnabled(j.get("appearanceEnabled").getAsBoolean());}
 }}catch(Exception e){dev.bkclient.BKClientInit.LOGGER.warn("Could not load clicksys state",e);}
 modules.getModuleByClass(dev.bkclient.ui.clicksys.port.module.client.ThemeModule.class).bindPersistence(this::save);
 }
 public ConfigData getConfigData(){return data;} public void setConfigData(ConfigData d){data=d;save();}
 public void setSupplier(Supplier<ConfigData> s){supplier=s;}public void setRunnable(Runnable r){restore=r;}
 public List<String> getList(){List<String> r=new ArrayList<>();for(var v:ModuleFavorites.save())r.add(v.getAsString());return r;}
 public void onList3(List<String> names){JsonArray a=new JsonArray();names.forEach(a::add);ModuleFavorites.load(a);dev.bkclient.BKClientInit.getInstance().getFileManager().writeFile("client");}
 public List<String> getList2(){if(defaultCollapsed){collapsed=new ArrayList<>(modules.getList32().stream().map(m->m.getName()).toList());defaultCollapsed=false;}return collapsed;}
 public void onList(List<String> list){defaultCollapsed=false;collapsed=new ArrayList<>(list);save();}
 public void collapseAllModules(){onList(modules.getList32().stream().map(m->m.getName()).toList());}
 public List<Integer> getList3(){return colors;}public void onList2(List<Integer> list){colors=new ArrayList<>(list);save();}
 public void save(){if(dev.bkclient.systems.setting.GuiSaveBatch.defer(this,this::save))return;try{JsonObject j=retained.deepCopy();j.add("state",new Gson().toJsonTree(data));j.add("collapsed",new Gson().toJsonTree(getList2()));j.add("colors",new Gson().toJsonTree(colors));j.addProperty("theme",dev.bkclient.ui.clicksys.port.data.ThemeConfig.getThemePalette().name());
 j.addProperty("cardLayoutVersion",2);
 var theme=modules.getModuleByClass(dev.bkclient.ui.clicksys.port.module.client.ThemeModule.class);JsonObject appearance=j.has("appearance") ? j.getAsJsonObject("appearance").deepCopy() : new JsonObject();for(var setting:theme.getSettings())appearance.add(setting.getNameHash(),setting.toJson());j.add("appearance",appearance);j.addProperty("appearanceEnabled",theme.isEnabled());
 dev.bkclient.systems.file.SafeJsonFile.write(path,j,o->o.has("state"));
 }catch(Exception e){dev.bkclient.BKClientInit.LOGGER.warn("Could not save clicksys state",e);}}
}
