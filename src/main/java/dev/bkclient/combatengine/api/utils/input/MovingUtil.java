package dev.bkclient.combatengine.api.utils.input;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.events.implement.EventMoveInput;
import dev.bkclient.combatengine.api.storages.implement.FreeLookStorage;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MovingUtil implements QClient {
   public static boolean hasPlayerMovement() {
      return mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F;
   }

   public static double[] calculateDirection(double distance) {
      float var2 = mc.player.input.movementForward;
      float var3 = mc.player.input.movementSideways;
      float var4 = mc.player.getYaw();
      if (var2 != 0.0F) {
         if (var3 > 0.0F) {
            var4 += var2 > 0.0F ? -45.0F : 45.0F;
         } else if (var3 < 0.0F) {
            var4 += var2 > 0.0F ? 45.0F : -45.0F;
         }

         var3 = 0.0F;
         var2 = var2 > 0.0F ? 1.0F : -1.0F;
      }

      double var5 = Math.sin(Math.toRadians(var4 + 90.0F));
      double var7 = Math.cos(Math.toRadians(var4 + 90.0F));
      double var9 = var2 * distance * var7 + var3 * distance * var5;
      double var11 = var2 * distance * var5 - var3 * distance * var7;
      return new double[]{var9, var11};
   }

   public static double getSpeedSqrt(Entity entity) {
      double var1 = entity.getX() - entity.prevX;
      double var3 = entity.getY() - entity.prevY;
      double var5 = entity.getZ() - entity.prevZ;
      return Math.sqrt(var1 * var1 + var5 * var5 + var3 * var3);
   }

   public static void setVelocity(double velocity) {
      double[] var2 = calculateDirection(velocity);
      Objects.requireNonNull(mc.player).setVelocity(var2[0], mc.player.getVelocity().getY(), var2[1]);
   }

   public static void setVelocity(double velocity, double y) {
      double[] var4 = calculateDirection(velocity);
      Objects.requireNonNull(mc.player).setVelocity(var4[0], y, var4[1]);
   }

   public static double getDegreesRelativeToView(Vec3d positionRelativeToPlayer, float yaw) {
      float var2 = (float)Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
      double var3 = Math.toRadians(MathHelper.wrapDegrees(yaw));
      return Math.toDegrees(MathHelper.wrapDegrees(var2 - var3));
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
      boolean var4 = input.forward();
      boolean var5 = input.backward();
      boolean var6 = input.left();
      boolean var7 = input.right();
      if (dgs >= -90.0F + deadAngle && dgs <= 90.0F - deadAngle) {
         var4 = true;
      } else if (dgs < -90.0F - deadAngle || dgs > 90.0F + deadAngle) {
         var5 = true;
      }

      if (dgs >= 0.0F + deadAngle && dgs <= 180.0F - deadAngle) {
         var7 = true;
      } else if (dgs >= -180.0F + deadAngle && dgs <= 0.0F - deadAngle) {
         var6 = true;
      }

      return new PlayerInput(var4, var5, var6, var7, input.jump(), input.sneak(), input.sprint());
   }

   public static void fixMovementFocus(EventMoveInput event, float yaw) {
      float var2 = event.getForward();
      float var3 = event.getStrafe();
      if (var2 != 0.0F || var3 != 0.0F) {
         double var4 = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, var2, var3)));
         float var6 = 0.0F;
         float var7 = 0.0F;
         float var8 = Float.MAX_VALUE;

         for (float var9 = -1.0F; var9 <= 1.0F; var9++) {
            for (float var10 = -1.0F; var10 <= 1.0F; var10++) {
               if (var9 != 0.0F || var10 != 0.0F) {
                  double var11 = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, var9, var10)));
                  float var13 = Math.abs(MathHelper.wrapDegrees((float)(var4 - var11)));
                  if (var13 < var8) {
                     var8 = var13;
                     var6 = var9;
                     var7 = var10;
                  }
               }
            }
         }

         event.setForward(var6);
         event.setStrafe(var7);
      }
   }

   public static void fixMovementFree(EventMoveInput event) {
      float var1 = event.getForward();
      float var2 = event.getStrafe();
      double var3 = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.isGliding() ? mc.player.getYaw() : FreeLookStorage.getFreeYaw(), var1, var2)));
      if (var1 != 0.0F || var2 != 0.0F) {
         float var5 = 0.0F;
         float var6 = 0.0F;
         float var7 = Float.MAX_VALUE;

         for (float var8 = -1.0F; var8 <= 1.0F; var8++) {
            for (float var9 = -1.0F; var9 <= 1.0F; var9++) {
               if (var9 != 0.0F || var8 != 0.0F) {
                  double var10 = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.getYaw(), var8, var9)));
                  double var12 = Math.abs(var3 - var10);
                  if (var12 < var7) {
                     var7 = (float)var12;
                     var5 = var8;
                     var6 = var9;
                  }
               }
            }
         }

         event.setForward(var5);
         event.setStrafe(var6);
      }
   }

   public static double direction(float rotationYaw, float moveForward, float moveStrafing) {
      if (moveForward < 0.0F) {
         rotationYaw += 180.0F;
      }

      float var3 = 1.0F;
      if (moveForward < 0.0F) {
         var3 = -0.5F;
      }

      if (moveForward > 0.0F) {
         var3 = 0.5F;
      }

      if (moveStrafing > 0.0F) {
         rotationYaw -= 90.0F * var3;
      }

      if (moveStrafing < 0.0F) {
         rotationYaw += 90.0F * var3;
      }

      return Math.toRadians(rotationYaw);
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
      return getDirectionalInputForDegrees(input, dgs, 20.0F);
   }

   @Generated
   private MovingUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
