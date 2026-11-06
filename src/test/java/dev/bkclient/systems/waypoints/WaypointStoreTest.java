package dev.bkclient.systems.waypoints;

import java.nio.file.*;
import java.util.*;

public final class WaypointStoreTest {
   public static void main(String[] args) throws Exception {
      Path directory = Files.createTempDirectory("bk-waypoint-test-");
      try {
         Path file = directory.resolve("waypoints.json");
         var store = new WaypointStore(file);
         var a = point("server:a", "minecraft:overworld", "Baza żółwia", 12.5);
         var b = point("server:b", "minecraft:overworld", "Baza żółwia", -200);
         var c = point("server:a", "minecraft:the_nether", "Portal", 30);
         store.put(a); store.put(b); store.put(c);
         var reopened = new WaypointStore(file);
         check(reopened.list(a.scope(), a.dimension()).equals(List.of(a)), "server isolation and UTF-8 persistence");
         check(reopened.list(c.scope(), c.dimension()).equals(List.of(c)), "dimension isolation");
         var edited = new WaypointStore.Point(a.id(), a.scope(), a.dimension(), "Nowa baza", 50, 80, 90, 0xffee2299, false);
         reopened.put(edited);
         check(new WaypointStore(file).list(a.scope(), a.dimension()).equals(List.of(edited)), "edit color, coordinates and visibility persists without duplication");
         reopened.remove(a.id());
         check(new WaypointStore(file).list(a.scope(), a.dimension()).isEmpty(), "delete persists");
         check(reopened.list(b.scope(), b.dimension()).equals(List.of(b)), "delete preserves other server");
         var deleteStore = new WaypointStore(directory.resolve("delete-by-name.json"));
         deleteStore.put(a); deleteStore.put(b);
         deleteStore.put(point(a.scope(), "minecraft:the_nether", a.name(), 10));
         deleteStore.put(point(a.scope(), a.dimension(), a.name(), 40));
         check(deleteStore.removeByName(a.scope(), a.dimension(), "BAZA ŻÓŁWIA") == 2, "case-insensitive exact name including spaces and duplicates");
         check(new WaypointStore(directory.resolve("delete-by-name.json")).list(a.scope(), a.dimension()).isEmpty(), "named deletion persists");
         check(deleteStore.list(b.scope(), b.dimension()).size() == 1, "named deletion preserves other server");
         check(deleteStore.list(a.scope(), "minecraft:the_nether").size() == 1, "named deletion preserves other dimension");
         check(deleteStore.removeByName(b.scope(), b.dimension(), "Baza") == 0, "no substring deletion");
         check(deleteStore.clear(b.scope(), b.dimension()) == 1, "clear returns removed count");
         check(new WaypointStore(directory.resolve("delete-by-name.json")).list(b.scope(), b.dimension()).isEmpty(), "clear persists");
         check(deleteStore.list(a.scope(), "minecraft:the_nether").size() == 1, "clear preserves other scope");
         check(deleteStore.clear(b.scope(), b.dimension()) == 0, "clear empty scope");
         Files.writeString(file, "{broken");
         check(new WaypointStore(file).list(a.scope(), a.dimension()).equals(List.of(edited)), "last valid backup recovery");
         expectInvalid(() -> point("s", "d", "bad", Double.NaN));
         expectInvalid(() -> point("s", "d", "bad", Double.POSITIVE_INFINITY));
         expectInvalid(() -> point("s", "d", "bad", 30_000_001));
         expectInvalid(() -> point("s", "d", " ", 0));
         expectInvalid(() -> point("s", "d", "line\nbreak", 0));
         Path badFile = directory.resolve("unrecoverable.json");
         Files.writeString(badFile, "{broken");
         try { new WaypointStore(badFile).put(a); throw new AssertionError("must not overwrite invalid storage"); }
         catch (java.io.IOException expected) { check(Files.readString(badFile).equals("{broken"), "invalid storage preserved"); }
         System.out.println("WaypointStore tests passed");
      } finally {
         try (var paths = Files.walk(directory)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
      }
   }
   private static WaypointStore.Point point(String scope, String dimension, String name, double x) {
      return new WaypointStore.Point(UUID.randomUUID().toString(), scope, dimension, name, x, 64, 20, 0xff8855cc, true);
   }
   private static void expectInvalid(Runnable action) {
      try { action.run(); throw new AssertionError("Expected validation failure"); } catch (IllegalArgumentException expected) { }
   }
   private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
