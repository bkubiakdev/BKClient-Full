package dev.bkclient.combatengine.client.modules.impl.combat.components.neuro;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.events.EventInvoker;
import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventAttackEntity;
import dev.bkclient.combatengine.api.events.implement.EventLook;
import dev.bkclient.combatengine.api.events.implement.EventUpdate;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.api.utils.chat.ChatUtils;
import dev.bkclient.combatengine.client.modules.impl.combat.Aura;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class StyleRecorder implements QClient {
   private static final int recoveredField1 = 4;
   private static final double recoveredField2 = 7.0;
   private static final long recoveredField3 = 5000L;
   private static final int recoveredField4 = 60;
   private static StyleRecorder recoveredField5;
   private volatile boolean recoveredField6;
   private String recoveredField7;
   private Path recoveredField8;
   private BufferedWriter recoveredField9;
   private long recoveredField10;
   private long recoveredField11;
   private int recoveredField12;
   private int recoveredField13;
   private int recoveredField14;
   private final StringBuilder recoveredField15 = new StringBuilder(512);
   private final List<double[]> recoveredField16 = new ArrayList<>();
   private long recoveredField17;
   private float recoveredField18;
   private float recoveredField19;
   private boolean recoveredField20;
   private int recoveredField21 = Integer.MIN_VALUE;
   private boolean recoveredField22;
   private boolean recoveredField23;
   private int recoveredField24;
   private LivingEntity recoveredField25;
   private int recoveredField26 = 40;

   public static StyleRecorder get() {
      if (recoveredField5 == null) {
         recoveredField5 = new StyleRecorder();
      }

      return recoveredField5;
   }

   private StyleRecorder() {
      EventInvoker.register(this);
   }

   public void noteModuleAttack() {
      this.recoveredField23 = true;
   }

   public boolean isRecording() {
      return this.recoveredField6;
   }

   public String styleName() {
      return this.recoveredField7;
   }

   public synchronized String start(String name) {
      if (this.recoveredField6) {
         return "Already recording style \"" + this.recoveredField7 + "\" - first use .neuro stop";
      }

      String var2 = NeuroStyles.sanitize(name);

      try {
         Path var3 = NeuroStyles.trackDir(var2);
         Files.createDirectories(var3);
         String var4 = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
         this.recoveredField8 = var3.resolve("track-" + var4 + ".jsonl");
         this.recoveredField9 = Files.newBufferedWriter(this.recoveredField8, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         double var5 = mc.options != null ? mc.options.getMouseSensitivity().getValue() : 0.5;
         this.recoveredField9
            .write(
               String.format(
                  Locale.ROOT,
                  "{\"type\":\"header\",\"version\":%d,\"name\":\"%s\",\"startedAt\":%d,\"tickRate\":20,\"sens\":%.4f,\"player\":\"%s\"}%n",
                  4,
                  var2,
                  System.currentTimeMillis(),
                  var5,
                  mc.player != null ? mc.player.getGameProfile().getName() : "?"
               )
            );
      } catch (IOException var7) {
         this.recoveredField9 = null;
         return "Could not open the recording file: " + var7.getMessage();
      }

      this.recoveredField7 = var2;
      this.recoveredField10 = System.currentTimeMillis();
      this.recoveredField11 = this.recoveredField10;
      this.recoveredField12 = 0;
      this.recoveredField13 = 0;
      this.recoveredField14 = 0;
      this.recoveredField20 = false;
      this.recoveredField16.clear();
      this.recoveredField25 = null;
      this.recoveredField26 = 40;
      this.recoveredField6 = true;
      return null;
   }

   public synchronized String stop() {
      if (!this.recoveredField6) {
         return "No recording is running";
      }

      this.recoveredField6 = false;

      try {
         this.recoveredField9.flush();
         this.recoveredField9.close();
      } catch (IOException var4) {
      }

      this.recoveredField9 = null;
      long var1 = Math.max(1L, (System.currentTimeMillis() - this.recoveredField10) / 1000L);
      return String.format(
         Locale.ROOT,
         "Style %s: %d ticks (%d:%02d), %d hits, file %s",
         this.recoveredField7,
         this.recoveredField12,
         var1 / 60L,
         var1 % 60L,
         this.recoveredField14,
         this.recoveredField8.getFileName()
      );
   }

   public static long totalRows(String name) {
      Path var1 = NeuroStyles.trackDir(name);
      if (!Files.isDirectory(var1)) {
         return 0L;
      }

      long var2 = 0L;

      try (Stream<Path> var4 = Files.list(var1)) {
         for (Path var6 : (Iterable<Path>)var4::iterator) {
            if (var6.getFileName().toString().endsWith(".jsonl")) {
               try (Stream<String> var7 = Files.lines(var6, StandardCharsets.UTF_8)) {
                  var2 += var7.filter(l -> l.startsWith("{\"t\"")).count();
               } catch (IOException var13) {
               }
            }
         }
      } catch (IOException var15) {
      }

      return var2;
   }

   @EventLink(priority = -100)
   public void onLook(EventLook event) {
      if (this.recoveredField6) {
         long var2 = System.nanoTime();
         long var4 = this.recoveredField17 == 0L ? 0L : (var2 - this.recoveredField17) / 1000L;
         this.recoveredField17 = var2;
         double var6 = event.getYaw() * 0.15;
         double var8 = event.getPitch() * 0.15;
         if (var6 != 0.0 || var8 != 0.0) {
            synchronized (this.recoveredField16) {
               if (this.recoveredField16.size() < 64) {
                  this.recoveredField16.add(new double[]{var4, var6, var8});
               }
            }
         }
      }
   }

   @EventLink(priority = -100)
   public void onAttack(EventAttackEntity event) {
      if (this.recoveredField6) {
         this.recoveredField22 = true;
      }
   }

   @EventLink(priority = -100)
   public void onTick(EventUpdate event) {
      if (this.recoveredField6 && mc.player != null && mc.world != null) {
         int var2 = mc.player.age;
         if (var2 != this.recoveredField21) {
            this.recoveredField21 = var2;
            float var3 = mc.player.getYaw();
            float var4 = mc.player.getPitch();
            float var5 = 0.0F;
            float var6 = 0.0F;
            if (this.recoveredField20) {
               var5 = MathHelper.wrapDegrees(var3 - this.recoveredField18);
               var6 = var4 - this.recoveredField19;
            }

            this.recoveredField18 = var3;
            this.recoveredField19 = var4;
            boolean var7 = !this.recoveredField20;
            this.recoveredField20 = true;
            boolean var8 = this.recoveredField22;
            boolean var9 = var8 && !this.recoveredField23;
            this.recoveredField22 = false;
            this.recoveredField23 = false;
            if (var8) {
               this.recoveredField14++;
            }

            LivingEntity var10 = this.recoveredMethod1();
            if (var10 == null) {
               if (++this.recoveredField24 == 60) {
                  this.recoveredMethod2();
               }

               synchronized (this.recoveredField16) {
                  this.recoveredField16.clear();
               }
            } else {
               this.recoveredField24 = 0;
               if (var10 != this.recoveredField25) {
                  this.recoveredField25 = var10;
                  this.recoveredField26 = 0;
               } else if (this.recoveredField26 < 40) {
                  this.recoveredField26++;
               }

               if (!var7) {
                  RotationStorage var11 = RotationStorage.instance;
                  boolean var12 = var11 != null && var11.currentPriority() >= 1 && var11.targetRotation() != null;
                  if (var12) {
                     this.recoveredField13++;
                  }

                  this.recoveredMethod3(var10, var5, var6, var8, var9, var12);
                  long var13 = System.currentTimeMillis();
                  if (var13 - this.recoveredField11 >= 5000L) {
                     this.recoveredField11 = var13;
                     long var15 = (var13 - this.recoveredField10) / 1000L;
                     ChatUtils.sendMessage(
                        String.format(
                           Locale.ROOT,
                           "§7Neuro: recording %s - %d ticks (%d:%02d), %d hits%s",
                           this.recoveredField7,
                           this.recoveredField12,
                           var15 / 60L,
                           var15 % 60L,
                           this.recoveredField14,
                           this.recoveredField13 > 0 ? " §8(rotation-contaminated samples: " + this.recoveredField13 + ")" : ""
                        )
                     );
                  }
               }
            }
         }
      }
   }

   private LivingEntity recoveredMethod1() {
      Aura var1 = Aura.INSTANCE;
      LivingEntity var2 = var1 != null && var1.isEnable() ? var1.getTarget() : null;
      if (var2 != null && var2.isAlive()) {
         return var2;
      }

      LivingEntity var3 = null;
      double var4 = 49.0;

      for (Entity var7 : mc.world.getEntities()) {
         if (var7 instanceof LivingEntity var8 && var8 != mc.player && (var8 instanceof PlayerEntity || var8.isAttackable()) && var8.isAlive()) {
            double var9 = var8.squaredDistanceTo(mc.player);
            if (var9 < var4) {
               var4 = var9;
               var3 = var8;
            }
         }
      }

      return var3;
   }

   private void recoveredMethod2() {
      try {
         this.recoveredField9.write("{\"type\":\"break\"}\n");
      } catch (IOException var2) {
      }
   }

   private void recoveredMethod3(LivingEntity target, float dy, float dp, boolean attacked, boolean manual, boolean foreign) {
      Box var7 = target.getBoundingBox();
      Vec3d var8 = var7.getCenter();
      Vec3d var9 = mc.player.getEyePos();
      double var10 = var8.x - var9.x;
      double var12 = var8.y - var9.y;
      double var14 = var8.z - var9.z;
      double var16 = Math.sqrt(var10 * var10 + var14 * var14);
      double var18 = Math.toDegrees(Math.atan2(var14, var10)) - 90.0;
      double var20 = -Math.toDegrees(Math.atan2(var12, var16));
      double var22 = Math.sqrt(var16 * var16 + var12 * var12);
      ClientPlayerEntity var24 = mc.player;
      double var25 = var24.getVelocity().horizontalLength();
      double var27 = var24.getVelocity().y;
      float var29 = var24.getAttackCooldownProgress(0.0F);
      int var30 = target.hurtTime;
      this.recoveredField15.setLength(0);
      this.recoveredField15.append("{\"t\":").append(var24.age);
      recoveredMethod4(this.recoveredField15, "dy", dy);
      recoveredMethod4(this.recoveredField15, "dp", dp);
      recoveredMethod4(this.recoveredField15, "ay", var18);
      recoveredMethod4(this.recoveredField15, "ap", var20);
      recoveredMethod4(this.recoveredField15, "py", MathHelper.wrapDegrees(var24.getYaw()));
      recoveredMethod4(this.recoveredField15, "pp", var24.getPitch());
      recoveredMethod4(this.recoveredField15, "d", var22);
      recoveredMethod4(this.recoveredField15, "ts", target.getVelocity().horizontalLength());
      this.recoveredField15.append(",\"sw\":").append(this.recoveredField26);
      recoveredMethod4(this.recoveredField15, "os", var25);
      recoveredMethod4(this.recoveredField15, "ov", var27);
      recoveredMethod4(this.recoveredField15, "cd", var29);
      this.recoveredField15.append(",\"ht\":").append(var30);
      this.recoveredField15.append(",\"g\":").append(var24.isOnGround() ? 1 : 0);
      this.recoveredField15.append(",\"w\":").append(var24.isTouchingWater() ? 1 : 0);
      this.recoveredField15.append(",\"sp\":").append(var24.isSprinting() ? 1 : 0);
      this.recoveredField15.append(",\"sn\":").append(var24.isSneaking() ? 1 : 0);
      this.recoveredField15.append(",\"atk\":").append(attacked ? 1 : 0);
      this.recoveredField15.append(",\"man\":").append(manual ? 1 : 0);
      this.recoveredField15.append(",\"rot\":").append(foreign ? 1 : 0);
      this.recoveredField15.append(",\"m\":[");
      synchronized (this.recoveredField16) {
         for (int var32 = 0; var32 < this.recoveredField16.size(); var32++) {
            double[] var33 = this.recoveredField16.get(var32);
            if (var32 > 0) {
               this.recoveredField15.append(',');
            }

            this.recoveredField15.append('[').append((long)var33[0]).append(',');
            recoveredMethod5(this.recoveredField15, var33[1]);
            this.recoveredField15.append(',');
            recoveredMethod5(this.recoveredField15, var33[2]);
            this.recoveredField15.append(']');
         }

         this.recoveredField16.clear();
      }

      this.recoveredField15.append("]}\n");

      try {
         this.recoveredField9.write(this.recoveredField15.toString());
         this.recoveredField12++;
      } catch (IOException var35) {
         this.recoveredField6 = false;
         ChatUtils.sendMessage("§cNeuro: recording interrupted - " + var35.getMessage());
      }
   }

   private static void recoveredMethod4(StringBuilder sb, String key, double value) {
      sb.append(",\"").append(key).append("\":");
      recoveredMethod5(sb, value);
   }

   private static void recoveredMethod5(StringBuilder sb, double value) {
      if (!Double.isFinite(value)) {
         value = 0.0;
      }

      sb.append(String.format(Locale.ROOT, "%.4f", value));
   }
}
