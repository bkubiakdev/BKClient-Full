package dev.bkclient.utility.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.mixin.accessors.PopRenderLayerAccessor;
import dev.bkclient.mixin.accessors.PopRenderPhasesAccessor;
import dev.bkclient.mixin.accessors.PopTextureAccessor;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Re-renders only the selected entity into a private mask, then scans its visible surface. */
public final class TargetScanRenderer implements AutoCloseable {
    private static final ShaderProgramKey MASK = new ShaderProgramKey(Identifier.of("bkclient", "core/target_scan/mask"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, Defines.EMPTY);
    private static final ShaderProgramKey SCAN = new ShaderProgramKey(Identifier.of("bkclient", "core/target_scan/scan"), VertexFormats.POSITION_TEXTURE, Defines.EMPTY);
    private final Map<RenderLayer, BufferAllocator> allocators = new LinkedHashMap<>();
    private final TargetScanTimeline timeline = new TargetScanTimeline();
    private SimpleFramebuffer mask;
    private BufferAllocator quadAllocator;
    private LivingEntity retained;
    private ClientWorld world;
    private boolean failed;
    private volatile boolean dirty;
    private int maxTextureSize;

    public void invalidate() { dirty = true; }

    public void render(Render3DEvent event, LivingEntity selected, ColorRGBA color, float passSeconds,
                       float width, float strength, boolean throughWalls) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (dirty || world != mc.world) { close(); dirty = false; world = mc.world; }
        if (failed || world == null) return;
        if (selected != null && selected != retained) { retained = selected; timeline.reset(); }
        if (retained == null || retained.isRemoved() || !retained.isAlive() || retained.getWorld() != world) {
            retained = null; timeline.reset(); return;
        }
        TargetScanTimeline.Sample sample = timeline.update(System.nanoTime(), selected != null, passSeconds);
        if (sample.alpha() <= 0) { if (selected == null) retained = null; return; }
        Framebuffer main = mc.getFramebuffer();
        if (main == null || main.textureWidth <= 0 || main.textureHeight <= 0
                || GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) != main.fbo) return;
        if (!(mc.getEntityRenderDispatcher().getRenderer(retained) instanceof LivingEntityRenderer<?, ?, ?> renderer)) return;
        mc.getBufferBuilders().getEntityVertexConsumers().draw();
        try {
            draw(event, renderer, retained, color, sample, width, strength, throughWalls, main);
        } catch (RuntimeException error) {
            close(); world = mc.world; failed = true;
            BKClientInit.LOGGER.warn("Target ESP Scan disabled until reload", error);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void draw(Render3DEvent event, LivingEntityRenderer renderer, LivingEntity target,
                      ColorRGBA color, TargetScanTimeline.Sample sample, float width, float strength,
                      boolean throughWalls, Framebuffer main) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ShaderProgram maskShader = mc.getShaderLoader().getOrCreateProgram(MASK);
        ShaderProgram scanShader = mc.getShaderLoader().getOrCreateProgram(SCAN);
        if (maskShader == null || scanShader == null) return;
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING), draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE), depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING), vbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] bindings = new int[2], slots = new int[2];
        for (int i = 0; i < 2; i++) { RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i); bindings[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D); slots[i] = RenderSystem.getShaderTexture(i); }
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        ProjectionType projectionType = RenderSystem.getProjectionType();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        Map<RenderLayer, BufferBuilder> batches = new LinkedHashMap<>();
        try (VisualRenderState ignored = new VisualRenderState()) {
            modelView.identity();
            RenderSystem.setProjectionMatrix(new Matrix4f(event.getProjectionMatrix()), ProjectionType.PERSPECTIVE);
            RenderSystem.disableScissor();
            if (maxTextureSize == 0) maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
            var maskSize = TargetScanQuality.maskSize(main.textureWidth, main.textureHeight, maxTextureSize);
            if (mask == null || mask.textureWidth != maskSize.width() || mask.textureHeight != maskSize.height()) {
                if (mask != null) mask.delete();
                mask = new SimpleFramebuffer(maskSize.width(), maskSize.height(), true);
                mask.setClearColor(0, 0, 0, 0);
            }
            mask.clear(); mask.beginWrite(true);
            RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL); RenderSystem.depthMask(true);
            RenderSystem.disableBlend(); RenderSystem.disableCull(); RenderSystem.setShaderColor(1, 1, 1, 1);

            // Use a fresh render state; equipment, pose and world entities are never mutated.
            LivingEntityRenderState state = (LivingEntityRenderState)renderer.createRenderState();
            renderer.updateRenderState(target, state, event.getTickDelta());
            state.displayName = null; state.leashData = null; state.onFire = false; state.hasOutline = false;
            state.headItemRenderState.clear(); state.wearingSkullType = null; state.wearingSkullProfile = null;
            if (state instanceof PlayerEntityRenderState player) {
                player.playerName = null; player.rightHandItemState.clear(); player.leftHandItemState.clear();
                player.spyglassState.clear(); player.capeVisible = false; player.stuckArrowCount = 0; player.stingerCount = 0;
                player.leftShoulderParrotVariant = null; player.rightShoulderParrotVariant = null;
            }
            Identifier fallback = renderer.getTexture(state);
            VertexConsumerProvider provider = layer -> {
                Identifier texture = texture(layer, fallback);
                if (layer.getDrawMode() != VertexFormat.DrawMode.QUADS || texture.getPath().contains("glint")) return DISCARD;
                return batches.computeIfAbsent(layer, id -> new BufferBuilder(
                        allocators.computeIfAbsent(id, key -> new BufferAllocator(8192)),
                        VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL));
            };
            Vec3d position = target.getLerpedPos(event.getTickDelta()).subtract(event.getCamera().getPos());
            MatrixStack matrices = new MatrixStack();
            matrices.multiplyPositionMatrix(event.getPositionMatrix());
            matrices.translate(position.x, position.y, position.z);
            renderer.render(state, matrices, provider, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            for (var batch : batches.entrySet()) {
                BuiltBuffer built = batch.getValue().endNullable();
                if (built == null) continue;
                // Armor uses vanilla view-depth layering. Match it rather than allowing scan holes
                // or increasing the global depth bias (which would bleed through nearby walls).
                RenderPhase.Layering layering = RenderPhase.NO_LAYERING;
                if (batch.getKey() instanceof PopRenderLayerAccessor accessor) {
                    layering = ((PopRenderPhasesAccessor)(Object)accessor.bkclient$getPhases()).bkclient$getLayering();
                }
                layering.startDrawing();
                try {
                    RenderSystem.setShader(maskShader);
                    maskShader.getUniformOrDefault("InverseView").set(new Matrix4f(event.getPositionMatrix()).invert());
                    RenderSystem.setShaderTexture(0, mc.getTextureManager().getTexture(texture(batch.getKey(), fallback)).getGlId());
                    BufferRenderer.drawWithGlobalProgram(built);
                } finally { layering.endDrawing(); }
            }
            main.beginWrite(true);
            if (throughWalls) RenderSystem.disableDepthTest(); else RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(scanShader);
            RenderSystem.setShaderTexture(0, mask.getColorAttachment());
            RenderSystem.setShaderTexture(1, mask.getDepthAttachment());
            scanShader.getUniformOrDefault("InverseViewProjection").set(new Matrix4f(event.getProjectionMatrix()).mul(event.getPositionMatrix()).invert());
            scanShader.getUniformOrDefault("OriginHeight").set((float)position.x, (float)position.y, (float)position.z, Math.max(.1f, target.getHeight()));
            scanShader.getUniformOrDefault("Tint").set(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
            scanShader.getUniformOrDefault("Scan").set(sample.height(), sample.direction(), width, strength);
            scanShader.getUniformOrDefault("Opacity").set(sample.alpha() * color.getAlpha()/255f);
            scanShader.getUniformOrDefault("PixelSize").set(1f / main.textureWidth, 1f / main.textureHeight);
            if (quadAllocator == null) quadAllocator = new BufferAllocator(256);
            BufferBuilder quad = new BufferBuilder(quadAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            quad.vertex(-1, -1, 0).texture(0, 0); quad.vertex(1, -1, 0).texture(1, 0);
            quad.vertex(1, 1, 0).texture(1, 1); quad.vertex(-1, 1, 0).texture(0, 1);
            BufferRenderer.drawWithGlobalProgram(quad.end());
        } finally {
            modelView.popMatrix(); RenderSystem.setProjectionMatrix(projection, projectionType);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read); GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            BufferRenderer.resetCurrentVertexBuffer(); GL30.glBindVertexArray(vao); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            for (int i = 0; i < 2; i++) { RenderSystem.activeTexture(GL13.GL_TEXTURE0+i); RenderSystem.bindTexture(bindings[i]); RenderSystem.setShaderTexture(i, slots[i]); }
            RenderSystem.activeTexture(active); RenderSystem.depthFunc(depthFunc);
            if (scissor) GlStateManager._enableScissorTest();
            // Retain only textures used by this target; switching skins cannot grow this cache indefinitely.
            allocators.entrySet().removeIf(entry -> { if (batches.containsKey(entry.getKey())) return false; entry.getValue().close(); return true; });
        }
    }

    private static Identifier texture(RenderLayer layer, Identifier fallback) {
        if (layer instanceof PopRenderLayerAccessor accessor) {
            var phases = (PopRenderPhasesAccessor)(Object)accessor.bkclient$getPhases();
            return ((PopTextureAccessor)phases.bkclient$getTexture()).bkclient$getId().orElse(fallback);
        }
        return fallback;
    }

    @Override public void close() {
        if (mask != null) {
            int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING), draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            try (VisualRenderState ignored = new VisualRenderState()) { mask.delete(); mask = null; }
            finally { GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read); GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw); }
        }
        allocators.values().forEach(BufferAllocator::close); allocators.clear();
        if (quadAllocator != null) { quadAllocator.close(); quadAllocator = null; }
        retained = null; world = null; failed = false; timeline.reset();
    }

    private static final VertexConsumer DISCARD = new VertexConsumer() {
        public VertexConsumer vertex(float x,float y,float z) { return this; }
        public VertexConsumer color(int r,int g,int b,int a) { return this; }
        public VertexConsumer texture(float u,float v) { return this; }
        public VertexConsumer overlay(int u,int v) { return this; }
        public VertexConsumer light(int u,int v) { return this; }
        public VertexConsumer normal(float x,float y,float z) { return this; }
    };
}
