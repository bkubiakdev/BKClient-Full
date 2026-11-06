package dev.bkclient.combatengine.client.modules.impl.combat.components.rotations;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import dev.bkclient.combatengine.api.utils.rotate.RotationUtils;
import dev.bkclient.combatengine.client.modules.impl.combat.Aura;
import dev.bkclient.combatengine.client.modules.impl.combat.components.RotationsSystem;
import dev.bkclient.combatengine.client.modules.impl.combat.components.gcd.GCDUtil;
import dev.bkclient.combatengine.client.modules.impl.combat.components.interpolation.BestPoint;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class WhiteRiseRotation extends RotationsSystem implements QClient {
   private final Aura recoveredField1;
   private LivingEntity recoveredField2;
   private float recoveredField3;
   private float recoveredField4;
   private float recoveredField5;
   private boolean recoveredField6;
   private boolean recoveredField7;
   private float recoveredField8;
   private int recoveredField9;

   public WhiteRiseRotation(Aura aura) {
      this.recoveredField1 = aura;
   }

   public void reset() {
      this.recoveredField2 = null;
      this.recoveredField5 = 0.0F;
      this.recoveredField6 = false;
      this.recoveredField8 = 0.0F;
      this.recoveredField9 = 0;
      this.recoveredField7 = mc.player != null;
      if (mc.player != null) {
         this.recoveredField3 = mc.player.getYaw();
         this.recoveredField4 = mc.player.getPitch();
      } else {
         this.recoveredField3 = 0.0F;
         this.recoveredField4 = 0.0F;
      }
   }

   public void onAttack() {
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (mc.player != null && target != null) {
         if (mc.player.isBlocking()) {
            this.rotate = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
            this.recoveredField3 = this.rotate.x;
            this.recoveredField4 = this.rotate.y;
         } else {
            if (!this.recoveredField7) {
               this.recoveredField3 = mc.player.getYaw();
               this.recoveredField4 = mc.player.getPitch();
               this.recoveredField7 = true;
            }

            if (this.recoveredField2 != target) {
               this.recoveredField2 = target;
               this.recoveredField5 = 0.0F;
               this.recoveredField6 = false;
               this.recoveredField9 = 0;
            }

            this.recoveredField9++;
            this.recoveredField8 = (float)((Math.sin(this.recoveredField9 * 0.17) * 0.12 + (Math.random() * 0.08 - 0.04)) * 0.7F);
            Vec3d var2 = BestPoint.getMultipoint(target, 128.0);
            Vec2f var3 = RotationUtils.getRotations(var2);
            float var4 = var3.x;
            float var5 = var3.y;
            float var6 = Math.abs(MathHelper.wrapDegrees(var4 - this.recoveredField3));
            boolean var7 = mc.player.getAttackCooldownProgress(1.0F) > 0.9F && this.recoveredField1.getWhiteRiseTicksToAttack() <= 1;
            if (!this.recoveredField6) {
               float var8 = 0.0055F;
               if (var6 > 60.0F) {
                  var8 += 0.028800001F;
               } else if (var6 > 30.0F) {
                  var8 += 0.014400001F;
               } else {
                  var8 += 0.0072000003F;
               }

               if (var7) {
                  var8 += 0.012857143F;
               }

               this.recoveredField5 = this.recoveredField5 + var8 * (1.6F + this.recoveredField8);
               if (this.recoveredField5 >= 0.22F) {
                  this.recoveredField6 = true;
               }
            } else {
               float var21 = var7 ? 0.045F : 0.008F;
               this.recoveredField5 = this.recoveredField5 - var21 * (2.1F + this.recoveredField8);
               if (this.recoveredField5 <= -0.04F) {
                  this.recoveredField6 = false;
               }
            }

            float var22 = MathHelper.clamp(this.recoveredField5, 0.0F, mc.player.isGliding() ? 0.38F : 0.26F);
            if (var7) {
               var22 = Math.min(var22 + 0.1F, mc.player.isGliding() ? 0.46F : 0.34F);
            }

            var22 += this.recoveredField8 * 0.5F;
            if (this.recoveredField9 % 7 == 0) {
               var22 += 0.03F;
            }

            float var9 = MathHelper.wrapDegrees(var4 - this.recoveredField3);
            float var10 = var5 - this.recoveredField4;
            float var11 = mc.player.isGliding() ? 42.0F : (var7 ? 28.0F : 20.0F);
            float var12 = mc.player.isGliding() ? 12.0F : (var7 ? 4.5F : 2.8F);
            var9 = MathHelper.clamp(var9, -var11, var11);
            var10 = MathHelper.clamp(var10, -var12, var12);
            float var13 = var22 * 0.28F;
            float var14 = var22 * (0.85F + this.recoveredField8 * 0.4F);
            float var15 = this.recoveredField3 + var9 * var14;
            float var16 = this.recoveredField4 + var10 * var13;
            float var17 = GCDUtil.getGCDValue();
            if (var17 > 0.0F) {
               var15 = this.recoveredField3 + Math.round((var15 - this.recoveredField3) / var17) * var17;
               var16 = this.recoveredField4 + Math.round((var16 - this.recoveredField4) / var17) * var17;
            }

            var16 = MathHelper.clamp(var16, -89.0F, 89.0F);
            Rotation var18 = new Rotation(var15, var16);
            float var19 = mc.player.isGliding() && target.isGliding() ? 360.0F : 45.0F;
            RotationStorage.update(var18, var19, var19, var19, var19, 0, 1, Aura.clientLook.isState());
            this.rotate = new Vec2f(var18.getYaw(), var18.getPitch());
            this.recoveredField3 = var18.getYaw();
            this.recoveredField4 = var18.getPitch();
         }
      }
   }

   private Vec3d recoveredMethod1(LivingEntity target) {
      Vec3d var2 = BestPoint.getPoint(target);
      if (var2 == null) {
         var2 = target.getBoundingBox().getCenter();
      }

      return this.shouldUseElytraPredict(target) ? this.getPredictedPoint(target, var2) : var2;
   }
}
