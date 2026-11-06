package dev.bkclient.combatengine.client.modules.impl.combat.components.interpolation;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.utils.combat.RayTraceUtil;
import dev.bkclient.combatengine.api.utils.math.MathUtils;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import dev.bkclient.combatengine.api.utils.rotate.RotationUtils;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;

public final class BestPoint implements QClient {
   private static Vec3d recoveredField1 = Vec3d.ZERO;
   private static Vec3d recoveredField2 = Vec3d.ZERO;

   public static Vec3d getRotationPoint() {
      return recoveredField1;
   }

   public static Vec3d getNearestPoint(Entity entity) {
      Box var1 = entity.getBoundingBox();
      double var2 = 0.1;
      Vec3d var4 = null;
      double var5 = Double.MAX_VALUE;

      for (double var7 = var1.minX; var7 <= var1.maxX; var7 += var2) {
         for (double var9 = var1.minY; var9 <= var1.maxY; var9 += var2) {
            for (double var11 = var1.minZ; var11 <= var1.maxZ; var11 += var2) {
               Vec3d var13 = new Vec3d(var7, var9, var11);
               double var14 = mc.player.getEyePos().distanceTo(var13);
               if (var14 < var5) {
                  var5 = var14;
                  var4 = var13;
               }
            }
         }
      }

      return var4;
   }

   public static Vec3d getPoint(Entity target) {
      Box var1 = target.getBoundingBox();
      double var2 = var1.maxX - var1.minX;
      double var4 = var1.maxY - var1.minY;
      double var6 = var1.maxZ - var1.minZ;
      double var8 = var1.minX + var2 / 2.0;
      double var10 = var1.minY + var4 * 0.7;
      double var12 = var1.minZ + var6 / 2.0;
      double var14 = System.currentTimeMillis() / 50.0;
      int var16 = target.getId();
      double var17 = Math.sin(var14 + var16) * (var2 * 0.45);
      double var19 = Math.cos(var14 * 0.8 + var16) * (var4 * 0.1);
      double var21 = Math.cos(var14 * 1.2 + var16) * (var6 * 0.45);
      return new Vec3d(var8 + var17, var10 + var19, var12 + var21);
   }

   public static Vec3d getPoint2(Entity target) {
      Box var1 = target.getBoundingBox();
      double var2 = var1.maxX - var1.minX;
      double var4 = var1.maxY - var1.minY;
      double var6 = var1.maxZ - var1.minZ;
      double var8 = var1.minX + var2 / 2.0;
      double var10 = var1.minY + var4 * 0.65;
      double var12 = var1.minZ + var6 / 2.0;
      double var14 = System.currentTimeMillis() / 65.0;
      int var16 = target.getId();
      double var17 = Math.sin(var14 + var16) * (var2 * 0.7);
      double var19 = Math.cos(var14 * 0.8 + var16) * (var4 * 0.4);
      double var21 = Math.cos(var14 * 1.2 + var16) * (var6 * 0.7);
      return new Vec3d(var8 + var17, var10 + var19, var12 + var21);
   }

   public static Vec3d getNearestVisiblePoint(Entity target, Vec3d preferredPoint, double range) {
      if (preferredPoint != null && mc.player != null && mc.world != null) {
         if (recoveredMethod1(target, preferredPoint, range)) {
            return preferredPoint;
         }

         Box var4 = target.getBoundingBox();
         double var5 = 0.12;
         Vec3d var7 = null;
         double var8 = Double.MAX_VALUE;

         for (double var10 = var4.minX; var10 <= var4.maxX; var10 += var5) {
            for (double var12 = var4.minY; var12 <= var4.maxY; var12 += var5) {
               for (double var14 = var4.minZ; var14 <= var4.maxZ; var14 += var5) {
                  Vec3d var16 = new Vec3d(var10, var12, var14);
                  if (recoveredMethod1(target, var16, range)) {
                     double var17 = var16.squaredDistanceTo(preferredPoint);
                     if (var17 < var8) {
                        var8 = var17;
                        var7 = var16;
                     }
                  }
               }
            }
         }

         return var7 != null ? var7 : preferredPoint;
      } else {
         return preferredPoint;
      }
   }

