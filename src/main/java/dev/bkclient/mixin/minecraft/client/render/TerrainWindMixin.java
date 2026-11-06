package dev.bkclient.mixin.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.utility.render.ambience.TerrainWind;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class TerrainWindMixin {
    @Inject(method="renderLayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/RenderLayer;startDrawing()V",shift=At.Shift.AFTER))
    private void bk$wind(RenderLayer layer,double x,double y,double z,Matrix4f view,Matrix4f projection,CallbackInfo ci){
        RenderSystem.setShader(TerrainWind.program(layer,RenderSystem.getShader()));
    }
}
