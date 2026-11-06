package dev.bkclient.systems.event.impl.window;

import lombok.Generated;
import dev.bkclient.systems.event.EventCancellable;

public class MouseEvent extends EventCancellable {
   private final int button;
   private final int action;

   @Generated
   public int getButton() {
      return this.button;
   }

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public MouseEvent(int button, int action) {
      this.button = button;
      this.action = action;
   }
}
