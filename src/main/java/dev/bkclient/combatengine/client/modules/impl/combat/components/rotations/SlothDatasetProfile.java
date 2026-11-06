package dev.bkclient.combatengine.client.modules.impl.combat.components.rotations;

import java.util.Random;
import net.minecraft.util.math.MathHelper;

public final class SlothDatasetProfile {
   private static final int recoveredField1 = 2645;
   private static final int recoveredField2 = 3;
   private static final float recoveredField3 = 22.0488F;
   private static final float recoveredField4 = 37.3496F;
   private static final float recoveredField5 = 3.3F;
   private static final float recoveredField6 = 6.45F;
   private static final float recoveredField7 = 2.9684F;
   private static final float recoveredField8 = 0.6671F;
   private static final float recoveredField9 = 5.4489F;
   private static final float recoveredField10 = 1.3854F;
   private static final float recoveredField11 = 0.7387F;
   private static final float recoveredField12 = 0.6763F;
   private static final float recoveredField13 = 0.4618F;
   private static final float recoveredField14 = 0.0F;
   private static final float recoveredField15 = 0.4101F;
   private static final float recoveredField16 = 0.7136F;
   private static final float recoveredField17 = 7.61F;
   private static final float recoveredField18 = 3.4415F;
   private static final float recoveredField19 = 3.4295F;
   private static final long[] recoveredField20 = new long[]{188L, 204L, 221L, 238L, 254L, 272L, 291L, 314L};
   private static final int[] recoveredField21 = new int[]{9, 17, 25, 31, 29, 22, 13, 6};
   private static final long[] recoveredField22 = new long[]{158L, 174L, 190L, 207L, 225L, 244L, 265L, 288L};
   private static final int[] recoveredField23 = new int[]{6, 13, 24, 31, 29, 22, 12, 5};
   private static final long[] recoveredField24 = new long[]{176L, 194L, 213L, 233L, 254L, 276L, 299L, 324L};
   private static final int[] recoveredField25 = new int[]{7, 16, 26, 31, 28, 19, 10, 4};
   private static final double[] recoveredField26 = new double[]{-0.052, 0.034, -0.026, 0.061, -0.038, 0.022, -0.071, 0.045};
   private static final int[] recoveredField27 = new int[]{
      14500, 15600, 16500, 17400, 18200, 19000, 19750, 20350, 21000, 21800, 22700, 23800, 25100, 26700, 28600, 31200
   };
   private static final float recoveredField28 = 1.2F;

   private SlothDatasetProfile() {
   }

   public static int choose(Random random, boolean action) {
      return recoveredMethod1(random, action ? recoveredField23 : recoveredField21);
   }

   static int chooseJump(Random random) {
      return recoveredMethod1(random, recoveredField25);
   }

   public static long duration(boolean action, int index) {
      long var2 = action ? recoveredField22[index] : recoveredField20[index];
      return Math.round((float)var2 * recoveredMethod3(recoveredMethod4(index, action ? 0.41 : 0.23), 0.955F, 1.045F));
   }

   static long jumpDuration(int index) {
      return Math.round((float)recoveredField24[index] * recoveredMethod3(recoveredMethod4(index, 0.67), 0.96F, 1.04F));
   }

   public static double progress(boolean action, int index, double progress) {
      double var4 = recoveredMethod6(progress);
      double var6 = 0.14564998F;
      double var8 = action ? 1.64 + var6 * 0.45 + index * 0.045 : 1.78 + var6 * 0.45 + index * 0.04;
      double var10 = action ? 1.82 + var6 * 0.45 - index * 0.035 : 1.96 + var6 * 0.45 - index * 0.04;
      double var12 = recoveredMethod5(var4, var8, var10);
      if (index == 6 && var4 > 0.66) {
         var12 += Math.sin((var4 - 0.66) / 0.34 * Math.PI) * 0.0 * 0.58;
      }

      return var12;
   }

   public static double lateral(boolean action, int index, double progress) {
      double var4 = recoveredMethod6(progress);
      double var6 = Math.sin(Math.PI * var4);
      double var8 = 0.31402400612831116 + (action ? 0.12 : 0.0);
      return recoveredField26[index] * var6 * (0.72 + var8 * (1.0 - var4));
   }

   static double jumpProgress(int index, double progress) {
      double var3 = recoveredMethod6(progress);
      double var5 = 1.68 + index * 0.055;
      double var7 = 1.82 - index * 0.025;
      return recoveredMethod5(var3, var5, var7);
   }

   static double jumpLateral(int index, double progress) {
      double var3 = recoveredMethod6(progress);
      double var5 = Math.sin(Math.PI * var3);
      return recoveredField26[(index + 3) % recoveredField26.length] * var5 * (0.9 + 0.16 * Math.sin((Math.PI * 2) * var3));
   }

