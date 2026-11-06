package dev.bkclient.combatengine.api.events.implement;

import dev.bkclient.combatengine.api.events.Event;
import lombok.Generated;

public class EventMoveInput extends Event {
   private float recoveredField1;
   private float recoveredField2;
   private boolean recoveredField3;
   private boolean recoveredField4;
   private boolean recoveredField5;

   @Generated
   public float getForward() {
      return this.recoveredField1;
   }

   @Generated
   public float getStrafe() {
      return this.recoveredField2;
   }

   @Generated
   public boolean isJump() {
      return this.recoveredField3;
   }

   @Generated
   public boolean isSneak() {
      return this.recoveredField4;
   }

   @Generated
   public boolean isSprint() {
      return this.recoveredField5;
   }

   @Generated
   public void setForward(float forward) {
      this.recoveredField1 = forward;
   }

   @Generated
   public void setStrafe(float strafe) {
      this.recoveredField2 = strafe;
   }

   @Generated
   public void setJump(boolean jump) {
      this.recoveredField3 = jump;
   }

   @Generated
   public void setSneak(boolean sneak) {
      this.recoveredField4 = sneak;
   }

   @Generated
   public void setSprint(boolean sprint) {
      this.recoveredField5 = sprint;
   }

   @Generated
   public EventMoveInput(float forward, float strafe, boolean jump, boolean sneak, boolean sprint) {
      this.recoveredField1 = forward;
      this.recoveredField2 = strafe;
      this.recoveredField3 = jump;
      this.recoveredField4 = sneak;
      this.recoveredField5 = sprint;
   }
}
