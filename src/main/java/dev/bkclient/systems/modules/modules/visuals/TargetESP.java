package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.combatengine.bridge.AuraRuntime;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.LinkedModeSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.render.CrystalRenderer;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.RenderUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.ArrayList;
import dev.bkclient.framework.shader.impl.TargetLensPost;
import dev.bkclient.framework.shader.impl.ParticleLightPost;

@ModuleInfo(name = "Target ESP", category = ModuleCategory.VISUALS, desc = "Highlights active target")
public class TargetESP extends BaseModule {
    public TargetESP() {
        new LinkedModeSetting(this, "color.source", () -> false, this.effectThemeSync, this.rainbow)
            .choice("Theme", () -> !rainbow.isEnabled() && usesThemeColor() && effectThemeSync.isEnabled(),
                () -> { rainbow.setEnabled(false); effectThemeSync.setEnabled(true); }, () -> !usesThemeColor())
            .choice("Custom", () -> (!rainbow.isEnabled() || !supportsRainbow()) && (!usesThemeColor() || !effectThemeSync.isEnabled()),
                () -> { rainbow.setEnabled(false); effectThemeSync.setEnabled(false); })
            .choice("Rainbow", () -> rainbow.isEnabled() && supportsRainbow(),
                () -> { rainbow.setEnabled(true); effectThemeSync.setEnabled(false); }, () -> !supportsRainbow());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES)
            .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                public Identifier getFabricId() { return BKClientInit.id("target_scan"); }
                public void reload(net.minecraft.resource.ResourceManager manager) { scanRenderer.invalidate(); }
            });
    }


    private final ModeSetting targetSource = new ModeSetting(this, "modules.settings.target_esp.target_source");
    private final ModeSetting.Value crosshairSource = new ModeSetting.Value(targetSource, "modules.settings.target_esp.source.crosshair").select();
    private final ModeSetting.Value auraSource = new ModeSetting.Value(targetSource, "modules.settings.target_esp.source.aura");
    private ModeSetting.Value lastTargetSource;

    private final ModeSetting mode = new ModeSetting(this, "modules.settings.target_esp.mode");
    private final ModeSetting.Value souls = new ModeSetting.Value(this.mode,
            "modules.settings.target_esp.mode.souls").select();
    private final ModeSetting.Value crystals = new ModeSetting.Value(this.mode,
            "modules.settings.target_esp.mode.crystals");
    private final BooleanSetting distortion = new BooleanSetting(this,
            "modules.settings.target_esp.distortion", () -> !this.isRefractiveMode()).enabled(true);
    private final SliderSetting distortionStrength = new SliderSetting(this,
            "modules.settings.target_esp.distortion_strength", () -> !this.isRefractiveMode() || !this.distortion.isEnabled())
            .min(0.05F).max(0.15F).step(0.01F).currentValue(0.10F);
    private final BooleanSetting effectLighting = new BooleanSetting(this,
            "modules.settings.target_esp.lighting", () -> !this.isRefractiveMode());
    private final BooleanSetting effectThemeSync = new BooleanSetting(this,
            "theme.sync", () -> !this.isRefractiveMode()).enabled(true);
    private final Animation effectOcclusion = new Animation(350L, Easing.BOTH_CUBIC);
    private float effectPassAlpha = 1.0F;
    private final ModeSetting.Value marker = new ModeSetting.Value(this.mode,
            "modules.settings.target_esp.mode.marker");
    private final ModeSetting.Value circles = new ModeSetting.Value(this.mode,
            "modules.settings.target_esp.mode.circles");
    private final ModeSetting.Value ring = new ModeSetting.Value(this.mode,
            "modules.settings.target_esp.mode.ring");
    private final ModeSetting.Value ring2 = new ModeSetting.Value(this.mode,
            "modules.settings.target_esp.mode.ring2");
    private final ModeSetting.Value ghosts = new ModeSetting.Value(this.mode,
            "modules.settings.target_esp.mode.ghosts");

    private final ModeSetting.Value glassGhosts = new ModeSetting.Value(this.mode,
            "modules.settings.target_esp.mode.glass_ghosts");

    private final ModeSetting.Value cubes = new ModeSetting.Value(this.mode, "Cubes");
    private final dev.bkclient.utility.render.TargetCubesRenderer cubeRenderer = new dev.bkclient.utility.render.TargetCubesRenderer();
    private final ModeSetting.Value fragScan = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.frag_scan");
    private final SliderSetting scanDuration = new SliderSetting(this, "modules.settings.target_esp.scan_duration", () -> !fragScan.isSelected())
            .min(.5f).max(6).currentValue(1.6f).step(.1f).suffix("s");
    private final SliderSetting scanWidth = new SliderSetting(this, "modules.settings.target_esp.scan_width", () -> !fragScan.isSelected())
            .min(5).max(80).currentValue(30).step(1).suffix("%");
    private final SliderSetting scanStrength = new SliderSetting(this, "modules.settings.target_esp.scan_strength", () -> !fragScan.isSelected())
            .min(.1f).max(2).currentValue(1).step(.05f);
    private final BooleanSetting scanThroughWalls = new BooleanSetting(this, "modules.settings.target_esp.scan_through_walls", () -> !fragScan.isSelected());
    private final dev.bkclient.utility.render.TargetScanRenderer scanRenderer = new dev.bkclient.utility.render.TargetScanRenderer();

    private final ModeSetting design = new ModeSetting(this, "modules.settings.target_esp.design") {
        @Override
        public boolean isVisible() {
            return marker.isSelected();
        }
    };
    private final ModeSetting.Value amongus = new ModeSetting.Value(this.design, "Among Us");
    private final ModeSetting.Value skull = new ModeSetting.Value(this.design, "Skull");
    private final ModeSetting.Value vegas = new ModeSetting.Value(this.design, "Vegas");
    private final ModeSetting.Value markerIcon = new ModeSetting.Value(this.design, "Marker").select();
    private final ModeSetting.Value scifi = new ModeSetting.Value(this.design, "Sci-Fi");
    private final ModeSetting.Value simple = new ModeSetting.Value(this.design, "Simple");
    private final ModeSetting.Value bo = new ModeSetting.Value(this.design, "Bo");
    private final ModeSetting.Value jeka = new ModeSetting.Value(this.design, "Jeka");
    private final ModeSetting.Value legacy = new ModeSetting.Value(this.design, "Legacy");
    private final ModeSetting.Value nova = new ModeSetting.Value(this.design, "Nova");

    private final ModeSetting animationStyle = new ModeSetting(this,
            "modules.settings.target_esp.animation_style") {
        @Override
        public boolean isVisible() {
            return marker.isSelected();
        }
    };
    private final ModeSetting.Value soulsStyle = new ModeSetting.Value(this.animationStyle,
            "modules.settings.target_esp.animation_style.souls").select();
    private final ModeSetting.Value crystalsStyle = new ModeSetting.Value(this.animationStyle,
            "modules.settings.target_esp.animation_style.crystals");

    private final BooleanSetting players = new BooleanSetting(this, "modules.settings.target_esp.players", () -> auraSource.isSelected())
            .enabled(true);
    private final BooleanSetting mobs = new BooleanSetting(this, "modules.settings.target_esp.mobs", () -> auraSource.isSelected()).enabled(true);

    private final SliderSetting speed = new SliderSetting(this, "modules.settings.target_esp.speed", () -> fragScan.isSelected())
            .min(0.1f).max(1.0f).step(0.1f).currentValue(0.5f);
    private final SliderSetting size = new SliderSetting(this, "modules.settings.target_esp.size", () -> fragScan.isSelected())
            .min(0.1f).max(1.0f).step(0.1f).currentValue(0.5f);
    private final SliderSetting particleThickness = new SliderSetting(this,
            "modules.settings.target_esp.particle_thickness",
            () -> this.ring.isSelected() || this.ring2.isSelected() || this.fragScan.isSelected())
            .min(0.1f).max(2.0f).step(0.1f).currentValue(1.0f);
    private final BooleanSetting changeColorOnDamage = new BooleanSetting(this,
            "modules.settings.target_esp.change_color_on_damage").enabled(true);
    private final BooleanSetting rainbow = new BooleanSetting(this, "modules.settings.target_esp.rainbow");

    private final ColorSetting colorTarget = new ColorSetting(this, "color",
            () -> this.rainbow.isEnabled() && this.supportsRainbow() || this.usesThemeColor() && this.effectThemeSync.isEnabled()).color(Colors.ACCENT);

    private final Animation animation = new Animation(300L, 0.0f, Easing.BOTH_CUBIC);
    private final Animation moving = new Animation(70L, 0.0f, Easing.LINEAR);
    private final Animation markerRotation = new Animation(50L, 0.0f, Easing.LINEAR);

    private LivingEntity target;
    private LivingEntity prevTarget;
    private float ringDamageFlashIntensity = 0.0f;
    private long lastRingDamageTime = 0L;
    private static final long RING_DAMAGE_FLASH_DURATION = 300L;

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (mc.world == null || mc.player == null) return;
        if (lastTargetSource != targetSource.getValue()) {
            onWorldChanged();
            lastTargetSource = targetSource.getValue();
        }
        LivingEntity targetFound = this.getTarget();
        // Aura-only mode must never retain a crosshair target or show a stale combat target.
        if (auraSource.isSelected() && targetFound == null) {
            onWorldChanged();
            return;
        }
        if (fragScan.isSelected()) {
            cubeRenderer.clear();
            // Damage feedback belongs to the current scan target, not a previous ESP mode.
            ColorRGBA scanColor = effectThemeSync.isEnabled() ? Colors.getAccent() : colorTarget.getColor();
            if (rainbow.isEnabled()) scanColor = ColorRGBA.fromHSB((System.currentTimeMillis() % 3000L) / 3000.0F, 1, 1);
            if (changeColorOnDamage.isEnabled() && targetFound != null) scanColor = scanColor.mix(ColorRGBA.RED, targetFound.hurtTime / 10f);
            scanRenderer.render(event, targetFound, scanColor, scanDuration.getCurrentValue(), scanWidth.getCurrentValue() / 100f,
                    scanStrength.getCurrentValue(), scanThroughWalls.isEnabled());
            return;
        }
        scanRenderer.close();
        if (this.cubes.isSelected()) {
            try {
                cubeRenderer.render(event, targetFound, this.colorTarget.getColor().getRGB(), this.changeColorOnDamage.isEnabled());
            } finally {
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
                RenderSystem.setShaderTexture(0, 0);
            }
            return;
        }
        cubeRenderer.clear();
        if (targetFound != null) {
            this.target = targetFound;
        }

        this.animation.setEasing(Easing.FIGMA_EASE_IN_OUT);
        this.animation.update(targetFound != null);

        float speedMul = this.speed.getCurrentValue();
        if (this.crystals.isSelected()) speedMul *= 2.0F;
        this.moving.update(this.moving.getValue() + 10.0f * speedMul + 50.0f * speedMul);
        this.markerRotation.update(this.markerRotation.getValue() + 5.0f * speedMul);

        if (targetFound != null && this.prevTarget == targetFound) {
            if (targetFound.hurtTime > 0 && this.ring.isSelected()) {
                this.lastRingDamageTime = System.currentTimeMillis();
            }
        }

        if (this.target != null) {
            this.prevTarget = this.target;
        }

        if (this.prevTarget == null || this.animation.getValue() == 0.0f) {
            return;
        }

        if (this.isRefractiveMode()) {
            this.renderRefractiveEffect(event, this.prevTarget);
            return;
        }

        MatrixStack ms = event.getMatrices();
        ms.push();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();

        if (mc.world.raycast(new RaycastContext(
                mc.gameRenderer.getCamera().getPos(),
                this.prevTarget.getEyePos(),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                (Entity) mc.player)).getType() != HitResult.Type.MISS) {
            RenderSystem.disableDepthTest();
        }

        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        if (this.circles.isSelected()) {
            this.drawCircles(ms, this.prevTarget);
        } else if (this.ring.isSelected()) {
            this.drawRing(ms, this.prevTarget);
        } else if (this.ring2.isSelected()) {
            this.drawRing2(ms, event.getCamera(), this.prevTarget);
        } else if (this.ghosts.isSelected()) {
            this.drawGhostsVurst(ms, event.getCamera(), this.prevTarget);
        } else if (this.marker.isSelected()) {
            this.drawMarker(ms, event.getCamera(), this.prevTarget);
        } else if (this.crystals.isSelected()) {
            this.drawCrystals(ms, event.getCamera(), this.prevTarget);
        } else {
            this.drawGhosts(ms, event.getCamera(), this.prevTarget);
        }

        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        ms.pop();
    };


    private void drawCircles(MatrixStack ms, LivingEntity target) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d targetPos = this.getRenderPos(target);

        double entX = targetPos.x - cameraPos.getX();
        double entY = targetPos.y - cameraPos.getY();
        double entZ = targetPos.z - cameraPos.getZ();

        float movingValue = this.moving.getValue();
        float width = target.getWidth() * 1.45f * this.getSizeMapped();
        float baseVal = Math.max(0.5f, 0.7f + 0.1f - 0.1f * this.animation.getValue());

        ColorRGBA color = this.colorTarget.getColor();

        RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        int step = 3;
        float particleSize = 0.4f * this.getSizeMapped();
        float bloomSize = 0.8f * this.getSizeMapped();
        int alpha = (int) (this.animation.getValue() * 255);
        ColorRGBA c = color.withAlpha(alpha);
        int bloomRgb = c.withAlpha((int) (alpha * 0.1f)).getRGB();
        int coreRgb = c.getRGB();
        // The vertical position does not depend on the angle.
        double radAngle = Math.toRadians(movingValue);
        float waveValue = (float) ((1.0 - Math.cos(radAngle)) / 2.0);
        float yPos = (float) (entY + target.getHeight() * waveValue);
        // Billboard rotation computed once (see drawRing2); each particle only takes its translation.
        Matrix4f base = ms.peek().getPositionMatrix();
        Matrix4f billboard = new Matrix4f(base).translate(0.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Matrix4f translated = this.ring2Scratch;

        BufferBuilder bloomBuffer = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < 360; i += step) {
            if ((int) (i / 45.0f) % 2 != 0) {
                double rad = Math.toRadians(i + movingValue);
                float sin = (float) (entX + Math.sin(rad) * width * baseVal);
                float cos = (float) (entZ + Math.cos(rad) * width * baseVal);
                moveBillboard(base, billboard, translated, sin, yPos, cos);
                drawImage(bloomBuffer, billboard,
                        -bloomSize / 2.0, -bloomSize / 2.0, 0.0, bloomSize, bloomSize, bloomRgb);
            }
        }
        RenderUtility.buildBuffer(bloomBuffer);

        BufferBuilder coreBuffer = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < 360; i += step) {
            if ((int) (i / 45.0f) % 2 != 0) {
                double rad = Math.toRadians(i + movingValue);
                float sin = (float) (entX + Math.sin(rad) * width * baseVal);
                float cos = (float) (entZ + Math.cos(rad) * width * baseVal);
                moveBillboard(base, billboard, translated, sin, yPos, cos);
                drawImage(coreBuffer, billboard,
                        -particleSize / 2.0, -particleSize / 2.0, 0.0, particleSize, particleSize, coreRgb);
            }
        }
        RenderUtility.buildBuffer(coreBuffer);
    }


    private void drawMarker(MatrixStack ms, Camera camera, LivingEntity target) {
        ColorRGBA color = this.colorTarget.getColor();
        String designName = this.design.getValue().getName().toLowerCase().replace(" ", "");
        // "markernowy" texture for Nova design
        String texName = designName.equals("nova") ? "markernowy" : designName;
        Identifier id = BKClientInit.id("textures/target_render/" + texName + ".png");

        RenderSystem.setShaderTexture(0, id);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);

        float width = this.prevTarget.getWidth() * 1.4f * this.getSizeMapped();
        RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));

        if (this.crystalsStyle.isSelected()) {
            int step = 30;
            for (int i = 0; i < 360; i += step) {
                float val = 1.1f - 0.2f * this.animation.getValue();
                float sin = (float) (MathUtility.sin(Math.toRadians((float) i + this.moving.getValue() * 0.8f))
                        * (double) width * (double) val);
                float cos = (float) (MathUtility.cos(Math.toRadians((float) i + this.moving.getValue() * 0.8f))
                        * (double) width * (double) val);

                ms.push();
                ms.translate((double) sin,
                        (double) 0.1f + (double) target.getHeight() * Math.abs(MathUtility.sin(Math.toRadians(i))),
                        (double) cos);
                ms.multiply(camera.getRotation());
                if (designName.contains("amongus")) {
                    ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
                }
                float sz = 0.35f * this.getSizeMapped();
                DrawUtility.drawImage(ms, builder, -sz / 2.0, -sz / 2.0, 0, sz, sz,
                        color.withAlpha(255.0f * this.animation.getValue()));
                ms.pop();
            }
        } else if (this.soulsStyle.isSelected()) {
            int step = 40;
            for (int i = 0; i < 360; i += step) {
                float sz = 0.4f * this.getSizeMapped();
                float val = Math.max(0.5f, 1.2f - 0.5f * this.animation.getValue());
                float sin = (float) (MathUtility
                        .sin((float) Math.toRadians((float) i + this.moving.getValue() * 1.0f))
                        * (double) width * (double) val);
                float cos = (float) (MathUtility
                        .cos((float) Math.toRadians((float) i + this.moving.getValue() * 1.0f))
                        * (double) width * (double) val);
                ms.push();
                ms.translate((double) sin,
                        (double) (this.prevTarget.getHeight() / 1.5f)
                                + (double) (this.prevTarget.getHeight() / 3.0f)
                                        * MathUtility.sin(Math.toRadians(
                                                (float) i / 2.0f + this.moving.getValue() / 5.0f)),
                        (double) cos);
                ms.multiply(camera.getRotation());
                if (designName.contains("amongus")) {
                    ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
                }
                DrawUtility.drawImage(ms, builder, (double) (-sz / 2.0f), (double) (-sz / 2.0f),
                        0.0, (double) sz, (double) sz,
                        color.withAlpha(255.0f * this.animation.getValue()));
                ms.pop();
            }
        }

        RenderUtility.buildBuffer(builder);
    }


    private boolean isRefractiveMode() {
        return this.crystals.isSelected() || this.glassGhosts.isSelected();
    }

    private boolean usesThemeColor() { return isRefractiveMode() || fragScan.isSelected(); }

    private boolean supportsRainbow() {
        return !this.cubes.isSelected() && !this.circles.isSelected() && !this.marker.isSelected();
    }

    private ColorRGBA effectColor() {
        ColorRGBA color = this.effectThemeSync.isEnabled() ? Colors.getAccent() : this.colorTarget.getColor();
        if (this.rainbow.isEnabled()) {
            color = ColorRGBA.fromHSB((System.currentTimeMillis() % 3000L) / 3000.0F, 1, 1);
        }
        if (this.changeColorOnDamage.isEnabled() && this.prevTarget != null) {
            color = color.mix(ColorRGBA.RED, this.prevTarget.hurtTime / 10.0F);
        }
        return color;
    }

    private void renderRefractiveEffect(Render3DEvent event, LivingEntity target) {
        Camera camera = event.getCamera();
        Vec3d origin = getRenderPos(target);
        float fade = animation.getValue();
        float scale = size.getCurrentValue();
        float radius = target.getWidth() * 1.5F * scale * (1.2F - 0.5F * fade);
        Matrix4f projection = event.getProjectionMatrix();
        Matrix4f viewProjection = new Matrix4f(projection).mul(event.getPositionMatrix());
        var lenses = new ArrayList<TargetLensPost.Lens>(18);
        var lights = new ArrayList<ParticleLightPost.Light>(6);
        ColorRGBA color = effectColor();
        var ghostSamples = glassGhosts.isSelected() ? createGlassGhosts(target, origin) : java.util.List.<GhostSample>of();
        var emitters = new ArrayList<EffectEmitter>(18);
        if (glassGhosts.isSelected()) {
            for (GhostSample sample : ghostSamples) {
                // Six lenses per trail, with one light at each head. The shader supports 18 lenses.
                if (sample.index() % 3 == 0) emitters.add(new EffectEmitter(sample.position(),
                        sample.size() * (0.65F + sample.opacity() * 0.5F), sample.index() == 0 ? fade / 3F : 0));
            }
        } else {
            for (int angle = 0; angle < 360; angle += 20) {
                double phase = Math.toRadians(angle + moving.getValue() * 0.3F);
                emitters.add(new EffectEmitter(origin.add(MathUtility.sin(phase) * radius,
                        0.1F + target.getHeight() * Math.abs(MathUtility.sin(angle)), MathUtility.cos(phase) * radius),
                        0.45F * scale, angle % 60 == 0 ? fade / 6F : 0));
            }
        }
        for (EffectEmitter emitter : emitters) {
            Vec3d position = emitter.position().subtract(camera.getPos());
            if (effectLighting.isEnabled() && emitter.lightIntensity() > 0) {
                lights.add(new ParticleLightPost.Light((float)position.x, (float)position.y, (float)position.z,
                        3.5F, color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, emitter.lightIntensity()));
            }
            if (distortion.isEnabled()) {
                Vector4f clip = viewProjection.transform(new Vector4f((float)position.x, (float)position.y, (float)position.z, 1));
                if (clip.w <= 0.05F) continue;
                float u = clip.x / clip.w * 0.5F + 0.5F, v = clip.y / clip.w * 0.5F + 0.5F;
                float lensRadius = Math.min(emitter.radius() * projection.m11() / clip.w * 0.5F, 0.35F);
                if (u < -lensRadius * 2 || u > 1 + lensRadius * 2 || v < -lensRadius * 2 || v > 1 + lensRadius * 2) continue;
                lenses.add(new TargetLensPost.Lens(u, v, clip.z / clip.w * 0.5F + 0.5F, lensRadius));
            }
        }
        if (!lights.isEmpty() && DrawUtility.particleLightPost != null) {
            DrawUtility.particleLightPost.apply(mc.getFramebuffer(), new Matrix4f(viewProjection).invert(), lights);
        }
        boolean hidden = target.isTouchingWater() || mc.world.raycast(new RaycastContext(camera.getPos(), target.getEyePos(),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() != HitResult.Type.MISS;
        effectOcclusion.update(hidden);
        MatrixStack matrices = event.getMatrices();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            effectPassAlpha = 1;
            matrices.push();
            try {
                if (glassGhosts.isSelected()) drawGlassGhosts(matrices, camera, ghostSamples);
                else drawCrystals(matrices, camera, target);
            } finally { matrices.pop(); }
            if (effectOcclusion.getValue() > 0.01F) {
                RenderSystem.depthFunc(GL11.GL_GREATER);
                effectPassAlpha = effectOcclusion.getValue();
                matrices.push();
                try {
                    if (glassGhosts.isSelected()) drawGlassGhosts(matrices, camera, ghostSamples);
                    else drawCrystals(matrices, camera, target);
                } finally { matrices.pop(); }
            }
        } finally {
            effectPassAlpha = 1;
            RenderSystem.depthFunc(depthFunction);
            RenderSystem.depthMask(mask);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            RenderSystem.setShaderTexture(0, 0);
        }
        if (!lenses.isEmpty() && DrawUtility.targetLensPost != null) {
            DrawUtility.targetLensPost.apply(mc.getFramebuffer(), projection.m11() / projection.m00(),
                    distortionStrength.getCurrentValue() * fade, lenses);
        }
    }

    private void drawCrystals(MatrixStack ms, Camera camera, LivingEntity target) {
        ColorRGBA color = this.effectColor();
        float width = this.prevTarget.getWidth() * 1.5f * this.size.getCurrentValue();
        RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));

        BufferBuilder crystalBuf = CrystalRenderer.createBuffer();
        // Constant for the whole frame; they used to be rebuilt for every crystal.
        Vec3d renderPos = this.getRenderPos(this.prevTarget);
        Vec3d targetPos = target.getPos().add(0.0, (double) target.getHeight() / 2.0, 0.0);
        ColorRGBA crystalColor = color.withAlpha(255.0f * this.animation.getValue() * this.effectPassAlpha);
        for (int i = 0; i < 360; i += 20) {
            float val = 1.2f - 0.5f * this.animation.getValue();
            float sin = (float) (MathUtility.sin((float) Math.toRadians((float) i + this.moving.getValue() * 0.3f))
                    * (double) width * (double) val);
            float cos = (float) (MathUtility.cos((float) Math.toRadians((float) i + this.moving.getValue() * 0.3f))
                    * (double) width * (double) val);
            float sz = 0.1f * this.size.getCurrentValue();
            ms.push();
            ms.translate((double) sin, (double) 0.1f + (double) target.getHeight() * Math.abs(MathUtility.sin(i)),
                    (double) cos);
            Vec3d crystalPos = renderPos.add((double) sin, 1.0, (double) cos);
            Vector3f directionToTarget = new Vector3f(
                    (float) (targetPos.x - crystalPos.x),
                    (float) (targetPos.y - crystalPos.y),
                    (float) (targetPos.z - crystalPos.z)).normalize();
            Vector3f initialDirection = new Vector3f(0.0f, 1.0f, 0.0f);
            Quaternionf rotation = new Quaternionf().rotationTo(initialDirection, directionToTarget);
            ms.multiply(rotation);
            CrystalRenderer.render(ms, crystalBuf, 0.0f, 0.0f, 0.0f, sz, crystalColor);
            ms.pop();
        }
        RenderUtility.buildBuffer(crystalBuf);

        RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);
        float bigSize = this.size.getCurrentValue();
        int glowRgb = color.withAlpha(255.0f * this.animation.getValue() * 0.2f * this.effectPassAlpha).getRGB();
        // Billboard rotation computed once (see drawRing2); each glow only takes its translation.
        Matrix4f base = ms.peek().getPositionMatrix();
        Matrix4f billboard = new Matrix4f(base).translate(0.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Matrix4f translated = this.ring2Scratch;
        for (int i = 0; i < 360; i += 20) {
            float val = 1.2f - 0.5f * this.animation.getValue();
            float sin = (float) (MathUtility.sin((float) Math.toRadians((float) i + this.moving.getValue() * 0.3f))
                    * (double) width * (double) val);
            float cos = (float) (MathUtility.cos((float) Math.toRadians((float) i + this.moving.getValue() * 0.3f))
                    * (double) width * (double) val);
            moveBillboard(base, billboard, translated, sin,
                    (float) ((double) 0.1f + (double) target.getHeight() * Math.abs(MathUtility.sin(i))), cos);
            drawImage(buffer, billboard, (double) (-bigSize / 2.0f), (double) (-bigSize / 2.0f), 0.0,
                    (double) bigSize, (double) bigSize, glowRgb);
        }
        RenderUtility.buildBuffer(buffer);
    }


    private void drawGhosts(MatrixStack ms, Camera camera, LivingEntity target) {
        float width = this.prevTarget.getWidth() * 1.5f;
        RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);
        RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));
        // Billboard rotation computed once (see drawRing2); each soul only takes its translation.
        Matrix4f base = ms.peek().getPositionMatrix();
        Matrix4f billboard = new Matrix4f(base).translate(0.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Matrix4f translated = this.ring2Scratch;

        int step = 2;
        int wormTick = 0;
        int wormCD = 0;
        // size slider scales the soul sprites themselves (not the orbit radius — that would
        // push them away from the target instead of enlarging them).
        float sizeScale = this.getSizeMapped();
        for (int i = 0; i < 360; i += step) {
            float sz = (0.13f + 0.005f * (float) wormTick) * sizeScale;
            float bigSize = (0.7f + 0.005f * (float) wormTick) * sizeScale;
            if (wormCD > 0) {
                wormCD -= step;
                continue;
            }
            if ((wormTick += step) > 50) {
                wormCD = 100;
                wormTick = 0;
                continue;
            }
            float val = Math.max(0.5f, 1.2f - 0.5f * this.animation.getValue());
            float sin = (float) (MathUtility.sin((float) Math.toRadians((float) i + this.moving.getValue() * 1.0f))
                    * (double) width * (double) val);
            float cos = (float) (MathUtility.cos((float) Math.toRadians((float) i + this.moving.getValue() * 1.0f))
                    * (double) width * (double) val);
            moveBillboard(base, billboard, translated, sin,
                    (float) ((double) (this.prevTarget.getHeight() / 1.5f)
                            + (double) (this.prevTarget.getHeight() / 3.0f)
                                    * MathUtility.sin(Math.toRadians(
                                            (float) i / 2.0f + this.moving.getValue() / 5.0f))),
                    cos);
            ColorRGBA color = ColorRGBA.fromInt(this.getModeColor(i));
            drawImage(builder, billboard, (double) (-bigSize / 2.0f), (double) (-bigSize / 2.0f),
                    (double) (-sz / 2.0f), (double) bigSize, (double) bigSize,
                    color.withAlpha(color.getAlpha() * this.animation.getValue() * 0.05f).getRGB());
            drawImage(builder, billboard, (double) (-sz / 2.0f), (double) (-sz / 2.0f),
                    (double) (-sz / 2.0f), (double) sz, (double) sz,
                    color.withAlpha(color.getAlpha() * this.animation.getValue()).getRGB());
        }
        RenderUtility.buildBuffer(builder);
    }

    // --- Ghosts — ported from vurst TargetEsp.renderGhosts (3 orbits with trailing tail) ---

    private record EffectEmitter(Vec3d position, float radius, float lightIntensity) {}
    private record GhostSample(Vec3d position, float size, float opacity, int index) {}
    private final long ghostAnimationStart = System.nanoTime();

    private java.util.List<GhostSample> createGlassGhosts(LivingEntity target, Vec3d origin) {
        float scale = getSizeMapped();
        double radius = Math.max(0.45, target.getWidth() * 0.95) * scale;
        double speedScale = Math.max(0.1, speed.getCurrentValue() * 2);
        double elapsedMillis = (System.nanoTime() - ghostAnimationStart) / 1_000_000.0;
        Vec3d anchor = origin.add(0, target.getHeight() * 0.5 + 0.05, 0);
        var samples = new ArrayList<GhostSample>(51);
        for (int orbit = 0; orbit < 3; orbit++) {
            for (int i = 0; i < 17; i++) {
                double angle = (elapsedMillis - i * 15.0) * speedScale / 180.0;
                double sin = Math.sin(angle) * radius, cos = Math.cos(angle) * radius;
                Vec3d position = switch (orbit) {
                    case 0 -> anchor.add(sin, cos, -cos);
                    case 1 -> anchor.add(-sin, sin, -cos);
                    default -> anchor.add(-sin, -sin, cos);
                };
                float opacity = (255 - i * 15) / 255.0F;
                samples.add(new GhostSample(position, 0.32F * scale * (0.45F + 0.55F * opacity), opacity, i));
            }
        }
        return samples;
    }

    private void drawGlassGhosts(MatrixStack matrices, Camera camera, java.util.List<GhostSample> samples) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float fade = MathHelper.clamp(animation.getValue(), 0, 1) * effectPassAlpha;
        ColorRGBA tint = effectColor();
        Matrix4f base = matrices.peek().getPositionMatrix();
        Matrix4f billboard = ghostBillboard(base, camera);
        Vec3d camPos = camera.getPos();
        for (GhostSample sample : samples) {
            ColorRGBA color = tint;
            if (rainbow.isEnabled()) {
                float hue = ((System.currentTimeMillis() % 3000L) / 3000F + sample.index() / 25F) % 1;
                color = ColorRGBA.fromHSB(hue, 1, 1);
                if (changeColorOnDamage.isEnabled()) color = color.mix(ColorRGBA.RED, prevTarget.hurtTime / 10F);
            }
            float alpha = fade * sample.opacity();
            drawGhostSprite(buffer, base, billboard, camPos, sample.position(), sample.size() * 2.2F,
                    color.withAlpha(255 * alpha * 0.18F).getRGB());
            drawGhostSprite(buffer, base, billboard, camPos, sample.position(), sample.size(),
                    color.mix(ColorRGBA.WHITE, 0.25F * sample.opacity()).withAlpha(255 * alpha).getRGB());
        }
        RenderUtility.buildBuffer(buffer);
    }

    private void drawGhostsVurst(MatrixStack ms, Camera camera, LivingEntity target) {
        float anim = MathHelper.clamp(this.animation.getValue(), 0.0f, 1.0f);
        if (anim <= 0.0f) return;

        float scaleFactor = this.getSizeMapped();
        double radius = Math.max(0.45, target.getWidth() * 0.95) * scaleFactor;
        float speedVal = 27.0f / Math.max(0.1f, this.speed.getCurrentValue() * 2.0f);
        float spriteSize = 0.32f * scaleFactor;
        double distance = 15.0;
        int trailLength = 25;
        int maxAlpha = 255;
        int alphaFactor = 15;

        Vec3d pos = this.getRenderPos(target);
        Vec3d camPos = camera.getPos();
        Vec3d anchor = new Vec3d(pos.x, pos.y + target.getHeight() * 0.5 + 0.05, pos.z);
        long now = System.currentTimeMillis();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f base = ms.peek().getPositionMatrix();
        Matrix4f billboard = ghostBillboard(base, camera);

        // Three interleaved orbits on different axes for a swirling cloud.
        for (int orbit = 0; orbit < 3; orbit++) {
            for (int i = 0; i < trailLength; i++) {
                double angle = 0.15 * ((now % 100000L) - i * distance) / speedVal;
                double s = Math.sin(angle) * radius;
                double c = Math.cos(angle) * radius;
                int alpha = clamp255(MathHelper.clamp(maxAlpha - i * alphaFactor, 0, maxAlpha) * anim);
                if (alpha <= 0) continue;
                int color = (alpha << 24) | (this.getModeColor(i * 360 / trailLength) & 0xFFFFFF);

                Vec3d p = switch (orbit) {
                    case 0 -> anchor.add(s, c, -c);
                    case 1 -> anchor.add(-s, s, -c);
                    default -> anchor.add(-s, -s, c);
                };
                this.drawGhostSprite(buffer, base, billboard, camPos, p, spriteSize, color);
            }
        }

        RenderUtility.buildBuffer(buffer);
    }

    /** Yaw/pitch billboard rotation of drawGhostSprite, computed once per draw (see drawRing2). */
    private static Matrix4f ghostBillboard(Matrix4f base, Camera camera) {
        return new Matrix4f(base).translate(0.0F, 0.0F, 0.0F)
                .rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()))
                .rotate(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
    }

    private final Matrix4f ghostScaled = new Matrix4f();

    /**
     * Same vertices as push / translate / rotate yaw / rotate pitch / scale(-size, -size, size) / pop:
     * scale only rewrites the 3x3 block and keeps the translation column, so the rotated block is
     * scaled here and the translation of the same translate call is copied in.
     */
    private void drawGhostSprite(BufferBuilder buffer, Matrix4f base, Matrix4f billboard,
                                 Vec3d camPos, Vec3d pos, float size, int color) {
        Matrix4f translated = this.ring2Scratch;
        // MatrixStack.translate(double...) narrows to float exactly like this.
        translated.set(base).translate((float) (pos.x - camPos.x), (float) (pos.y - camPos.y), (float) (pos.z - camPos.z));
        Matrix4f matrix = this.ghostScaled.set(billboard).scale(-size, -size, size);
        matrix.m30(translated.m30()).m31(translated.m31()).m32(translated.m32()).m33(translated.m33());
        buffer.vertex(matrix, -0.5f, -0.5f, 0.0f).texture(0.0f, 1.0f).color(color);
        buffer.vertex(matrix, 0.5f, -0.5f, 0.0f).texture(1.0f, 1.0f).color(color);
        buffer.vertex(matrix, 0.5f, 0.5f, 0.0f).texture(1.0f, 0.0f).color(color);
        buffer.vertex(matrix, -0.5f, 0.5f, 0.0f).texture(0.0f, 0.0f).color(color);
    }

    // --- Ring (band) — ported from vurst TargetEsp.renderRing ---

    private void drawRing(MatrixStack ms, LivingEntity target) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d renderPos = this.getRenderPos(target).subtract(camera.getPos());

        float scale = Math.max(0.35f, this.getSizeMapped());
        float radius = target.getWidth() * 0.85f * scale;
        float height = target.getHeight();

        // Vertical bob: rises and falls across a 2s cycle with eased motion.
        double duration = 2000.0 / Math.max(0.1f, this.speed.getCurrentValue() * 2.0f);
        double elapsed = System.currentTimeMillis() % duration;
        boolean side = elapsed > duration / 2.0;
        double progress = elapsed / (duration / 2.0);
        progress = side ? progress - 1.0 : 1.0 - progress;
        progress = MathHelper.clamp((float) progress, 0.0f, 1.0f);
        progress = progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;
        double eased = (height / 2.0f) * (progress > 0.5 ? 1.0 - progress : progress) * (side ? -1.0 : 1.0);
        double y0 = renderPos.y + height * progress;
        double y1 = y0 + eased * 1.5;

        float alphaValue = this.easeOutCubic(this.animation.getValue());
        int glowAlpha = clamp255(145.0f * alphaValue) << 24;
        int coreAlpha = clamp255(17.1f * alphaValue) << 24;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(Math.max(1.0f, 1.5f * scale));
        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder band = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            int rgb = this.getModeColor(i) & 0xFFFFFF;
            band.vertex(matrix, (float) (renderPos.x + x), (float) y0, (float) (renderPos.z + z)).color(glowAlpha | rgb);
            band.vertex(matrix, (float) (renderPos.x + x), (float) y1, (float) (renderPos.z + z)).color(coreAlpha | rgb);
        }
        RenderUtility.buildBuffer(band);

        BufferBuilder outline = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            int rgb = this.getModeColor(i) & 0xFFFFFF;
            outline.vertex(matrix, (float) (renderPos.x + x), (float) y0, (float) (renderPos.z + z)).color(coreAlpha | rgb);
        }
        RenderUtility.buildBuffer(outline);
        RenderSystem.lineWidth(1.0f);
    }

    // --- Ring 2 (dense glow billboards with tails) — ported from vurst TargetEsp.renderRing2 ---

    private void drawRing2(MatrixStack ms, Camera camera, LivingEntity target) {
        Vec3d renderPos = this.getRenderPos(target).subtract(camera.getPos());

        float scale = this.getSizeMapped();
        float entityWidth = target.getWidth() * 0.9f * scale;
        float entityHeight = target.getHeight();
        float animationAlpha = this.easeOutCubic(this.animation.getValue());

        double step = System.currentTimeMillis() / 1000.0 * (this.speed.getCurrentValue() * 2.0f);
        double headY = this.absSinAnimation(step) * entityHeight;
        double tailBaseY = this.absSinAnimation(step - 0.4) * entityHeight;

        float headSize = 0.12f * scale * this.particleThickness.getCurrentValue();
        float tailSize = 0.08f * scale * this.particleThickness.getCurrentValue();
        int totalPoints = 138;
        int tailSegments = 16;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        // Every billboard used to push the stack, translate, rotate by yaw and pitch and pop: 2346 times a
        // frame. The rotation part is the same for all of them (JOML's rotate reads only the 3x3 block,
        // and translate never touches it), so it is computed once; each billboard only takes the
        // translation the same translate call produces. Positions come out bit for bit as before.
        Matrix4f base = ms.peek().getPositionMatrix();
        Matrix4f billboard = new Matrix4f(base).translate(0.0F, 0.0F, 0.0F)
                .rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()))
                .rotate(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        Matrix4f translated = this.ring2Scratch;

        for (int i = 0; i < totalPoints; i++) {
            double angleRadians = (Math.PI * 2) * i / totalPoints;
            float xOffset = (float) (Math.cos(angleRadians) * entityWidth);
            float zOffset = (float) (Math.sin(angleRadians) * entityWidth);

            int rgb = this.getModeColor(i * 360 / totalPoints) & 0xFFFFFF;
            this.drawRingBillboard(buffer, base, billboard, translated,
                    renderPos.x + xOffset, renderPos.y + headY, renderPos.z + zOffset, headSize,
                    packAlpha(animationAlpha * 0.9f * 255.0f) | rgb);

            for (int t = 1; t <= tailSegments; t++) {
                float tailProgress = (float) t / (tailSegments + 1);
                double currentTailY = headY + (tailBaseY - headY) * tailProgress;
                float tailAlpha = animationAlpha * (1.0f - tailProgress) * 0.6f;
                if (tailAlpha <= 0.0f) continue;
                this.drawRingBillboard(buffer, base, billboard, translated,
                        renderPos.x + xOffset, renderPos.y + currentTailY, renderPos.z + zOffset, tailSize,
                        packAlpha(tailAlpha * 255.0f) | rgb);
            }
        }

        RenderUtility.buildBuffer(buffer);
    }

    private final Matrix4f ring2Scratch = new Matrix4f();
    private static final Identifier BLOOM_TEXTURE = BKClientInit.id("textures/bloom.png");
    private static final Identifier GLOW_TEXTURE = BKClientInit.id("textures/target_render/glow.png");

    /**
     * Puts into {@code billboard} (base rotated once per frame) the translation that
     * {@code ms.translate(x, y, z)} followed by a rotation would leave: rotate keeps column 3.
     */
    private static void moveBillboard(Matrix4f base, Matrix4f billboard, Matrix4f translated, float x, float y, float z) {
        translated.set(base).translate(x, y, z);
        billboard.m30(translated.m30()).m31(translated.m31()).m32(translated.m32()).m33(translated.m33());
    }

    /** {@link DrawUtility#drawImage(MatrixStack, BufferBuilder, double, double, double, double, double, ColorRGBA)} on a matrix. */
    private static void drawImage(BufferBuilder builder, Matrix4f matrix, double x, double y, double z,
                                  double width, double height, int rgb) {
        builder.vertex(matrix, (float) x, (float) (y + height), (float) z).texture(0.0F, 1.0F).color(rgb);
        builder.vertex(matrix, (float) (x + width), (float) (y + height), (float) z).texture(1.0F, 1.0F).color(rgb);
        builder.vertex(matrix, (float) (x + width), (float) y, (float) z).texture(1.0F, 0.0F).color(rgb);
        builder.vertex(matrix, (float) x, (float) y, (float) z).texture(0.0F, 0.0F).color(rgb);
    }

    /** {@code ColorRGBA.withAlpha(alpha).getRGB()} alpha byte, without the object. */
    private static int packAlpha(float alpha) {
        return (Math.round(Math.max(0.0F, Math.min(255.0F, alpha))) & 0xFF) << 24;
    }

    private void drawRingBillboard(BufferBuilder buffer, Matrix4f base, Matrix4f billboard, Matrix4f translated,
                                   double x, double y, double z, float size, int rgba) {
        if (size <= 0.0f) return;
        // MatrixStack.translate(double...) narrows to float exactly like this.
        translated.set(base).translate((float) x, (float) y, (float) z);
        billboard.m30(translated.m30()).m31(translated.m31()).m32(translated.m32());
        float half = size / 2.0f;
        buffer.vertex(billboard, -half, -half, 0.0f).texture(0.0f, 1.0f).color(rgba);
        buffer.vertex(billboard, half, -half, 0.0f).texture(1.0f, 1.0f).color(rgba);
        buffer.vertex(billboard, half, half, 0.0f).texture(1.0f, 0.0f).color(rgba);
        buffer.vertex(billboard, -half, half, 0.0f).texture(0.0f, 0.0f).color(rgba);
    }

    /**
     * Per-point color for ring/orbit modes. With Rainbow on, hue rotates over time and across
     * the ring's angle (each point a different hue) — ported from vurst getThemeColorAngle.
     * With Rainbow off, returns the static colorTarget. Damage flash applied either way.
     */
    private int getModeColor(int angleDeg) {
        int rgb;
        if (this.rainbow.isEnabled()) {
            float timeFactor = (System.currentTimeMillis() % 3000L) / 3000.0f;
            float hue = (timeFactor + angleDeg / 360.0f) % 1.0f;
            rgb = ColorRGBA.fromHSB(hue, 1.0f, 1.0f).getRGB() & 0xFFFFFF;
        } else {
            rgb = this.colorTarget.getColor().getRGB() & 0xFFFFFF;
        }
        // Full opacity (0xFF) — callers overwrite alpha via withAlpha / bit-shift as needed.
        return this.applyRingDamageFlash(0xFF000000 | rgb);
    }

    private int applyRingDamageFlash(int color) {
        if (!this.changeColorOnDamage.isEnabled()) return color;

        float targetIntensity = 0.0f;
        long timeSinceDamage = System.currentTimeMillis() - this.lastRingDamageTime;
        if (timeSinceDamage < RING_DAMAGE_FLASH_DURATION) {
            float progress = (float) timeSinceDamage / RING_DAMAGE_FLASH_DURATION;
            targetIntensity = 1.0f - this.easeOutCubic(progress);
        }
        this.ringDamageFlashIntensity = MathHelper.lerp(1.0f, this.ringDamageFlashIntensity, targetIntensity);

        if (this.ringDamageFlashIntensity < 0.05f) return color;

        int alpha = (color >> 24) & 0xFF;
        int red   = (color >> 16) & 0xFF;
        int green = (color >> 8)  & 0xFF;
        int blue  =  color        & 0xFF;
        int finalRed   = (int) MathHelper.lerp(this.ringDamageFlashIntensity, red,   255);
        int finalGreen = (int) MathHelper.lerp(this.ringDamageFlashIntensity, green,  50);
        int finalBlue  = (int) MathHelper.lerp(this.ringDamageFlashIntensity, blue,   50);
        return alpha << 24 | finalRed << 16 | finalGreen << 8 | finalBlue;
    }

    private static int clamp255(float value) {
        return MathHelper.clamp((int) value, 0, 255);
    }

    private float easeOutCubic(float x) {
        return 1.0f - (float) Math.pow(1.0f - x, 3.0);
    }

    private double absSinAnimation(double step) {
        return Math.abs(Math.sin(step));
    }


    // Look-direction raycast with extended reach (vurst targets via crosshair, but vanilla
    // mc.crosshairTarget is capped at ~3-4.5 blocks; this casts up to TARGET_REACH so the ESP
    // locks onto whoever you're aiming at from far away).
    private static final double TARGET_REACH = 24.0;

    private LivingEntity getTarget() {
        if (mc.player == null || mc.world == null) return null;
        if (auraSource.isSelected()) {
            LivingEntity active = AuraRuntime.auraEnabled() ? AuraRuntime.currentTarget() : null;
            return active != null && active != mc.player && active.isAlive() && !active.isRemoved()
                    && active.getWorld() == mc.world ? active : null;
        }

        // isValid rejects every entity when both filters are off; the search would always return null.
        if (!this.players.isEnabled() && !this.mobs.isEnabled()) return null;

        Vec3d eyes = mc.player.getCameraPosVec(1.0f);
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end = eyes.add(look.x * TARGET_REACH, look.y * TARGET_REACH, look.z * TARGET_REACH);
        Box searchBox = mc.player.getBoundingBox().stretch(look.multiply(TARGET_REACH)).expand(1.0);

        LivingEntity best = null;
        double bestDist = TARGET_REACH * TARGET_REACH;

        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox)) {
            if (!(entity instanceof LivingEntity living) || !this.isValid(living)) continue;

            // Slightly inflate the hitbox so aim doesn't need to be pixel-perfect at range.
            Box box = entity.getBoundingBox().expand(0.35);
            var hit = box.raycast(eyes, end);
            if (hit.isEmpty()) continue;

            double dist = eyes.squaredDistanceTo(hit.get());
            if (dist < bestDist) {
                bestDist = dist;
                best = living;
            }
        }

        return best;
    }

    private boolean isValid(LivingEntity living) {
        if (!living.isAlive() || living.isRemoved()) return false;
        if (living instanceof net.minecraft.entity.player.PlayerEntity) {
            return this.players.isEnabled();
        }
        return this.mobs.isEnabled();
    }

    private float getSizeMapped() {
        return 0.8f + this.size.getCurrentValue() * 0.7f;
    }

    private Vec3d getRenderPos(LivingEntity target) {
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
        return new Vec3d(
                MathHelper.lerp((double) tickDelta, (double) target.prevX, (double) target.getX()),
                MathHelper.lerp((double) tickDelta, (double) target.prevY, (double) target.getY()),
                MathHelper.lerp((double) tickDelta, (double) target.prevZ, (double) target.getZ()));
    }

    @Override
    public void onDisable() {
        onWorldChanged();
        lastTargetSource = null;
        super.onDisable();
    }

    public void onWorldChanged() {
        scanRenderer.close(); cubeRenderer.clear();
        if (DrawUtility.targetLensPost != null) DrawUtility.targetLensPost.releaseTargets();
        target = prevTarget = null; animation.setValue(0); effectOcclusion.setValue(0);
    }

    @Override
    public void tick() {
        super.tick();
        if (mc.world == null || mc.player == null) return;
        // The visual module must not overwrite the active Aura's shared combat target.
        if (auraSource.isSelected() || AuraRuntime.auraEnabled()) return;
        dev.bkclient.systems.target.TargetSettings settings = new dev.bkclient.systems.target.TargetSettings.Builder()
                .targetPlayers(this.players.isEnabled())
                .targetMobs(this.mobs.isEnabled())
                .requiredRange(16.0f)
                .build();
        BKClientInit.getInstance().getTargetManager().update(settings);
    }
}
