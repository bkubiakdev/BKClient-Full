package dev.bkclient.mixin.minecraft.client.network;

import dev.bkclient.systems.modules.modules.player.FreecamUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {ClientCommonNetworkHandler.class})
public class ClientCommonNetworkHandlerMixin {
    @Inject(method = {"sendPacket"}, at = {@At(value = "HEAD")}, cancellable = true)
    private void bkclient$freecamCancelMovePackets(Packet<?> packet, CallbackInfo ci) {
        if (!FreecamUtil.shouldCancelPackets()) {
            return;
        }
        if (packet instanceof PlayerMoveC2SPacket) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = {"sendPacket"}, at = @At(value = "HEAD"), argsOnly = true)
    private Packet<?> bkclient$freecamPearlFromStanding(Packet<?> packet) {
        if (!FreecamUtil.isActive() || !(packet instanceof PlayerInteractItemC2SPacket itemPacket)) {
            return packet;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return packet;
        }
        ItemStack stack = client.player.getStackInHand(itemPacket.getHand());
        if (stack.isEmpty() || stack.getItem() != Items.ENDER_PEARL) {
            return packet;
        }
        return new PlayerInteractItemC2SPacket(itemPacket.getHand(), itemPacket.getSequence(), FreecamUtil.originYaw(), FreecamUtil.originPitch());
    }
}
