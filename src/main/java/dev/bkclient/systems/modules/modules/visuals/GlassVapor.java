package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IWindow;
import dev.bkclient.utility.render.DrawUtility;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(name = "Glass Vapor", category = ModuleCategory.VISUALS, desc = "3D raymarched glass vapor smoke and metaball trails")
public class GlassVapor extends BaseModule implements IMinecraft, IWindow {

    private final SliderSetting refrStrength = new SliderSetting(this, "Refraction")
            .min(0.01f).max(0.12f).currentValue(0.045f).step(0.005f);
    private final SliderSetting rimStrength = new SliderSetting(this, "Rim Light")
            .min(0.0f).max(2.0f).currentValue(1.1f).step(0.1f);
    private final SliderSetting tintStrength = new SliderSetting(this, "Tint Strength")
            .min(0.0f).max(1.0f).currentValue(0.6f).step(0.05f);
    private final SliderSetting noiseStrength = new SliderSetting(this, "Noise Warp")
            .min(0.0f).max(0.05f).currentValue(0.015f).step(0.005f);
    private final BooleanSetting chromatic = new BooleanSetting(this, "Chromatic").enabled(true);

    private final ModeSetting colorMode = new ModeSetting(this, "Color Mode");
    private final ModeSetting.Value colTheme = new ModeSetting.Value(colorMode, "Theme");
    private final ModeSetting.Value colRainbow = new ModeSetting.Value(colorMode, "Rainbow");
    private final ModeSetting.Value colCustom = new ModeSetting.Value(colorMode, "Custom");

    private final ColorSetting customColor = new ColorSetting(this, "Color", () -> colorMode.is(colCustom))
            .color(new ColorRGBA(120, 200, 255));

    private final SliderSetting liveTime = new SliderSetting(this, "Live Time")
            .min(0.5f).max(4.0f).currentValue(1.8f).step(0.1f).suffix("s");
    private final SliderSetting nodeRadius = new SliderSetting(this, "Vapor Size")
            .min(0.2f).max(2.0f).currentValue(0.7f).step(0.05f);

    private final List<VaporNode> nodes = new CopyOnWriteArrayList<>();
    private final float[] uniformScratch = new float[16 + 64 * 16];
    private Vec3d lastPos = null;

    public GlassVapor() {
    }

    @Override
    public void onDisable() {
        nodes.clear();
        lastPos = null;
        super.onDisable();
    }

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (mc.player == null) return;

        Vec3d curPos = mc.player.getPos().add(0, 0.4, 0);
        if (lastPos == null) {
            lastPos = curPos;
            return;
        }

        double dist = curPos.distanceTo(lastPos);
        if (dist > 0.25 || (mc.player.getVelocity().lengthSquared() > 0.01 && Math.random() < 0.35)) {
            ColorRGBA col = getVaporColor();
            nodes.add(new VaporNode(curPos, (float) Math.random() * 6.28f, col));
            lastPos = curPos;
            if (nodes.size() > 50) {
                nodes.remove(0);
            }
        }

