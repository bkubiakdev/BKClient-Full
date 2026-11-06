package dev.bkclient.mixin.minecraft.network;

import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin implements IMinecraft {
    @org.spongepowered.asm.mixin.injection.Inject(method="onEntityStatus", at=@org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void bkclient$totemGhost(net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket packet, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (mc.world == null || packet.getStatus() != 35) return;
        var module = dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.PopChams.class);
        if (module != null) module.onTotemPop(packet.getEntity(mc.world));
    }
    @org.spongepowered.asm.mixin.injection.Inject(method="onItemPickupAnimation", at=@org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void bkclient$pickupEffect(net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket packet, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!mc.isOnThread()) return;
        var module = dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.PickBlowup.class);
        if (module != null) module.pickup(packet.getEntityId(), packet.getCollectorEntityId());
    }
}
