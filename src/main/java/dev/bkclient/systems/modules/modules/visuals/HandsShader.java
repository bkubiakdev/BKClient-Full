package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.bkclient.utility.render.shader.HandShaderHelper;
import dev.bkclient.utility.render.shader.FlameHandsRenderer;
import dev.bkclient.utility.render.shader.Shader;
import net.minecraft.client.gl.Framebuffer;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.*;

@ModuleInfo(name = "Hands Shader", category = ModuleCategory.VISUALS, desc = "Applies visual shaders to first-person hands")
public class HandsShader extends BaseModule {

    @Override public void onEnable() {
        HandShaderHelper.retryAfterFailure();
    }

    private static HandsShader INSTANCE;
    public static HandsShader getInstance() { return INSTANCE; }

    // Mode selection
    public final ModeSetting shader = new ModeSetting((SettingsContainer) this, "modules.settings.hands_shader.shader") {
        @Override public void load(JsonElement element) {
            String saved = element.getAsString();
            if ("Chroma".equalsIgnoreCase(saved) || "Balatro".equalsIgnoreCase(saved) || "Stripes".equalsIgnoreCase(saved)) {
                super.load(new JsonPrimitive("Solid"));
                return;
            }
            // Preserve configs saved under the former separate mode name.
            super.load("04da09655d48e4920ce526c4776b120936b2a674fac7d4a74dbf815969f729bd".equals(legacyFingerprint(saved.toLowerCase(java.util.Locale.ROOT))) ? new JsonPrimitive("Flame") : element);
        }
    };
    public final ModeSetting.Value sNone    = new ModeSetting.Value(shader, "None");
    public final ModeSetting.Value sSolid   = new ModeSetting.Value(shader, "Solid");
    public final ModeSetting.Value sSmoke   = new ModeSetting.Value(shader, "Smoke");
    public final ModeSetting.Value sGlow    = new ModeSetting.Value(shader, "Glow");
    public final ModeSetting.Value sGlass   = new ModeSetting.Value(shader, "Glass");
    public final ModeSetting.Value sFlame   = new ModeSetting.Value(shader, "Flame");

    public final ColorSetting color1 = new ColorSetting(
            this, "modules.settings.hands_shader.color1",
            () -> shader.is(sNone) || shader.is(sFlame)
    ).color(new ColorRGBA(233, 30, 99));

    // Color2 — only shaders that use a second color
    public final ColorSetting color2 = new ColorSetting(
            this, "modules.settings.hands_shader.color2",
            () -> !shader.is(sSolid)
    ).color(new ColorRGBA(155, 89, 182));

    // Speed — animated shaders only (Flame drives its own clock)
    public final SliderSetting speed = new SliderSetting(
            (SettingsContainer) this, "modules.settings.hands_shader.speed",
            () -> shader.is(sNone) || shader.is(sSolid) || shader.is(sGlass) || shader.is(sFlame)
    ).min(0.1f).max(2f).currentValue(0.5f).step(0.05f);

    // Glass-specific
    public final BooleanSetting glassHideHand = new BooleanSetting(
            this, "modules.settings.hands_shader.glass_hide_hand",
            () -> !shader.is(sGlass)
    ).enable();

    public final ModeSetting flameColorMode = new ModeSetting(this,
            "modules.settings.hands_shader.flame_color_mode", () -> !isFlame());
    public final ModeSetting.Value flameItem = new ModeSetting.Value(flameColorMode, "Item");
    public final ModeSetting.Value flameTheme = new ModeSetting.Value(flameColorMode, "Theme");
    public final ModeSetting.Value flameCustom = new ModeSetting.Value(flameColorMode, "Custom");

    public float getFlameColorMix() { return flameColorMode.is(flameItem) ? 0f : 1f; }
    public ColorRGBA getFlameColor() {
        return flameColorMode.is(flameTheme)
                ? dev.bkclient.utility.colors.Colors.getAccent() : flameColor.getColor();
    }

