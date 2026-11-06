package dev.bkclient.combatengine.api.events.implement;

import dev.bkclient.combatengine.api.events.Event;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;

public class EventKeyboardInput extends Event {
   private float recoveredField1;
   private float recoveredField2;

   public void setYaw(float yaw, float yaw2) {
      float var3 = this.getMovementForward();
      float var4 = this.getMovementSideways();
      double var5 = MathHelper.wrapDegrees(Math.toDegrees(this.recoveredMethod1(yaw2, var3, var4)));
      if (var3 != 0.0F || var4 != 0.0F) {
         float var7 = 0.0F;
         float var8 = 0.0F;
         float var9 = Float.MAX_VALUE;

         for (float var10 = -1.0F; var10 <= 1.0F; var10++) {
            for (float var11 = -1.0F; var11 <= 1.0F; var11++) {
               if (var11 != 0.0F || var10 != 0.0F) {
                  double var12 = MathHelper.wrapDegrees(Math.toDegrees(this.recoveredMethod1(yaw, var10, var11)));
                  double var14 = Math.abs(var5 - var12);
                  if (var14 < var9) {
                     var9 = (float)var14;
                     var7 = var10;
                     var8 = var11;
                  }
               }
            }
         }

         this.setMovementForward(var7);
         this.setMovementSideways(var8);
      }
   }

   private double recoveredMethod1(float yaw, double movementForward, double movementSideways) {
      if (movementForward < 0.0) {
         yaw += 180.0F;
      }

      float var6 = 1.0F;
      if (movementForward < 0.0) {
         var6 = -0.5F;
      } else if (movementForward > 0.0) {
         var6 = 0.5F;
      }

      if (movementSideways > 0.0) {
         yaw -= 90.0F * var6;
      }

      if (movementSideways < 0.0) {
         yaw += 90.0F * var6;
      }

      return Math.toRadians(yaw);
   }

   @Generated
   public EventKeyboardInput(float movementForward, float movementSideways) {
      this.recoveredField1 = movementForward;
      this.recoveredField2 = movementSideways;
   }

   @Generated
   public float getMovementForward() {
      return this.recoveredField1;
   }

   @Generated
   public float getMovementSideways() {
      return this.recoveredField2;
   }

   @Generated
   public void setMovementForward(float movementForward) {
      this.recoveredField1 = movementForward;
   }

   @Generated
   public void setMovementSideways(float movementSideways) {
      this.recoveredField2 = movementSideways;
   }
}
