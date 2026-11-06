package dev.bkclient.ui.clicksys.port.util;

import dev.bkclient.ui.clicksys.port.module.Feature;
import dev.bkclient.systems.modules.modules.visuals.esp.SimpleNametags;
import dev.bkclient.ui.clicksys.port.render.DepthState;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class ItemPickupMath {
   private static float value = Float.NaN;
   private static int value2 = -1;
   private static int value3 = -1;
   private static double value4 = -2.0;
   private static double value5 = 0.0;
   private static boolean flag = true;
   private static float value6 = Float.NaN;
   private static float value7 = Float.NaN;
   private static Vec3d vec3d = null;

   public static boolean isDoubleFloatVec3dBooleanCameraBooleanModulePlayerEntityPlayerEntity(
      double value, float value2, Vec3d vec3d, boolean flag, Camera camera, boolean flag2, SimpleNametags module2, PlayerEntity playerEntity, PlayerEntity playerEntity2
   ) {
      if (!isDoublePlayerEntityVec3dModuleBooleanBooleanPlayerEntity(value, playerEntity2, vec3d, module2, flag, flag2, playerEntity)) {
         return false;
      } else if (playerEntity == playerEntity2) {
         return true;
      } else if (playerEntity.squaredDistanceTo(playerEntity2) < 4.0) {
         return true;
      } else {
         Vec3d vec3dx = playerEntity.getPos();
         float f = playerEntity.getHeight();
         double d6 = vec3dx.y + f + 0.5;
         double d2 = vec3dx.z;
         double d1 = d6;
         double d0 = vec3dx.x;
         if (!isCameraDoubleFloatDoubleDouble(camera, d1, value2, d2, d0)) {
            d6 = vec3dx.y + f / 2.0;
            double d5 = vec3dx.z;
            double d4 = d6;
            double d3 = vec3dx.x;
            if (!isCameraDoubleFloatDoubleDouble(camera, d4, value2, d5, d3)) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean isDoublePlayerEntityVec3dModuleBooleanBooleanPlayerEntity(
      double value, PlayerEntity playerEntity, Vec3d vec3d, SimpleNametags module2, boolean flag, boolean flag2, PlayerEntity playerEntity2
   ) {
      if (playerEntity2 == playerEntity) {
         if (!flag2) {
            return false;
         }

         if (Feature.mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            return false;
         }
      }

      if (!playerEntity2.isAlive()) {
         return false;
      } else {
         Vec3d vec3dx = playerEntity2.getPos();
         return !DepthState.isVec3dDoubleVec3d(vec3dx, value, vec3d) ? false : flag || !module2.isFriend(playerEntity2);
      }
   }

   private static void setFloat(float value6) {
      int i = Feature.mc.getWindow().getFramebufferWidth();
      int j = Feature.mc.getWindow().getFramebufferHeight();
      if (value6 != value || i != value2 || j != value3) {
         double d0 = j > 0 ? (double)i / j : 1.7777777777777777;
         double d1 = Math.toRadians(value6);
         double d2 = Math.tan(d1 / 2.0);
         double d3 = 2.0 * Math.atan(d2 * d0);
         double d4 = Math.tan(d3 / 2.0);
         double d5 = 2.0 * Math.atan(Math.sqrt(d4 * d4 + d2 * d2));
         double d6 = d5 / 2.0 + Math.toRadians(10.0);
         double d7 = Math.cos(d6);
         value = value6;
         value2 = i;
         value3 = j;
         value4 = d7;
         value5 = d7 * d7;
         flag = d7 >= 0.0;
      }
   }

   public static boolean isVec3dFloatCamera(Vec3d vec3d, float value, Camera camera) {
      double d2 = vec3d.z;
      double d1 = vec3d.y;
      double d0 = vec3d.x;
      return isCameraDoubleFloatDoubleDouble(camera, d1, value, d2, d0);
   }

   public static boolean isCameraDoubleFloatDoubleDouble(Camera camera, double value, float value2, double value3, double value4) {
      Vec3d vec3dx = camera.getPos();
      double d0 = value4 - vec3dx.x;
      double d1 = value - vec3dx.y;
      double d2 = value3 - vec3dx.z;
      double d3 = d0 * d0 + d1 * d1 + d2 * d2;
      if (d3 < 1.0E-9) {
         return true;
      } else {
         Vec3d vec3d1 = getVec3dByCamera(camera);
         double d4 = d0 * vec3d1.x + d1 * vec3d1.y + d2 * vec3d1.z;
         setFloat(value2);
         return flag ? d4 >= 0.0 && d4 * d4 >= value5 * d3 : d4 >= 0.0 || d4 * d4 >= value5 * d3;
      }
   }

   private static Vec3d getVec3dByCamera(Camera camera) {
      float f = camera.getYaw();
      float f1 = camera.getPitch();
      if (vec3d != null && f == value6 && f1 == value7) {
         return vec3d;
      } else {
         value6 = f;
         value7 = f1;
         vec3d = Vec3d.fromPolar(f1, f);
         return vec3d;
      }
   }
}