    public final ColorSetting flameColor = new ColorSetting(this, "modules.settings.hands_shader.flame_color",
            () -> !isFlame() || !flameColorMode.is(flameCustom)).color(new ColorRGBA(255, 112, 32)).alpha(false);
    public final SliderSetting flameRadius = flameNumber("radius", 15f, 4f, 30f, 1f);
    public final SliderSetting flameStrength = flameNumber("strength", 1.18f, 0.2f, 2f, 0.02f);
    public final SliderSetting flameSpeed = flameNumber("speed", 1.1f, 0.2f, 3f, 0.05f);
    // Read old configs without exposing a second control that can override the selected source.
    public final SliderSetting flameColorMix = new SliderSetting(this,
            "modules.settings.hands_shader.flame_color_mix", () -> true)
            .min(0f).max(1f).step(0.05f).currentValue(0f);
    public final SliderSetting flameTrailStrength = flameNumber("trail_strength", 1.16f, 0f, 2f, 0.02f);
    public final SliderSetting flameHeight = flameNumber("height", 28f, 8f, 56f, 1f);
    public final BooleanSetting flameFill = new BooleanSetting(this, "modules.settings.hands_shader.flame_fill",
            () -> !isFlame());

    private SliderSetting flameNumber(String name, float value, float min, float max, float step) {
        return new SliderSetting(this, "modules.settings.hands_shader.flame_" + name, () -> !isFlame())
                .min(min).max(max).step(step).currentValue(value);
    }

    private static final java.util.Map<String, String> LEGACY_SETTING_KEYS = java.util.Map.ofEntries(
            java.util.Map.entry("8eb9f569a6266bfd8a8683d16d69b0862f11b35f873d224f631c7218ec59f624", "modules.settings.hands_shader.flame_color"),
            java.util.Map.entry("9d7c4d9306311288d9ebe10f5807d66026fa041e957b14d7add4b29b6238852b", "modules.settings.hands_shader.flame_radius"),
            java.util.Map.entry("5a4d7b61968f70db5fb5a9ca8ce770583bc3a4aa97f1a0b6399f84ed2101994b", "modules.settings.hands_shader.flame_strength"),
            java.util.Map.entry("e957b058cc7a6827c880583cb197d641f128a4ab5b005db0e13547f1086348aa", "modules.settings.hands_shader.flame_speed"),
            java.util.Map.entry("489d215afb341671ad103503f90267dd412096e55b3a1e35cb79119e353d8ae5", "modules.settings.hands_shader.flame_color_mix"),
            java.util.Map.entry("5ea55064fe031592f60c69c849073263f154c962842220b27ca79c0015c50fa8", "modules.settings.hands_shader.flame_trail_strength"),
            java.util.Map.entry("6d8723a235ed1a111e4018425175827437c8d66ef6933ca7cae4de980a1e9376", "modules.settings.hands_shader.flame_height"),
            java.util.Map.entry("b7d76640ec6c1dca60dda3906afcc0e62d1d5dc4e29942810d1410b0d6a38e4e", "modules.settings.hands_shader.flame_fill"));

    public static com.google.gson.JsonObject migrateSettings(com.google.gson.JsonObject settings) {
        com.google.gson.JsonObject migrated = settings.deepCopy();
        for (var entry : settings.entrySet()) {
            String currentKey = LEGACY_SETTING_KEYS.get(legacyFingerprint(entry.getKey()));
            if (currentKey == null) continue;
            if (!migrated.has(currentKey)) migrated.add(currentKey, entry.getValue().deepCopy());
            migrated.remove(entry.getKey());
        }
        return migrated;
    }

    private static String legacyFingerprint(String value) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    public boolean isFlame() { return shader.is(sFlame); }

    public HandsShader() {
        INSTANCE = this;
        sSolid.select();

    }

    @Override
    public void onDisable() {

        FlameHandsRenderer.release();

        HandShaderHelper.releaseFramebuffers();
        super.onDisable();
    }

