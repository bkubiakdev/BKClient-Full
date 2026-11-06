package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.PreHudRenderEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.ui.menu.crosshair.CrosshairEditorScreen;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "Crosshair", category = ModuleCategory.VISUALS, desc = "Customizable dynamic and pixel crosshair")
public class Crosshair extends BaseModule implements IMinecraft {

    // Mode
    private final ModeSetting mode = new ModeSetting(this, "Type");
    private final ModeSetting.Value cross = new ModeSetting.Value(mode, "Cross");
    private final ModeSetting.Value circle = new ModeSetting.Value(mode, "Circle");
    private final ModeSetting.Value custom = new ModeSetting.Value(mode, "Custom");

    // Cross Mode Settings
    private final SliderSetting attackOffset = new SliderSetting(this, "Attack Spread", () -> !mode.is(cross))
            .min(0.0f).max(20.0f).currentValue(10.0f).step(1.0f);
    private final SliderSetting indent = new SliderSetting(this, "Gap", () -> !mode.is(cross))
            .min(0.0f).max(10.0f).currentValue(2.0f).step(0.5f);
    private final SliderSetting length = new SliderSetting(this, "Length", () -> !mode.is(cross))
            .min(1.0f).max(15.0f).currentValue(5.0f).step(0.5f);
    private final SliderSetting thickness = new SliderSetting(this, "Thickness", () -> !mode.is(cross))
            .min(0.5f).max(4.0f).currentValue(1.0f).step(0.5f);
    private final BooleanSetting drawDot = new BooleanSetting(this, "Dot", () -> !mode.is(cross)).enabled(false);
    private final BooleanSetting outline = new BooleanSetting(this, "Outline", () -> !mode.is(cross)).enabled(true);

    // Circle Mode Settings
    private final SliderSetting circleRadius = new SliderSetting(this, "Radius", () -> !mode.is(circle))
            .min(2.0f).max(15.0f).currentValue(5.0f).step(0.5f);
    private final SliderSetting circleThickness = new SliderSetting(this, "Ring Width", () -> !mode.is(circle))
            .min(0.5f).max(4.0f).currentValue(1.5f).step(0.5f);
    private final BooleanSetting inertia = new BooleanSetting(this, "Inertia", () -> !mode.is(circle)).enabled(true);

    // Custom Pixel Mode Settings
    private final ButtonSetting editorButton = new ButtonSetting(this, "Open Editor", () -> !mode.is(custom)).action(this::openEditor);
    private final ModeSetting customColor = new ModeSetting(this, "Color", () -> true);
    private final ModeSetting.Value colWhite = new ModeSetting.Value(customColor, "White");
    private final ModeSetting.Value colTheme = new ModeSetting.Value(customColor, "Theme");
    private final ModeSetting.Value colRainbow = new ModeSetting.Value(customColor, "Rainbow");

    private final SliderSetting customScale = new SliderSetting(this, "Scale", () -> true)
            .min(0.5f).max(3.0f).currentValue(1.0f).step(0.5f);
    private final BooleanSetting customOutline = new BooleanSetting(this, "Pixel Outline", () -> true).enabled(true);
    private final BooleanSetting targetReact = new BooleanSetting(this, "Target React", () -> mode.is(custom)).enabled(true);
    private final StringSetting customPixels = new StringSetting(this, "Pixels", () -> true).text(defaultGrid());

    // Inertia & Target state
    private float prevYaw;
    private float prevPitch;
    private float offsetX;
    private float offsetY;
    private boolean initAngles;
    private float targetRed = 0.0f;
    private long lastNs = System.nanoTime();

    public Crosshair() {
    }

