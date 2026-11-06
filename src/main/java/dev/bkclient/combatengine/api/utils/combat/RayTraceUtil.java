package dev.bkclient.combatengine.api.utils.combat;

import dev.bkclient.combatengine.api.QClient;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector3f;

public final class RayTraceUtil implements QClient {
   public static HitResult rayTrace(double rayTraceDistance, float yaw, float pitch, Entity entity) {
      Vec3d var5 = mc.player.getEyePos();
      Vec3d var6 = getVectorForRotation(pitch, yaw);
      Vec3d var7 = var5.add(var6.x * rayTraceDistance, var6.y * rayTraceDistance, var6.z * rayTraceDistance);
      return mc.world.raycast(new RaycastContext(var5, var7, ShapeType.OUTLINE, FluidHandling.NONE, entity));
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType) {
      return raycast(start, end, shapeType, mc.player);
   }

   public static BlockHitResult raycast(Vec3d start, Vec3d end, ShapeType shapeType, Entity entity) {
      return mc.world.raycast(new RaycastContext(start, end, shapeType, FluidHandling.NONE, entity));
   }

   public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
      Vec3d var4 = Objects.requireNonNull(mc.player).getEyePos();
      return box.contains(var4) || box.raycast(var4, var4.add(clientVec.multiply(range))).isPresent();
   }

   public static boolean isViewEntity(LivingEntity target, float yaw, float pitch, float distance, boolean ignoreWalls) {
      Entity var5 = mc.getCameraEntity();
      if (var5 != null && mc.world != null) {
         double var6 = distance * distance;
         Vec3d var8 = var5.getEyePos();
         Vector3f var9 = calculateViewVector(yaw, pitch);
         var9.mul(distance, distance, distance);
         Vec3d var10 = var8.add(var9.x, var9.y, var9.z);
         Box var11 = target.getBoundingBox();
         EntityHitResult var12 = ProjectileUtil.raycast(
            var5, var8, var10, var11, entityIn -> !entityIn.isSpectator() && entityIn.isAlive() && entityIn == target, var6
         );
         return var12 != null;
      } else {
         return false;
      }
   }

   public static Vector3f calculateViewVector(float yaw, float pitch) {
      float var2 = pitch * (float) (Math.PI / 180.0);
      float var3 = -yaw * (float) (Math.PI / 180.0);
      float var4 = MathHelper.cos(var3);
      float var5 = MathHelper.sin(var3);
      float var6 = MathHelper.cos(var2);
      float var7 = MathHelper.sin(var2);
      return new Vector3f(var5 * var6, -var7, var4 * var6);
   }

   public static Vec3d getVectorForRotation(float pitch, float yaw) {
      float var2 = -yaw * (float) (Math.PI / 180.0) - (float) Math.PI;
      float var3 = -pitch * (float) (Math.PI / 180.0);
      float var4 = MathHelper.cos(var2);
      float var5 = MathHelper.sin(var2);
      float var6 = -MathHelper.cos(var3);
      float var7 = MathHelper.sin(var3);
      return new Vec3d(var5 * var6, var7, var4 * var6);
   }

   public static boolean rayTraceSingleEntity(float yaw, float pitch, double distance, Entity entity) {
      Vec3d var5 = mc.player.getEyePos();
      Vec3d var6 = mc.player.getRotationVector(pitch, yaw);
      Vec3d var7 = var5.add(var6.multiply(distance));
      Box var8 = entity.getBoundingBox();
      return var8.contains(var5) || var8.raycast(var5, var7).isPresent();
   }

   @Generated
   private RayTraceUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
