package dev.bkclient.systems.waypoints;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class WayPointsManager {
   private final Map<String, Vec3d> waypoints = new HashMap<>();
   private final Map<String, com.google.gson.JsonObject> storedWaypoints = new HashMap<>();

   public void load(com.google.gson.JsonArray array) {
      this.waypoints.clear();
      this.storedWaypoints.clear();
      for (var element : array) {
         try {
            var object = element.getAsJsonObject();
            String name = object.get("name").getAsString();
            this.waypoints.put(name, new Vec3d(object.get("x").getAsDouble(), object.get("y").getAsDouble(), object.get("z").getAsDouble()));
            this.storedWaypoints.put(name, object.deepCopy());
         } catch (RuntimeException error) {
            dev.bkclient.BKClientInit.LOGGER.warn("Skipping invalid saved waypoint", error);
         }
      }
   }

   public com.google.gson.JsonArray save() {
      var array = new com.google.gson.JsonArray();
      for (var entry : this.waypoints.entrySet()) {
         var object = this.storedWaypoints.containsKey(entry.getKey())
            ? this.storedWaypoints.get(entry.getKey()).deepCopy() : new com.google.gson.JsonObject();
         object.addProperty("name", entry.getKey());
         object.addProperty("x", entry.getValue().x);
         object.addProperty("y", entry.getValue().y);
         object.addProperty("z", entry.getValue().z);
         array.add(object);
      }
      return array;
   }

   public void add(String name, int x, int y, int z) {
      Vec3d pos = new Vec3d(x, y, z);
      if (this.waypoints.containsKey(name)) {
         MessageUtility.error(Text.of(Localizator.translate("modules.waypoints.exists", name)));
      } else {
         this.waypoints.put(name, pos);
         MessageUtility.info(Text.of(Localizator.translate("modules.waypoints.added", name, x, y, z)));
      }
   }

   public void del(String name) {
      this.storedWaypoints.remove(name);
      if (this.waypoints.remove(name) != null) {
         MessageUtility.info(Text.of(Localizator.translate("modules.waypoints.deleted", name)));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("modules.waypoints.not_found", name)));
      }
   }

   public void clear() {
      this.storedWaypoints.clear();
      this.waypoints.clear();
      MessageUtility.info(Text.of(Localizator.translate("modules.waypoints.cleared")));
   }

   public boolean contains(String name) {
      return this.waypoints.containsKey(name);
   }

   public Set<Entry<String, Vec3d>> getEntries() {
      return this.waypoints.entrySet();
   }
}
