package dev.bkclient.larp.mixin;
import dev.bkclient.larp.feature.FakeEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class LarpServerEffectMixin {
    @Inject(method="onEntityStatusEffect",at=@At("TAIL"))
    private void larp$serverOwnsEffect(EntityStatusEffectS2CPacket packet,CallbackInfo ci) {
        var player=MinecraftClient.getInstance().player;
        if(player!=null && packet.getEntityId()==player.getId()) FakeEffects.relinquish(packet.getEffectId());
    }
}
