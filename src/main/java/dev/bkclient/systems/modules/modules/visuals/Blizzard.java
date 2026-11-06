package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.PreHudRenderEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.blizzard.*;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import org.joml.Matrix4f;

/** Local weather presentation; never changes world blocks or server weather. */
@ModuleInfo(name="Blizzard", category=ModuleCategory.VISUALS, desc="modules.descriptions.blizzard")
public final class Blizzard extends BaseModule {
    private static final String KEY = "modules.settings.blizzard.";
    public final ModeSetting section = new ModeSetting(this, KEY+"section");
    private final ModeSetting.Value stormSection = new ModeSetting.Value(section, KEY+"storm").select();
    private final ModeSetting.Value atmosphereSection = new ModeSetting.Value(section, KEY+"atmosphere");
    private final ModeSetting.Value groundSection = new ModeSetting.Value(section, KEY+"ground");
    private boolean hideStorm() { return !stormSection.isSelected(); }
    private boolean hideAtmosphere() { return !atmosphereSection.isSelected(); }
    private boolean hideGround() { return !groundSection.isSelected(); }
    public final SliderSetting intensity = slider("intensity", .85f, .1f, 1, .05f, this::hideStorm);
    public final SliderSetting density = slider("density", 1, .2f, 3, .1f, this::hideStorm);
    public final SliderSetting wind = slider("wind", .7f, 0, 1, .05f, this::hideStorm);
    public final SliderSetting turbulence = slider("turbulence", .6f, 0, 1, .05f, this::hideStorm);
    public final SliderSetting size = slider("size", 1, .5f, 2.5f, .1f, this::hideStorm);
    public final SliderSetting radius = slider("radius", 12, 6, 24, 1, this::hideStorm);
    public final SliderSetting opacity = slider("opacity", .8f, .1f, 1, .05f, this::hideStorm);
    public final BooleanSetting snowfallOnly = new BooleanSetting(this, KEY+"snowfall_only", this::hideStorm);
    public final BooleanSetting hideVanilla = new BooleanSetting(this, KEY+"hide_vanilla", this::hideStorm).enabled(true);
    public final BooleanSetting whiteout = new BooleanSetting(this, KEY+"whiteout", this::hideAtmosphere).enabled(true);
    public final SliderSetting visibility = slider("visibility", 28, 8, 128, 1, () -> hideAtmosphere() || !whiteout.isEnabled());
    public final BooleanSetting frost = new BooleanSetting(this, KEY+"frost", this::hideAtmosphere).enabled(true);
    public final ColorSetting tint = new ColorSetting(this, KEY+"tint", this::hideAtmosphere).color(new ColorRGBA(236,243,255)).alpha(false);
    public final BooleanSetting snowCover = new BooleanSetting(this, KEY+"snow_cover", this::hideGround).enabled(true);
    public final SliderSetting snowDepth = slider("snow_depth", .2f, .05f, .5f, .01f, () -> hideGround() || !snowCover.isEnabled());
    public final SliderSetting coverTime = slider("cover_time", 45, 5, 180, 5, () -> hideGround() || !snowCover.isEnabled());
    public final SliderSetting coverRadius = slider("cover_radius", 18, 8, 32, 1, () -> hideGround() || !snowCover.isEnabled());
    private final BlizzardState state = new BlizzardState();
    private final SnowflakeRenderer flakes = new SnowflakeRenderer();
    private final SnowCoverRenderer ground = new SnowCoverRenderer();
    private ClientWorld world;
    private Fog terrainFog = Fog.DUMMY;
    private final EventListener<dev.bkclient.systems.event.impl.game.WorldChangeEvent> onWorldChange = event -> reset();

    private SliderSetting slider(String key, float value, float min, float max, float step, java.util.function.BooleanSupplier hidden) {
        return new SliderSetting(this, KEY+key, hidden).min(min).max(max).step(step).currentValue(value);
    }
    @Override public void onEnable() { reset(); }
    @Override public void onDisable() { reset(); }
    private void reset() { state.reset(); flakes.reset(); ground.reset(); world=null; terrainFog=Fog.DUMMY; }