    private void openEditor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setScreen(new CrosshairEditorScreen(this, mc.currentScreen));
    }

    public boolean[] customGrid() {
        String s = this.customPixels.getText();
        if (s == null || s.length() != 225) {
            s = defaultGrid();
        }
        boolean[] grid = new boolean[225];
        for (int i = 0; i < 225; i++) {
            grid[i] = s.charAt(i) == '1';
        }
        return grid;
    }

    public void setPixels(boolean[] grid) {
        StringBuilder sb = new StringBuilder(225);
        for (boolean b : grid) {
            sb.append(b ? '1' : '0');
        }
        this.customPixels.setText(sb.toString());
    }

    public void loadDefault() {
        this.customPixels.setText(defaultGrid());
    }

    public static String defaultGrid() {
        StringBuilder sb = new StringBuilder(225);
        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (r == 7 && c == 7) {
                    sb.append('1');
                } else if ((r == 7 && ((c >= 2 && c <= 5) || (c >= 9 && c <= 12)))
                        || (c == 7 && ((r >= 2 && r <= 5) || (r >= 9 && r <= 12)))) {
                    sb.append('1');
                } else {
                    sb.append('0');
                }
            }
        }
        return sb.toString();
    }

    private final EventListener<PreHudRenderEvent> onRender = event -> {
        if (mc.player == null || mc.world == null) return;
        CustomDrawContext context = event.getContext();
        if (context == null) return;

        float cx = mc.getWindow().getScaledWidth() / 2.0f;
        float cy = mc.getWindow().getScaledHeight() / 2.0f;

        // Target reaction lerp
        boolean onTarget = targetReact.isEnabled() && mc.targetedEntity != null;
        this.targetRed = MathHelper.lerp(0.15f, this.targetRed, onTarget ? 1.0f : 0.0f);

        ColorRGBA baseColor = Colors.getTextColor();
        if (this.targetRed > 0.01f) {
            int origR = (baseColor.getRGB() >> 16) & 0xFF;
            int origG = (baseColor.getRGB() >> 8) & 0xFF;
            int origB = baseColor.getRGB() & 0xFF;
            int r = (int) MathHelper.lerp(this.targetRed, (float) origR, 255.0f);
            int g = (int) MathHelper.lerp(this.targetRed, (float) origG, 60.0f);
            int b = (int) MathHelper.lerp(this.targetRed, (float) origB, 60.0f);
            baseColor = new ColorRGBA(r, g, b);
        }

        if (mode.is(cross)) {
            renderCross(context, cx, cy, baseColor);
        } else if (mode.is(circle)) {
            renderCircle(context, cx, cy, baseColor);
        } else if (mode.is(custom)) {
            renderCustom(context, cx, cy, baseColor);
        }
    };

    private void renderCross(CustomDrawContext context, float cx, float cy, ColorRGBA color) {
        float cd = mc.player.getAttackCooldownProgress(0.0f);
        float spread = (1.0f - cd) * attackOffset.getCurrentValue();
        float gap = indent.getCurrentValue() + spread;
        float len = length.getCurrentValue();
        float th = thickness.getCurrentValue();
        ColorRGBA black = new ColorRGBA(0, 0, 0, 180);

        if (outline.isEnabled()) {
            float outTh = th + 1.0f;
            float outLen = len + 1.0f;
            context.drawRect(cx - th / 2.0f - 0.5f, cy - gap - len - 0.5f, outTh, outLen, black);
            context.drawRect(cx - th / 2.0f - 0.5f, cy + gap - 0.5f, outTh, outLen, black);
            context.drawRect(cx - gap - len - 0.5f, cy - th / 2.0f - 0.5f, outLen, outTh, black);
            context.drawRect(cx + gap - 0.5f, cy - th / 2.0f - 0.5f, outLen, outTh, black);
        }

        context.drawRect(cx - th / 2.0f, cy - gap - len, th, len, color);
        context.drawRect(cx - th / 2.0f, cy + gap, th, len, color);
        context.drawRect(cx - gap - len, cy - th / 2.0f, len, th, color);
        context.drawRect(cx + gap, cy - th / 2.0f, len, th, color);

        if (drawDot.isEnabled()) {
            if (outline.isEnabled()) {
                context.drawRect(cx - th / 2.0f - 0.5f, cy - th / 2.0f - 0.5f, th + 1.0f, th + 1.0f, black);
            }
            context.drawRect(cx - th / 2.0f, cy - th / 2.0f, th, th, color);
        }
    }

    private void renderCircle(CustomDrawContext context, float cx, float cy, ColorRGBA color) {
        long now = System.nanoTime();
        float dt = MathHelper.clamp((float) (now - this.lastNs) / 1_000_000_000.0f, 0.001f, 0.05f);
        this.lastNs = now;

        float renderCx = cx;
        float renderCy = cy;

        if (inertia.isEnabled()) {
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            if (!this.initAngles) {
                this.prevYaw = yaw;
                this.prevPitch = pitch;
                this.initAngles = true;
            }

            float dy = MathHelper.wrapDegrees(yaw - this.prevYaw);
            float dp = pitch - this.prevPitch;
            this.prevYaw = yaw;
            this.prevPitch = pitch;

            this.offsetX += -dy * 0.45f;
            this.offsetY += dp * 0.45f;

            float decay = 1.0f - (float) Math.exp(-dt * 4.5f);
            this.offsetX = MathHelper.lerp(decay, this.offsetX, 0.0f);
            this.offsetY = MathHelper.lerp(decay, this.offsetY, 0.0f);

            this.offsetX = MathHelper.clamp(this.offsetX, -12.0f, 12.0f);
            this.offsetY = MathHelper.clamp(this.offsetY, -12.0f, 12.0f);

            renderCx += this.offsetX;
            renderCy += this.offsetY;
        }

        float swing = mc.player.getHandSwingProgress(0.0f);
        float swingScale = swing > 0.0f ? (1.0f - swing * 0.35f) : 1.0f;
        float r = circleRadius.getCurrentValue() * swingScale;
        float th = circleThickness.getCurrentValue();

        context.drawRoundedBorder(renderCx - r, renderCy - r, r * 2.0f, r * 2.0f, th, BorderRadius.all(r), color);
    }

    private void renderCustom(CustomDrawContext context, float cx, float cy, ColorRGBA baseColor) {
        boolean[] grid = customGrid();
        float scale = customScale.getCurrentValue();
        ColorRGBA black = new ColorRGBA(0, 0, 0, 180);

        for (int r = 0; r < 15; r++) {
            for (int c = 0; c < 15; c++) {
                if (grid[r * 15 + c]) {
                    float px = cx + (c - 7) * scale;
                    float py = cy + (r - 7) * scale;

                    ColorRGBA pixelCol = baseColor;
                    if (this.targetRed < 0.5f) {
                        if (customColor.is(colTheme)) {
                            pixelCol = Colors.getAccentColor();
                        } else if (customColor.is(colRainbow)) {
                            float hue = ((System.currentTimeMillis() % 4000L) / 4000.0f + c * 0.04f) % 1.0f;
                            int rgb = java.awt.Color.HSBtoRGB(hue, 0.75f, 1.0f);
                            pixelCol = new ColorRGBA(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
                        }
                    }

                    if (customOutline.isEnabled()) {
                        context.drawRect(px - 0.5f, py - 0.5f, scale + 1.0f, scale + 1.0f, black);
                    }
                    context.drawRect(px, py, scale, scale, pixelCol);
                }
            }
        }
    }
}
