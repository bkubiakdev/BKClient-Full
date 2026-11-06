package dev.bkclient.utility.render.ambience;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Sky;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.Map;

/** BKClient sky effects. Programs belong to Minecraft's reloadable shader loader. */
public final class SkyEffectsRenderer {
    private static final Map<String, ShaderProgramKey> PROGRAMS = Map.of(
        "Nebula", key("nebula"), "Aurora", key("aurora"), "Classic Stars", key("stars"),
        "Plasma", key("plasma"), "Neon", key("neon"),
        "Stars", key("bkclient_stars"), "Overcast", key("overcast"), "Starfall", key("starfall"), "Aurora Curtains", key("aurora_curtains"),
        "Cyberspace", key("cyberspace"));
    public final CyberspaceSkySettings cyberspace;
    public final ColorSetting color;
    public final ModeSetting palette;
    public final ColorSetting neonColor1, neonColor2, neonColor3;
    public final SliderSetting scale, intensity, starSize, starDensity;
    public final ColorSetting secondColor;
    public final SliderSetting brightness, strikeInterval, boltPower, worldFlash;
    public final BooleanSetting lightning;
    private long started = System.nanoTime();
    private final Map<String, Long> failedModes = new java.util.HashMap<>();
    private final SkyWorldStreakRenderer worldStreaks = new SkyWorldStreakRenderer();

    public SkyEffectsRenderer(Sky owner) {
        cyberspace = new CyberspaceSkySettings(owner);
        java.util.function.BooleanSupplier extraHidden = () -> !java.util.Set.of("Overcast", "Starfall", "Aurora Curtains", "Stars", "Cyberspace").contains(String.valueOf(owner.getSkyEffectMode()));
        java.util.function.BooleanSupplier stormHidden = () -> !"Overcast".equals(owner.getSkyEffectMode());
        starSize = new SliderSetting(owner, "Star Size", () -> !"Stars".equals(owner.getSkyEffectMode())).min(50).max(300).step(10).currentValue(200).suffix("%");
        starDensity = new SliderSetting(owner, "Star Density", () -> !"Stars".equals(owner.getSkyEffectMode())).min(25).max(300).step(5).currentValue(150).suffix("%");
        secondColor = new ColorSetting(owner, "Sky Second Color", () -> extraHidden.getAsBoolean() || "Stars".equals(owner.getSkyEffectMode()) || "Overcast".equals(owner.getSkyEffectMode()) || "Cyberspace".equals(owner.getSkyEffectMode())).color(ColorRGBA.fromInt(0xffb440ff)).alpha(false);
        brightness = new SliderSetting(owner, "Sky Brightness", extraHidden).min(10).max(200).step(5).currentValue(100).suffix("%");
        lightning = new BooleanSetting(owner, "Storm Lightning", stormHidden).enable();
        strikeInterval = new SliderSetting(owner, "Strike Interval", stormHidden).min(2).max(45).step(.5f).currentValue(9).suffix("s");
        boltPower = new SliderSetting(owner, "Bolt Power", stormHidden).min(10).max(200).step(5).currentValue(100).suffix("%");
        worldFlash = new SliderSetting(owner, "World Flash", stormHidden).min(0).max(100).step(5).currentValue(35).suffix("%");
        color = new ColorSetting(owner, "Sky Effect Color", () -> owner.getSkyEffectMode() == null || "Neon".equals(owner.getSkyEffectMode()) || "Cyberspace".equals(owner.getSkyEffectMode()))
            .color(ColorRGBA.fromInt(0xff3399ff)).alpha(false);
        palette = new ModeSetting(owner, "Neon Palette", () -> !"Neon".equals(owner.getSkyEffectMode()));
        new ModeSetting.Value(palette, "RGB").select();
        new ModeSetting.Value(palette, "Dark"); new ModeSetting.Value(palette, "Light"); new ModeSetting.Value(palette, "Custom");
        neonColor1 = neonColor(owner, "Neon Color 1", 0xffff0000);
        neonColor2 = neonColor(owner, "Neon Color 2", 0xff00ff00);
        neonColor3 = neonColor(owner, "Neon Color 3", 0xff0000ff);
        scale = new SliderSetting(owner, "Sky Scale", () -> owner.getSkyEffectMode() == null || !extraHidden.getAsBoolean()).min(1).max(20).step(.5f).currentValue(5);
        intensity = new SliderSetting(owner, "Sky Intensity", () -> owner.getSkyEffectMode() == null || !extraHidden.getAsBoolean()).min(.001f).max(.05f).step(.001f).currentValue(.01f);
    }

