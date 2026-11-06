package dev.bkclient.combatengine.client.modules.impl.combat.components.rotations;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.storages.implement.FreeLookStorage;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.api.utils.rotate.MultipointUtils;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import dev.bkclient.combatengine.api.utils.rotate.RotationUtils;
import dev.bkclient.combatengine.client.modules.impl.combat.components.RotationsSystem;
import dev.bkclient.combatengine.client.modules.impl.combat.components.interpolation.BestPoint;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class Holy2Rotation extends RotationsSystem implements QClient {
   private static final float recoveredField1 = 180.0F;
   private static final float recoveredField2 = 1080.0F;
   private static final float recoveredField3 = 1080.0F;
   private static final float recoveredField4 = 1080.0F;
   private static final float recoveredField5 = 0.05F;
   private static final float recoveredField6 = 0.16F;
   private static final float recoveredField7 = 0.07F;
   private static final float recoveredField8 = 0.015F;
   private static final float recoveredField9 = 0.09F;
   private static final float recoveredField10 = 54.0F;
   private static final float recoveredField11 = 0.68F;
   private static final float recoveredField12 = 0.08F;
   private static final float recoveredField13 = 0.06F;
   private static final int recoveredField14 = 0;
   private static final int recoveredField15 = 1;
   private static final int recoveredField16 = 2;
   private static final int recoveredField17 = 3;
   private static final int recoveredField18 = 4;
   private static final int recoveredField19 = 5;
   private final Random recoveredField20 = new Random();
   private float recoveredField21 = 180.0F;
   private LivingEntity recoveredField22;
   private boolean recoveredField23;
   private boolean recoveredField24;
   private int recoveredField25;
   private long recoveredField26;
   private long recoveredField27;
   private float recoveredField28;
   private float recoveredField29;
   private float recoveredField30;
   private float recoveredField31;
   private float recoveredField32;
   private boolean recoveredField33;
   private int recoveredField34;
   private float recoveredField35;
   private float recoveredField36;
   private float recoveredField37;
   private float recoveredField38;
   private float recoveredField39;
   private float recoveredField40;
   private float recoveredField41;
   private float recoveredField42;
   private float recoveredField43;
   private int recoveredField44;
   private boolean recoveredField45;
   private float recoveredField46;
   private float recoveredField47;
   private float recoveredField48;
   private float recoveredField49;
   private float recoveredField50;
   private float recoveredField51;
   private float recoveredField52;

   public float getCurrentFov() {
      return this.recoveredField21;
   }

   public void reset() {
      this.recoveredField21 = 180.0F;
      this.recoveredMethod7();
      this.recoveredMethod21();
   }

   public void onAttack() {
      this.recoveredField21 = 180.0F;
   }

   public boolean prepareAttack(LivingEntity target) {
      return !this.isInFov(target) ? false : this.recoveredMethod1(target);
   }

   public boolean isInFov(LivingEntity target) {
      if (mc.player != null && target != null) {
         Vec3d var2 = this.recoveredMethod2(target);
         return var2 == null ? false : this.recoveredMethod5(MathHelper.wrapDegrees(RotationUtils.getRotations(var2).x));
      } else {
         return false;
      }
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (mc.player != null) {
         if (target == null) {
            this.recoveredField21 = 180.0F;
            this.recoveredMethod7();
            this.recoveredMethod21();
         } else {
            Vec3d var2 = this.recoveredMethod2(target);
            if (var2 == null) {
               this.recoveredMethod7();
               this.recoveredMethod21();
            } else {
               Vec2f var3 = RotationUtils.getRotations(var2);
               float var4 = MathHelper.wrapDegrees(var3.x);
               float var5 = MathHelper.clamp(var3.y, -89.0F, 89.0F);
               if (!this.recoveredMethod5(var4)) {
                  this.recoveredMethod7();
                  this.recoveredMethod21();
               } else {
                  long var6 = System.nanoTime();
                  this.recoveredMethod8(target, var6, var4, var5);
                  Rotation var8 = this.recoveredField23 ? this.recoveredMethod10(var6) : new Rotation(var4, var5);
                  RotationStorage.update(var8, 1080.0F, 1080.0F, 1080.0F, 1080.0F, 0, 1, false);
               }
            }
         }
      }
   }

   private boolean recoveredMethod1(LivingEntity target) {
      if (mc.player != null && target != null) {
         Vec3d var2 = this.recoveredMethod2(target);
         if (var2 == null) {
            return false;
         }

         Vec2f var3 = RotationUtils.getRotations(var2);
         float var4 = MathHelper.wrapDegrees(var3.x);
         float var5 = MathHelper.clamp(var3.y, -89.0F, 89.0F);
         RotationStorage.update(new Rotation(var4, var5), 1080.0F, 1080.0F, 1080.0F, 1080.0F, 0, 1, false);
         this.recoveredMethod6(target, var4, var5);
         this.recoveredField21 = 180.0F;
         return true;
      } else {
         return false;
      }
   }

   private Vec3d recoveredMethod2(LivingEntity target) {
      Vec3d var2 = BestPoint.getNearestPoint(target);
      if (var2 == null) {
         var2 = MultipointUtils.getClosestPoint(target);
      }

      if (var2 == null) {
         var2 = this.getPredictedBox(target).getCenter();
      }

      return var2;
   }

   private float recoveredMethod3() {
      return FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : mc.player.getYaw();
   }

   private float recoveredMethod4() {
      return FreeLookStorage.isActive()
         ? MathHelper.clamp(FreeLookStorage.getFreePitch(), -89.0F, 89.0F)
         : MathHelper.clamp(mc.player.getPitch(), -89.0F, 89.0F);
   }

   private boolean recoveredMethod5(float targetYaw) {
      return Math.abs(MathHelper.wrapDegrees(targetYaw - this.recoveredMethod3())) <= this.recoveredField21;
   }

   private void recoveredMethod6(LivingEntity target, float yaw, float pitch) {
      this.recoveredField22 = target;
      this.recoveredField23 = false;
      this.recoveredField29 = MathHelper.wrapDegrees(yaw);
      this.recoveredField31 = MathHelper.clamp(pitch, -89.0F, 89.0F);
      this.recoveredField32 = this.recoveredField29;
      this.recoveredField33 = true;
   }

   private void recoveredMethod7() {
      this.recoveredField22 = null;
      this.recoveredField23 = false;
      this.recoveredField33 = false;
      this.recoveredField34 = 0;
      this.recoveredField44 = 0;
      this.recoveredField45 = false;
      this.recoveredField46 = 0.0F;
      this.recoveredField47 = 0.0F;
      this.recoveredField48 = 0.0F;
      this.recoveredField49 = 0.0F;
      this.recoveredField50 = 0.0F;
      this.recoveredField51 = 0.0F;
      this.recoveredField52 = 0.0F;
   }

   private void recoveredMethod8(LivingEntity target, long now, float targetYaw, float targetPitch) {
      Rotation var6 = this.recoveredField23 ? this.recoveredMethod10(now) : this.recoveredMethod12();
      float var7 = var6.getYaw();
      float var8 = var6.getPitch();
      float var9 = (float)Math.hypot(MathHelper.wrapDegrees(targetYaw - var7), targetPitch - var8);
      float var10 = this.recoveredField22 == target
         ? (float)Math.hypot(MathHelper.wrapDegrees(targetYaw - this.recoveredField29), targetPitch - this.recoveredField31)
         : var9;
      float var11 = this.recoveredField33 ? Math.abs(MathHelper.wrapDegrees(targetYaw - this.recoveredField32)) : 0.0F;
      double var12 = this.recoveredMethod15(now);
      boolean var14 = this.recoveredField22 != target;
      boolean var15 = !this.recoveredField23 && (var14 || var9 > 0.05F || var10 > 0.05F);
      boolean var16 = this.recoveredField23
         && (var12 >= 0.999 || var10 > 0.16F && var12 >= 0.015F || var9 > 0.19999999F && var12 >= 0.09F || var11 > 0.07F && var12 >= 0.015F);
      if (var15 || var16) {
         boolean var17 = var9 > 2.0F || var11 > 0.07F;
         this.recoveredMethod9(target, now, var6.getYaw(), var6.getPitch(), targetYaw, targetPitch, var17);
      }

      this.recoveredField32 = targetYaw;
      this.recoveredField33 = true;
   }

   private void recoveredMethod9(LivingEntity target, long now, float startYaw, float startPitch, float targetYaw, float targetPitch, boolean action) {
      this.recoveredField22 = target;
      this.recoveredField23 = true;
      this.recoveredField24 = action;
      this.recoveredField25 = SlothDatasetProfile.choose(this.recoveredField20, action);
      float var9 = this.recoveredMethod13();
      this.recoveredField45 = var9 > 0.08F;
      this.recoveredField44 = this.recoveredMethod11(action, var9);
      this.recoveredField46 = this.recoveredMethod14();
      this.recoveredField47 = this.recoveredMethod20(this.recoveredField45 ? 0.34F : 0.08F, this.recoveredField45 ? 0.82F : 0.24F) * Math.max(0.25F, var9);
      this.recoveredField48 = this.recoveredMethod20(this.recoveredField45 ? 0.24F : 0.06F, this.recoveredField45 ? 0.58F : 0.16F) * Math.max(0.2F, var9);
      this.recoveredField26 = now;
      this.recoveredField28 = MathHelper.wrapDegrees(startYaw);
      this.recoveredField30 = MathHelper.clamp(startPitch, -89.0F, 89.0F);
      this.recoveredField29 = MathHelper.wrapDegrees(targetYaw);
      this.recoveredField31 = MathHelper.clamp(targetPitch, -89.0F, 89.0F);
      float var10 = Math.abs(MathHelper.wrapDegrees(this.recoveredField29 - this.recoveredField28));
      float var11 = Math.abs(this.recoveredField31 - this.recoveredField30);
      if (this.recoveredField45) {
         this.recoveredField34 = action ? this.recoveredField20.nextInt(3) + 1 : this.recoveredField20.nextInt(2) + 1;
      } else {
         this.recoveredField34 = action ? this.recoveredField20.nextInt(5) + 2 : this.recoveredField20.nextInt(4) + 1;
      }

      if (this.recoveredField44 == 5) {
         this.recoveredField34 = this.recoveredField34 + (this.recoveredField45 ? 1 : 2);
      } else if (this.recoveredField44 == 1 && this.recoveredField20.nextFloat() < 0.55F) {
         this.recoveredField34++;
      }

      if (var10 > 120.0F && this.recoveredField20.nextFloat() < 0.78F) {
         this.recoveredField34++;
      }

      float var12 = Math.signum(MathHelper.wrapDegrees(this.recoveredField29 - this.recoveredField28));
      if (var12 == 0.0F) {
         var12 = this.recoveredField20.nextBoolean() ? 1.0F : -1.0F;
      }

      this.recoveredField35 = this.recoveredField20.nextFloat() < 0.42F ? -var12 : var12;
      this.recoveredField36 = this.recoveredMethod20(action ? 42.0F : 28.0F, action ? 140.0F : 104.0F);
      this.recoveredField37 = this.recoveredMethod20(action ? 14.0F : 9.0F, action ? 56.0F : 34.0F);
      this.recoveredField49 = this.recoveredMethod20(this.recoveredField45 ? 14.0F : 8.0F, this.recoveredField45 ? 42.0F : 30.0F);
      this.recoveredField50 = this.recoveredMethod20(this.recoveredField45 ? 6.0F : 4.0F, this.recoveredField45 ? 24.0F : 16.0F);
      this.recoveredField51 = this.recoveredMethod20(action ? 8.0F : 4.0F, action ? 28.0F : 16.0F);
      this.recoveredField52 = this.recoveredMethod20(action ? 10.0F : 5.0F, action ? 38.0F : 20.0F);
      this.recoveredField38 = this.recoveredField20.nextFloat() * (float) (Math.PI * 2);
      this.recoveredField39 = this.recoveredMethod20(action ? 3.2F : 2.5F, action ? 6.6F : 4.7F);
      this.recoveredField40 = this.recoveredMethod20(action ? 1.7F : 1.3F, action ? 3.2F : 2.5F);
      this.recoveredField41 = this.recoveredMethod20(this.recoveredField45 ? 0.82F : 1.28F, this.recoveredField45 ? 1.62F : 2.8F);
      this.recoveredField42 = this.recoveredMethod20(this.recoveredField45 ? 4.2F : 3.0F, this.recoveredField45 ? 9.8F : 7.2F);
      this.recoveredField43 = this.recoveredMethod20(this.recoveredField45 ? 2.5F : 1.8F, this.recoveredField45 ? 6.4F : 4.9F);
      float var13 = MathHelper.clamp(0.38F + var10 / 115.0F + var11 / 80.0F, 0.42F, 1.18F);
      float var14 = this.recoveredMethod20(action ? 18.0F : 24.0F, action ? 56.0F : 74.0F);
      if (this.recoveredField45) {
         var14 *= this.recoveredMethod20(0.58F, 0.84F);
      }

      if (this.recoveredField44 == 3) {
         var14 *= 0.76F;
      } else if (this.recoveredField44 == 5) {
         var14 *= 1.08F;
      }

      long var15 = (long)(var14 * var13);
      if (this.recoveredField34 >= 4) {
         var15 += this.recoveredField45 ? 4L : 8L;
      }

      long var17 = this.recoveredField45 ? 84L : 108L;
      this.recoveredField27 = Math.max(12L, Math.min(var15, var17)) * 1000000L;
   }

   private Rotation recoveredMethod10(long now) {
      if (this.recoveredField23 && this.recoveredField27 > 0L) {
         double var3 = this.recoveredMethod15(now);
         double var5 = SlothDatasetProfile.progress(this.recoveredField24, this.recoveredField25, var3);
         double var7 = this.recoveredMethod19((float)var5, (float)this.recoveredMethod17(var3, this.recoveredField39), 0.58F);
         double var9 = this.recoveredMethod17(var3, this.recoveredField40);
         double var11 = SlothDatasetProfile.lateral(this.recoveredField24, this.recoveredField25, var3);
         float var13 = MathHelper.wrapDegrees(this.recoveredField29 - this.recoveredField28);
         float var14 = this.recoveredField31 - this.recoveredField30;
         float var15 = MathHelper.wrapDegrees(this.recoveredField28 + var13 * (float)var7);
         float var16 = MathHelper.clamp(this.recoveredField30 + var14 * (float)var7, -89.0F, 89.0F);
         float var17 = Math.min((float)Math.hypot(var13, var14), 54.0F);
         float var18 = var13 == 0.0F ? this.recoveredField35 : Math.signum(var13);
         float var19 = var18 * var17 * (float)(var11 * this.recoveredField41);
         float var20 = (float)(-var18 * var17 * var11 * 0.68F);
         float var21 = this.recoveredField35 * this.recoveredField34 * 360.0F * (float)var9;
         float var22 = (float)Math.pow(Math.max(0.0, 1.0 - Math.abs(var3 * 2.0 - 1.0)), 0.3);
         float var23 = (float)Math.pow(Math.max(0.0, 1.0 - var3), 0.18);
         float var24 = this.recoveredField36 * var22 * (float)Math.sin(var3 * Math.PI * this.recoveredField42 + this.recoveredField38);
         float var25 = this.recoveredField37 * var22 * (float)Math.cos(var3 * Math.PI * this.recoveredField43 + this.recoveredField38 * 0.71F);
         float var26 = this.recoveredField49
            * var23
            * this.recoveredMethod18(var3 * Math.PI * (this.recoveredField42 * 1.65F + 1.2F) + this.recoveredField38 * 1.4F);
         float var27 = this.recoveredField50 * var23 * (float)Math.sin(var3 * Math.PI * (this.recoveredField43 * 1.85F + 0.9F) + this.recoveredField38 * 1.1F);
         float var28;
         float var29;
         switch (this.recoveredField44) {
            case 1:
               float var41 = (float)Math.pow(Math.sin(var3 * Math.PI), 0.72);
               float var43 = -this.recoveredField35 * this.recoveredField52 * var41;
               var28 = var21 * 0.82F + var19 * 1.34F + var24 * 0.72F + var43 + var26;
               var29 = var20 * 0.48F + var25 * 0.86F + var27;
               break;
            case 2:
               float var40 = this.recoveredMethod18(var3 * Math.PI * (this.recoveredField42 * 0.78F) + this.recoveredField38);
               var28 = var21 * 0.46F + var19 * 1.18F + var40 * this.recoveredField36 + var26 * 0.9F;
               var29 = var20 * 0.82F + var40 * this.recoveredField37 * 0.62F + var25 * 0.38F + var27;
               break;
            case 3:
               float var39 = (float)Math.sin(var3 * Math.PI * (this.recoveredField42 * 0.58F) + this.recoveredField38);
               var28 = var21 * 0.32F + var19 * 0.92F + var39 * this.recoveredField36 * 0.68F + var26 * 0.75F;
               var29 = var20 * 0.34F + var25 * 0.26F + var27 * 0.45F;
               break;
            case 4:
               float var38 = (float)Math.sin(var3 * Math.PI * (this.recoveredField42 * 1.35F) + this.recoveredField38 * 1.4F);
               var28 = var21 * 0.74F + var19 * 1.52F + var24 * 0.85F + var38 * this.recoveredField49 * 1.45F;
               var29 = var20 + var25 * 0.55F - var38 * this.recoveredField37 * 0.7F + var27;
               break;
            case 5:
               float var30 = (float)Math.sin(var3 * Math.PI * (this.recoveredField42 * 0.92F) + this.recoveredField38);
               float var31 = (float)(1.0 - Math.pow(1.0 - var3, 2.4));
               var28 = var21 * 1.18F + var19 * 1.12F + var24 * 0.58F + var26 * 1.1F + var30 * this.recoveredField36 * 0.42F;
               var29 = var20 + var25 + var27 + (var31 - 0.35F) * this.recoveredField51;
               break;
            default:
               var28 = var21 + var19 + var24 + var26 * 0.55F;
               var29 = var20 + var25 + var27 * 0.65F;
         }

         float var42 = this.recoveredMethod13();
         float var44 = this.recoveredMethod19(this.recoveredField46, this.recoveredMethod14(), 0.42F + var42 * 0.38F);
         float var32 = (float)Math.pow(Math.max(0.0, 1.0 - var3), this.recoveredField45 ? 0.42F : 0.9F);
         float var33 = MathHelper.wrapDegrees(var44 - var15) * this.recoveredField47 * var32 * (0.35F + var42 * 0.75F);
         float var34 = (this.recoveredMethod4() - var16) * this.recoveredField48 * var32 * (0.28F + var42 * 0.6F);
         float var35 = -var18 * this.recoveredField52 * (float)Math.pow(var3, 1.65F) * (float)Math.pow(Math.max(0.0, 1.0 - var3), 0.18F);
         float var36 = MathHelper.wrapDegrees(var15 + var28 + var33 - var35);
         float var37 = MathHelper.clamp(var16 + var29 + var34, -89.0F, 89.0F);
         if (var3 >= 0.999) {
            this.recoveredField23 = false;
            var36 = this.recoveredField29;
            var37 = this.recoveredField31;
         }

         return new Rotation(var36, var37);
      } else {
         return new Rotation(this.recoveredField29, this.recoveredField31);
      }
   }

   private int recoveredMethod11(boolean action, float movementIntensity) {
      double var3 = this.recoveredField20.nextDouble();
      if (movementIntensity > 0.45F) {
         if (var3 < 0.3) {
            return 3;
         } else if (var3 < 0.54) {
            return 2;
         } else if (var3 < 0.74) {
            return 1;
         } else {
            return var3 < 0.9 ? 4 : 5;
         }
      } else if (action) {
         if (var3 < 0.22) {
            return 5;
         } else if (var3 < 0.42) {
            return 1;
         } else if (var3 < 0.6) {
            return 4;
         } else {
            return var3 < 0.8 ? 0 : 2;
         }
      } else if (var3 < 0.18) {
         return 0;
      } else if (var3 < 0.38) {
         return 2;
      } else if (var3 < 0.56) {
         return 3;
      } else if (var3 < 0.74) {
         return 1;
      } else {
         return var3 < 0.88 ? 4 : 5;
      }
   }

   private Rotation recoveredMethod12() {
      return new Rotation(MathHelper.wrapDegrees(mc.player.getYaw()), MathHelper.clamp(mc.player.getPitch(), -89.0F, 89.0F));
   }

   private float recoveredMethod13() {
      if (mc.player != null && mc.player.input != null) {
         float var1 = MathHelper.clamp(Math.abs(mc.player.input.movementForward) + Math.abs(mc.player.input.movementSideways) * 0.85F, 0.0F, 1.0F);
         Vec3d var2 = mc.player.getVelocity();
         double var3 = Math.sqrt(var2.x * var2.x + var2.z * var2.z);
         float var5 = MathHelper.clamp((float)(var3 / 0.23), 0.0F, 1.0F);
         float var6 = mc.player.isSprinting() ? 0.14F : 0.0F;
         return MathHelper.clamp(Math.max(var1, var5) + var6, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   private float recoveredMethod14() {
      float var1 = this.recoveredMethod3();
      if (mc.player != null && mc.player.input != null) {
         float var2 = mc.player.input.movementForward;
         float var3 = mc.player.input.movementSideways;
         if (var2 == 0.0F && var3 == 0.0F) {
            Vec3d var4 = mc.player.getVelocity();
            double var5 = Math.sqrt(var4.x * var4.x + var4.z * var4.z);
            return var5 > 0.06F ? (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var4.z, var4.x)) - 90.0) : var1;
         } else {
            return MathHelper.wrapDegrees((float)Math.toDegrees(this.recoveredMethod16(var1, var2, var3)));
         }
      } else {
         return var1;
      }
   }

   private double recoveredMethod15(long now) {
      return this.recoveredField23 && this.recoveredField27 > 0L
         ? Math.max(0.0, Math.min(1.0, (double)(now - this.recoveredField26) / this.recoveredField27))
         : 1.0;
   }

   private double recoveredMethod16(float rotationYaw, float moveForward, float moveStrafing) {
      if (moveForward < 0.0F) {
         rotationYaw += 180.0F;
      }

      float var4 = 1.0F;
      if (moveForward < 0.0F) {
         var4 = -0.5F;
      } else if (moveForward > 0.0F) {
         var4 = 0.5F;
      }

      if (moveStrafing > 0.0F) {
         rotationYaw -= 90.0F * var4;
      } else if (moveStrafing < 0.0F) {
         rotationYaw += 90.0F * var4;
      }

      return Math.toRadians(rotationYaw);
   }

   private double recoveredMethod17(double progress, float exponent) {
      return 1.0 - Math.pow(1.0 - progress, exponent);
   }

   private float recoveredMethod18(double phase) {
      return Math.signum((float)Math.sin(phase));
   }

   private float recoveredMethod19(float start, float end, float progress) {
      return start + (end - start) * MathHelper.clamp(progress, 0.0F, 1.0F);
   }

   private float recoveredMethod20(float minimum, float maximum) {
      return minimum + (maximum - minimum) * this.recoveredField20.nextFloat();
   }

   private void recoveredMethod21() {
      if (RotationStorage.instance != null && RotationStorage.instance.isRotating()) {
         RotationStorage.instance.stopRotation();
      }
   }
}
