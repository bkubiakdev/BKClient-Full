package dev.bkclient.utility.rotations;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationPointUtil {
   private RotationPointUtil() {
   }

   public static float pitchBetween(Vec3d from, Vec3d to) {
      Vec3d delta = to.subtract(from);
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      return (float)(-Math.toDegrees(Math.atan2(delta.y, horizontal)));
   }

   public static Vec3d nearestPoint(Box box, Vec3d from) {
      return new Vec3d(
         MathHelper.clamp(from.x, box.minX, box.maxX),
         MathHelper.clamp(from.y, box.minY, box.maxY),
         MathHelper.clamp(from.z, box.minZ, box.maxZ)
      );
   }

   public static Vec3d bestPoint(Box box, Vec3d from, float pitch) {
      double centerX = (box.minX + box.maxX) * 0.5;
      double centerZ = (box.minZ + box.maxZ) * 0.5;
      double height = box.maxY - box.minY;
      if (height < 1.0E-4) {
         return new Vec3d(centerX, box.minY, centerZ);
      }

      double edge = Math.max(height * 0.06, 0.06);
      Vec3d top = new Vec3d(centerX, box.maxY - edge, centerZ);
      Vec3d bottom = new Vec3d(centerX, box.minY + edge, centerZ);
      float topPitch = pitchBetween(from, top);
      float bottomPitch = pitchBetween(from, bottom);
      double y;
      if (Math.abs(bottomPitch - topPitch) < 0.4F) {
         y = box.minY + height * 0.52;
      } else {
         float factor = (pitch - topPitch) / (bottomPitch - topPitch);
         factor = MathHelper.clamp(factor, 0.0F, 1.0F);
         y = box.maxY - edge + factor * (box.minY + edge - (box.maxY - edge));
      }

      return new Vec3d(centerX, y, centerZ);
   }

   public static Vec3d adjustPoint(Vec3d point, Vec3d from, Box box, float height, float yaw) {
      Vec3d delta = point.subtract(from);
      if (Math.hypot(delta.x, delta.z) >= 0.22) {
         return delta;
      }

      if (box != null) {
         Vec3d center = new Vec3d((box.minX + box.maxX) * 0.5, box.minY + height * 0.52, (box.minZ + box.maxZ) * 0.5)
            .subtract(from);
         if (Math.hypot(center.x, center.z) >= 0.22) {
            return center;
         }
      }

      double radians = Math.toRadians(yaw);
      return new Vec3d(-Math.sin(radians) * 0.22, delta.y, Math.cos(radians) * 0.22);
   }

   public static float yawTo(Vec3d delta) {
      return (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
   }

   public static float pitchTo(Vec3d delta) {
      return (float)(-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
   }
}