    public boolean isGlassHideHand() {
        return shader.is(sGlass) && glassHideHand.isEnabled();
    }

    public void render() {
        if (isFlame()) {
            FlameHandsRenderer.render(this);
            return;
        }
        if (!HandShaderHelper.isInitialized()) return;
        if (shader.is(sNone)) return;
        try {
            renderShader(shader.getValue().getName());
        } catch (Exception ignored) {}
    }

    public void captureBefore() {
        FlameHandsRenderer.beginFrame(isFlame());
    }
    private void renderShader(String name) {
        Framebuffer mainFbo = mc.getFramebuffer();
        Framebuffer copyFbo = HandShaderHelper.getCopyFbo();
        if (mainFbo == null || copyFbo == null) return;

        Shader s = getShaderByName(name);
        if (s == null) return;

        // Copy the frame with a GPU blit. Drawing the source framebuffer through a
        // textured quad here added a second full-screen render pass every frame.
        HandShaderHelper.blitColorExact(mainFbo, copyFbo);
        copyFbo.copyDepthFrom(mainFbo);

        mainFbo.beginWrite(true);
        RenderSystem.enableBlend();
        if (name.equals("Glass")) {
            RenderSystem.blendFuncSeparate(GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableDepthTest();

        RenderSystem.activeTexture(GL_TEXTURE0);
        RenderSystem.bindTexture(copyFbo.getColorAttachment());
        RenderSystem.activeTexture(GL_TEXTURE1);
        RenderSystem.bindTexture(copyFbo.getDepthAttachment());

        float t = System.nanoTime() / 1_000_000_000f;
        Vector2f res = new Vector2f(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        Vector3f c1 = toVec3(color1.getColor());
        Vector3f c2 = toVec3(color2.getColor());

        s.bind();
        s.setUniform1i("ColorTexture", 0);
        s.setUniform1i("DepthTexture", 1);
        s.setUniform1f("time", t * speed.getCurrentValue());

        switch (name) {
            case "Solid" -> {
                s.setUniform3f("customColor1", c1);
                s.setUniform3f("customColor2", c2);
                s.setUniform1f("effectAlpha", Math.max(color1.getColor().getAlpha(), color2.getColor().getAlpha()) / 255f);
            }
            case "Smoke" -> {
                s.setUniform2f("resolution", res);
                s.setUniform3f("smokeColor", c1);
                s.setUniform1f("smokeIntensity", 1.0f);
            }
            case "Glow" -> {
                s.setUniform2f("resolution", res);
                s.setUniformBool("fillGlow", true);
                s.setUniform1f("glowRadius", 0.04f);
                s.setUniform1f("glowPower", 0.75f);
                s.setUniform1f("glowDispersion", 5.0f);
                s.setUniform3f("glowColor", c1);
            }
            case "Glass" -> {
                s.setUniform2f("resolution", res);
                s.setUniform1f("blurSize", 24f);
                s.setUniform1f("quality", 4f);
                s.setUniform1f("direction", 6f);
                s.setUniform1f("refraction", 1.0f);
                s.setUniform1f("brightness", 1.1f);
                s.setUniformBool("enableChromatic", true);
                s.setUniformBool("enableDistortion", false);
                s.setUniformBool("hideHand", glassHideHand.isEnabled());
                s.setUniform1f("backgroundBlur", 28f);
            }
        }

        HandShaderHelper.drawFullScreenQuad();
        s.unbind();

        RenderSystem.activeTexture(GL_TEXTURE0);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        mc.getFramebuffer().beginWrite(true);
    }

    private Shader getShaderByName(String name) {
        return switch (name) {
            case "Solid"   -> HandShaderHelper.getSolid();
            case "Smoke"   -> HandShaderHelper.getSmoke();
            case "Glow"    -> HandShaderHelper.getGlow();
            case "Glass"   -> HandShaderHelper.getGlass();
            default        -> null;
        };
    }

    private Vector3f toVec3(ColorRGBA c) {
        return new Vector3f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
    }
}
