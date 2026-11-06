package dev.bkclient.systems.event.impl.window;

import lombok.Generated;
import dev.bkclient.systems.event.EventCancellable;

public class KeyPressEvent extends EventCancellable {
   private final int action;
   private final int key;

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   public KeyPressEvent(int action, int key) {
      this.action = action;
      this.key = key;
   }
}
