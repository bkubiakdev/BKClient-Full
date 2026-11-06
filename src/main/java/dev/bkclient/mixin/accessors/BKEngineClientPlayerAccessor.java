package dev.bkclient.mixin.accessors;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface BKEngineClientPlayerAccessor {
    @Invoker("sendSprintingPacket")
    void bkclient$syncScaffoldSprint();

    @Accessor("lastSprinting")
    boolean bkengine$lastSentSprint();
}
