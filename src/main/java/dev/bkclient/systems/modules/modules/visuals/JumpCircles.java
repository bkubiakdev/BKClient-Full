package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.RenderUtility;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(name = "Jump Circles", category = ModuleCategory.VISUALS)
public class JumpCircles extends BaseModule {

    // Style
    private final ModeSetting style = new ModeSetting(this, "Style");
    private final ModeSetting.Value liquid = new ModeSetting.Value(style, "Liquid");
    private final ModeSetting.Value circle = new ModeSetting.Value(style, "Circle");
    private final ModeSetting.Value bold = new ModeSetting.Value(style, "Bold");
    private final ModeSetting.Value hexagon = new ModeSetting.Value(style, "Hexagon");
    private final ModeSetting.Value portal = new ModeSetting.Value(style, "Portal");
    private final ModeSetting.Value scifi = new ModeSetting.Value(style, "Sci-Fi");
    private final ModeSetting.Value soup = new ModeSetting.Value(style, "Soup");

    // Easing Mode
    private final ModeSetting easing = new ModeSetting(this, "Easing", () -> this.liquid.isSelected() && !this.advancedLiquid.isEnabled());
    private final ModeSetting.Value elasticOut = new ModeSetting.Value(easing, "Elastic");
    private final ModeSetting.Value backOut = new ModeSetting.Value(easing, "Back Out");
    private final ModeSetting.Value linear = new ModeSetting.Value(easing, "Linear");

    private final ModeSetting colorMode = new ModeSetting(this, "Color Mode", this::hideColors);
    private final ModeSetting.Value customColorMode = new ModeSetting.Value(colorMode, "Custom");
    private final ModeSetting.Value clientColorMode = new ModeSetting.Value(colorMode, "Client");
    private final ModeSetting.Value rainbowColorMode = new ModeSetting.Value(colorMode, "Rainbow");

    // General Sliders
    private final SliderSetting liveTime = new SliderSetting(this, "Live Time")
            .min(0.5f).max(5.0f).currentValue(2.5f).step(0.1f).suffix("s");
    private final SliderSetting scale = new SliderSetting(this, "Scale")
            .min(10f).max(300f).currentValue(75f).step(1f).suffix("%");
    private final SliderSetting spinSpeed = new SliderSetting(this, "Spin Speed", () -> this.liquid.isSelected())
            .min(0f).max(360f).currentValue(180f).step(1f).suffix("deg/s");
    private final SliderSetting alpha = new SliderSetting(this, "Alpha", () -> this.liquid.isSelected())
            .min(0f).max(255f).currentValue(128f).step(1f);
    private final BooleanSetting fadeOut = new BooleanSetting(this, "Fade Out", () -> this.liquid.isSelected()).enabled(true);

    private final ColorSetting color = new ColorSetting(this, "Color", () -> hideColors() || !this.customColorMode.isSelected()).color(new ColorRGBA(100, 180, 255));
    private final BooleanSetting useSecondColor = new BooleanSetting(this, "Second Color", () -> hideColors() || !this.customColorMode.isSelected() || this.liquid.isSelected() && !this.advancedLiquid.isEnabled()).enabled(false);
    private final ColorSetting secondColor = new ColorSetting(this, "Second Color Picker", () -> hideColors() || !this.customColorMode.isSelected() || !this.useSecondColor.isEnabled() || this.liquid.isSelected() && !this.advancedLiquid.isEnabled()).color(new ColorRGBA(220, 100, 255));

    // Liquid / Distortion Settings
    private final SliderSetting distortStrength = new SliderSetting(this, "Distort Strength", () -> !this.liquid.isSelected())
            .min(0.1f).max(3.0f).currentValue(1.5f).step(0.1f);
    private final SliderSetting distortThickness = new SliderSetting(this, "Distort Width", this::hideLiquidDetails)
            .min(0.1f).max(2.0f).currentValue(1.0f).step(0.05f);
    private final BooleanSetting distortWarp = new BooleanSetting(this, "Turbulence", this::hideLiquidDetails).enabled(true);
    private final SliderSetting distortWarpStrength = new SliderSetting(this, "Turbulence Strength", () -> hideLiquidDetails() || !this.distortWarp.isEnabled())
            .min(0.1f).max(2.0f).currentValue(1.0f).step(0.1f);

