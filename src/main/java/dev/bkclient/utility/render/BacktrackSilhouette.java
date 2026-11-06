package dev.bkclient.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Visual-only player copies; never moves an entity or changes its tracked state. */
public final class BacktrackSilhouette implements IMinecraft {
    private static final java.util.LinkedHashMap<RenderLayer, net.minecraft.client.util.BufferAllocator> buffers = new java.util.LinkedHashMap<>();
    private static net.minecraft.client.util.BufferAllocator fallback;
    private static VertexConsumerProvider.Immediate ghostBuffers;
    private BacktrackSilhouette() {}

    public static void release() {
        if (!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(BacktrackSilhouette::release); return; }
        buffers.values().forEach(net.minecraft.client.util.BufferAllocator::close);
        buffers.clear();
        if (fallback != null) fallback.close();
        fallback = null;
        ghostBuffers = null;
    }


    public static void render(Render3DEvent event, AbstractClientPlayerEntity player, Vec3d networkPosition) {
        // A position marker must use the exact tracked feet position, not a trailing filter.
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var projectionType = RenderSystem.getProjectionType();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.identity();
            RenderSystem.setProjectionMatrix(new Matrix4f(event.getProjectionMatrix()), com.mojang.blaze3d.systems.ProjectionType.PERSPECTIVE);
            drawGhost(event, player, networkPosition);
        } finally {
            modelView.popMatrix();
            RenderSystem.setProjectionMatrix(projection, projectionType);
        }
    }

    private static void drawGhost(Render3DEvent event, AbstractClientPlayerEntity player, Vec3d networkPosition) {
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerEntityRenderer renderer)) return;
        var state = renderer.createRenderState();
        renderer.updateRenderState(player, state, event.getTickDelta());
        // Strip equipment only from this visual snapshot, never from the real player.
        state.equippedHeadStack = net.minecraft.item.ItemStack.EMPTY;
        state.equippedChestStack = net.minecraft.item.ItemStack.EMPTY;
        state.equippedLegsStack = net.minecraft.item.ItemStack.EMPTY;
        state.equippedFeetStack = net.minecraft.item.ItemStack.EMPTY;
        state.headItemRenderState.clear();
        state.wearingSkullType = null;
        state.wearingSkullProfile = null;
        state.rightHandItemState.clear();
        state.leftHandItemState.clear();
        state.spyglassState.clear();
        state.capeVisible = false;
        state.stuckArrowCount = 0;
        state.stingerCount = 0;
        state.leftShoulderParrotVariant = null;
        state.rightShoulderParrotVariant = null;
        state.displayName = null;
        state.playerName = null;
        state.leashData = null;
        state.onFire = false;
        state.hurt = false;
        state.invisible = false;
        state.invisibleToPlayer = false;
        state.hasOutline = false;
        var matrices = new net.minecraft.client.util.math.MatrixStack();
        matrices.multiplyPositionMatrix(event.getPositionMatrix());
        state.x = networkPosition.x;
        state.y = networkPosition.y;
        state.z = networkPosition.z;
        mc.getBufferBuilders().getEntityVertexConsumers().draw();
        if (buffers.size() > 64) release();
        if (ghostBuffers == null) {
            fallback = new net.minecraft.client.util.BufferAllocator(4096);
            ghostBuffers = VertexConsumerProvider.immediate(buffers, fallback);
        }
        var immediate = ghostBuffers;
        // Armor/glint can retain more than one consumer simultaneously. Never flush one
        // texture's builder just because a second texture is requested.
        java.util.Map<RenderLayer, VertexConsumer> consumers = new java.util.HashMap<>();
        float[] previousColor = RenderSystem.getShaderColor().clone();
        try {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            int copies = 1;
            for (int i = 0; i < copies; i++) {
                Vec3d relative = networkPosition.subtract(event.getCamera().getPos());
                ColorRGBA accent = i == 0 ? Colors.getAccent() : new ColorRGBA(65, 210, 255, 255);
                float alpha = .48F;
                Matrix4f inverseView = new Matrix4f(matrices.peek().getPositionMatrix()).invert();
                VertexConsumerProvider provider = layer -> {
                    RenderLayer mapped = RenderLayer.getEntityTranslucent(layerTexture(layer, state.skinTextures.texture()));
                    return consumers.computeIfAbsent(mapped, key -> {
                        buffers.computeIfAbsent(key, unused -> new net.minecraft.client.util.BufferAllocator(8192));
                        return new GhostConsumer(immediate.getBuffer(key), alpha, accent, true,
                                inverseView, (float)relative.y, state.height);
                    });
                };
                matrices.push();
                try {
                    matrices.translate(relative.x, relative.y, relative.z);
                    renderer.render(state, matrices, provider, LightmapTextureManager.MAX_LIGHT_COORDINATE);
                    immediate.draw();
                } finally { matrices.pop(); }
            }
        } finally {
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
        }
    }

    private static Identifier layerTexture(RenderLayer layer, Identifier fallback) {
        if (layer instanceof dev.bkclient.mixin.accessors.PopRenderLayerAccessor accessor) {
            var phases = (dev.bkclient.mixin.accessors.PopRenderPhasesAccessor)(Object)accessor.bkclient$getPhases();
            return ((dev.bkclient.mixin.accessors.PopTextureAccessor)phases.bkclient$getTexture()).bkclient$getId().orElse(fallback);
        }
        return fallback;
    }

    private static final class GhostConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;
        private final ColorRGBA accent;
        private final boolean textured;
        private final Matrix4f inverseView;
        private final float bottom, height;
        private final Vector3f point = new Vector3f();
        private float shade;

        private GhostConsumer(VertexConsumer delegate, float alpha, ColorRGBA accent, boolean textured,
                              Matrix4f inverseView, float bottom, float height) {
            this.delegate = delegate; this.alpha = alpha; this.accent = accent; this.textured = textured;
            this.inverseView = inverseView; this.bottom = bottom; this.height = Math.max(height, 0.1F);
        }
        @Override public VertexConsumer vertex(float x, float y, float z) {
            inverseView.transformPosition(point.set(x, y, z));
            shade = MathHelper.clamp((point.y - bottom) / height, 0, 1) * 0.55F;
            delegate.vertex(x, y, z); return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            if (!textured) {
                r = Math.round(MathHelper.lerp(shade, accent.getRed(), 255));
                g = Math.round(MathHelper.lerp(shade, accent.getGreen(), 255));
                b = Math.round(MathHelper.lerp(shade, accent.getBlue(), 255));
            }
            delegate.color(r, g, b, Math.round(a * alpha)); return this;
        }
        @Override public VertexConsumer texture(float u, float v) { delegate.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { delegate.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { delegate.light(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
    }
}
