package dev.bkclient.combatengine.api.storages.implement;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.events.EventInvoker;
import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventLook;
import dev.bkclient.combatengine.api.events.implement.EventRotation;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;

public class FreeLookStorage implements QClient {
   private static boolean recoveredField1;
   private static float recoveredField2;
   private static float recoveredField3;

   public FreeLookStorage() {
      EventInvoker.register(this);
   }

   public static boolean isActive() {
      return recoveredField1;
   }

   @EventLink
   public void onLook(EventLook event) {
      if (recoveredField1) {
         this.recoveredMethod1(event.getYaw(), event.getPitch());
         event.cancel();
      }
   }

   @EventLink
   public void onRotation(EventRotation event) {
      if (recoveredField1) {
         event.setYaw(recoveredField2);
         event.setPitch(recoveredField3);
      } else {
         recoveredField2 = event.getYaw();
         recoveredField3 = event.getPitch();
      }
   }

   private void recoveredMethod1(double targetYaw, double targetPitch) {
      recoveredField3 = MathHelper.clamp((float)(recoveredField3 + targetPitch * 0.15), -90.0F, 90.0F);
      recoveredField2 = (float)(recoveredField2 + targetYaw * 0.15);
   }

   @Generated
   public static void setActive(boolean active) {
      recoveredField1 = active;
   }

   @Generated
   public static float getFreeYaw() {
      return recoveredField2;
   }

   @Generated
   public static float getFreePitch() {
      return recoveredField3;
   }

   @Generated
   public static void setFreeYaw(float freeYaw) {
      recoveredField2 = freeYaw;
   }

   @Generated
   public static void setFreePitch(float freePitch) {
      recoveredField3 = freePitch;
   }
}
