package dev.bkclient.mixin.accessors;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
   @org.spongepowered.asm.mixin.gen.Accessor("lastSelectedSlot")
   void aura$setLastSelectedSlot(int slot);
   @Invoker("sendSequencedPacket")
   void invokeSendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator);
}