        nodes.removeIf(VaporNode::isExpired);
    };

    public void onRenderWorldPost(Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera) {
        if (!isEnabled() || nodes.isEmpty() || DrawUtility.glassVaporPost == null) {
            return;
        }

        Framebuffer framebuffer = mc.getFramebuffer();
        if (framebuffer == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
            return;
        }

        float aspect = (float) framebuffer.textureWidth / (float) framebuffer.textureHeight;
        float time = (float) ((System.currentTimeMillis() % 100000L) / 1000.0);
        float near = 0.05f;
        float far = mc.gameRenderer != null ? mc.gameRenderer.getFarPlaneDistance() : 128.0f;

        Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(positionMatrix);

        int count = 0;
        int max = Math.min(nodes.size(), 60);

        for (int i = 0; i < max; i++) {
            VaporNode node = nodes.get(i);
            if (node.isExpired()) continue;

            Vec3d rel = node.pos.subtract(camera.getPos());
            Vector4f clip = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1.0f);
            viewProj.transform(clip);

            if (clip.w <= 0.01f) continue;

            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;
            float uvX = ndcX * 0.5f + 0.5f;
            float uvY = ndcY * 0.5f + 0.5f;

            if (uvX < -0.2f || uvX > 1.2f || uvY < -0.2f || uvY > 1.2f) continue;

            float age = (System.currentTimeMillis() - node.spawnTime) / 1000.0f;
            float progress = age / liveTime.getCurrentValue();
            float env = 1.0f - MathHelper.clamp(progress, 0.0f, 1.0f);
            env = (float) Math.sin(env * Math.PI * 0.5);

            float rWorld = nodeRadius.getCurrentValue() * (1.0f + progress * 0.8f);
            float screenR = (rWorld / clip.w) * 0.5f;

            int base = 16 + count * 16;
            // d0
            uniformScratch[base]     = uvX;
            uniformScratch[base + 1] = uvY;
            uniformScratch[base + 2] = clip.w;
            uniformScratch[base + 3] = env;

            // d1 (axis vectors)
            uniformScratch[base + 4] = screenR;
            uniformScratch[base + 5] = 0.0f;
            uniformScratch[base + 6] = 0.0f;
            uniformScratch[base + 7] = screenR;

            // d2 (phase, wob, shine)
            uniformScratch[base + 8]  = (float) i / (float) max;
            uniformScratch[base + 9]  = node.wobPhase;
            uniformScratch[base + 10] = 0.35f;
            uniformScratch[base + 11] = 0.8f;

            // d3 (color)
            uniformScratch[base + 12] = (float) node.color.getRed() / 255.0f;
            uniformScratch[base + 13] = (float) node.color.getGreen() / 255.0f;
            uniformScratch[base + 14] = (float) node.color.getBlue() / 255.0f;
            uniformScratch[base + 15] = 1.0f;

            count++;
        }

        if (count == 0) return;

        uniformScratch[0] = count;
        uniformScratch[1] = aspect;
        uniformScratch[2] = time;
        uniformScratch[3] = refrStrength.getCurrentValue();

        // header2
        uniformScratch[4] = rimStrength.getCurrentValue();
        uniformScratch[5] = tintStrength.getCurrentValue();
        uniformScratch[6] = noiseStrength.getCurrentValue();
        uniformScratch[7] = 1.0f; // depthGateOn

        // header3
        uniformScratch[8]  = near;
        uniformScratch[9]  = far;
        uniformScratch[10] = chromatic.isEnabled() ? 0.015f : 0.0f;
        uniformScratch[11] = colorMode.is(colTheme) ? 1.0f : 0.0f;

        // header4 (theme color)
        ColorRGBA themeCol = Colors.getAccentColor();
        uniformScratch[12] = (float) themeCol.getRed() / 255.0f;
        uniformScratch[13] = (float) themeCol.getGreen() / 255.0f;
        uniformScratch[14] = (float) themeCol.getBlue() / 255.0f;
        uniformScratch[15] = 1.0f;

        DrawUtility.glassVaporPost.apply(framebuffer, uniformScratch);
    }

    private ColorRGBA getVaporColor() {
        if (colorMode.is(colRainbow)) {
            float hue = ((System.currentTimeMillis() % 4000L) / 4000.0f) % 1.0f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.7f, 1.0f);
            return new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
        if (colorMode.is(colCustom)) {
            return customColor.getColor();
        }
        return Colors.getAccentColor();
    }

    private class VaporNode {
        private final Vec3d pos;
        private final float wobPhase;
        private final ColorRGBA color;
        private final long spawnTime;

        public VaporNode(Vec3d pos, float wobPhase, ColorRGBA color) {
            this.pos = pos;
            this.wobPhase = wobPhase;
            this.color = color;
            this.spawnTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - spawnTime) / 1000.0f > liveTime.getCurrentValue();
        }
    }
}
