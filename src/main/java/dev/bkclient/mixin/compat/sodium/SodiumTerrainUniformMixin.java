package dev.bkclient.mixin.compat.sodium;

import dev.bkclient.utility.render.ambience.SodiumTerrainUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface", remap=false)
public class SodiumTerrainUniformMixin {
    @Unique private final SodiumTerrainUniforms bk$effects = new SodiumTerrainUniforms();
    @Inject(method="setupState", at=@At("TAIL"))
    private void bk$uniforms(CallbackInfo ci) { bk$effects.upload(); }
}
