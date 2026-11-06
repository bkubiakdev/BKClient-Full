package dev.bkclient.combatengine.client.modules.impl.combat.components.neuro;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.utils.chat.ChatUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public final class NeuroTrainerLauncher implements QClient {
   private static NeuroTrainerLauncher recoveredField1;
   private volatile Process recoveredField2;
   private volatile String recoveredField3;

   public static NeuroTrainerLauncher get() {
      if (recoveredField1 == null) {
         recoveredField1 = new NeuroTrainerLauncher();
      }

      return recoveredField1;
   }

   private NeuroTrainerLauncher() {
   }

   public boolean isRunning() {
      Process var1 = this.recoveredField2;
      return var1 != null && var1.isAlive();
   }

   public String trainingStyle() {
      return this.recoveredField3;
   }

   public synchronized void start(String rawName) {
      if (this.isRunning()) {
         ChatUtils.sendMessage("§cNeuro: training §f" + this.recoveredField3 + "§c is still running. Wait for it to finish or stop Python.");
      } else {
         String var2 = NeuroStyles.sanitize(rawName);
         Path var3 = NeuroStyles.trackDir(var2);
         if (!Files.isDirectory(var3)) {
            ChatUtils.sendMessage("§cNeuro: no recordings for style §f" + var2 + "§c. First use §f.neuro record " + var2);
         } else {
            Path var4 = recoveredMethod1();
            if (var4 == null) {
               ChatUtils.sendMessage("§cNeuro: python/train_style.py was not found next to the project.");
            } else {
               ProcessBuilder var5 = new ProcessBuilder("py", "-u", var4.toString(), "--name", var2, "--styles-dir", NeuroStyles.stylesDir().toString());
               var5.directory(var4.getParent().getParent().toFile());
               var5.redirectErrorStream(true);
               var5.environment().put("PYTHONIOENCODING", "utf-8");
               var5.environment().put("PYTHONUTF8", "1");

               try {
                  this.recoveredField2 = var5.start();
               } catch (IOException var7) {
                  ChatUtils.sendMessage("§cNeuro: Python could not start: " + var7.getMessage());
                  this.recoveredField2 = null;
                  return;
               }

               this.recoveredField3 = var2;
               ChatUtils.sendMessage("§aNeuro: training style §f" + var2 + "§a started through §ftrain_style.py§a.");
               Thread var6 = new Thread(() -> this.recoveredMethod2(this.recoveredField2), "neuro-train-pump");
               var6.setDaemon(true);
               var6.start();
            }
         }
      }
   }

   private static Path recoveredMethod1() {
      Path var0 = FabricLoader.getInstance().getGameDir();
      Path[] var1 = new Path[]{var0.resolve("../python/train_style.py").normalize(), var0.resolve("python/train_style.py")};

      for (Path var5 : var1) {
         if (Files.isRegularFile(var5)) {
            return var5;
         }
      }

      return null;
   }

   private void recoveredMethod2(Process p) {
      String var3;
      try (BufferedReader var2 = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
         while ((var3 = var2.readLine()) != null) {
            String var4 = var3.trim();
            if (!var4.isEmpty()
               && (
                  var4.startsWith("дорожек:")
                     || var4.startsWith("сверка по свежей дорожке:")
                     || var4.startsWith("политика:")
                     || var4.startsWith("подхвачено +")
                     || var4.startsWith("кривая преследования:")
                     || var4.startsWith("готово:")
                     || var4.startsWith("ОШИБКА:")
                     || var4.startsWith("эпоха ")
                     || var4.startsWith("Traceback")
                     || var4.startsWith("ModuleNotFoundError")
                     || var4.startsWith("ImportError")
                     || var4.contains("No module named")
               )) {
               recoveredMethod3("§7[train] §f" + var4);
            }
         }
      } catch (IOException var8) {
      }

      int var9;
      try {
         var9 = p.waitFor();
      } catch (InterruptedException var5) {
         Thread.currentThread().interrupt();
         return;
      }

      var3 = this.recoveredField3;
      if (var9 == 0) {
         if (var3 != null && var3.equals(NeuroStyles.activeName()) && NeuroStyles.activate(var3) != null) {
            recoveredMethod3("§aNeuro: style §f" + var3 + "§a reloaded from the new .wnn file.");
         }

         recoveredMethod3("§aNeuro: training §f" + var3 + "§a completed.");
      } else {
         recoveredMethod3("§cNeuro: trainer exited with code " + var9 + " - see the console.");
      }

      this.recoveredField2 = null;
   }

   private static void recoveredMethod3(String message) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1 != null) {
         var1.execute(() -> {
            try {
               ChatUtils.sendMessage(message);
            } catch (Throwable var2) {
            }
         });
      }
   }
}
