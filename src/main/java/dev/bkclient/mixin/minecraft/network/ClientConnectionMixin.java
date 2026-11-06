package dev.bkclient.mixin.minecraft.network;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.network.SendPacketEvent;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin implements IMinecraft {
   @Unique
   private static boolean stackOverflowFix;

   @Inject(method = "handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V", at = @At("HEAD"), cancellable = true)
   private static <T extends PacketListener> void triggerReceivePacketEvent(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
      if (MinecraftClient.getInstance().getNetworkHandler() != listener) {
         return;
      }
      ReceivePacketEvent event = new ReceivePacketEvent(packet);
      BKClientInit.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   // Source timing: observe incoming packets once, before vanilla schedules handling.
   @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
   private void aura$receive(io.netty.channel.ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
      if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getConnection() != (Object)this) return;
      var event = new dev.bkclient.combatengine.api.events.implement.EventPacket(
            packet, dev.bkclient.combatengine.api.events.implement.EventPacket.Type.RECEIVE);
      dev.bkclient.combatengine.bridge.AuraRuntime.emit(event);
      if (event.isCancelled()) ci.cancel();
   }

   @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
   public void triggerSendPacketEvent(Packet<?> packet, CallbackInfo ci) {
      if (mc.getNetworkHandler() == null || mc.getNetworkHandler().getConnection() != (Object)this) {
         return;
      }
      SendPacketEvent event = new SendPacketEvent(packet);
      if (!stackOverflowFix) {
         BKClientInit.getInstance().getEventManager().triggerEvent(event);
         var sourceEvent=new dev.bkclient.combatengine.api.events.implement.EventPacket(event.getPacket(),dev.bkclient.combatengine.api.events.implement.EventPacket.Type.SEND);
         sourceEvent.setCancelled(event.isCancelled());dev.bkclient.combatengine.bridge.AuraRuntime.emit(sourceEvent);
         if (event.isCancelled() || sourceEvent.isCancelled()) {
            ci.cancel();
         }
         if (!event.isCancelled() && !sourceEvent.isCancelled() && dev.bkclient.combatengine.bridge.AuraRuntime.active()
               && packet instanceof net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket slotPacket
               && mc.player != null && mc.interactionManager != null
               && mc.player.getInventory().selectedSlot == slotPacket.getSelectedSlot()) {
            ((dev.bkclient.mixin.accessors.ClientPlayerInteractionManagerAccessor)mc.interactionManager).aura$setLastSelectedSlot(slotPacket.getSelectedSlot());
         }

         Packet<?> newPacket = event.getPacket();
         if (newPacket != packet) {
            ci.cancel();
            stackOverflowFix = true;
            mc.getNetworkHandler().sendPacket(newPacket);
            stackOverflowFix = false;
         }
      }
   }
}
