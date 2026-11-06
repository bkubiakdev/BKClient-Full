package dev.bkclient.ui.clicksys.port.render;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
/** Player position and range math ported from BKClient. */
public final class DepthState {
   public static boolean isVec3dDoubleVec3d(Vec3d vec3d, double value, Vec3d vec3d2) {
      double d0 = value * value;
      return vec3d.squaredDistanceTo(vec3d2) <= d0;
   }

   public static Vec3d getVec3dByFloatEntityVec3d(float value, Entity entity2, Vec3d vec3d) {
      double d0 = MathHelper.lerp(value, entity2.prevX, entity2.getX()) - vec3d.x;
      double d1 = MathHelper.lerp(value, entity2.prevY, entity2.getY()) - vec3d.y;
      double d2 = MathHelper.lerp(value, entity2.prevZ, entity2.getZ()) - vec3d.z;
      return new Vec3d(d0, d1, d2);
   }

}
