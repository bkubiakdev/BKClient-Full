package dev.bkclient.combatengine.api.events.implement;

import dev.bkclient.combatengine.api.events.Event;
import lombok.Generated;

public class EventLook extends Event {
   private double recoveredField1;
   private double recoveredField2;

   @Generated
   public double getYaw() {
      return this.recoveredField1;
   }

   @Generated
   public double getPitch() {
      return this.recoveredField2;
   }

   @Generated
   public void setYaw(double yaw) {
      this.recoveredField1 = yaw;
   }

   @Generated
   public void setPitch(double pitch) {
      this.recoveredField2 = pitch;
   }

   @Generated
   public EventLook(double yaw, double pitch) {
      this.recoveredField1 = yaw;
      this.recoveredField2 = pitch;
   }
}
