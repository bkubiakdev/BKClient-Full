package dev.bkclient.utility.render.ambience;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockRenderView;

public final class WindWaveTagger {
    private static final int TAG_PLANT = 254;
    private static final int TAG_LEAF = 253;
    private static final int TAG_VINE = 252;
    private static final int KIND_NONE = 0;
    private static final int KIND_GROUND = 1;
    private static final int KIND_LEAF = 2;
    private static final int KIND_HANGING = 3;
    private static final int KIND_VINE = 4;
    private static final int ANCHOR_NONE = 0;
    private static final int ANCHOR_BOTTOM = 1;
    private static final int ANCHOR_TOP = 2;
    private static final Set<Block> GROUND_BLOCKS = Set.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN, Blocks.DEAD_BUSH, Blocks.SUGAR_CANE, Blocks.BAMBOO, Blocks.BAMBOO_SAPLING, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS, Blocks.KELP, Blocks.KELP_PLANT, Blocks.SWEET_BERRY_BUSH, Blocks.NETHER_WART, Blocks.CRIMSON_ROOTS, Blocks.WARPED_ROOTS, Blocks.NETHER_SPROUTS, Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.SMALL_DRIPLEAF, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM, Blocks.ATTACHED_MELON_STEM, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.TORCHFLOWER_CROP, Blocks.PITCHER_CROP, Blocks.PITCHER_PLANT, Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY);
    private static final Set<Block> HANGING_BLOCKS = Set.of(Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT, Blocks.HANGING_ROOTS, Blocks.SPORE_BLOSSOM, Blocks.PALE_HANGING_MOSS);
    private static final Set<Block> EXCLUDED_BLOCKS = Set.of(Blocks.MANGROVE_PROPAGULE, Blocks.CHORUS_FLOWER, Blocks.AZALEA, Blocks.FLOWERING_AZALEA);

    /** Classification per interned BlockState; chunk building runs on several threads. Cleared on world change (tags may reload). */
    private static final java.util.concurrent.ConcurrentHashMap<BlockState, Integer> KINDS = new java.util.concurrent.ConcurrentHashMap<>();
    public static void clearCache() { KINDS.clear(); }

    private WindWaveTagger() {
    }

    public static VertexConsumer wrap(VertexConsumer vertexConsumer, BlockState blockState, BlockPos blockPos, BlockRenderView blockRenderView) {
        return WindWaveTagger.wrap(vertexConsumer, blockState, blockPos, blockRenderView, RenderLayers.getBlockLayer((BlockState)blockState));
    }

    public static VertexConsumer wrap(VertexConsumer vertexConsumer, BlockState blockState, BlockPos blockPos, BlockRenderView blockRenderView, RenderLayer blockRenderLayer) {
        if (blockRenderLayer != RenderLayer.getSolid() && blockRenderLayer != RenderLayer.getCutout() && blockRenderLayer != RenderLayer.getCutoutMipped()) {
            return vertexConsumer;
        }
        if (!(vertexConsumer instanceof net.minecraft.client.render.BufferBuilder)) return vertexConsumer;
        int n = WindWaveTagger.kindOf(blockState);
        if (n == 0) {
            return vertexConsumer;
        }
        float f = ChunkSectionPos.getLocalCoord((int)blockPos.getY());
        if (n == 2) {
            return new WindWaveTagger.TaggingConsumer(vertexConsumer, 253, 0, f);
        }
        if (n == 4) {
            return new WindWaveTagger.TaggingConsumer(vertexConsumer, 252, 0, f);
        }
        if (n == 1) {
            boolean bl = WindWaveTagger.kindOf(blockRenderView.getBlockState(blockPos.down())) != 1;
            return new WindWaveTagger.TaggingConsumer(vertexConsumer, 254, bl ? 1 : 0, f);
        }
        boolean bl = WindWaveTagger.kindOf(blockRenderView.getBlockState(blockPos.up())) != 3;
        return new WindWaveTagger.TaggingConsumer(vertexConsumer, 254, bl ? 2 : 0, f);
    }

    public static int kindOf(BlockState blockState) {
        Integer cached = KINDS.get(blockState);
        if (cached != null) return cached;
        int kind = classify(blockState);
        KINDS.put(blockState, kind);
        return kind;
    }

    private static int classify(BlockState blockState) {
        Block block = blockState.getBlock();
        if (EXCLUDED_BLOCKS.contains(block)) {
            return 0;
        }
        if (blockState.isIn(BlockTags.LEAVES)) {
            return 2;
        }
        if (block == Blocks.VINE) {
            return 4;
        }
        if (HANGING_BLOCKS.contains(block)) {
            return 3;
        }
        if (GROUND_BLOCKS.contains(block)) {
            return 1;
        }
        if (blockState.isIn(BlockTags.SMALL_FLOWERS) || blockState.isIn(BlockTags.CROPS) || blockState.isIn(BlockTags.SAPLINGS) || blockState.isIn(BlockTags.FLOWERS)) {
            return 1;
        }
        return 0;
    }


    public static final class TaggingConsumer
    implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int waveAlpha;
        private final int anchorMode;
        private final float blockY;
        private boolean waveCurrent;
    
        private TaggingConsumer(VertexConsumer vertexConsumer, int n, int n2, float f) {
            this.delegate = vertexConsumer;
            this.waveAlpha = n;
            this.anchorMode = n2;
            this.blockY = f;
        }
    
        public VertexConsumer color(int argb) {
            this.delegate.color(this.waveCurrent ? argb & 0xFFFFFF | this.waveAlpha << 24 : argb);
            return this;
        }
    
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.delegate.color(red, green, blue, this.waveCurrent ? this.waveAlpha : alpha);
            return this;
        }
    
        public VertexConsumer texture(float u, float v) {
            this.delegate.texture(u, v);
            return this;
        }
    
        public VertexConsumer overlay(int u, int v) {
            this.delegate.overlay(u, v);
            return this;
        }
    
        public VertexConsumer vertex(float x, float y, float z) {
            float f = y - this.blockY;
            this.waveCurrent = switch (this.anchorMode) {
                case 1 -> {
                    if (f >= 0.5f) {
                        yield true;
                    }
                    yield false;
                }
                case 2 -> {
                    if (f <= 0.5f) {
                        yield true;
                    }
                    yield false;
                }
                default -> true;
            };
            this.delegate.vertex(x, y, z);
            return this;
        }
    
    
        public VertexConsumer normal(float x, float y, float z) {
            this.delegate.normal(x, y, z);
            return this;
        }
    
        public VertexConsumer light(int u, int v) {
            this.delegate.light(u, v);
            return this;
        }
    }
}

