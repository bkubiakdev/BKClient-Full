package dev.bkclient.systems.modules.modules.visuals.esp;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.mixin.accessors.MatrixStackEntryAccessor;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Sodium only: draws the ESP spawn-egg icons from a recorded copy of the item model.
 *
 * <p>Sodium's item path ({@code BakedModelEncoder.writeQuadVertices}) opens a {@code MemoryStack} frame
 * for every quad; a generated egg model has over a hundred quads, so every label paid hundreds of
 * thread-local lookups per frame. The model is recorded once with an identity pose, and each draw
 * transforms the recorded vertices with the same {@link MatrixHelper} functions Sodium uses, so the
 * buffer receives the same bytes in one push per icon. The first draw of every egg compares the replay
 * with Sodium's own output byte for byte; an egg that differs keeps the regular item path.
 */
public final class EspEggReplay {
    private static final int LIGHT = 15728880;
    private static final int STRIDE = EntityVertex.STRIDE;
    private static final int POSITION = EntityVertex.FORMAT.getOffset(VertexFormatElement.POSITION);
    private static final int NORMAL = EntityVertex.FORMAT.getOffset(VertexFormatElement.NORMAL);
    /** Vertices per push; keeps one push well inside the default 64 KiB MemoryStack. */
    private static final int CHUNK = 1024;
    private static final Egg UNSUPPORTED = new Egg(null, null, null, null, 0, false, null);
    private static volatile int reloads;

