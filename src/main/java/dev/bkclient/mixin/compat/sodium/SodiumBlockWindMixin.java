package dev.bkclient.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.bkclient.utility.render.ambience.SodiumWindTags;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=BlockRenderer.class, remap=false)
public abstract class SodiumBlockWindMixin extends AbstractBlockRenderContext {
    @Shadow @Final private Vector3f posOffset;
    @WrapOperation(method="bufferQuad", at=@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V"))
    private void bk$wind(ChunkMeshBufferBuilder buffer, ChunkVertexEncoder.Vertex[] vertices, int material, Operation<Void> original) {
        int previous = SodiumWindTags.begin(state, pos, level, posOffset.y, vertices);
        try { original.call(buffer, vertices, material); }
        finally { SodiumWindTags.restore(previous); }
    }
}
