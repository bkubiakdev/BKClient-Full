package dev.bkclient.ui.clicksys.port.data;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import dev.bkclient.systems.file.SafeJsonFile;

/** GUI metadata only: nicknames and module friend checks remain owned by BK FriendManager. */
public final class FriendLabels {
   private final Path path;
   private final Map<String,String> aliases=new HashMap<>(), groups=new HashMap<>(), dates=new HashMap<>();
   public FriendLabels(Path path) throws java.io.IOException {
      this.path=path;
      if(Files.isRegularFile(path)) {
         JsonObject object=SafeJsonFile.read(path);
         read(object,"aliases",aliases); read(object,"groups",groups); read(object,"dates",dates);
      }
   }
   private static String key(String name) {return name.trim().toLowerCase(Locale.ROOT);}
   private static void read(JsonObject object,String field,Map<String,String> output) {
      if(object.has(field)) object.getAsJsonObject(field).entrySet().forEach(e->output.put(key(e.getKey()),e.getValue().getAsString()));
   }
   public String alias(String name) {return aliases.get(key(name));}
   public String group(String name) {return groups.getOrDefault(key(name),"Friend");}
   public String date(String name) {return dates.getOrDefault(key(name),"");}
   public void added(String name,String date) {dates.putIfAbsent(key(name),date);}
   public void remove(String name) {aliases.remove(key(name));groups.remove(key(name));dates.remove(key(name));}
   private static void edit(Map<String,String> values,String name,String value) {
      if(value==null||value.isBlank()) values.remove(key(name)); else values.put(key(name),value.trim());
   }
   public void alias(String name,String value) {edit(aliases,name,value);}
   public void group(String name,String value) {edit(groups,name,value);}
   public void save() throws java.io.IOException {
      JsonObject object=new JsonObject();Gson gson=new Gson();
      object.add("aliases",gson.toJsonTree(aliases));object.add("groups",gson.toJsonTree(groups));object.add("dates",gson.toJsonTree(dates));
      SafeJsonFile.write(path,object,o->o.has("aliases"));
   }
}
