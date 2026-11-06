package dev.bkclient.mixin.freecam;

import dev.bkclient.systems.modules.modules.player.FreecamUtil;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.render.entity.EntityRenderDispatcher.class)
public class FreecamEntityRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void freecam$hidePlayer(net.minecraft.entity.Entity entity, double x, double y, double z,
            float delta, net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (FreecamUtil.isActive() && entity == MinecraftClient.getInstance().player) ci.cancel();
    }
}

