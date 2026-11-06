package dev.bkclient.systems.waypoints;

import com.google.gson.*;
import dev.bkclient.systems.file.SafeJsonFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Independent from module configs: loading a config never replaces personal locations. */
public final class WaypointStore {
   public record Point(String id, String scope, String dimension, String name,
                       double x, double y, double z, int color, boolean visible) {
      public Point {
         UUID.fromString(id);
         if (scope == null || scope.isBlank() || dimension == null || dimension.isBlank())
            throw new IllegalArgumentException("Missing world");
         name = name == null ? "" : name.strip();
         if (name.isEmpty() || name.length() > 48 || name.chars().anyMatch(c -> Character.isISOControl(c) || c == 167))
            throw new IllegalArgumentException("Name: 1–48 characters");
         if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
            || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || Math.abs(y) > 4096)
            throw new IllegalArgumentException("Invalid coordinates");
         color |= 0xff000000;
      }
      public Point visibility(boolean value) { return new Point(id, scope, dimension, name, x, y, z, color, value); }
   }

   private final Path file;
   private List<Point> points = List.of();
   private boolean loaded;
   public WaypointStore(Path file) { this.file = file; }
   public void load() throws IOException {
      if (loaded) return;
      if (Files.exists(file) || Files.exists(SafeJsonFile.backup(file)))
         points = decode(SafeJsonFile.read(file, WaypointStore::valid));
      loaded = true;
   }
   public List<Point> list(String scope, String dimension) throws IOException {
      load();
      return points.stream().filter(p -> p.scope.equals(scope) && p.dimension.equals(dimension)).toList();
   }
   public void put(Point point) throws IOException {
      load();
      var next = new ArrayList<>(points);
      next.removeIf(p -> p.id.equals(point.id));
      if (next.size() >= 2048 || next.stream().filter(p -> p.scope.equals(point.scope) && p.dimension.equals(point.dimension)).count() >= 128)
         throw new IllegalArgumentException("Limit: 128 points per dimension / 2048 total");
      next.add(point);
      commit(next);
   }
   public void remove(String id) throws IOException {
      load();
      var next = new ArrayList<>(points);
      next.removeIf(p -> p.id.equals(id));
      commit(next);
   }
   public int removeByName(String scope, String dimension, String name) throws IOException {
      load();
      var next = new ArrayList<>(points);
      next.removeIf(p -> p.scope.equals(scope) && p.dimension.equals(dimension) && p.name.equalsIgnoreCase(name.strip()));
      int removed = points.size() - next.size();
      if (removed > 0) commit(next);
      return removed;
   }
   public int clear(String scope, String dimension) throws IOException {
      load();
      var next = new ArrayList<>(points);
      next.removeIf(p -> p.scope.equals(scope) && p.dimension.equals(dimension));
      int removed = points.size() - next.size();
      if (removed > 0) commit(next);
      return removed;
   }
   private void commit(List<Point> next) throws IOException {
      var root = new JsonObject(); root.addProperty("version", 1);
      var array = new JsonArray();
      for (Point p : next) {
         var j = new JsonObject();
         j.addProperty("id", p.id); j.addProperty("scope", p.scope); j.addProperty("dimension", p.dimension);
         j.addProperty("name", p.name); j.addProperty("x", p.x); j.addProperty("y", p.y); j.addProperty("z", p.z);
         j.addProperty("color", p.color); j.addProperty("visible", p.visible); array.add(j);
      }
      root.add("points", array);
      SafeJsonFile.write(file, root, WaypointStore::valid);
      points = List.copyOf(next);
   }
   private static boolean valid(JsonObject root) {
      try { decode(root); return true; } catch (RuntimeException invalid) { return false; }
   }
   private static List<Point> decode(JsonObject root) {
      if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Version");
      var result = new ArrayList<Point>(); var ids = new HashSet<String>();
      for (var value : root.getAsJsonArray("points")) {
         var j = value.getAsJsonObject();
         var point = new Point(j.get("id").getAsString(), j.get("scope").getAsString(), j.get("dimension").getAsString(),
            j.get("name").getAsString(), j.get("x").getAsDouble(), j.get("y").getAsDouble(), j.get("z").getAsDouble(),
            j.get("color").getAsInt(), j.get("visible").getAsBoolean());
         if (!ids.add(point.id) || result.size() >= 2048) throw new IllegalArgumentException("Invalid point list");
         result.add(point);
      }
      return List.copyOf(result);
   }
}
