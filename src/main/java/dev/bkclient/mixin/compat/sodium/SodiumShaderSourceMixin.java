package dev.bkclient.mixin.compat.sodium;

import dev.bkclient.utility.render.ambience.SodiumTerrainShaders;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap=false)
public class SodiumShaderSourceMixin {
    @Inject(method="getShaderSource", at=@At("RETURN"), cancellable=true)
    private static void bk$terrain(Identifier name, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(SodiumTerrainShaders.patch(name.getNamespace(), name.getPath(), cir.getReturnValue()));
    }
}
