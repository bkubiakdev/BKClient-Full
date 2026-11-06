package dev.bkclient.utility.render.ambience;

import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

/** Applies atmosphere to the completed world, before HUD and world markers are drawn. */
public final class AmbienceRenderer implements IMinecraft {
    private static FogNoiseTexture noise;
    private static RainHeightmap roof;
    private static RainRenderer rain;
    private static ClientWorld lastWorld;
    private static final Animation exposure = new Animation(350L, 0f, Easing.FIGMA_EASE_IN_OUT);
    private static final long START = System.nanoTime();

    public static void initialize() {
        noise = new FogNoiseTexture();
        roof = new RainHeightmap();
        rain = new RainRenderer();
    }

    public static void clearWorld() {
        if (rain != null) rain.close();
        if (roof != null) roof.close();
        if (DrawUtility.ambiencePost != null) DrawUtility.ambiencePost.releaseTargets();
        WindWaveTagger.clearCache();
        lastWorld = null;
    }

    public static void close() {
        clearWorld();
        if (noise != null) noise.close();
    }

    private static Vector3f rgb(ColorRGBA color) {
        return new Vector3f(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f);
    }

    public static void render(AmbienceEffects effects, Matrix4f view, Matrix4f projection, Camera camera, float tickDelta) {
        var post = DrawUtility.ambiencePost;
        if (post == null || mc.world == null || mc.player == null || !camera.isReady()) return;
        if (lastWorld != mc.world) { clearWorld(); lastWorld = mc.world; }
        var blizzard = dev.bkclient.BKClientInit.getInstance().getModuleManager()
            .getModule(dev.bkclient.systems.modules.modules.visuals.Blizzard.class);
        boolean renderRain = effects.rain.isEnabled() && (blizzard == null || !blizzard.hidesVanillaWeather());
        if (!effects.wetWorld.isEnabled() && !effects.rain.isEnabled() && !effects.fog.isEnabled()
                && !effects.colorIsolation.isEnabled() && !effects.saturation.active()) {
            if (roof.isReady()) { roof.close(); rain.close(); }
            post.releaseTargets();
            return;
        }
        var main = mc.getFramebuffer();
        var pos = camera.getPos();
        Matrix4f vp = new Matrix4f(projection).mul(view);
        Matrix4f inverse = new Matrix4f(vp).invert();
        float time = (System.nanoTime()-START)/1_000_000_000f;
        boolean aboveWater = camera.getSubmersionType() == CameraSubmersionType.NONE;
        // Which passes run this frame: effects chain through the post's ping-pong targets and main is
        // written only at a chain end. The rain geometry draws into main, so it splits the chain in two.
        boolean wetPass = effects.wetWorld.isEnabled() && aboveWater;
        boolean rainPass = renderRain && aboveWater;
        int fogTexture = effects.fog.isEnabled() && aboveWater ? noise.getTexture() : 0;
        boolean fogPass = fogTexture != 0;
        boolean isoPass = effects.colorIsolation.isEnabled();
        boolean satPass = effects.saturation.active();
        int afterRain = (fogPass?1:0) + (isoPass?1:0) + (satPass?1:0);
        boolean wetLast = rainPass || afterRain == 0;
        boolean fogLast = !isoPass && !satPass, isoLast = !satPass;
        if (!post.begin(main)) return;
        try {
        if (wetPass) {
            post.apply("wet_world",main,0,shader -> {
                shader.setUniformMatrix4f("ViewProj",vp);
                shader.setUniformMatrix4f("InvViewProj",inverse);
                shader.setUniform3f("CamPos",new Vector3f((float)pos.x,(float)pos.y,(float)pos.z));
                int sky = mc.world.getSkyColor(pos,tickDelta);
                shader.setUniform3f("SkyTint",new Vector3f((sky>>16&255)/255f,(sky>>8&255)/255f,(sky&255)/255f));
                float angle = mc.world.getSkyAngleRadians(tickDelta);
                shader.setUniform3f("SunDir",new Vector3f(-(float)Math.sin(angle),(float)Math.cos(angle),0));
                shader.setUniform1f("Reflectivity",effects.wetWorldReflection.getCurrentValue()/100f);
                shader.setUniform1f("Wetness",effects.wetWorldAmount.getCurrentValue()/100f);
                shader.setUniform1f("Ripple",0.35f);
                shader.setUniform1f("Gloss",mc.world.getDimension().hasSkyLight()?0.45f:0f);
                shader.setUniform1f("Steps",effects.wetWorldQualityLow.isSelected()?14:effects.wetWorldQualityHigh.isSelected()?40:24);
                shader.setUniform1f("HitThickness",0.98f);
                shader.setUniform1f("MaxDistance",24f);
                shader.setUniform1f("UpOnly",1f);
                shader.setUniform1f("Time",time);
                WetRipples.upload(shader, effects, pos);
            }, 1.0f, wetLast);
        }
        if (rainPass) {
            roof.update(pos.x,pos.y,pos.z);
            rain.draw(camera,vp,roof,effects.rainDensity.getCurrentValue()/100f,effects.rainOpacity.getCurrentValue()/100f,time);
            exposure.update(mc.world.isSkyVisible(BlockPos.ofFloored(pos)));
            post.apply("rain",main,roof.texture(),shader -> {
                shader.setUniformMatrix4f("InvViewProj",inverse);
                shader.setUniform3f("CamPos",new Vector3f((float)pos.x,(float)pos.y,(float)pos.z));
                shader.setUniform3f("Tint",new Vector3f(178/255f,196/255f,214/255f));
                shader.setUniform1f("Time",time);
                shader.setUniform1f("Drops",effects.rainDrops.getCurrentValue()/100f*exposure.getValue());
                shader.setUniform1f("Splashes",effects.rainSplashes.getCurrentValue()/100f);
                shader.setUniform1f("Aspect",(float)main.textureWidth/main.textureHeight);
                shader.setUniform4f("Roof",roof.x(),roof.z(),roof.y(),roof.isReady()?1f:0f);
                shader.setUniform1f("RoofSpan",192f);
            }, 1.0f, afterRain == 0);
        } else if (roof.isReady()) { roof.close(); rain.close(); }
        if (fogPass) {
            int texture = fogTexture;
            post.apply("fog",main,texture,shader -> {
                shader.setUniformMatrix4f("InvViewProj",inverse);
                shader.setUniform3f("CamPos",new Vector3f((float)(pos.x-Math.floor(pos.x/1152)*1152),(float)pos.y,(float)(pos.z-Math.floor(pos.z/1152)*1152)));
                ColorRGBA color;
                if (effects.fogColorModeTheme.isSelected()) color = Colors.getFlatColor();
                else if(effects.fogColorModeCustom.isSelected()) color = effects.fogColor.getColor();
                else {
                    int biome = mc.world.getBiome(BlockPos.ofFloored(pos)).value().getFogColor();
                    color = new ColorRGBA((biome>>16&255)*0.45f+140.25f,(biome>>8&255)*0.45f+140.25f,(biome&255)*0.45f+140.25f);
                }
                shader.setUniform3f("FogTint",rgb(color));
                shader.setUniform2f("Flow",new Vector2f(time*0.0224f,time*0.0152f));
                shader.setUniform2f("FlowPatch",new Vector2f(time*-0.0091f,time*0.0134f));
                shader.setUniform1f("Density",effects.fogDensity.getCurrentValue()/100f*0.07f);
                shader.setUniform1f("Coverage",0.75f-effects.fogCoverage.getCurrentValue()/100f*0.55f);
                shader.setUniform1f("LayerY",effects.fogLevel.getCurrentValue());
                shader.setUniform1f("Thickness",effects.fogThickness.getCurrentValue());
                shader.setUniform1f("MaxDistance",Math.min(160,mc.options.getViewDistance().getValue()*16));
                shader.setUniform1f("TileScale",1f/288);
                shader.setUniform1f("StepWorld",effects.fogQualityHigh.isSelected()?3.5f:5f);
            }, effects.fogQualityHigh.isSelected()?1.0f:0.5f, fogLast);
        }
        if (isoPass) post.apply("isolation",main,0,shader -> {
            float sensitivity = effects.colorIsolationModeCustom.isSelected()?effects.colorIsolationSensitivity.getCurrentValue()/100f
                :effects.colorIsolationModeStrict.isSelected()?0.15f:effects.colorIsolationModeLoose.isSelected()?0.85f:0.5f;
            shader.setUniform1f("IsolationStrength",1f);
            shader.setUniform3f("IsolationColor",rgb(effects.colorIsolationColor.getColor()));
            shader.setUniform1f("HueTolerance",(8+sensitivity*47)/360f);
            shader.setUniform1f("MinSat",0.55f-sensitivity*0.43f);
            shader.setUniform1f("MinVal",0.45f-sensitivity*0.37f);
            shader.setUniform1f("BackgroundSat",effects.colorIsolationBackground.getCurrentValue()/100f);
        }, 1.0f, isoLast);
        var saturation = effects.saturation;
        if (satPass) {
            // Subtract in double precision before uploading. The radius follows the player,
            // including in third person/freecam, without jitter far from the world origin.
            var center = mc.player.getLerpedPos(tickDelta).add(0, mc.player.getHeight() * .5, 0).subtract(pos);
            post.apply("saturation", main, 0, shader -> {
                shader.setUniformMatrix4f("InvViewProj", inverse);
                shader.setUniform3f("CenterRelative", new Vector3f((float)center.x, (float)center.y, (float)center.z));
                shader.setUniform1i("Radial", saturation.radial.isSelected() ? 1 : 0);
                shader.setUniform1i("Spherical", saturation.sphere.isSelected() ? 1 : 0);
                shader.setUniform1i("IncludeSky", saturation.sky.isEnabled() ? 1 : 0);
                shader.setUniform1f("GlobalSaturation", saturation.amount.getCurrentValue() / 100);
                shader.setUniform2f("NearFar", new Vector2f(saturation.near.getCurrentValue() / 100,
                    saturation.far.getCurrentValue() / 100));
                shader.setUniform2f("RadiusTransition", new Vector2f(saturation.radius.getCurrentValue(),
                    saturation.transition.getCurrentValue()));
            }, 1.0f, true);
        }
        } finally { post.end(); }
    }
}