   static int mouseReportIntervalMicros(Random random) {
      return recoveredField27[random.nextInt(recoveredField27.length)];
   }

   static float initialSpeedScale(Random random) {
      return recoveredMethod2(random, 0.91F, 1.09F);
   }

   static float driftingSpeedScale(Random random) {
      return recoveredMethod2(random, 0.84F, 1.18F);
   }

   public static float maximumYawSpeed(Random random) {
      return random.nextFloat() < 0.15F ? recoveredMethod2(random, 746.99207F, 700.0F) : recoveredMethod2(random, 440.97598F, 746.99207F);
   }

   public static float maximumPitchSpeed(Random random) {
      return recoveredMethod2(random, 66.0F, 129.0F);
   }

   public static float yawAcceleration(Random random) {
      return recoveredMethod2(random, 1691.988F, 2404.404F) * 1.2F;
   }

   public static float pitchAcceleration(Random random) {
      return recoveredMethod2(random, 400.26F, 600.39F) * 1.2F;
   }

   public static float yawDeceleration(Random random) {
      return recoveredMethod2(random, 2196.616F, 2998.084F) * 1.2F;
   }

   public static float pitchDeceleration(Random random) {
      return recoveredMethod2(random, 510.3315F, 763.8295F) * 1.2F;
   }

   public static float yawJerkLimit(Random random) {
      return recoveredMethod2(random, 36507.633F, 52854.332F);
   }

   public static float pitchJerkLimit(Random random) {
      float var1 = MathHelper.clamp(0.18204994F, 0.5F, 1.0F);
      return recoveredMethod2(random, 7065.5405F, 1.3854F * (6900.0F + 1100.0F * var1));
   }

   static float perceptionScale(Random random) {
      return recoveredMethod2(random, 0.82F, 1.08F);
   }

   static float perceptionBias(Random random) {
      return recoveredMethod2(random, -0.08F, 0.035F);
   }

   static long reactionMillis(Random random, float angle, boolean precision) {
      if (!precision && !(angle < 6.0F)) {
         float var3 = 0.7075F;
         float var4 = 8.0F + angle * (0.2F - var3 * 0.04F);
         float var5 = (float)random.nextGaussian() * 2.5F;
         return Math.round(MathHelper.clamp(var4 + var5, 6.0F, 42.0F));
      } else {
         return Math.round(recoveredMethod2(random, 0.0F, 5.0F));
      }
   }

   public static float pathNoiseScale(Random random) {
      float var1 = 0.017F * (1.0F - profileConfidence() * 0.35F);
      return recoveredMethod2(random, 0.051F - var1, 0.051F + var1);
   }

   static float pathCurve(Random random, boolean precision) {
      float var2 = -0.0119998455F;
      float var3 = var2 * 0.018F;
      return precision ? recoveredMethod2(random, 0.024F, 0.052F + var3 * 0.35F) : recoveredMethod2(random, 0.044F, 0.096F + var3);
   }

   static float profileConfidence() {
      return MathHelper.clamp(0.1F * Math.min(1.0F, 0.330625F), 0.0F, 1.0F);
   }

   private static int recoveredMethod1(Random random, int[] weights) {
      int var2 = 0;

      for (int var6 : weights) {
         var2 += var6;
      }

      int var7 = random.nextInt(var2);

      for (int var8 = 0; var8 < weights.length; var8++) {
         var7 -= weights[var8];
         if (var7 < 0) {
            return var8;
         }
      }

      return weights.length - 1;
   }

   private static float recoveredMethod2(Random random, float minimum, float maximum) {
      float var3 = (random.nextFloat() + random.nextFloat() + random.nextFloat()) / 3.0F;
      return minimum + (maximum - minimum) * var3;
   }

   private static float recoveredMethod3(double seed, float minimum, float maximum) {
      double var4 = seed - Math.floor(seed);
      return minimum + (maximum - minimum) * (float)var4;
   }

   private static double recoveredMethod4(int index, double offset) {
      return Math.sin((index + 1.0) * 12.9898 + offset * 78.233) * 43758.5453;
   }

   private static double recoveredMethod5(double progress, double in, double out) {
      if (progress <= 0.0) {
         return 0.0;
      }

      if (progress >= 1.0) {
         return 1.0;
      }

      double var6 = Math.pow(progress, in);
      double var8 = Math.pow(1.0 - progress, out);
      return var6 / (var6 + var8);
   }

   private static double recoveredMethod6(double value) {
      return Math.max(0.0, Math.min(1.0, value));
   }
}
