package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.LinkedModeSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.DrawUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * ChinaHat — renders a conical gradient hat above the player's head.
 * Ported from soup-visuals.
 * The actual rendering is triggered from ChinaHatFeatureRenderer (registered via mixin).
 */
@ModuleInfo(name = "China Hat", category = ModuleCategory.VISUALS, desc = "Renders a rotating gradient hat above players")
public class ChinaHat extends BaseModule {

    private static ChinaHat INSTANCE;

    public static ChinaHat getInstance() {
        return INSTANCE;
    }

    public final ModeSetting mode = new ModeSetting(this, "modules.settings.china_hat.mode");
    public final ModeSetting.Value modeHat = new ModeSetting.Value(mode, "China Hat");
    public final ModeSetting.Value modeHalo = new ModeSetting.Value(mode, "Halo");

    public final BooleanSetting rainbow = new BooleanSetting(this, "modules.settings.china_hat.rainbow");
    public final ColorSetting color1 = new ColorSetting(this, "modules.settings.china_hat.color1",
            () -> this.rainbow.isEnabled()).color(new ColorRGBA(233, 30, 99));
    public final SliderSetting centerAlpha = new SliderSetting((SettingsContainer) this, "modules.settings.china_hat.center_alpha", () -> mode.is(modeHalo)).min(0f).max(1f).currentValue(1f).step(0.01f);
    public final SliderSetting edgeAlpha = new SliderSetting((SettingsContainer) this, "modules.settings.china_hat.edge_alpha", () -> mode.is(modeHalo)).min(0.5f).max(1f).currentValue(0.5f).step(0.01f);
    public final SliderSetting rotationSpeed = new SliderSetting((SettingsContainer) this, "modules.settings.china_hat.rotation_speed").min(-3f).max(3f).currentValue(-0.2f).step(0.05f);
    public final BooleanSetting renderOnFriends = new BooleanSetting(this, "modules.settings.china_hat.render_on_friends").enable();

    // Halo (nimbus) tuning — ported from wonderful Cosmetics.renderNimbus
    private static final int NIMBUS_ARMS = 2;
    private static final int NIMBUS_SEGMENTS = 17;
    private static final float NIMBUS_RADIUS = 0.45f;
    private static final float NIMBUS_BASE_SIZE = 0.23f;
    private static final double NIMBUS_STEP_RADIANS = 0.11;
    private static final int NIMBUS_MAX_ALPHA = 255;
    private static final int NIMBUS_ALPHA_FALLOFF = 9;
    private static final float NIMBUS_SPEED = 170.0f;
    private static final Identifier NIMBUS_TEX = Identifier.of("bkclient", "textures/bloom.png");

    public static final RenderLayer HAT_LAYER = RenderLayer.of(
            "china_hat_bk",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.TRIANGLES,
            1536,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .build(false)
    );

    public ChinaHat() {
        LinkedModeSetting.toggle(this, "color.source", this.rainbow, "Rainbow", "Custom");

        INSTANCE = this;
    }

    /**
     * Main hat geometry render — called from ChinaHatFeatureRenderer.
     */
    public void renderHat(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(HAT_LAYER);

        int step = 9;
        float centerY = -0.5f;  // peak (top of cone, above head)
        float brimY = 0.0f;     // brim (flat circle at base)
        float radius = 0.65f;

        for (int i = 0; i < 360; i += step) {
            float angle1 = i * MathHelper.RADIANS_PER_DEGREE;
            float angle2 = (i + step) * MathHelper.RADIANS_PER_DEGREE;

            float x1 = MathHelper.sin(angle1) * radius;
            float z1 = -MathHelper.cos(angle1) * radius;
            float x2 = MathHelper.sin(angle2) * radius;
            float z2 = -MathHelper.cos(angle2) * radius;

            int c1 = getGradientColor(i);
            int c2 = getGradientColor(i + step);
            int cc = getGradientColor(i);

            float ea = edgeAlpha.getCurrentValue();
            float ca = centerAlpha.getCurrentValue();
            int alphaC1 = (c1 & 0x00FFFFFF) | ((int)(ea * 255) << 24);
            int alphaC2 = (c2 & 0x00FFFFFF) | ((int)(ea * 255) << 24);
            int alphaCc = (cc & 0x00FFFFFF) | ((int)(ca * 255) << 24);

            vertexConsumer.vertex(matrix, 0, centerY, 0).color(alphaCc);
            vertexConsumer.vertex(matrix, x1, brimY, z1).color(alphaC1);
            vertexConsumer.vertex(matrix, x2, brimY, z2).color(alphaC2);
        }

        // Back face
        for (int i = 0; i < 360; i += step) {
            float angle1 = i * MathHelper.RADIANS_PER_DEGREE;
            float angle2 = (i + step) * MathHelper.RADIANS_PER_DEGREE;

            float x1 = MathHelper.sin(angle1) * radius;
            float z1 = -MathHelper.cos(angle1) * radius;
            float x2 = MathHelper.sin(angle2) * radius;
            float z2 = -MathHelper.cos(angle2) * radius;

            int c1 = darker(getGradientColor(i), 4);
            int c2 = darker(getGradientColor(i + step), 4);
            int cc = darker(getGradientColor(i), 1);

            float ea = edgeAlpha.getCurrentValue();
            float ca = centerAlpha.getCurrentValue();
            int alphaC1 = (c1 & 0x00FFFFFF) | ((int)(ea * 255) << 24);
            int alphaC2 = (c2 & 0x00FFFFFF) | ((int)(ea * 255) << 24);
            int alphaCc = (cc & 0x00FFFFFF) | ((int)(ca * 255) << 24);

            vertexConsumer.vertex(matrix, x2, brimY, z2).color(alphaC2);
            vertexConsumer.vertex(matrix, x1, brimY, z1).color(alphaC1);
            vertexConsumer.vertex(matrix, 0, centerY, 0).color(alphaCc);
        }

        renderOutline(matrices, step, radius, centerY, brimY);
    }

