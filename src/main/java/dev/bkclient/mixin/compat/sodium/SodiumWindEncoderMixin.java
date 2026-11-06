package dev.bkclient.mixin.compat.sodium;

import dev.bkclient.utility.render.ambience.SodiumWindTags;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.system.MemoryUtil;

@Mixin(value=CompactChunkVertex.class, remap=false)
public class SodiumWindEncoderMixin {
    @Inject(method="getEncoder", at=@At("RETURN"), cancellable=true)
    private void bk$encoder(CallbackInfoReturnable<ChunkVertexEncoder> cir) {
        ChunkVertexEncoder original = cir.getReturnValue();
        cir.setReturnValue((ptr, material, vertices, section) -> {
            long end = original.write(ptr, material, vertices, section);
            int tags = SodiumWindTags.current();
            // CompactChunkVertex uses only bits 0..29 of each packed position
            // word. Store two per-vertex bits without touching color, UV or light.
            if (tags != 0) for (int i=0; i<4; i++) {
                long address = ptr + (long)i * CompactChunkVertex.STRIDE;
                MemoryUtil.memPutInt(address, MemoryUtil.memGetInt(address) | ((tags >>> (i*2) & 3) << 30));
            }
            return end;
        });
    }
}
