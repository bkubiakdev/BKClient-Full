package dev.bkclient.utility.render.ambience;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

/** Per worker, per quad scope; independent from module toggles so no chunk rebuild is needed. */
public final class SodiumWindTags {
    private static final ThreadLocal<Integer> TAGS = ThreadLocal.withInitial(() -> 0);
    private SodiumWindTags() {}
    public static int current() { return TAGS.get(); }
    public static void restore(int tags) { TAGS.set(tags); }
    public static int begin(BlockState state, BlockPos pos, BlockRenderView world, float originY, ChunkVertexEncoder.Vertex[] vertices) {
        int previous = current(), tags = 0;
        int kind = WindWaveTagger.kindOf(state);
        boolean bottom = kind==1 && WindWaveTagger.kindOf(world.getBlockState(pos.down()))!=1;
        boolean top = kind==3 && WindWaveTagger.kindOf(world.getBlockState(pos.up()))!=3;
        for (int i=0; i<4; i++) tags |= tag(kind, vertices[i].y-originY, bottom, top) << (i*2);
        TAGS.set(tags);
        return previous;
    }
    public static int tag(int kind, float localY, boolean bottom, boolean top) {
        if (kind==0 || bottom && localY<.5f || top && localY>.5f) return 0;
        return kind==2 ? 2 : kind==4 ? 3 : 1;
    }
}
