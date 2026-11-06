package dev.bkclient.combatengine.api.events;

import java.lang.reflect.InvocationTargetException;
import lombok.Generated;

public class Event {
   private boolean recoveredField1;

   public void cancel() {
      this.recoveredField1 = true;
   }

   public void call() {
      try {
         EventInvoker.invoke(this);
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException var2) {
         throw new RuntimeException("Failed to Invoke Method", var2);
      }
   }

   @Generated
   public void setCancelled(boolean cancelled) {
      this.recoveredField1 = cancelled;
   }

   @Generated
   public boolean isCancelled() {
      return this.recoveredField1;
   }
}
