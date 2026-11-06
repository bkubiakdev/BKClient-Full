package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.mixin.accessors.PopRenderLayerAccessor;
import dev.bkclient.mixin.accessors.PopRenderPhasesAccessor;
import dev.bkclient.mixin.accessors.PopTextureAccessor;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.PopCubeParticles;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Frozen render states preserve the skin, equipment and pose at the moment a totem pops. */
@ModuleInfo(name = "Pop Chams", category = ModuleCategory.VISUALS, desc = "modules.descriptions.pop_chams")
public final class PopChams extends BaseModule implements IMinecraft {
    private static final Identifier WHITE = BKClientInit.id("textures/white.png");
    private final BooleanSetting cubes = new BooleanSetting(this, "modules.settings.pop_chams.cubes").enable();
    private final ModeSetting mode = new ModeSetting(this, "modules.settings.pop_chams.mode", () -> cubes.isEnabled());
    private final ModeSetting.Value gradient = new ModeSetting.Value(mode, "Gradient");
    private final ModeSetting.Value texture = new ModeSetting.Value(mode, "Texture");
    private final SliderSetting cubeSize = new SliderSetting(this, "modules.settings.pop_chams.cube_size", () -> !cubes.isEnabled())
        .min(0.035F).max(0.12F).currentValue(0.065F).step(0.005F);
    private final SliderSetting spread = new SliderSetting(this, "modules.settings.pop_chams.spread", () -> !cubes.isEnabled())
        .min(0.1F).max(2).currentValue(0.7F).step(0.05F);
    private final BooleanSetting self = new BooleanSetting(this, "modules.settings.pop_chams.self").enabled(false);
    private final SliderSetting lifetime = new SliderSetting(this, "modules.settings.pop_chams.lifetime")
        .min(150).max(4000).currentValue(950).step(25).suffix("ms");
    private final SliderSetting rise = new SliderSetting(this, "modules.settings.pop_chams.rise")
        .min(0).max(5).currentValue(1.15F).step(0.05F);
    private final SliderSetting opacity = new SliderSetting(this, "modules.settings.pop_chams.opacity")
        .min(10).max(255).currentValue(185).step(5);
    private final List<Ghost> ghosts = new ArrayList<>();
    private ClientWorld world;

    /** Called after vanilla has scheduled entity-status handling on the client thread. */
    public void onTotemPop(Entity entity) {
        if (!isEnabled() || mc.world == null || !(entity instanceof AbstractClientPlayerEntity player)) return;
        if (player == mc.player && !self.isEnabled()) return;
        checkWorld();
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerEntityRenderer renderer)) return;
        PlayerEntityRenderState state = renderer.createRenderState();
        renderer.updateRenderState(player, state, 1.0F);
        state.displayName = null;
        state.playerName = null;
        state.leashData = null;
        state.onFire = false;
        state.hurt = false;
        state.invisible = false;
        state.invisibleToPlayer = false;
        state.hasOutline = false;
        var particles = cubes.isEnabled() ? PopCubeParticles.capture(renderer, state, cubeSize.getCurrentValue()) : null;
        ghosts.add(new Ghost(renderer, state, player.getPos(), System.nanoTime(), particles));
        while (ghosts.size() > (cubes.isEnabled() ? 8 : 24)) ghosts.removeFirst();
    }

    private final EventListener<Render3DEvent> onRender = event -> {
        checkWorld();
        if (mc.world == null || mc.player == null || ghosts.isEmpty()) return;
        long now = System.nanoTime();
        float life = lifetime.getCurrentValue();
        ghosts.removeIf(ghost -> (now - ghost.started) / 1_000_000F >= life);
        var matrices = event.getMatrices();
        var immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        immediate.draw();
        float[] previousColor = RenderSystem.getShaderColor().clone();
        try {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            for (Ghost ghost : ghosts) {
                float progress = MathHelper.clamp((now - ghost.started) / 1_000_000F / life, 0, 1);
                float alpha = opacity.getCurrentValue() / 255F * (1 - progress * progress);
                if (alpha <= 0.001F) continue;
                if (ghost.particles != null) {
                    ghost.particles.render(event, ghost.position, progress, rise.getCurrentValue(), spread.getCurrentValue(), alpha, Colors.getAccent());
                    continue;
                }
                Vec3d position = ghost.position.add(0, rise.getCurrentValue() * (1 - (1 - progress) * (1 - progress)), 0);
                Vec3d relative = position.subtract(event.getCamera().getPos());
                Matrix4f inverseView = new Matrix4f(matrices.peek().getPositionMatrix()).invert();
                ColorRGBA accent = Colors.getAccent();
                boolean textured = texture.isSelected();
                VertexConsumerProvider provider = layer -> {
                    Identifier id = textured ? layerTexture(layer, ghost.state.skinTextures.texture()) : WHITE;
                    VertexConsumer buffer = immediate.getBuffer(RenderLayer.getEntityTranslucent(id));
                    return new GhostConsumer(buffer, alpha, accent, textured, inverseView, (float)relative.y, ghost.state.height);
                };
                matrices.push();
                try {
                    matrices.translate(relative.x, relative.y, relative.z);
                    ghost.renderer.render(ghost.state, matrices, provider, LightmapTextureManager.MAX_LIGHT_COORDINATE);
                    immediate.draw();
                } finally {
                    matrices.pop();
                }
            }
        } finally {
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
        }
    };

    private static Identifier layerTexture(RenderLayer layer, Identifier fallback) {
        if (layer instanceof PopRenderLayerAccessor accessor) {
            var phases = (PopRenderPhasesAccessor)(Object)accessor.bkclient$getPhases();
            return ((PopTextureAccessor)phases.bkclient$getTexture()).bkclient$getId().orElse(fallback);
        }
        return fallback;
    }

    private void checkWorld() {
        if (world != mc.world) {
            ghosts.clear();
            world = mc.world;
        }
    }

    @Override public void onDisable() { ghosts.clear(); world = null; }

    private record Ghost(PlayerEntityRenderer renderer, PlayerEntityRenderState state, Vec3d position, long started, PopCubeParticles particles) { }

    /** Alpha belongs to vertices; cutout player/armor layers otherwise ignore shader alpha. */
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
