package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.AttackEvent;
import dev.bkclient.systems.event.impl.game.EntityDeathEvent;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.modules.modules.visuals.frageffect.BodyDissolveParticles;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.LinkedModeSetting;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.render.PostHudRenderEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.visuals.frageffect.FragEffectDeathTracker;
import dev.bkclient.systems.modules.modules.visuals.frageffect.FragEffectEasing;
import dev.bkclient.systems.modules.modules.visuals.frageffect.FragEffectParticles;
import dev.bkclient.systems.modules.modules.visuals.frageffect.FragEffectSoundQueue;
import dev.bkclient.systems.modules.modules.visuals.frageffect.FragScanRenderer;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.sounds.ClientSounds;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Kill Effects", category = ModuleCategory.VISUALS, desc = "modules.descriptions.kill_effects")
public class KillEffects extends BaseModule {
    private static final long BASE_EFFECT_DURATION_MS = 5200L;
    private static final Identifier VIGNETTE_TEXTURE = BKClientInit.id("textures/features/frageffect_vignette.png");
    private static final Identifier BLOOM_TEXTURE = BKClientInit.id("textures/bloom.png");

    public final ModeSetting style = new ModeSetting(this, "modules.settings.kill_effects.style");
    public final ModeSetting.Value lightningStyle = new ModeSetting.Value(style, "Lightning");
    public final ModeSetting.Value fragStyle = new ModeSetting.Value(style, "Frag");
    public final ModeSetting.Value scatterStyle = new ModeSetting.Value(style, "modules.settings.kill_effects.style.scatter");
    public final ModeSetting.Value gravityStyle = new ModeSetting.Value(style, "modules.settings.kill_effects.style.gravity");
    private final BooleanSetting particleThemeSync = new BooleanSetting(this, "theme.sync", () -> !isBodyDissolve()).enabled(true);
    public final ColorSetting color = new ColorSetting(this, "modules.settings.kill_effects.color", () -> isBodyDissolve() && particleThemeSync.isEnabled()).color(Colors.ACCENT);
    public final SelectSetting targets = new SelectSetting(this, "modules.settings.kill_effects.targets");
    public final SelectSetting.Value players = new SelectSetting.Value(targets, "Players").select();
    public final SelectSetting.Value friends = new SelectSetting.Value(targets, "Friends");
    public final SelectSetting.Value mobs = new SelectSetting.Value(targets, "Mobs").select();
    public final SelectSetting.Value animals = new SelectSetting.Value(targets, "Animals");
    public final SliderSetting speed = new SliderSetting(this, "modules.settings.kill_effects.speed", () -> !style.is(fragStyle))
            .min(25f).max(200f).currentValue(100f).step(1f);

    private final List<Lightning> lightnings = new ArrayList<>();
    private final BodyDissolveParticles bodyParticles = new BodyDissolveParticles();
    private final FragEffectSoundQueue soundQueue = new FragEffectSoundQueue();
    private final FragEffectDeathTracker deathTracker = new FragEffectDeathTracker();
    private final FragEffectParticles particles = new FragEffectParticles();
    private final FragScanRenderer scanRenderer = new FragScanRenderer();
    private boolean animate;
    private long effectStartMillis = System.currentTimeMillis();
    private float activeSpeedMultiplier = 1.0f;

    public KillEffects() {
        LinkedModeSetting.toggle(this, "color.source", this.particleThemeSync, "Theme", "Custom");

        fragStyle.select();
    }

    @Override
    public void onDisable() {
        lightnings.clear();
        bodyParticles.clear();
        resetFragState();
        super.onDisable();
    }

    private final EventListener<AttackEvent> onAttack = event -> {
        if (event.getEntity() instanceof LivingEntity target && target.isAlive()
                && target.getHealth() > 0.0f && matchesTarget(target)) {
            deathTracker.remember(target, true);
        }
    };

