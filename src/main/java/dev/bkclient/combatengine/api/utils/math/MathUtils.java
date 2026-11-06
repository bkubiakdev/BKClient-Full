package dev.bkclient.combatengine.api.utils.math;

import dev.bkclient.combatengine.api.QClient;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MathUtils implements QClient {
   public static FastRandom fastRandomize = new FastRandom();

   public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
      if (moveForward < 0.0) {
         rotationYaw += 180.0F;
      }

      float var5 = 1.0F;
      if (moveForward < 0.0) {
         var5 = -0.5F;
      } else if (moveForward > 0.0) {
         var5 = 0.5F;
      }

      if (moveStrafing > 0.0) {
         rotationYaw -= 90.0F * var5;
      }

      if (moveStrafing < 0.0) {
         rotationYaw += 90.0F * var5;
      }

      return Math.toRadians(rotationYaw);
   }

   public static float randomNew(double min, double max) {
      return min > max ? (float)(fastRandomize.nextFloat() * (min - max) + max) : (float)(fastRandomize.nextFloat() * (max - min) + min);
   }

   public static double getBps(Entity player) {
      double var1 = player.getX() - player.prevX;
      double var3 = player.getY() - player.prevY;
      double var5 = player.getZ() - player.prevZ;
      double var7 = Math.sqrt(var1 * var1 + var3 * var3 + var5 * var5);
      return var7 * 20.0;
   }

   public static float calculateBPS() {
      if (mc.player == null) {
         return 0.0F;
      }

      double var0 = mc.player.getX() - mc.player.prevX;
      double var2 = mc.player.getY() - mc.player.prevY;
      double var4 = mc.player.getZ() - mc.player.prevZ;
      double var6 = Math.sqrt(var0 * var0 + var2 * var2 + var4 * var4);
      float var8 = 1.0F;
      float var9 = (float)(var6 * var8 * 20.0);
      return Math.round(var9 * 10.0F) / 10.0F;
   }

   public static double getTargetCompensatedSpeed(Entity target) {
      double var1 = 1.5;
      if (target == null) {
         return 1.5;
      }

      double var3 = calculateBPS();
      double var5 = 0.00342;
      double var7 = var3 * 0.00342;
      return 1.5 + var7;
   }

   public static float random(float min, float max) {
      ThreadLocalRandom var2 = ThreadLocalRandom.current();
      double var3 = var2.nextDouble();
      double var5 = var2.nextDouble();
      double var7 = var2.nextGaussian() * 0.02F;
      double var9 = Math.pow(var3, 1.0 + var2.nextDouble() * 0.7);
      double var11 = (var5 * 0.8 + 0.1) * (Math.log1p(var3 * 3.0) * 0.5 + 0.5);
      return (float)(min + (max - min) * var9 * var11 + var7);
   }

   public static double randomBest(double min, double max) {
      return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
   }

   public static boolean isHovered(double x, double y, double width, double height, double mouseX, double mouseY) {
      return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
   }

   public static float interpolate(float prev, float to, float value) {
      return prev + (to - prev) * value;
   }

   public static Vec3d interpolate(Vec3d end, Vec3d start, float multiple) {
      return new Vec3d(
         interpolate(end.getX(), start.getX(), multiple), interpolate(end.getY(), start.getY(), multiple), interpolate(end.getZ(), start.getZ(), multiple)
      );
   }

   public static Vec3d interpolate(Entity entity, float partialTicks) {
      double var2 = MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
      double var4 = MathHelper.lerp(partialTicks, entity.prevY, entity.getY());
      double var6 = MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());
      return new Vec3d(var2, var4, var6);
   }

   public static double interpolate(double current, double old, double scale) {
      return old + (current - old) * scale;
   }

   public static float round(float number) {
      return Math.round(number * 10.0F) / 10.0F;
   }

   public static double round(double num, double increment) {
      if (increment <= 0.0) {
         return num;
      }

      double var4 = Math.round(num / increment) * increment;
      return Math.round(var4 * 100.0) / 100.0;
   }

   public static float lerp(float current, float old, float scale) {
      return current + (old - current) * clamp(scale, 0.0F, 1.0F);
   }

   public static float clamp(float value, float min, float max) {
      return value <= min ? min : Math.min(value, max);
   }

   public static double clamp(double min, double max, double n) {
      return Math.max(min, Math.min(max, n));
   }

   public static double ler1p(double input, double target, double step) {
      return input + step * (target - input);
   }

   public static float ler1p(float input, float target, double step) {
      return (float)(input + step * (target - input));
   }

   public static int ler1p(int input, int target, double step) {
      return (int)Math.round(input + step * (target - input));
   }
}
