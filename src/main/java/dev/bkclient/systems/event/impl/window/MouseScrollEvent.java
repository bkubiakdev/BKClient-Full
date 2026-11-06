package dev.bkclient.systems.event.impl.window;

import lombok.Generated;
import dev.bkclient.systems.event.EventCancellable;

public class MouseScrollEvent extends EventCancellable {
   private final double verticalAmount;

   @Generated
   public double getVerticalAmount() {
      return this.verticalAmount;
   }

   @Generated
   public MouseScrollEvent(double verticalAmount) {
      this.verticalAmount = verticalAmount;
   }
}