    private final EventListener<EntityDeathEvent> onEntityDeath = event -> {
        LivingEntity dead = event.getEntity();
        if (dead == null || dead == mc.player || dead.isRemoved() || !matchesTarget(dead)) return;
        LivingEntity killer = event.getKillerEntity();
        if (killer != mc.player && !deathTracker.wasRecentlyHit(dead)) return;
        deathTracker.forget(dead);
        if (style.is(lightningStyle)) lightnings.add(new Lightning(dead.getPos(), color.getColor()));
        else if (isBodyDissolve()) bodyParticles.spawn(dead, particleThemeSync.isEnabled() ? Colors.getAccent() : color.getColor(), style.is(gravityStyle));
        else triggerFrag(dead);
    };

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (mc.player == null || mc.world == null) {
            bodyParticles.clear();
            resetFragState();
            return;
        }
        deathTracker.tick();
        particles.tick();
        soundQueue.tick();
    };

    private final EventListener<Render3DEvent> on3DRender = event -> {
        if (style.is(lightningStyle)) renderLightning(event);
        else if (style.is(fragStyle)) renderFrag(event);
        bodyParticles.render(event);
    };

    private final EventListener<WorldChangeEvent> onWorldChange = event -> {
        bodyParticles.clear();
        lightnings.clear();
        resetFragState();
    };

    private boolean isBodyDissolve() { return style.is(scatterStyle) || style.is(gravityStyle); }

    private final EventListener<PostHudRenderEvent> onHudRender = event -> {
        if (style.is(fragStyle)) drawVignette(event);
    };

    private void triggerFrag(LivingEntity entity) {
        float effectSpeed = speedMultiplier();
        animate = true;
        activeSpeedMultiplier = MathHelper.clamp(effectSpeed, 0.25f, 2.0f);
        effectStartMillis = System.currentTimeMillis();
        Vec3d position = entity.getEyePos();
        scanRenderer.ping(position, effectSpeed);
        particles.spawnBurst(position, 480, 900, 1.9f, 22, 0.012f);
        long offset = scaleDuration(150L);
        soundQueue.schedule(ClientSounds.KILL_PULSE, 0L, 1.0f);
        soundQueue.schedule(ClientSounds.KILL_KNOCK, offset, 1.0f);
        soundQueue.schedule(ClientSounds.KILL_SPARKS, offset * 2L, 0.2f);
        soundQueue.schedule(ClientSounds.KILL_ECHO, offset * 3L, 0.6f);
    }

    private void renderFrag(Render3DEvent event) {
        if (!scanRenderer.isIdle()) {
            scanRenderer.render(event.getProjectionMatrix(), event.getPositionMatrix(), event.getCamera().getPos(),
                    c1(), c2(), c3(), c4(), c5(), c6(), c7(), c8());
        }
        if (!particles.isEmpty()) particles.render(event.getMatrices(), event.getTickDelta(), accent());
    }

    private void drawVignette(PostHudRenderEvent event) {
        float progress = effectProgress();
        if (progress <= 0.0f) return;
        float alpha = effectEnvelope(380L, 1100L);
        if (alpha <= 0.0f) return;
        float animation = FragEffectEasing.quintOut(progress);
        float scale = Math.min((animation > 0.5f ? 1.0f - animation : animation) * 4.25f, 1.0f);
        float width = mc.getWindow().getScaledWidth();
        float height = mc.getWindow().getScaledHeight();
        float extendX = width * 0.5f * (1.0f - scale);
        float extendY = height * 0.5f * (1.0f - scale);
        ColorRGBA tinted = lighten(color.getColor(), 0.22f).mix(Colors.WHITE, progress).withAlpha(Math.round(80.0f * alpha));
        DrawUtility.drawTexture(event.getContext().getMatrices(), VIGNETTE_TEXTURE,
                -extendX, -extendY, width + extendX * 2.0f, height + extendY * 2.0f, tinted);
    }

    private void resetFragState() {
        animate = false;
        activeSpeedMultiplier = speedMultiplier();
        soundQueue.clear();
        deathTracker.clear();
        particles.clear();
        scanRenderer.clear();
    }

    private float effectProgress() {
        long duration = scaleDuration(BASE_EFFECT_DURATION_MS);
        float progress = animate ? Math.min((float) elapsedMillis() / duration, 1.0f) : 0.0f;
        if (progress >= 1.0f) animate = false;
        return progress;
    }

    private float effectEnvelope(long attackMs, long releaseMs) {
        if (!animate) return 0.0f;
        long duration = scaleDuration(BASE_EFFECT_DURATION_MS);
        long elapsed = Math.min(elapsedMillis(), duration);
        if (elapsed <= 0L) return 0.0f;
        long attack = MathHelper.clamp(scaleDuration(attackMs), 1L, duration);
        long release = MathHelper.clamp(scaleDuration(releaseMs), 1L, duration);
        long releaseStart = Math.max(duration - release, attack);
        if (elapsed < attack) return FragEffectEasing.sineOut((float) elapsed / attack);
        if (elapsed >= releaseStart) {
            float releaseProgress = MathHelper.clamp((float) (elapsed - releaseStart) / release, 0.0f, 1.0f);
            return 1.0f - FragEffectEasing.sineInOut(releaseProgress);
        }
        return 1.0f;
    }

    private long elapsedMillis() { return System.currentTimeMillis() - effectStartMillis; }
    private float speedMultiplier() { return MathHelper.clamp(speed.getCurrentValue() / 100.0f, 0.25f, 2.0f); }
    private long scaleDuration(long duration) { return Math.max(1L, Math.round(duration / MathHelper.clamp(activeSpeedMultiplier, 0.25f, 2.0f))); }

    private boolean matchesTarget(LivingEntity entity) {
        if (entity == null || entity == mc.player) return false;
        if (entity instanceof PlayerEntity player) return isFriend(player) ? friends.isSelected() : players.isSelected();
        if (entity instanceof AnimalEntity) return animals.isSelected();
        return entity instanceof MobEntity && mobs.isSelected();
    }

    private static boolean isFriend(PlayerEntity player) {
        try {
            return BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int accent() { return color.getColor().getRGB(); }
    private int c1() { return lightenInt(accent(), 0.45f); }
    private int c2() { return lightenInt(accent(), 0.18f); }
    private int c3() { return darkenInt(accent(), 0.18f); }
    private int c4() { return lightenInt(accent(), 0.60f); }
    private int c5() { return lightenInt(accent(), 0.70f); }
    private int c6() { return interpolateInt(accent(), 0xFFFFFFFF, 0.28f); }
    private int c7() { return darkenInt(accent(), 0.08f); }
    private int c8() { return darkenInt(accent(), 0.35f); }

    private static int lightenInt(int argb, float amount) {
        float t = MathHelper.clamp(amount, 0.0f, 1.0f);
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (argb & 0xFF000000) | (Math.round(r + (255 - r) * t) << 16)
                | (Math.round(g + (255 - g) * t) << 8) | Math.round(b + (255 - b) * t);
    }

    private static int darkenInt(int argb, float amount) {
        float factor = 1.0f - MathHelper.clamp(amount, 0.0f, 1.0f);
        return (argb & 0xFF000000) | (Math.round(((argb >> 16) & 0xFF) * factor) << 16)
                | (Math.round(((argb >> 8) & 0xFF) * factor) << 8) | Math.round((argb & 0xFF) * factor);
    }

    private static int interpolateInt(int from, int to, float t) {
        float f = MathHelper.clamp(t, 0.0f, 1.0f);
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * f);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static ColorRGBA lighten(ColorRGBA value, float amount) {
        float t = MathHelper.clamp(amount, 0.0f, 1.0f);
        return new ColorRGBA(value.getRed() + (255f - value.getRed()) * t,
                value.getGreen() + (255f - value.getGreen()) * t,
                value.getBlue() + (255f - value.getBlue()) * t, value.getAlpha());
    }

    private void renderLightning(Render3DEvent event) {
        if (lightnings.isEmpty()) return;
        MatrixStack matrices = event.getMatrices();
        Camera camera = event.getCamera();
        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        // Every point used to push, translate, rotate by the camera and pop (200 per bolt). The rotation
        // part is the same for all of them (JOML's rotate reads only the 3x3 block and translate never
        // touches it), so it is computed once; each point only takes its translation. Bit-identical.
        Matrix4f base = matrices.peek().getPositionMatrix();
        Matrix4f billboard = new Matrix4f(base).translate(0.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Matrix4f translated = new Matrix4f();
        // RenderUtility.prepareMatrices reads the game renderer's camera position.
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        for (Lightning lightning : lightnings) {
            lightning.render(builder, base, billboard, translated, cameraPos);
            if (lightning.animation.getValue() == 1.0f) lightning.showing = false;
        }
        BuiltBuffer built = builder.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        matrices.pop();
        lightnings.removeIf(value -> !value.showing && value.animation.getValue() == 0.0f);
    }

    static class Lightning {
        final Vec3d pos;
        final ColorRGBA color;
        boolean showing = true;
        final Animation animation = new Animation(300L, 0.0f, Easing.FIGMA_EASE_IN_OUT);
        final List<Vec3d> poses = new ArrayList<>();

        Lightning(Vec3d pos, ColorRGBA color) {
            this.pos = pos;
            this.color = color;
            Vec3d last = pos;
            for (int i = 0; i < 200; i++) poses.add(last = last.add(MathUtility.random(-0.4f, 0.4f), 0.25, MathUtility.random(-0.4f, 0.4f)));
        }

        void render(BufferBuilder builder, Matrix4f base, Matrix4f billboard, Matrix4f translated, Vec3d cameraPos) {
            animation.setEasing(Easing.BOUNCE_IN);
            animation.setDuration(500L);
            animation.update(showing);
            int rgb = color.withAlpha(255.0f * animation.getValue() * 0.4f).getRGB();
            for (Vec3d point : poses) {
                float size = (float) (2.0 + 5.0 * (point.y - pos.y) / 50.0);
                // prepareMatrices: point.subtract(cameraPos), then MatrixStack.translate(double...) narrows to float.
                translated.set(base).translate((float) (point.x - cameraPos.x), (float) (point.y - cameraPos.y),
                        (float) (point.z - cameraPos.z));
                billboard.m30(translated.m30()).m31(translated.m31()).m32(translated.m32()).m33(translated.m33());
                // DrawUtility.drawImage(-size / 2, -size / 2, 0, size, size) on the billboard matrix.
                double x = -size / 2.0f, y = -size / 2.0f;
                builder.vertex(billboard, (float) x, (float) (y + size), 0.0F).texture(0.0F, 1.0F).color(rgb);
                builder.vertex(billboard, (float) (x + size), (float) (y + size), 0.0F).texture(1.0F, 1.0F).color(rgb);
                builder.vertex(billboard, (float) (x + size), (float) y, 0.0F).texture(1.0F, 0.0F).color(rgb);
                builder.vertex(billboard, (float) x, (float) y, 0.0F).texture(0.0F, 0.0F).color(rgb);
            }
        }
    }
}
