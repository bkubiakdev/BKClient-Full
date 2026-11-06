package dev.bkclient.systems.event.impl.network;

import lombok.Generated;
import dev.bkclient.systems.event.EventCancellable;
import net.minecraft.network.packet.Packet;

public class ReceivePacketEvent extends EventCancellable {
   private final Packet<?> packet;

   @Generated
   public Packet<?> getPacket() {
      return this.packet;
   }

   @Generated
   public ReceivePacketEvent(Packet<?> packet) {
      this.packet = packet;
   }
}