    private ColorSetting neonColor(Sky owner, String name, int value) {
        return new ColorSetting(owner, name, () -> !"Neon".equals(owner.getSkyEffectMode()) || !palette.getValue().getName().equals("Custom"))
            .color(ColorRGBA.fromInt(value)).alpha(false);
    }
    private static ShaderProgramKey key(String name) {
        return new ShaderProgramKey(Identifier.of("bkclient", "core/sky_effects/sky_" + name), VertexFormats.POSITION, Defines.EMPTY);
    }
    public void reset() { started = System.nanoTime(); failedModes.clear(); worldStreaks.reset(); }
    public void renderWorldStreaks(dev.bkclient.systems.event.impl.render.Render3DEvent event, boolean active, float speed, float opacity) {
        if (active) worldStreaks.render(event, cyberspace, speed, opacity, brightness.getCurrentValue()/100f);
        else worldStreaks.reset();
    }

    public boolean render(String mode, float speed, float opacity) {
        ShaderProgramKey key = PROGRAMS.get(mode);
        if (key == null) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        ShaderLoader loader = mc.getShaderLoader();
        long now = System.nanoTime();
        if (now < failedModes.getOrDefault(mode, 0L)) return false;
        ShaderProgram program;
        try { program = loader.getOrCreateProgram(key); }
        catch (Exception error) {
            failedModes.put(mode, now + 5_000_000_000L);
            BKClientInit.LOGGER.warn("Unable to load sky effect {}", mode, error);
            return false;
        }
        if (program == null) return false;
        failedModes.remove(mode);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        ShaderProgram previous = RenderSystem.getShader();
        int previousGlProgram = GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);
        try {
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
            RenderSystem.setShader(program);
            scalar(program, "uTime", (System.nanoTime() - started) / 1_000_000_000f);
            scalar(program, "uSpeed", speed);
            // Neon uses a 1.0 base scale in its shader descriptor; the other effects use 5.0.
            scalar(program, "uScale", scale.getCurrentValue() * ("Neon".equals(mode) ? .2f : 1));
            scalar(program, "uIntensity", intensity.getCurrentValue()); scalar(program, "uAlpha", opacity);
            var resolution = program.getUniform("uResolution");
            if (resolution != null) resolution.set((float)mc.getWindow().getFramebufferWidth(), (float)Math.max(1, mc.getWindow().getFramebufferHeight()));
            color(program, "uColor", color.getColor());
            color(program, "uSecondColor", secondColor.getColor());
            scalar(program, "uBrightness", brightness.getCurrentValue()/100f);
            scalar(program, "uStarSize", starSize.getCurrentValue()/100f);
            scalar(program, "uStarDensity", starDensity.getCurrentValue()/100f);
            if ("Cyberspace".equals(mode)) {
                color(program, "uStarColor", cyberspace.starColor.getColor());
                color(program, "uHorizonColor", cyberspace.horizonColor.getColor());
                scalar(program, "uAurora", cyberspace.aurora.getCurrentValue()/100f);
                scalar(program, "uStarAmount", cyberspace.starAmount.getCurrentValue()/100f);
                scalar(program, "uGlow", cyberspace.glow.getCurrentValue()/100f);
                scalar(program, "uFlow", cyberspace.flow.getCurrentValue()/100f);
                scalar(program, "uStars", cyberspace.stars.isEnabled() ? 1 : 0);

            }
            scalar(program, "uFlash", flash());
            scalar(program, "uStormCell", (float)Math.floor(elapsed()/strikeInterval.getCurrentValue()));
            var pos = mc.gameRenderer.getCamera().getPos();
            vector(program,"CameraPosition",(float)(pos.x%4096),(float)pos.y,(float)(pos.z%4096));
            var camera = mc.gameRenderer.getCamera();
            float yaw = (float)Math.toRadians(-camera.getYaw()), pitch = (float)Math.toRadians(-camera.getPitch());
            float cy = (float)Math.cos(yaw), sy = (float)Math.sin(yaw), cp = (float)Math.cos(pitch), sp = (float)Math.sin(pitch);
            vector(program, "uCamRight", cy, 0, -sy); vector(program, "uCamUp", sy * sp, cp, cy * sp);
            vector(program, "uCamForward", -sy * cp, sp, -cy * cp);
            scalar(program, "uFov", (float)Math.toDegrees(2 * Math.atan(1 / Math.abs(RenderSystem.getProjectionMatrix().m11()))));
            switch (palette.getValue().getName()) {
                case "Dark" -> { vector(program,"uColor1",.1f,.1f,.1f); vector(program,"uColor2",.25f,.25f,.25f); vector(program,"uColor3",.45f,.45f,.45f); }
                case "Light" -> { vector(program,"uColor1",.8f,.8f,.8f); vector(program,"uColor2",.95f,.95f,.95f); vector(program,"uColor3",1,1,1); }
                case "Custom" -> { color(program,"uColor1",neonColor1.getColor()); color(program,"uColor2",neonColor2.getColor()); color(program,"uColor3",neonColor3.getColor()); }
                default -> { vector(program,"uColor1",1,0,0); vector(program,"uColor2",0,1,0); vector(program,"uColor3",0,0,1); }
            }
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            buffer.vertex(-1,-1,1); buffer.vertex(1,-1,1); buffer.vertex(1,1,1); buffer.vertex(-1,1,1);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            return true;
        } finally {
            RenderSystem.setShader(previous);
            org.lwjgl.opengl.GL20.glUseProgram(previousGlProgram);
            RenderSystem.depthMask(depthWrite);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(src,dst,srcAlpha,dstAlpha);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        }
    }
    private double elapsed() { return (System.nanoTime()-started)/1_000_000_000.0; }
    public float flash() {
        if (!lightning.isEnabled()) return 0;
        double t=elapsed(), interval=strikeInterval.getCurrentValue();
        long cell=(long)Math.floor(t/interval);
        // Stable cell seed; the same pulse drives the bolt and terrain illumination.
        double hash=Math.sin(cell*91.3458)*47453.5453;
        if (hash-Math.floor(hash)<.48) return 0;
        double local=t-cell*interval;
        return (float)Math.min(1,Math.exp(-local*28)+Math.exp(-Math.abs(local-.16)*36)*.55+Math.exp(-Math.abs(local-.32)*18)*.22)*boltPower.getCurrentValue()/100f;
    }
    public void renderWorldFlash() {
        float strength=flash()*worldFlash.getCurrentValue()/100f;
        if(strength<.001f) return;
        try (var state=new dev.bkclient.utility.render.VisualRenderState()) {
            RenderSystem.enableBlend(); RenderSystem.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
            var program=MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(new ShaderProgramKey(Identifier.of("bkclient","core/sky_effects/storm_flash"),VertexFormats.POSITION,Defines.EMPTY));
            if(program==null)return;
            RenderSystem.setShader(program); scalar(program,"Strength",strength*.22f);
            BufferBuilder b=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION);
            b.vertex(-1,-1,0);b.vertex(1,-1,0);b.vertex(1,1,0);b.vertex(-1,1,0);
            BufferRenderer.drawWithGlobalProgram(b.end());
        }
    }
    private static void scalar(ShaderProgram program, String name, float value) { var u=program.getUniform(name); if(u!=null)u.set(value); }
    private static void vector(ShaderProgram program, String name, float r, float g, float b) { var u=program.getUniform(name); if(u!=null)u.set(r,g,b); }
    private static void color(ShaderProgram program, String name, ColorRGBA c) { vector(program,name,c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f); }
}
