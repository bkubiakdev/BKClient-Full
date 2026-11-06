package dev.bkclient.combatengine.client.modules.impl.combat.components.neuro;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.RotationsSystem;
import dev.bkclient.combatengine.client.modules.impl.combat.components.gcd.GCDUtil;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class NeuroRotation extends RotationsSystem implements QClient {
   private static final int recoveredField1 = 1;
   private static final int recoveredField2 = 40;
   private static final float recoveredField3 = 0.034064F;
   private static final float recoveredField4 = 0.6F;
   private static final float recoveredField5 = 3.0F;
   private static final float recoveredField6 = 12.0F;
   private final Random recoveredField7 = new Random();
   private float[] recoveredField8 = new float[20];
   private final float[] recoveredField9 = new float[2];
   private NeuroPolicy recoveredField10;
   private NeuroWeights recoveredField11;
   private boolean recoveredField12;
   private boolean recoveredField13 = true;
   private LivingEntity recoveredField14;
   private int recoveredField15 = Integer.MIN_VALUE;
   private int recoveredField16;
   private int recoveredField17;
   private float recoveredField18;
   private float recoveredField19;
   private boolean recoveredField20;
   private float recoveredField21;
   private float recoveredField22;
   private float recoveredField23;
   private float recoveredField24;
   private float recoveredField25 = 0.6F;
   private float recoveredField26 = 3.0F;
   private float recoveredField27 = 12.0F;
   private float recoveredField28;
   private float recoveredField29;
   private float recoveredField30 = 0.55F;

   public boolean isAvailable() {
      this.recoveredMethod1();
      return this.recoveredField10 != null;
   }

   private void recoveredMethod1() {
      NeuroWeights var1 = NeuroStyles.activeWeights();
      if (var1 == null) {
         if (this.recoveredField12) {
            return;
         }

         var1 = NeuroWeights.get();
         if (var1 == null) {
            this.recoveredField12 = true;
            return;
         }
      }

      if (this.recoveredField10 == null || this.recoveredField11 != var1) {
         this.recoveredField10 = new NeuroPolicy(var1, this.recoveredField7);
         this.recoveredField11 = var1;
         if (this.recoveredField8.length != var1.obs) {
            this.recoveredField8 = new float[var1.obs];
         }
      }
   }

   public void reset() {
      this.recoveredField14 = null;
      this.recoveredField15 = Integer.MIN_VALUE;
      this.recoveredField16 = 0;
      this.recoveredField17 = 40;
      this.recoveredField20 = false;
      this.recoveredField21 = this.recoveredField22 = this.recoveredField23 = this.recoveredField24 = 0.0F;
      this.recoveredField25 = 0.6F;
      this.recoveredField26 = 3.0F;
      this.recoveredField27 = 12.0F;
      if (this.recoveredField10 != null) {
         this.recoveredField10.reseed();
      }
   }

   @Override
   public void updateRotations(LivingEntity target) {
      this.recoveredMethod1();
      if (this.recoveredField10 != null && mc.player != null && target != null) {
         RotationStorage var2 = RotationStorage.instance;
         if (var2 == null || var2.currentPriority() <= 1) {
            int var3 = mc.player.age;
            if (var3 != this.recoveredField15) {
               this.recoveredField15 = var3;
               if (this.recoveredField14 != target) {
                  this.recoveredField14 = target;
                  this.recoveredField17 = 0;
                  this.recoveredField20 = false;
                  this.recoveredField10.reseed();
               } else if (this.recoveredField17 < 40) {
                  this.recoveredField17++;
               }

               Vec3d var4 = this.recoveredMethod4(target);
               Vec3d var5 = mc.player.getEyePos();
               double var6 = var4.x - var5.x;
               double var8 = var4.y - var5.y;
               double var10 = var4.z - var5.z;
               double var12 = Math.sqrt(var6 * var6 + var10 * var10);
               float var14 = (float)(Math.toDegrees(Math.atan2(var10, var6)) - 90.0);
               float var15 = (float)(-Math.toDegrees(Math.atan2(var8, var12)));
               float var16 = mc.player.getYaw();
               float var17 = mc.player.getPitch();
               float var18 = MathHelper.wrapDegrees(var14 - var16);
               float var19 = var15 - var17;
               float var20 = recoveredMethod3(var18, var19, var17);
               float var21 = 0.0F;
               float var22 = 0.0F;
               if (this.recoveredField20) {
                  var21 = MathHelper.wrapDegrees(var14 - this.recoveredField18);
                  var22 = var15 - this.recoveredField19;
               }

               this.recoveredField18 = var14;
               this.recoveredField19 = var15;
               this.recoveredField20 = true;
               this.recoveredMethod2(target, var18, var19, var20, var21, var22, (float)Math.sqrt(var12 * var12 + var8 * var8));
               this.recoveredField10.step(this.recoveredField8);
               this.recoveredField10.sample(this.recoveredField9, GCDUtil.getGCDValue());
               this.recoveredField13 = !this.recoveredField11.hasAttackHead() || this.recoveredField10.sampleAttack();
               float var23 = var16 + this.recoveredField9[0];
               float var24 = MathHelper.clamp(var17 + this.recoveredField9[1], -89.0F, 89.0F);
               RotationStorage.update(new Rotation(var23, var24), 360.0F, 360.0F, 30.0F, 22.0F, 2, 1, false);
               float var25 = recoveredMethod3(MathHelper.wrapDegrees(var14 - var23), var15 - var24, var24);
               this.recoveredField23 = this.recoveredField21;
               this.recoveredField24 = this.recoveredField22;
               this.recoveredField21 = this.recoveredField9[0];
               this.recoveredField22 = this.recoveredField9[1];
               this.recoveredField25 = this.recoveredField25 + 0.034064F * ((this.recoveredField10.moved ? 1.0F : 0.0F) - this.recoveredField25);
               this.recoveredField26 = this.recoveredField26 + 0.034064F * (Math.abs(this.recoveredField9[0]) - this.recoveredField26);
               this.recoveredField27 = this.recoveredField27 + 0.034064F * (var25 - this.recoveredField27);
               this.recoveredField16++;
            }
         }
      }
   }

   public boolean attackConsent() {
      return this.recoveredField10 == null || this.recoveredField13;
   }

   private void recoveredMethod2(LivingEntity target, float errorYaw, float errorPitch, float offset, float rateYaw, float ratePitch, float distance) {
      float var8 = (float)target.getVelocity().horizontalLength();
      double var9 = (Math.PI * 2) * (this.recoveredField16 % 40) / 40.0;
      this.recoveredField8[0] = recoveredMethod5(errorYaw / 30.0F, -6.0F, 6.0F);
      this.recoveredField8[1] = recoveredMethod5(errorPitch / 15.0F, -6.0F, 6.0F);
      this.recoveredField8[2] = recoveredMethod6(errorYaw / 8.0F);
      this.recoveredField8[3] = recoveredMethod6(errorPitch / 5.0F);
      this.recoveredField8[4] = recoveredMethod5(offset / 30.0F, 0.0F, 6.0F);
      this.recoveredField8[5] = recoveredMethod6(offset / 6.0F);
      this.recoveredField8[6] = recoveredMethod5(rateYaw / 8.0F, -6.0F, 6.0F);
      this.recoveredField8[7] = recoveredMethod5(ratePitch / 4.0F, -6.0F, 6.0F);
      this.recoveredField8[8] = recoveredMethod5(this.recoveredField21 / 8.0F, -6.0F, 6.0F);
      this.recoveredField8[9] = recoveredMethod5(this.recoveredField22 / 4.0F, -6.0F, 6.0F);
      this.recoveredField8[10] = recoveredMethod5(this.recoveredField23 / 8.0F, -6.0F, 6.0F);
      this.recoveredField8[11] = recoveredMethod5(this.recoveredField24 / 4.0F, -6.0F, 6.0F);
      this.recoveredField8[12] = recoveredMethod5(distance / 6.0F, 0.0F, 2.0F);
      this.recoveredField8[13] = recoveredMethod5(var8 / 0.34F, 0.0F, 2.0F);
      this.recoveredField8[14] = recoveredMethod5(this.recoveredField17 / 40.0F, 0.0F, 1.0F);
      this.recoveredField8[15] = (float)Math.sin(var9);
      this.recoveredField8[16] = (float)Math.cos(var9);
      this.recoveredField8[17] = this.recoveredField25;
      this.recoveredField8[18] = recoveredMethod5(this.recoveredField26 / 12.0F, 0.0F, 6.0F);
      this.recoveredField8[19] = recoveredMethod5(this.recoveredField27 / 25.0F, 0.0F, 6.0F);
      if (this.recoveredField8.length >= 28) {
         this.recoveredField8[20] = recoveredMethod5((float)mc.player.getVelocity().horizontalLength() / 0.34F, 0.0F, 2.0F);
         this.recoveredField8[21] = recoveredMethod6((float)mc.player.getVelocity().y / 0.5F);
         this.recoveredField8[22] = mc.player.isOnGround() ? 1.0F : 0.0F;
         this.recoveredField8[23] = mc.player.isTouchingWater() ? 1.0F : 0.0F;
         this.recoveredField8[24] = mc.player.isSprinting() ? 1.0F : 0.0F;
         this.recoveredField8[25] = mc.player.isSneaking() ? 1.0F : 0.0F;
         this.recoveredField8[26] = recoveredMethod5(mc.player.getAttackCooldownProgress(0.0F), 0.0F, 1.0F);
         this.recoveredField8[27] = recoveredMethod5(target.hurtTime / 10.0F, 0.0F, 1.0F);
      }
   }

   private static float recoveredMethod3(float errorYaw, float errorPitch, float pitch) {
      float var3 = Math.max((float)Math.cos(Math.toRadians(pitch)), 0.05F);
      float var4 = errorYaw * var3;
      return (float)Math.sqrt(var4 * var4 + errorPitch * errorPitch);
   }

   private Vec3d recoveredMethod4(LivingEntity target) {
      Box var2 = this.getPredictedBox(target);
      if (this.recoveredField11 != null && this.recoveredField11.obs >= 28) {
         return var2.getCenter();
      }

      this.recoveredField28 = this.recoveredField28 + (0.035F + this.recoveredField7.nextFloat() * 0.02F);
      this.recoveredField29 = this.recoveredField29 + (this.recoveredField7.nextFloat() - 0.5F) * 0.14F;
      this.recoveredField30 = this.recoveredField30 + ((float)Math.sin(this.recoveredField28 * 0.31F) * 0.14F + 0.55F - this.recoveredField30) * 0.08F;
      double var3 = var2.getLengthX() * 0.3 * (0.4 + 0.6 * Math.abs(Math.sin(this.recoveredField28 * 0.17F)));
      Vec3d var5 = var2.getCenter();
      return new Vec3d(
         var5.x + Math.cos(this.recoveredField29) * var3,
         var2.minY + var2.getLengthY() * MathHelper.clamp(this.recoveredField30, 0.25F, 0.85F),
         var5.z + Math.sin(this.recoveredField29) * var3
      );
   }

   private static float recoveredMethod5(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private static float recoveredMethod6(float x) {
      if (x > 8.0F) {
         return 1.0F;
      }

      if (x < -8.0F) {
         return -1.0F;
      }

      float var1 = (float)Math.exp(2.0F * x);
      return (var1 - 1.0F) / (var1 + 1.0F);
   }
}
