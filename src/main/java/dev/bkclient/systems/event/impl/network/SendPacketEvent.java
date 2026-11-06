package dev.bkclient.systems.event.impl.network;

import lombok.Generated;
import dev.bkclient.systems.event.EventCancellable;
import net.minecraft.network.packet.Packet;

public class SendPacketEvent extends EventCancellable {
   private Packet<?> packet;

   @Generated
   public Packet<?> getPacket() {
      return this.packet;
   }

   @Generated
   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   @Generated
   public SendPacketEvent(Packet<?> packet) {
      this.packet = packet;
   }
}