    static {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public Identifier getFabricId() { return BKClientInit.id("esp_egg_replay"); }
            @Override public void reload(ResourceManager manager) { reloads++; }
        });
    }

    private final Map<ItemStack, Egg> eggs = new IdentityHashMap<>();
    private final ItemRenderState scratch = new ItemRenderState();
    private int seenReloads = reloads;

    /** Queues {@code stack} like {@link CustomDrawContext#drawBatchItem(ItemStack, float, float, float)}; false = not handled. */
    public boolean draw(CustomDrawContext context, ItemStack stack, float x, float y, float size) {
        if (seenReloads != reloads) { eggs.clear(); seenReloads = reloads; }
        Egg egg = eggs.get(stack);
        if (egg == UNSUPPORTED) return false;
        if (egg == null) {
            egg = record(stack);
            if (egg == null || !matchesSodium(egg, stack, context.getMatrices(), x, y, size)) egg = UNSUPPORTED;
            eggs.put(stack, egg);
            if (egg == UNSUPPORTED) return false;
        }
        VertexConsumer consumer = context.batchVertexConsumers().getBuffer(egg.layer);
        VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);
        if (writer == null) return false;
        MatrixStack matrices = context.getMatrices();
        // Same matrix and lighting sequence as CustomDrawContext.drawBatchItem + ItemRenderState.render.
        matrices.push();
        matrices.translate(x, y, 0.0F);
        matrices.scale(size, size, size);
        matrices.push();
        matrices.translate(8.0F, 8.0F, 150.0F);
        matrices.scale(16.0F, -16.0F, 16.0F);
        if (!egg.sideLit) DiffuseLighting.disableGuiDepthLighting();
        matrices.push();
        matrices.translate(-0.5F, -0.5F, -0.5F);
        MatrixStack.Entry entry = matrices.peek();
        egg.forPose(entry.getPositionMatrix()).write(writer, entry);
        matrices.pop();
        if (!egg.sideLit) DiffuseLighting.enableGuiDepthLighting();
        matrices.pop();
        matrices.pop();
        return true;
    }

    public void clear() {
        eggs.clear();
    }

    /** Records the egg with a pose that is exactly the identity where Sodium encodes it. */
    private Egg record(ItemStack stack) {
        var mc = MinecraftClient.getInstance();
        if (stack.isEmpty()) return null;
        mc.getItemModelManager().update(scratch, stack, ModelTransformationMode.GUI, false, mc.world, mc.player, 0);
        if (scratch.isEmpty() || scratch.getTransformation() != Transformation.IDENTITY) return null;
        MatrixStack matrices = new MatrixStack();
        // ItemRenderState translates by -0.5 before encoding; 0.5 - 0.5 leaves an exact identity.
        matrices.translate(0.5F, 0.5F, 0.5F);
        Recording recording = new Recording();
        scratch.render(matrices, recording, LIGHT, OverlayTexture.DEFAULT_UV);
        Recorder only = recording.single();
        if (only == null || only.count == 0) return null;
        byte[] data = Arrays.copyOf(only.data, only.count * STRIDE);
        float[] position = new float[only.count * 3];
        int[] normal = new int[only.count];
        for (int i = 0; i < only.count; i++) {
            int base = i * STRIDE;
            position[i * 3] = floatAt(data, base + POSITION);
            position[i * 3 + 1] = floatAt(data, base + POSITION + 4);
            position[i * 3 + 2] = floatAt(data, base + POSITION + 8);
            normal[i] = intAt(data, base + NORMAL);
        }
        Egg full = new Egg(only.layer, data, position, normal, only.count, scratch.isSideLit(), null);
        Egg faces = full.count % 4 == 0 ? full.facesOnly() : null;
        BKClientInit.LOGGER.debug("[ESP] Egg icon {}: {} vertices, {} facing the screen", stack.getItem(), full.count, faces == null ? full.count : faces.count);
        return new Egg(full.layer, full.data, full.position, full.normal, full.count, full.sideLit, faces);
    }

    /** Encodes the egg at the real pose both ways and requires identical bytes. */
    private boolean matchesSodium(Egg egg, ItemStack stack, MatrixStack matrices, float x, float y, float size) {
        var mc = MinecraftClient.getInstance();
        mc.getItemModelManager().update(scratch, stack, ModelTransformationMode.GUI, false, mc.world, mc.player, 0);
        Recording expected = new Recording();
        Recording actual = new Recording();
        matrices.push();
        matrices.translate(x, y, 0.0F);
        matrices.scale(size, size, size);
        matrices.push();
        matrices.translate(8.0F, 8.0F, 150.0F);
        matrices.scale(16.0F, -16.0F, 16.0F);
        try {
            scratch.render(matrices, expected, LIGHT, OverlayTexture.DEFAULT_UV);
            matrices.push();
            matrices.translate(-0.5F, -0.5F, -0.5F);
            egg.write((VertexBufferWriter) actual.getBuffer(egg.layer), matrices.peek());
            matrices.pop();
        } finally {
            matrices.pop();
            matrices.pop();
        }
        Recorder want = expected.single(), got = actual.single();
        return want != null && got != null && want.layer == got.layer && want.count == got.count
            && Arrays.equals(want.data, 0, want.count * STRIDE, got.data, 0, got.count * STRIDE);
    }

    private static float floatAt(byte[] data, int offset) {
        return Float.intBitsToFloat(intAt(data, offset));
    }

    private static int intAt(byte[] data, int offset) {
        // Native vertex memory is little-endian on every platform Minecraft runs on.
        return (data[offset] & 255) | (data[offset + 1] & 255) << 8 | (data[offset + 2] & 255) << 16 | (data[offset + 3] & 255) << 24;
    }

    private record Egg(RenderLayer layer, byte[] data, float[] position, int[] normal, int count, boolean sideLit, Egg faces) {
        /**
         * The generated item model has a side quad for every pixel edge. Under a pose that only scales and
         * translates, screen x depends on model x alone and screen y on model y alone, so a quad whose four
         * vertices share one x (or one y) is a line of zero area and covers no pixel; only the other quads
         * are written. Any other pose keeps the complete model.
         */
        Egg forPose(Matrix4f pose) {
            return faces != null && pose.m10() == 0.0F && pose.m20() == 0.0F && pose.m01() == 0.0F
                && pose.m21() == 0.0F && pose.m03() == 0.0F && pose.m13() == 0.0F && pose.m23() == 0.0F ? faces : this;
        }

        /** Drops quads with zero extent in model x or y. */
        Egg facesOnly() {
            int kept = 0;
            boolean[] keep = new boolean[count / 4];
            for (int q = 0; q < keep.length; q++) {
                int b = q * 12;
                boolean flatX = position[b] == position[b + 3] && position[b] == position[b + 6] && position[b] == position[b + 9];
                boolean flatY = position[b + 1] == position[b + 4] && position[b + 1] == position[b + 7] && position[b + 1] == position[b + 10];
                keep[q] = !flatX && !flatY;
                if (keep[q]) kept++;
            }
            byte[] d = new byte[kept * 4 * STRIDE];
            float[] p = new float[kept * 4 * 3];
            int[] nn = new int[kept * 4];
            int o = 0;
            for (int q = 0; q < keep.length; q++) {
                if (!keep[q]) continue;
                System.arraycopy(data, q * 4 * STRIDE, d, o * STRIDE, 4 * STRIDE);
                System.arraycopy(position, q * 12, p, o * 3, 12);
                System.arraycopy(normal, q * 4, nn, o, 4);
                o += 4;
            }
            return new Egg(layer, d, p, nn, o, sideLit, null);
        }

        void write(VertexBufferWriter writer, MatrixStack.Entry entry) {
            Matrix4f pose = entry.getPositionMatrix();
            Matrix3f normalMatrix = entry.getNormalMatrix();
            boolean skipNormalization = ((MatrixStackEntryAccessor) (Object) entry).bk$canSkipNormalization();
            for (int start = 0; start < count; start += CHUNK) {
                int n = Math.min(CHUNK, count - start);
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    long ptr = stack.nmalloc(n * STRIDE);
                    MemoryUtil.memByteBuffer(ptr, n * STRIDE).put(data, start * STRIDE, n * STRIDE);
                    for (int i = 0; i < n; i++) {
                        int v = start + i;
                        float px = position[v * 3], py = position[v * 3 + 1], pz = position[v * 3 + 2];
                        long base = ptr + (long) i * STRIDE;
                        MemoryUtil.memPutFloat(base + POSITION, MatrixHelper.transformPositionX(pose, px, py, pz));
                        MemoryUtil.memPutFloat(base + POSITION + 4, MatrixHelper.transformPositionY(pose, px, py, pz));
                        MemoryUtil.memPutFloat(base + POSITION + 8, MatrixHelper.transformPositionZ(pose, px, py, pz));
                        MemoryUtil.memPutInt(base + NORMAL, MatrixHelper.transformNormal(normalMatrix, skipNormalization, normal[v]));
                    }
                    writer.push(stack, ptr, n, EntityVertex.FORMAT);
                }
            }
        }
    }

    /** Collects what the item renderer writes; a single getBuffer call is all an egg may use. */
    private static final class Recording implements VertexConsumerProvider {
        private Recorder first;
        private int calls;

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            calls++;
            if (first == null) first = new Recorder(layer);
            return first.layer == layer ? first : new Recorder(layer);
        }

        Recorder single() {
            return calls == 1 && first != null && !first.failed ? first : null;
        }
    }

    private static final class Recorder implements VertexConsumer, VertexBufferWriter {
        final RenderLayer layer;
        byte[] data = new byte[STRIDE * 512];
        int count;
        boolean failed;

        Recorder(RenderLayer layer) {
            this.layer = layer;
        }

        @Override
        public void push(MemoryStack stack, long ptr, int vertices, VertexFormat format) {
            if (format != EntityVertex.FORMAT) { failed = true; return; }
            int needed = (count + vertices) * STRIDE;
            if (needed > data.length) data = Arrays.copyOf(data, Math.max(needed, data.length * 2));
            MemoryUtil.memByteBuffer(ptr, vertices * STRIDE).get(data, count * STRIDE, vertices * STRIDE);
            count += vertices;
        }

        @Override public boolean canUseIntrinsics() { return true; }
        @Override public VertexConsumer vertex(float x, float y, float z) { failed = true; return this; }
        @Override public VertexConsumer color(int red, int green, int blue, int alpha) { failed = true; return this; }
        @Override public VertexConsumer texture(float u, float v) { failed = true; return this; }
        @Override public VertexConsumer overlay(int u, int v) { failed = true; return this; }
        @Override public VertexConsumer light(int u, int v) { failed = true; return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { failed = true; return this; }
    }
}
