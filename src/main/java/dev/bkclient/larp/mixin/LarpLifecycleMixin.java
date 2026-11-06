package dev.bkclient.larp.mixin;
import dev.bkclient.larp.LarpRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class LarpLifecycleMixin {
    @Inject(method="setWorld", at=@At("HEAD"))
    private void larp$worldChange(ClientWorld next, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world != next) LarpRuntime.reset();
    }
}
