package dev.bkclient.combatengine.api.utils.rotate;

import dev.bkclient.combatengine.api.QClient;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class MultipointUtils implements QClient {
   public static Vec3d getClosestPoint(Entity entity) {
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

   @Generated
   private MultipointUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