    // Liquid Glow / Smoke
    private final BooleanSetting distortGlow = new BooleanSetting(this, "Liquid Smoke", () -> !this.liquid.isSelected()).enabled(false);
    private final SliderSetting distortGlowHeight = new SliderSetting(this, "Smoke Height", () -> hideLiquidDetails() || !this.distortGlow.isEnabled())
            .min(10f).max(50f).currentValue(30f).step(1f);
    private final SliderSetting distortGlowWidth = new SliderSetting(this, "Smoke Width", () -> hideLiquidDetails() || !this.distortGlow.isEnabled())
            .min(5f).max(30f).currentValue(18f).step(1f);
    private final SliderSetting distortGlowTint = new SliderSetting(this, "Smoke Tint", () -> hideLiquidDetails() || !this.distortGlow.isEnabled())
            .min(0f).max(100f).currentValue(50f).step(1f).suffix("%");
    private final SliderSetting distortGlowAlpha = new SliderSetting(this, "Smoke Alpha", () -> hideLiquidDetails() || !this.distortGlow.isEnabled())
            .min(0f).max(100f).currentValue(50f).step(1f).suffix("%");

    private final BooleanSetting distortTint = new BooleanSetting(this, "Wave Tint", () -> !this.liquid.isSelected()).enabled(true);
    private final SliderSetting distortTintStrength = new SliderSetting(this, "Tint Strength", () -> hideLiquidDetails() || !this.distortTint.isEnabled())
            .min(0f).max(100f).currentValue(55f).step(1f).suffix("%");
    private final SliderSetting distortSaturation = new SliderSetting(this, "Saturation", this::hideLiquidDetails)
            .min(-1.0f).max(1.0f).currentValue(0.0f).step(0.05f);

    // Keep existing config keys/values; this switch only changes the amount of detail in the UI.
    private final BooleanSetting advancedLiquid = new BooleanSetting(this, "Advanced Liquid", () -> !this.liquid.isSelected());

    private boolean hideLiquidDetails() {
        return !this.liquid.isSelected() || !this.advancedLiquid.isEnabled();
    }

    private boolean hideColors() {
        return this.liquid.isSelected() && !this.distortTint.isEnabled() && !this.distortGlow.isEnabled();
    }

    private final List<JumpCircle> circles = new CopyOnWriteArrayList<>();
    private final float[] uniformScratch = new float[412];
    private final Matrix4f invViewProj = new Matrix4f();
    private boolean wasJumping = false;

    @Override
    public void onDisable() {
        circles.clear();
        wasJumping = false;
        super.onDisable();
    }

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (mc.player == null)
            return;

        boolean jumping = !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isSubmergedInWater();

        if (jumping && !wasJumping) {
            circles.add(new JumpCircle(mc.player.getPos().add(0, 0.01, 0)));
        }

