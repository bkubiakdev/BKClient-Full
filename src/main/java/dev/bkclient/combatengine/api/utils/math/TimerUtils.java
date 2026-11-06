package dev.bkclient.combatengine.api.utils.math;

import lombok.Generated;

public class TimerUtils {
   private long recoveredField1;

   public TimerUtils() {
      this.reset();
   }

   public boolean finished(float delay) {
      return (float)System.currentTimeMillis() - delay >= (float)this.recoveredField1;
   }

   public boolean finished(long delay) {
      return System.currentTimeMillis() - this.recoveredField1 >= delay;
   }

   public void reset() {
      this.recoveredField1 = System.currentTimeMillis();
   }

   public long getElapsedTime() {
      return System.currentTimeMillis() - this.recoveredField1;
   }

   @Generated
   public long getMillis() {
      return this.recoveredField1;
   }

   @Generated
   public void setMillis(long millis) {
      this.recoveredField1 = millis;
   }
}
