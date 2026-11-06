package dev.bkclient.systems.event.impl.render;

import lombok.Generated;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.systems.event.Event;

public class ChatRenderEvent extends Event {
   private final CustomDrawContext context;
   private final float tickDelta;

   @Generated
   public CustomDrawContext getContext() {
      return this.context;
   }

   @Generated
   public float getTickDelta() {
      return this.tickDelta;
   }

   @Generated
   public ChatRenderEvent(CustomDrawContext context, float tickDelta) {
      this.context = context;
      this.tickDelta = tickDelta;
   }
}