    /**
     * Halo (nimbus) render — world-space rotating spiral of camera-facing additive billboards
     * above the head. Ported from wonderful Cosmetics.renderNimbus / renderNimbusBillboard,
     * using the same camera-billboard technique as TargetESP.drawRing so each glow segment is
     * a real 3D sprite (faces the camera from any angle), not a flat 2D quad.
     */
    private final EventListener<Render3DEvent> onRender3DHalo = event -> {
        if (!mode.is(modeHalo)) return;
        if (mc.player == null || mc.world == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        MatrixStack ms = event.getMatrices();
        Vec3d cameraPos = camera.getPos();
        long nowMs = System.currentTimeMillis();
        double radiansPerMillisecond = NIMBUS_SPEED * Math.PI / 180.0 / 1000.0;

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, NIMBUS_TEX);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldRenderHaloFor(player)) continue;
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;

            float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
            double px = MathHelper.lerp(tickDelta, player.prevX, player.getX());
            double py = MathHelper.lerp(tickDelta, player.prevY, player.getY()) + player.getHeight() + 0.1;
            double pz = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

            for (int arm = 0; arm < NIMBUS_ARMS; arm++) {
                double baseAngle = radiansPerMillisecond * nowMs + arm * (Math.PI * 2.0 / NIMBUS_ARMS);
                for (int segment = 0; segment < NIMBUS_SEGMENTS; segment++) {
                    double segmentAngle = baseAngle - segment * NIMBUS_STEP_RADIANS;
                    double offsetX = Math.cos(segmentAngle) * NIMBUS_RADIUS;
                    double offsetZ = Math.sin(segmentAngle) * NIMBUS_RADIUS;

                    float progress = segment / (float) Math.max(1, NIMBUS_SEGMENTS - 1);
                    float size = NIMBUS_BASE_SIZE * (1.0f - progress * 0.7f);
                    int alpha = MathHelper.clamp(NIMBUS_MAX_ALPHA - segment * NIMBUS_ALPHA_FALLOFF, 0, NIMBUS_MAX_ALPHA);
                    if (alpha <= 0) continue;

                    int segRgb = this.getGradientColor(arm * 180f + segment * (360f / NIMBUS_SEGMENTS));
                    ColorRGBA segColor = ColorRGBA.fromInt(segRgb).withAlpha((float) alpha);

                    ms.push();
                    ms.translate(px - cameraPos.x + offsetX, py - cameraPos.y, pz - cameraPos.z + offsetZ);
                    ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                    ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                    DrawUtility.drawImage(ms, buffer, -size / 2.0, -size / 2.0, 0.0, size, size, segColor);
                    ms.pop();
                }
            }
        }

        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    };

    /** Local player always; others only when renderOnFriends is on (matches feature-renderer rule). */
    private boolean shouldRenderHaloFor(PlayerEntity player) {
        if (mc.player == null) return false;
        if (player == mc.player) return true;
        return renderOnFriends.isEnabled();
    }

    private void renderOutline(MatrixStack matrices, int step, float radius, float centerY, float brimY) {
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
                || mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
            return;
        }

        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(4.0f);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        MatrixStack.Entry entry = matrices.peek();

        for (int i = 0; i < 360; i += step) {
            float x1 = MathHelper.sin(i * MathHelper.RADIANS_PER_DEGREE) * radius;
            float z1 = -MathHelper.cos(i * MathHelper.RADIANS_PER_DEGREE) * radius;
            float x2 = MathHelper.sin((i + step) * MathHelper.RADIANS_PER_DEGREE) * radius;
            float z2 = -MathHelper.cos((i + step) * MathHelper.RADIANS_PER_DEGREE) * radius;

            int dc1 = darker(getGradientColor(i), 1) | 0xFF000000;
            int dc2 = darker(getGradientColor(i + step), 1) | 0xFF000000;

            vertexLine(entry, buffer, x1, brimY, z1, x2, brimY, z2, dc1, dc2);
        }

        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void vertexLine(MatrixStack.Entry entry, VertexConsumer buffer,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            int color1, int color2) {
        org.joml.Vector3f dir = new org.joml.Vector3f(x2 - x1, y2 - y1, z2 - z1).normalize();
        buffer.vertex(entry, x1, y1, z1).color(color1).normal(entry, dir.x, dir.y, dir.z);
        buffer.vertex(entry, x2, y2, z2).color(color2).normal(entry, dir.x, dir.y, dir.z);
    }

    /**
     * Per-angle color. Rainbow on: hue rotates over time and across the angle (each point a
     * different hue). Rainbow off: the single color1. Used by both Hat gradient and Halo.
     */
    private int getGradientColor(float angle) {
        if (this.rainbow.isEnabled()) {
            float normalizedAngle = angle % 360.0f;
            if (normalizedAngle < 0) normalizedAngle += 360.0f;
            float timeFactor = (System.currentTimeMillis() % 3000L) / 3000.0f;
            float hue = (timeFactor + normalizedAngle / 360.0f) % 1.0f;
            return ColorRGBA.fromHSB(hue, 1.0f, 1.0f).getRGB();
        }
        return color1.getColor().getRGB();
    }

    private int darker(int color, int factor) {
        int r = Math.max(0, ((color >> 16) & 0xFF) - factor * 20);
        int g = Math.max(0, ((color >> 8) & 0xFF) - factor * 20);
        int b = Math.max(0, (color & 0xFF) - factor * 20);
        int a = (color >> 24) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
