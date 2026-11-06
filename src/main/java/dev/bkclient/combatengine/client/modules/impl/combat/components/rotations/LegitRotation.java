package dev.bkclient.combatengine.client.modules.impl.combat.components.rotations;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.events.EventInvoker;
import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventLook;
import dev.bkclient.combatengine.client.modules.impl.combat.Aura;
import dev.bkclient.combatengine.client.modules.impl.combat.components.RotationsSystem;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class LegitRotation extends RotationsSystem implements QClient {
   private static final double recoveredField1 = 0.15;
   private static final double recoveredField2 = 0.5;
   private static final double recoveredField3 = 0.15;
   private static final double recoveredField4 = 5.0;
   private static final double recoveredField5 = 3.2;
   private static final float recoveredField6 = 0.09F;
   private static final float recoveredField7 = 0.005F;
   private static final float recoveredField8 = 0.16F;
   private static final float recoveredField9 = 0.0055F;
   private static final float recoveredField10 = 0.1F;
   private static final float recoveredField11 = 0.0035F;
   private final Random recoveredField12 = new Random();
   private LivingEntity recoveredField13;
   private long recoveredField14;
   private float recoveredField15;
   private float recoveredField16;
   private float recoveredField17;
   private float recoveredField18;
   private float recoveredField19;
   private float recoveredField20;

   public LegitRotation() {
      EventInvoker.register(this);
   }

   @Override
   public void updateRotations(LivingEntity target) {
      this.recoveredField13 = target;
      this.recoveredField14 = System.nanoTime();
   }

   public void reset() {
      this.recoveredField13 = null;
      this.recoveredField15 = 0.0F;
      this.recoveredField16 = 0.0F;
   }

   @EventLink(priority = -100)
   public void onLook(EventLook event) {
      if (!event.isCancelled()) {
         if (mc.player != null && mc.world != null && mc.currentScreen == null) {
            if (this.recoveredMethod4()) {
               LivingEntity var2 = this.recoveredMethod3();
               if (var2 == null) {
                  this.recoveredField15 = 0.0F;
                  this.recoveredField16 = 0.0F;
               } else {
                  double var3 = event.getYaw();
                  double var5 = event.getPitch();
                  boolean var7 = Math.abs(var3) >= 0.5 || Math.abs(var5) >= 0.5;
                  if (!var7) {
                     Vec3d var32 = this.getPredictedBox(var2).getCenter();
                     Vec3d var33 = mc.player.getEyePos();
                     double var34 = var32.x - var33.x;
                     double var35 = var32.y - var33.y;
                     double var36 = var32.z - var33.z;
                     float var37 = (float)(Math.toDegrees(Math.atan2(var36, var34)) - 90.0);
                     float var38 = MathHelper.wrapDegrees(var37 - mc.player.getYaw());
                     if (Math.abs(var38) > 3.2) {
                        this.recoveredField16 = 0.0F;
                     } else {
                        this.recoveredField16 = Math.min(this.recoveredField16 + 0.005F, 0.09F);
                        this.recoveredMethod1(event, var38 * this.recoveredField16);
                     }
                  } else {
                     this.recoveredField16 = 0.0F;
                     Vec3d var8 = this.recoveredMethod2(var2);
                     Vec3d var9 = mc.player.getEyePos();
                     double var10 = var8.x - var9.x;
                     double var12 = var8.z - var9.z;
                     float var14 = (float)(Math.toDegrees(Math.atan2(var12, var10)) - 90.0);
                     float var15 = MathHelper.wrapDegrees(var14 - mc.player.getYaw());
                     float var16 = Math.abs(var15);
                     if (var16 < 0.15) {
                        this.recoveredField15 = 0.0F;
                     } else {
                        boolean var17 = var3 * var15 > 0.0;
                        if (!var17) {
                           this.recoveredField15 *= 0.9F;
                           float var39 = 1.0F - Math.min(1.0F, (float)(var16 / 3.2));
                           float var40 = 0.75F - 0.3F * var39;
                           event.setYaw(var3 * var40);
                        } else {
                           this.recoveredField18 = this.recoveredField18 + (0.05F + this.recoveredField12.nextFloat() * 0.04F);
                           float var18 = var16 < 5.0 ? 0.1F : 0.16F;
                           float var19 = var16 < 5.0 ? 0.0035F : 0.0055F;
                           this.recoveredField15 = Math.min(this.recoveredField15 + var19, var18);
                           float var20 = (float)(Math.sin(this.recoveredField18 * 0.5) * 0.0035);
                           float var21 = Math.max(0.0F, this.recoveredField15 + var20);
                           double var22 = var15 * var21;
                           double var24 = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
                           double var26 = var24 * var24 * var24;
                           double var28 = var26 * 1.2;
                           long var30 = Math.round(var22 / var28);
                           if (var30 != 0L) {
                              event.setYaw(var3 + var30 * var26 * 8.0);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void recoveredMethod1(EventLook event, double desiredYawDeg) {
      double var4 = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
      double var6 = var4 * var4 * var4;
      double var8 = var6 * 1.2;
      long var10 = Math.round(desiredYawDeg / var8);
      if (var10 != 0L) {
         event.setYaw(event.getYaw() + var10 * var6 * 8.0);
      }
   }

   private Vec3d recoveredMethod2(LivingEntity entity) {
      this.recoveredField17 = this.recoveredField17 + (0.05F + this.recoveredField12.nextFloat() * 0.04F);
      Box var2 = this.getPredictedBox(entity);
      float var3 = (float)(Math.sin(this.recoveredField17 * 0.3) * 0.2 + 0.5);
      this.recoveredField19 = this.recoveredField19 + (this.recoveredField12.nextFloat() - 0.5F) * 0.35F;
      if (this.recoveredField19 > (float) (Math.PI * 2)) {
         this.recoveredField19 -= (float) (Math.PI * 2);
      }

      if (this.recoveredField19 < 0.0F) {
         this.recoveredField19 += (float) (Math.PI * 2);
      }

      float var4 = this.recoveredField12.nextFloat() * (float)var2.getLengthX() * 0.5F;
      this.recoveredField20 = this.recoveredField20 + (var4 - this.recoveredField20) * 0.12F;
      Vec3d var5 = var2.getCenter();
      double var6 = var5.x + Math.cos(this.recoveredField19) * this.recoveredField20;
      double var8 = var5.z + Math.sin(this.recoveredField19) * this.recoveredField20;
      double var10 = var2.minY + var2.getLengthY() * var3;
      return new Vec3d(var6, var10, var8);
   }

   private LivingEntity recoveredMethod3() {
      if (this.recoveredField13 != null && this.recoveredField13.isAlive() && !this.recoveredField13.isRemoved()) {
         return System.nanoTime() - this.recoveredField14 > 400000000L ? null : this.recoveredField13;
      } else {
         return null;
      }
   }

   private boolean recoveredMethod4() {
      Aura var1 = Aura.INSTANCE;
      return var1 != null && var1.isEnable() && var1.rotationType.is("HolyLegit");
   }
}