    /** Runs once before world fog is evaluated, including while the game is paused. */
    public void beginFrame(Camera camera, float tickDelta) {
        if (!isEnabled() || mc.world==null || mc.player==null || !camera.isReady()) {
            if (world!=null) reset();
            return;
        }
        if (world!=mc.world) { reset(); world=mc.world; }
        BlockPos pos=BlockPos.ofFloored(camera.getPos());
        boolean loaded=world.isChunkLoaded(pos);
        boolean outdoors=loaded && world.getDimension().hasSkyLight()
            && camera.getPos().y >= world.getTopY(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ());
        float target=world.getDimension().hasSkyLight() ? intensity.getCurrentValue() : 0;
        if (snowfallOnly.isEnabled()) {
            float rain=world.getRainGradient(tickDelta);
            target=loaded && rain>.02f && world.getBiome(pos).value().getPrecipitation(pos, world.getSeaLevel())==Biome.Precipitation.SNOW
                ? target*rain : 0;
        }
        state.update(System.nanoTime(), mc.isPaused(), target, outdoors, snowCover.isEnabled(), coverTime.getCurrentValue());
    }
    public boolean hidesVanillaWeather() { return isEnabled() && hideVanilla.isEnabled() && state.strength()>.01f; }
    private boolean hasAirView(Camera camera) {
        if (camera==null || camera.getSubmersionType()!=CameraSubmersionType.NONE) return false;
        return !(camera.getFocusedEntity() instanceof LivingEntity living
            && (living.hasStatusEffect(StatusEffects.BLINDNESS) || living.hasStatusEffect(StatusEffects.DARKNESS)));
    }
    public Fog applyFog(Camera camera, BackgroundRenderer.FogType type, Fog original) {
        Fog result=original;
        float amount=state.strength()*state.exposure();
        if (isEnabled() && whiteout.isEnabled() && amount>.01f && hasAirView(camera)) {
            float end=visibility.getCurrentValue()/amount;
            if (type==BackgroundRenderer.FogType.FOG_SKY) end*=.6f;
            float brightness=MathHelper.clamp(Math.max(original.red(), Math.max(original.green(),original.blue()))*1.6f+.15f, 0, 1);
            ColorRGBA color=tint.getColor();
            result=new Fog(Math.min(original.start(),end*.05f), Math.min(original.end(),end), FogShape.SPHERE,
                MathHelper.lerp(amount*.9f,original.red(),color.getRed()/255*brightness),
                MathHelper.lerp(amount*.9f,original.green(),color.getGreen()/255*brightness),
                MathHelper.lerp(amount*.9f,original.blue(),color.getBlue()/255*brightness),original.alpha());
        }
        if (type==BackgroundRenderer.FogType.FOG_TERRAIN) terrainFog=result;
        return result;
    }
    public void renderWorld(Matrix4f view, Matrix4f projection, Camera camera) {
        if (!isEnabled() || world==null || mc.world!=world || mc.player==null || !camera.isReady()) return;
        // Cover stays on terrain even when the camera is indoors or underwater.
        if (snowCover.isEnabled() && state.cover()>.001f)
            ground.render(view,projection,camera,state.cover(),(int)coverRadius.getCurrentValue(),snowDepth.getCurrentValue(),tint.getColor().getRGB(),terrainFog);
        if (camera.getSubmersionType()==CameraSubmersionType.NONE)
            flakes.render(view,projection,camera,state.delta(),state.seconds(),state.strength(),density.getCurrentValue(),wind.getCurrentValue(),
                turbulence.getCurrentValue(),size.getCurrentValue(),radius.getCurrentValue(),
                opacity.getCurrentValue()*Math.min(1,state.strength()/Math.max(.1f,intensity.getCurrentValue())),tint.getColor().getRGB(),terrainFog);
    }
    private final EventListener<PreHudRenderEvent> onHud = event -> {
        if (mc.world==null || mc.player==null || mc.options.hudHidden || !frost.isEnabled()
                || !hasAirView(mc.gameRenderer.getCamera())) return;
        float amount=state.strength()*state.exposure()*intensity.getCurrentValue();
        if (amount<=.01f) return;
        var context=event.getContext();
        float w=mc.getWindow().getScaledWidth(), h=mc.getWindow().getScaledHeight();
        ColorRGBA color=tint.getColor();
        // 129 rectangles in submission order as one POSITION_COLOR draw instead of 129 draws;
        // same vertices, shader and blending order, so the image is unchanged.
        var batch=new dev.bkclient.utility.render.batching.impl.RectBatching(VertexFormats.POSITION_COLOR, context.getMatrices());
        try {
            context.drawRect(0,0,w,h,color.withAlpha(255*.12f*amount));
            float depth=Math.min(w,h)*.2f;
            ColorRGBA band=color.withAlpha(255*.55f*amount/32);
            for (int i=0;i<32;i++) {
                float d=depth*(1-(float)Math.sqrt(i/32f));
                context.drawRect(0,0,w,d,band); context.drawRect(0,h-d,w,d,band);
                context.drawRect(0,d,d,h-2*d,band); context.drawRect(w-d,d,d,h-2*d,band);
            }
        } finally {
            batch.draw();
        }
    };
}
