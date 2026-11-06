package dev.bkclient.combatengine.api.utils.rotate;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.client.modules.impl.combat.components.gcd.GCDUtil;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.jetbrains.annotations.NotNull;

public final class RotationUtils implements QClient {
   public static HitResult rayTrace(double dst, float yaw, float pitch) {
      Vec3d var4 = mc.player.getCameraPosVec(1.0F);
      Vec3d var5 = getRotationVector(pitch, yaw);
      Vec3d var6 = var4.add(var5.x * dst, var5.y * dst, var5.z * dst);
      return mc.world.raycast(new RaycastContext(var4, var6, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
   }

   static Vec3d getBestVector(Entity entity) {
      Vec3d var1 = mc.player.getEyePos();
      Box var2 = entity.getBoundingBox();
      double var3 = 0.1;
      Vec3d var5 = null;
      double var6 = Double.MAX_VALUE;

      for (double var8 = var2.minX; var8 <= var2.maxX; var8 += var3) {
         for (double var10 = var2.minY; var10 <= var2.maxY; var10 += var3) {
            for (double var12 = var2.minZ; var12 <= var2.maxZ; var12 += var3) {
               Vec3d var14 = new Vec3d(var8, var10, var12);
               double var15 = var1.distanceTo(var14);
               if (var15 < var6) {
                  var6 = var15;
                  var5 = var14;
               }
            }
         }
      }

      return var5;
   }

   public static Rotation fromVec3d(Vec3d vector) {
      return new Rotation(
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0),
         (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
      );
   }

   @NotNull
   public static Vec3d getRotationVector(float yaw, float pitch) {
      return new Vec3d(
         MathHelper.sin(-pitch * (float) (Math.PI / 180.0)) * MathHelper.cos(yaw * (float) (Math.PI / 180.0)),
         -MathHelper.sin(yaw * (float) (Math.PI / 180.0)),
         MathHelper.cos(-pitch * (float) (Math.PI / 180.0)) * MathHelper.cos(yaw * (float) (Math.PI / 180.0))
      );
   }

   public static Vec2f getRotations(Entity entity) {
      return getRotations(entity.getX(), entity.getY(), entity.getZ());
   }

   public static Vec2f getRotations(Vec3d vec3d) {
      return getRotations(vec3d.x, vec3d.y, vec3d.z);
   }

   public static Vec2f getRotations(double x, double y, double z) {
      double var6 = x - mc.player.getX();
      double var8 = y - mc.player.getEyeY();
      double var10 = z - mc.player.getZ();
      double var12 = MathHelper.sqrt((float)(var6 * var6 + var10 * var10));
      float var14 = (float)(MathHelper.atan2(var10, var6) * (180.0 / Math.PI) - 90.0);
      float var15 = (float)(-MathHelper.atan2(var8, var12) * (180.0 / Math.PI));
      return new Vec2f(var14, var15);
   }

   public static float[] getRotations(Direction direction) {
      return switch (direction) {
         case DOWN -> new float[]{mc.player.getYaw(), 90.0F};
         case UP -> new float[]{mc.player.getYaw(), -90.0F};
         case NORTH -> new float[]{180.0F, mc.player.getPitch()};
         case SOUTH -> new float[]{0.0F, mc.player.getPitch()};
         case WEST -> new float[]{90.0F, mc.player.getPitch()};
         case EAST -> new float[]{-90.0F, mc.player.getPitch()};
      };
   }

   public static float[] correctRotation(float[] rotations) {
      rotations[0] -= rotations[0] % GCDUtil.getGCDValue();
      rotations[1] -= rotations[1] % GCDUtil.getGCDValue();
      return new float[]{rotations[0], rotations[1]};
   }

   public static float getFixRotate(float rot) {
      return getDeltaMouse(rot) * GCDUtil.getGCDValue();
   }

   public static float getDeltaMouse(float delta) {
      return Math.round(delta / GCDUtil.getGCDValue());
   }

   @Generated
   private RotationUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