        wasJumping = jumping;
        circles.removeIf(JumpCircle::isExpired);
    };

    public final EventListener<Render3DEvent> onRender3D = event -> {
        if (circles.isEmpty() || liquid.isSelected())
            return;

        RenderUtility.setupRender3D(true);

        for (JumpCircle c : circles) {
            c.renderTexturedQuad(event.getMatrices(), event.getCamera());
        }

        RenderUtility.endRender3D();
    };

    /**
     * Post-processing hook invoked after world rendering.
     */
    public void onRenderWorldPost(Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera) {
        if (!isEnabled() || !liquid.isSelected() || circles.isEmpty() || DrawUtility.jumpDistortPost == null) {
            return;
        }

        Framebuffer framebuffer = mc.getFramebuffer();
        if (framebuffer == null || camera == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
            return;
        }

        long l = System.currentTimeMillis();
        float f = Math.max(0.1f, liveTime.getCurrentValue());
        float f2 = (scale.getCurrentValue() / 100f) * 2.0f;
        float f3 = Math.max(0.15f, f2 * distortThickness.getCurrentValue() / 2.0f);
        float f4 = 0.018f * distortStrength.getCurrentValue();
        float f5 = (float) framebuffer.textureWidth / (float) Math.max(1, framebuffer.textureHeight);
        boolean bl = distortTint.isEnabled();
        boolean bl2 = distortGlow.isEnabled();
        boolean bl3 = bl || bl2;
        Vec3d vec3d = camera.getPos();

        this.invViewProj.set(projectionMatrix).mul(positionMatrix).invert();
        float[] fArray = this.uniformScratch;
        int n = 0;
        float f6 = 0.0f;

        for (JumpCircle jumpRenderer : this.circles) {
            if (n >= 16) break;
            float f7 = jumpRenderer.getProgress(f);
            if (f7 < 0.0f || f7 >= 1.0f) continue;

            float f8 = this.getEasingProgress(f7);
            float f9 = MathHelper.clamp(f7 / 0.1f, 0.0f, 1.0f);
            float f10 = MathHelper.clamp((1.0f - f7) / 0.3f, 0.0f, 1.0f);
            float f11 = f9 * f10;
            float f12 = f4 * f11;

            int n3 = 28 + n * 8;
            fArray[n3] = (float) (jumpRenderer.position.x - vec3d.x);
            fArray[n3 + 1] = (float) (jumpRenderer.position.y - vec3d.y);
            fArray[n3 + 2] = (float) (jumpRenderer.position.z - vec3d.z);
            fArray[n3 + 3] = f8 * f2;
            fArray[n3 + 4] = f3;
            fArray[n3 + 5] = f12;
            fArray[n3 + 6] = f2;
            fArray[n3 + 7] = f11;

            if (bl3) {
                int n4 = 156 + n * 4 * 4;
                int n5 = (int) f6;
                putColor(fArray, n4, this.getColorAt(n5, 1.0f));
                putColor(fArray, n4 + 4, this.getColorAt(90 + n5, 1.0f));
                putColor(fArray, n4 + 8, this.getColorAt(180 + n5, 1.0f));
                putColor(fArray, n4 + 12, this.getColorAt(270 + n5, 1.0f));
            }

            f6 += 45.0f * (1.0f - f7);
            ++n;
        }

        if (n == 0) {
            return;
        }

        fArray[0] = n;
        fArray[1] = f5;
        fArray[2] = (float) (l % 100000L) / 1000.0f;
        fArray[3] = this.distortWarp.isEnabled() ? 0.01f * this.distortWarpStrength.getCurrentValue() : 0.0f;
        fArray[4] = bl ? MathHelper.clamp(this.distortTintStrength.getCurrentValue() / 100.0f, 0.0f, 1.0f) : 0.0f;
        fArray[5] = MathHelper.clamp(1.0f + this.distortSaturation.getCurrentValue(), 0.0f, 2.0f);
        fArray[6] = bl2 ? 1.0f : 0.0f;
        fArray[7] = 0.6f;
        fArray[8] = MathHelper.clamp(this.distortGlowHeight.getCurrentValue() / 100.0f, 0.05f, 5.0f);
        fArray[9] = MathHelper.clamp(this.distortGlowWidth.getCurrentValue() / 100.0f, 0.05f, 5.0f);
        fArray[10] = MathHelper.clamp(this.distortGlowTint.getCurrentValue() / 100.0f, 0.0f, 1.0f);
        fArray[11] = MathHelper.clamp(this.distortGlowAlpha.getCurrentValue() / 100.0f, 0.0f, 1.0f);

        this.invViewProj.get(fArray, 12);

        DrawUtility.jumpDistortPost.apply(framebuffer, fArray);
    }

    public float getEasingProgress(float t) {
        float clamped = MathHelper.clamp(t, 0.0f, 1.0f);
        if (elasticOut.isSelected()) return elasticOut(clamped);
        if (backOut.isSelected()) return backOut(clamped);
        return clamped;
    }

    private static float backOut(float f) {
        return (float) (1.0 + 2.70158 * Math.pow((double) f - 1.0, 3.0) + 1.70158 * Math.pow((double) f - 1.0, 2.0));
    }

    private static float elasticOut(float f) {
        if (f <= 0.0f) return 0.0f;
        if (f >= 1.0f) return 1.0f;
        return (float) (Math.pow(2.0, -10.0 * (double) f) * Math.sin(((double) f * 10.0 - 0.75) * 2.0943951023931953) + 1.0);
    }

    public int getColorAt(int offsetDeg, float alphaMultiplier) {
        if (rainbowColorMode.isSelected()) {
            int hue = (int) ((System.currentTimeMillis() / 8L + offsetDeg) % 360L);
            float[] hsb = new float[]{(float) hue / 360f, 1.0f, 1.0f};
            int rgb = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
            int a = Math.round(MathHelper.clamp(alphaMultiplier, 0.0f, 1.0f) * 255.0f);
            return (a << 24) | (rgb & 0x00FFFFFF);
        }

        if (clientColorMode.isSelected()) {
            ColorRGBA c1 = Colors.getAccentColor();
            ColorRGBA c2 = c1.mix(new ColorRGBA(255f, 120f, 220f), 0.45f);
            float blend = (float) (Math.sin((System.currentTimeMillis() + offsetDeg * 10L) / 400.0) + 1.0) / 2.0f;
            ColorRGBA interpolated = c1.mix(c2, blend);
            int a = Math.round(MathHelper.clamp(alphaMultiplier, 0.0f, 1.0f) * 255.0f);
            return (a << 24) | (interpolated.getRGB() & 0x00FFFFFF);
        }

        if (useSecondColor.isEnabled()) {
            ColorRGBA c1 = color.getColor();
            ColorRGBA c2 = secondColor.getColor();
            float blend = (float) (Math.sin((System.currentTimeMillis() + offsetDeg * 10L) / 400.0) + 1.0) / 2.0f;
            ColorRGBA interpolated = c1.mix(c2, blend);
            int a = Math.round(MathHelper.clamp(alphaMultiplier, 0.0f, 1.0f) * c1.getAlpha());
            return (a << 24) | (interpolated.getRGB() & 0x00FFFFFF);
        }

        ColorRGBA c = color.getColor();
        int a = Math.round(MathHelper.clamp(alphaMultiplier, 0.0f, 1.0f) * c.getAlpha());
        return (a << 24) | (c.getRGB() & 0x00FFFFFF);
    }

    private static void putColor(float[] array, int offset, int rgba) {
        array[offset]     = (float) ((rgba >> 16) & 0xFF) / 255.0f;
        array[offset + 1] = (float) ((rgba >> 8) & 0xFF) / 255.0f;
        array[offset + 2] = (float) (rgba & 0xFF) / 255.0f;
        array[offset + 3] = (float) ((rgba >> 24) & 0xFF) / 255.0f;
    }

    private class JumpCircle {
        private final Vec3d position;
        private final long startTime;
        private float rotationAngle;
        private long lastUpdateTime;

        public JumpCircle(Vec3d position) {
            this.position = position;
            this.startTime = System.currentTimeMillis();
            this.lastUpdateTime = startTime;
            this.rotationAngle = 0f;
        }

        public float getProgress(float totalLifetimeSec) {
            float elapsed = (System.currentTimeMillis() - startTime) / 1000f;
            return elapsed / totalLifetimeSec;
        }

        public boolean isExpired() {
            float elapsed = (System.currentTimeMillis() - startTime) / 1000f;
            return elapsed > liveTime.getCurrentValue();
        }

        public void renderTexturedQuad(MatrixStack matrices, Camera camera) {
            updateRotation();
            long elapsed = System.currentTimeMillis() - startTime;
            float rawProgress = (float) elapsed / (liveTime.getCurrentValue() * 1000f);
            float eased = getEasingProgress(rawProgress);
            float interpolatedRadius = eased * (scale.getCurrentValue() / 100f) * 2.0f;

            float finalAlpha = alpha.getCurrentValue();
            if (fadeOut.isEnabled()) {
                finalAlpha *= (1.0f - MathHelper.clamp(rawProgress, 0f, 1f));
            }

            String textureName = circle.isSelected() ? "circle.png"
                    : bold.isSelected() ? "circle_bold_unblack.png"
                    : hexagon.isSelected() ? "hexagon_unblack.png"
                    : portal.isSelected() ? "portal_unblack.png"
                    : scifi.isSelected() ? "scifi.png"
                    : soup.isSelected() ? "circle_unblack.png"
                    : "circle.png";

            Identifier texture = BKClientInit.id("textures/jump_circles/" + textureName);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader((ShaderProgramKey) ShaderProgramKeys.POSITION_TEX_COLOR);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR);

            int c = getColorAt(0, finalAlpha / 255.0f);
            float scaleValue = interpolatedRadius * 2;

            matrices.push();
            RenderUtility.prepareMatrices(matrices, position);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.toDegrees(rotationAngle)));
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            buffer.vertex(matrix, -interpolatedRadius, -interpolatedRadius + scaleValue, 0)
                    .texture(0, 1)
                    .color(c);
            buffer.vertex(matrix, -interpolatedRadius + scaleValue, -interpolatedRadius + scaleValue, 0)
                    .texture(1, 1)
                    .color(c);
            buffer.vertex(matrix, -interpolatedRadius + scaleValue, -interpolatedRadius, 0)
                    .texture(1, 0)
                    .color(c);
            buffer.vertex(matrix, -interpolatedRadius, -interpolatedRadius, 0)
                    .texture(0, 0)
                    .color(c);

            matrices.pop();

            RenderUtility.buildBuffer(buffer);
        }

        private void updateRotation() {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastUpdateTime) / 1000f;
            lastUpdateTime = currentTime;

            deltaTime = Math.min(deltaTime, 0.1f);
            float angularVelocity = (float) Math.toRadians(spinSpeed.getCurrentValue());

            float totalLifetime = liveTime.getCurrentValue();
            float elapsedTime = (currentTime - startTime) / 1000f;
            float remainingFraction = MathHelper.clamp((totalLifetime - elapsedTime) / totalLifetime, 0, 1);

            rotationAngle += angularVelocity * deltaTime * remainingFraction;
        }
    }
}
