package dev.bkclient.combatengine.api.events.implement;

import dev.bkclient.combatengine.api.events.Event;
import lombok.Generated;

public class EventRotation extends Event {
   private float recoveredField1;
   private float recoveredField2;
   private float recoveredField3;

   @Generated
   public float getYaw() {
      return this.recoveredField1;
   }

   @Generated
   public float getPitch() {
      return this.recoveredField2;
   }

   @Generated
   public float getPartialTicks() {
      return this.recoveredField3;
   }

   @Generated
   public void setYaw(float yaw) {
      this.recoveredField1 = yaw;
   }

   @Generated
   public void setPitch(float pitch) {
      this.recoveredField2 = pitch;
   }

   @Generated
   public void setPartialTicks(float partialTicks) {
      this.recoveredField3 = partialTicks;
   }

   @Generated
   public EventRotation(float yaw, float pitch, float partialTicks) {
      this.recoveredField1 = yaw;
      this.recoveredField2 = pitch;
      this.recoveredField3 = partialTicks;
   }
}
