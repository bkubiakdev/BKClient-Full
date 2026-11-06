package dev.bkclient.mixin.freecam;

import dev.bkclient.systems.modules.modules.player.FreecamUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.render.Camera.class)
public abstract class FreecamCameraMixin {
    @Shadow protected abstract void setPos(net.minecraft.util.math.Vec3d pos);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void freecam$clip(float distance, CallbackInfoReturnable<Float> cir) {
        if (FreecamUtil.isActive()) cir.setReturnValue(distance);
    }
    @Inject(method = "update", at = @At("TAIL"))
    private void freecam$camera(net.minecraft.world.BlockView world, net.minecraft.entity.Entity entity,
            boolean thirdPerson, boolean inverseView, float delta, CallbackInfo ci) {
        if (!FreecamUtil.isActive()) return;
        setPos(FreecamUtil.getCameraPos(delta));
        setRotation(FreecamUtil.getCameraYaw(delta), FreecamUtil.getCameraPitch(delta));
    }
}

