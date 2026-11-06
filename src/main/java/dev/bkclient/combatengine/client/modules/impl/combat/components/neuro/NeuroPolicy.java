package dev.bkclient.combatengine.client.modules.impl.combat.components.neuro;

import java.util.Arrays;
import java.util.Random;

public final class NeuroPolicy {
   private final NeuroWeights recoveredField1;
   private final Random recoveredField2;
   private final float[] recoveredField3;
   private final float[] recoveredField4;
   private final float[] recoveredField5;
   private final float[] recoveredField6;
   private final float[] recoveredField7;
   private final float[] recoveredField8;
   private final float[] recoveredField9;
   public float muYaw;
   public float muPitch;
   public float sigmaYaw;
   public float sigmaPitch;
   public float moveProbability;
   public float attackProbability;
   public boolean moved;

   public NeuroPolicy(NeuroWeights weights, Random random) {
      this.recoveredField1 = weights;
      this.recoveredField2 = random;
      this.recoveredField3 = new float[weights.hidden];
      this.recoveredField4 = new float[weights.input];
      this.recoveredField5 = new float[3 * weights.hidden];
      this.recoveredField6 = new float[3 * weights.hidden];
      this.recoveredField7 = new float[weights.head];
      this.recoveredField8 = new float[weights.output];
      this.recoveredField9 = new float[weights.latent];
      this.reseed();
   }

   public int observationSize() {
      return this.recoveredField1.obs;
   }

   public void reseed() {
      Arrays.fill(this.recoveredField3, 0.0F);
      if (this.recoveredField1.zeroLatent()) {
         Arrays.fill(this.recoveredField9, 0.0F);
      } else {
         for (int var1 = 0; var1 < this.recoveredField9.length; var1++) {
            this.recoveredField9[var1] = (float)this.recoveredField2.nextGaussian();
         }
      }
   }

   public void resetState() {
      Arrays.fill(this.recoveredField3, 0.0F);
   }

   public void setLatent(float[] values) {
      System.arraycopy(values, 0, this.recoveredField9, 0, Math.min(values.length, this.recoveredField9.length));
      Arrays.fill(this.recoveredField3, 0.0F);
   }

   public void step(float[] observation) {
      int var2 = this.recoveredField1.hidden;
      int var3 = this.recoveredField1.input;
      System.arraycopy(observation, 0, this.recoveredField4, 0, this.recoveredField1.obs);
      System.arraycopy(this.recoveredField9, 0, this.recoveredField4, this.recoveredField1.obs, this.recoveredField1.latent);
      float[] var4 = this.recoveredField1.weightIh;
      float[] var5 = this.recoveredField1.weightHh;

      for (int var6 = 0; var6 < 3 * var2; var6++) {
         int var7 = var6 * var3;
         float var8 = this.recoveredField1.biasIh[var6];

         for (int var9 = 0; var9 < var3; var9++) {
            var8 += var4[var7 + var9] * this.recoveredField4[var9];
         }

         this.recoveredField5[var6] = var8;
         int var21 = var6 * var2;
         float var10 = this.recoveredField1.biasHh[var6];

         for (int var11 = 0; var11 < var2; var11++) {
            var10 += var5[var21 + var11] * this.recoveredField3[var11];
         }

         this.recoveredField6[var6] = var10;
      }

      for (int var12 = 0; var12 < var2; var12++) {
         float var15 = recoveredMethod4(this.recoveredField5[var12] + this.recoveredField6[var12]);
         float var18 = recoveredMethod4(this.recoveredField5[var2 + var12] + this.recoveredField6[var2 + var12]);
         float var22 = recoveredMethod5(this.recoveredField5[2 * var2 + var12] + var15 * this.recoveredField6[2 * var2 + var12]);
         this.recoveredField3[var12] = (1.0F - var18) * var22 + var18 * this.recoveredField3[var12];
      }

      for (int var13 = 0; var13 < this.recoveredField1.head; var13++) {
         int var16 = var13 * var2;
         float var19 = this.recoveredField1.headBias[var13];

         for (int var23 = 0; var23 < var2; var23++) {
            var19 += this.recoveredField1.headWeight[var16 + var23] * this.recoveredField3[var23];
         }

         this.recoveredField7[var13] = recoveredMethod5(var19);
      }

      for (int var14 = 0; var14 < this.recoveredField1.output; var14++) {
         int var17 = var14 * this.recoveredField1.head;
         float var20 = this.recoveredField1.outBias[var14];

         for (int var24 = 0; var24 < this.recoveredField1.head; var24++) {
            var20 += this.recoveredField1.outWeight[var17 + var24] * this.recoveredField7[var24];
         }

         this.recoveredField8[var14] = var20;
      }

      this.muYaw = recoveredMethod2(this.recoveredField8[0], this.recoveredField1.yawScale, this.recoveredField1.yawMax);
      this.muPitch = recoveredMethod2(this.recoveredField8[1], this.recoveredField1.pitchScale, this.recoveredField1.pitchMax);
      this.sigmaYaw = recoveredMethod3(recoveredMethod6(this.recoveredField8[2]), this.recoveredField1.sigmaMin, this.recoveredField1.sigmaMax);
      this.sigmaPitch = recoveredMethod3(recoveredMethod6(this.recoveredField8[3]), this.recoveredField1.sigmaMin, this.recoveredField1.sigmaMax);
      this.moveProbability = recoveredMethod4(this.recoveredField8[4]);
      this.attackProbability = this.recoveredField1.hasAttackHead() ? recoveredMethod4(this.recoveredField8[5]) : 0.0F;
   }

   public void sample(float[] result, float gcd) {
      this.moved = this.recoveredField2.nextFloat() < this.moveProbability;
      if (!this.moved) {
         result[0] = 0.0F;
         result[1] = 0.0F;
      } else {
         float var3 = this.muYaw + this.sigmaYaw * this.recoveredMethod1();
         float var4 = this.muPitch + this.sigmaPitch * this.recoveredMethod1();
         result[0] = Math.round(var3 / gcd) * gcd;
         result[1] = Math.round(var4 / gcd) * gcd;
      }
   }

   public boolean sampleAttack() {
      return this.recoveredField1.hasAttackHead() && this.recoveredField2.nextFloat() < this.attackProbability;
   }

   private float recoveredMethod1() {
      float var1 = (float)this.recoveredField2.nextGaussian();
      return var1 < -2.0F ? -2.0F : (var1 > 2.0F ? 2.0F : var1);
   }

   private static float recoveredMethod2(float raw, float scale, float maximum) {
      return maximum * recoveredMethod5(raw * (scale / maximum));
   }

   private static float recoveredMethod3(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private static float recoveredMethod4(float x) {
      if (x >= 0.0F) {
         float var2 = (float)Math.exp(-x);
         return 1.0F / (1.0F + var2);
      } else {
         float var1 = (float)Math.exp(x);
         return var1 / (1.0F + var1);
      }
   }

   private static float recoveredMethod5(float x) {
      if (x > 8.0F) {
         return 1.0F;
      }

      if (x < -8.0F) {
         return -1.0F;
      }

      float var1 = (float)Math.exp(2.0F * x);
      return (var1 - 1.0F) / (var1 + 1.0F);
   }

   private static float recoveredMethod6(float x) {
      return x > 20.0F ? x : (float)Math.log1p(Math.exp(x));
   }
}
