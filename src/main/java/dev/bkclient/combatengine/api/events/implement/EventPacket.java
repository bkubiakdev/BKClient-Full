package dev.bkclient.combatengine.api.events.implement;

import dev.bkclient.combatengine.api.events.Event;
import lombok.Generated;
import net.minecraft.network.packet.Packet;

public class EventPacket extends Event {
   private final Packet<?> recoveredField1;
   private final EventPacket.Type recoveredField2;

   @Generated
   public EventPacket(Packet<?> packet, EventPacket.Type type) {
      this.recoveredField1 = packet;
      this.recoveredField2 = type;
   }

   @Generated
   public Packet<?> getPacket() {
      return this.recoveredField1;
   }

   @Generated
   public EventPacket.Type getType() {
      return this.recoveredField2;
   }

   public enum Type {
      SEND,
      RECEIVE;
   }
}
