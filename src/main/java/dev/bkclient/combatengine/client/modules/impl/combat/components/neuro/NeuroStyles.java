package dev.bkclient.combatengine.client.modules.impl.combat.components.neuro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public final class NeuroStyles {
   private static volatile String recoveredField1;
   private static volatile NeuroWeights recoveredField2;

   private NeuroStyles() {
   }

   public static Path baseDir() {
      return FabricLoader.getInstance().getGameDir().resolve("aura_neuro");
   }

   public static Path stylesDir() {
      return baseDir().resolve("styles");
   }

   public static Path trackDir(String name) {
      return stylesDir().resolve(sanitize(name));
   }

   public static Path modelFile(String name) {
      return stylesDir().resolve(sanitize(name) + ".wnn");
   }

   public static String sanitize(String name) {
      String var1 = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}0-9-_]", "_");
      return var1.isEmpty() ? "style" : var1;
   }

   public static NeuroWeights activate(String name) {
      NeuroWeights var1 = NeuroWeights.load(modelFile(name));
      if (var1 != null) {
         recoveredField1 = sanitize(name);
         recoveredField2 = var1;
      }

      return var1;
   }

   public static void deactivate() {
      recoveredField1 = null;
      recoveredField2 = null;
   }

   public static NeuroWeights activeWeights() {
      return recoveredField2;
   }

   public static String activeName() {
      return recoveredField1;
   }

   public static List<String> listTrained() {
      ArrayList<String> var0 = new ArrayList<>();
      Path var1 = stylesDir();
      if (!Files.isDirectory(var1)) {
         return var0;
      }

      try (Stream<Path> var2 = Files.list(var1)) {
         var2.filter(p -> p.getFileName().toString().endsWith(".wnn")).forEach(p -> {
            String var2x = p.getFileName().toString();
            var0.add(var2x.substring(0, var2x.length() - 4));
         });
      } catch (IOException var7) {
      }

      var0.sort(String::compareTo);
      return var0;
   }

   public static List<String> listRecorded() {
      ArrayList<String> var0 = new ArrayList<>();
      Path var1 = stylesDir();
      if (!Files.isDirectory(var1)) {
         return var0;
      }

      try (Stream<Path> var2 = Files.list(var1)) {
         var2.filter(x$0 -> Files.isDirectory(x$0)).forEach(p -> var0.add(p.getFileName().toString()));
      } catch (IOException var7) {
      }

      var0.sort(String::compareTo);
      return var0;
   }

   public static long recordedBytes(String name) {
      Path var1 = trackDir(name);
      if (!Files.isDirectory(var1)) {
         return 0L;
      }

      long var2 = 0L;

      try (Stream<Path> var4 = Files.list(var1)) {
         for (Path var6 : var4.filter(f -> f.getFileName().toString().endsWith(".jsonl")).toList()) {
            try {
               var2 += Files.size(var6);
            } catch (IOException var9) {
            }
         }
      } catch (IOException var11) {
      }

      return var2;
   }
}
