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

public class HolyWorldRotation extends RotationsSystem implements QClient {
   private static final float recoveredField1 = 40.0F;
   private static final float recoveredField2 = 360.0F;
   private static final float recoveredField3 = 360.0F;
   private static final float recoveredField4 = 360.0F;
   private static final float recoveredField5 = 0.35F;
   private static final float recoveredField6 = 0.9F;
   private static final float recoveredField7 = 0.7F;
   private static final float recoveredField8 = 0.18F;
   private static final float recoveredField9 = 0.52F;
   private static final float recoveredField10 = 18.0F;
   private static final float recoveredField11 = 0.14F;
   private final Random recoveredField12 = new Random();
   private float recoveredField13 = 40.0F;
   private LivingEntity recoveredField14;
   private boolean recoveredField15;
   private boolean recoveredField16;
   private int recoveredField17;
   private long recoveredField18;
   private long recoveredField19;
   private float recoveredField20;
   private float recoveredField21;
   private float recoveredField22;
   private float recoveredField23;
   private float recoveredField24;
   private boolean recoveredField25;

   public float getCurrentFov() {
      return this.recoveredField13;
   }

   public void reset() {
      this.recoveredField13 = 40.0F;
      this.recoveredMethod7();
      this.recoveredMethod14();
   }

   public void onAttack() {
      this.recoveredField13 = 40.0F;
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
            this.recoveredField13 = 40.0F;
            this.recoveredMethod7();
            this.recoveredMethod14();
         } else {
            Vec3d var2 = this.recoveredMethod2(target);
            if (var2 == null) {
               this.recoveredMethod7();
               this.recoveredMethod14();
            } else {
               Vec2f var3 = RotationUtils.getRotations(var2);
               float var4 = MathHelper.wrapDegrees(var3.x);
               if (!this.recoveredMethod5(var4)) {
                  this.recoveredMethod7();
                  this.recoveredMethod14();
               } else {
                  long var5 = System.nanoTime();
                  float var7 = this.recoveredMethod4();
                  this.recoveredMethod8(target, var5, var4, var7);
                  Rotation var8 = this.recoveredField15 ? this.recoveredMethod10(var5) : new Rotation(var4, var7);
                  RotationStorage.update(var8, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, false);
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
         float var5 = this.recoveredMethod4();
         RotationStorage.update(new Rotation(var4, var5), 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, false);
         this.recoveredMethod6(target, var4, var5);
         this.recoveredField13 = 40.0F;
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
      return Math.abs(MathHelper.wrapDegrees(targetYaw - this.recoveredMethod3())) <= this.recoveredField13;
   }

   private void recoveredMethod6(LivingEntity target, float yaw, float pitch) {
      this.recoveredField14 = target;
      this.recoveredField15 = false;
      this.recoveredField21 = MathHelper.wrapDegrees(yaw);
      this.recoveredField23 = MathHelper.clamp(pitch, -89.0F, 89.0F);
      this.recoveredField24 = this.recoveredField21;
      this.recoveredField25 = true;
   }

   private void recoveredMethod7() {
      this.recoveredField14 = null;
      this.recoveredField15 = false;
      this.recoveredField25 = false;
   }

   private void recoveredMethod8(LivingEntity target, long now, float targetYaw, float targetPitch) {
      Rotation var6 = this.recoveredField15 ? this.recoveredMethod10(now) : this.recoveredMethod11();
      float var7 = var6.getYaw();
      float var8 = Math.abs(MathHelper.wrapDegrees(targetYaw - var7));
      float var9 = this.recoveredField14 == target ? Math.abs(MathHelper.wrapDegrees(targetYaw - this.recoveredField21)) : var8;
      float var10 = this.recoveredField25 ? Math.abs(MathHelper.wrapDegrees(targetYaw - this.recoveredField24)) : 0.0F;
      double var11 = this.recoveredMethod12(now);
      boolean var13 = this.recoveredField14 != target;
      boolean var14 = !this.recoveredField15 && (var13 || var8 > 0.35F || var9 > 0.35F);
      boolean var15 = this.recoveredField15
         && (var11 >= 0.999 || var9 > 0.9F && var11 >= 0.18F || var8 > 1.3499999F && var11 >= 0.52F || var10 > 0.7F && var11 >= 0.18F);
      if (var14 || var15) {
         boolean var16 = var8 > 5.5F || var10 > 0.7F;
         this.recoveredMethod9(target, now, var6.getYaw(), var6.getPitch(), targetYaw, targetPitch, var16);
      }

      this.recoveredField24 = targetYaw;
      this.recoveredField25 = true;
   }

   private void recoveredMethod9(LivingEntity target, long now, float startYaw, float startPitch, float targetYaw, float targetPitch, boolean action) {
      this.recoveredField14 = target;
      this.recoveredField15 = true;
      this.recoveredField16 = action;
      this.recoveredField17 = SlothDatasetProfile.choose(this.recoveredField12, action);
      this.recoveredField18 = now;
      this.recoveredField20 = MathHelper.wrapDegrees(startYaw);
      this.recoveredField22 = MathHelper.clamp(startPitch, -89.0F, 89.0F);
      this.recoveredField21 = MathHelper.wrapDegrees(targetYaw);
      this.recoveredField23 = MathHelper.clamp(targetPitch, -89.0F, 89.0F);
      float var9 = Math.abs(MathHelper.wrapDegrees(this.recoveredField21 - this.recoveredField20));
      float var10 = MathHelper.clamp(0.72F + var9 / 28.0F, 0.8F, 1.24F);
      long var11 = (long)((float)SlothDatasetProfile.duration(action, this.recoveredField17) * var10 * this.recoveredMethod13(0.92F, 1.08F));
      long var13 = action ? 260L : 320L;
      var11 = Math.max(75L, Math.min(var11, var13));
      this.recoveredField19 = var11 * 1000000L;
   }

   private Rotation recoveredMethod10(long now) {
      if (this.recoveredField15 && this.recoveredField19 > 0L) {
         double var3 = this.recoveredMethod12(now);
         double var5 = SlothDatasetProfile.progress(this.recoveredField16, this.recoveredField17, var3);
         double var7 = SlothDatasetProfile.lateral(this.recoveredField16, this.recoveredField17, var3);
         float var9 = MathHelper.wrapDegrees(this.recoveredField21 - this.recoveredField20);
         float var10 = this.recoveredField23 - this.recoveredField22;
         float var11 = MathHelper.wrapDegrees(this.recoveredField20 + var9 * (float)var5);
         float var12 = MathHelper.clamp(this.recoveredField22 + var10 * (float)var5, -89.0F, 89.0F);
         float var13 = Math.min((float)Math.hypot(var9, var10), 18.0F);
         float var14 = var9 == 0.0F ? 1.0F : Math.signum(var9);
         float var15 = (float)(var14 * var13 * var7);
         float var16 = (float)(-var14 * var13 * var7 * 0.14F);
         float var17 = MathHelper.wrapDegrees(var11 + var15);
         float var18 = MathHelper.clamp(var12 + var16, -89.0F, 89.0F);
         if (var3 >= 0.999) {
            this.recoveredField15 = false;
            var17 = this.recoveredField21;
            var18 = this.recoveredField23;
         }

         return new Rotation(var17, var18);
      } else {
         return new Rotation(this.recoveredField21, this.recoveredField23);
      }
   }

   private Rotation recoveredMethod11() {
      return new Rotation(MathHelper.wrapDegrees(mc.player.getYaw()), MathHelper.clamp(mc.player.getPitch(), -89.0F, 89.0F));
   }

   private double recoveredMethod12(long now) {
      return this.recoveredField15 && this.recoveredField19 > 0L
         ? Math.max(0.0, Math.min(1.0, (double)(now - this.recoveredField18) / this.recoveredField19))
         : 1.0;
   }

   private float recoveredMethod13(float minimum, float maximum) {
      return minimum + (maximum - minimum) * this.recoveredField12.nextFloat();
   }

   private void recoveredMethod14() {
      if (RotationStorage.instance != null && RotationStorage.instance.isRotating()) {
         RotationStorage.instance.stopRotation();
      }
   }
}