   private static boolean recoveredMethod1(Entity target, Vec3d point, double range) {
      Vec3d var4 = mc.player.getEyePos();
      double var5 = var4.distanceTo(point);
      if (var5 > range) {
         return false;
      }

      Vec3d var7 = point.subtract(var4).normalize();
      if (!RayTraceUtil.rayTrace(var7, var5 + 0.2, target.getBoundingBox())) {
         return false;
      }

      BlockHitResult var8 = RayTraceUtil.raycast(var4, point, ShapeType.COLLIDER, mc.player);
      return var8.getType() == Type.MISS || var4.squaredDistanceTo(var8.getPos()) >= var4.squaredDistanceTo(point) - 1.0E-4;
   }

   public static Vec3d getMultipoint(Entity target, double distance) {
      float var3 = 0.005F;
      float var4 = 0.015F;
      float var5 = 0.0015F;
      float var6 = 0.015F;
      double var7 = target.getBoundingBox().getLengthX();
      double var9 = target.getBoundingBox().getLengthY();
      double var11 = target.getBoundingBox().getLengthZ();
      if (recoveredField2.equals(Vec3d.ZERO)) {
         recoveredField2 = new Vec3d(MathUtils.randomBest(-0.02F, 0.02F), MathUtils.randomBest(-0.02F, 0.02F), MathUtils.randomBest(-0.02F, 0.02F));
      }

      if (recoveredField1.equals(Vec3d.ZERO)) {
         recoveredField1 = new Vec3d(0.0, var9 * 0.5, 0.0);
      }

      recoveredField1 = recoveredField1.add(recoveredField2);
      double var13 = (var7 - 0.1) / 2.0;
      double var15 = (var11 - 0.1) / 2.0;
      if (recoveredField1.x >= var13) {
         recoveredField2 = new Vec3d(-MathUtils.randomBest(var3, var4), recoveredField2.getY(), recoveredField2.getZ());
      } else if (recoveredField1.x <= -var13) {
         recoveredField2 = new Vec3d(MathUtils.randomBest(var3, var4), recoveredField2.getY(), recoveredField2.getZ());
      }

      if (recoveredField1.y >= var9 * 0.75) {
         recoveredField2 = new Vec3d(recoveredField2.getX(), -MathUtils.randomBest(var5, var6), recoveredField2.getZ());
      } else if (recoveredField1.y <= var9 * 0.3) {
         recoveredField2 = new Vec3d(recoveredField2.getX(), MathUtils.randomBest(var5, var6), recoveredField2.getZ());
      }

      if (recoveredField1.z >= var15) {
         recoveredField2 = new Vec3d(recoveredField2.getX(), recoveredField2.getY(), -MathUtils.randomBest(var3, var4));
      } else if (recoveredField1.z <= -var15) {
         recoveredField2 = new Vec3d(recoveredField2.getX(), recoveredField2.getY(), MathUtils.randomBest(var3, var4));
      }

      recoveredField1.add(MathUtils.randomBest(-0.05F, 0.05F), 0.0, MathUtils.randomBest(-0.05F, 0.05F));
      if (!RayTraceUtil.rayTrace(mc.player.getRotationVector(), distance, target.getBoundingBox())) {
         float var18 = (float)(var7 / 2.0) * 0.8F;

         for (float var19 = -var18; var19 <= var18; var19 += 0.1F) {
            for (float var20 = -var18; var20 <= var18; var20 += 0.1F) {
               for (float var21 = (float)(var9 * 0.9); var21 >= var9 * 0.3; var21 -= 0.1F) {
                  Vec3d var22 = new Vec3d(target.getX() + var19, target.getY() + var21, target.getZ() + var20);
                  Rotation var17 = RotationUtils.fromVec3d(var22);
                  if (RayTraceUtil.rayTrace(var17.toVector(), distance, target.getBoundingBox())) {
                     recoveredField1 = new Vec3d(var19, var21, var20);
                     return target.getPos().add(recoveredField1);
                  }
               }
            }
         }
      }

      return target.getPos().add(recoveredField1);
   }

   @Generated
   private BestPoint() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
