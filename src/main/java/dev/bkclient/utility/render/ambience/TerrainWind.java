package dev.bkclient.utility.render.ambience;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Ambience;
import dev.bkclient.systems.setting.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;

/** Vegetation wind using reloadable 1.21.4 terrain shaders. */
public final class TerrainWind {
    public final BooleanSetting enabled,gusts;
    public final SliderSetting grass,leaves,speed;
    private static final long START=System.nanoTime();
    private static long retryAt;
    private static final ShaderProgramKey[] KEYS = {
        new ShaderProgramKey(Identifier.of("bkclient","core/wind/solid"),VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,Defines.EMPTY),
        new ShaderProgramKey(Identifier.of("bkclient","core/wind/cutout"),VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,Defines.EMPTY),
        new ShaderProgramKey(Identifier.of("bkclient","core/wind/cutout_mipped"),VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,Defines.EMPTY)};
    public TerrainWind(Ambience owner){
        enabled=new BooleanSetting(owner,"Vegetation Wind", owner::hideTerrain);
        grass=new SliderSetting(owner,"Grass Wind",()->owner.hideTerrain()||!enabled.isEnabled()).min(0).max(100).step(5).currentValue(60).suffix("%");
        leaves=new SliderSetting(owner,"Leaves Wind",()->owner.hideTerrain()||!enabled.isEnabled()).min(0).max(100).step(5).currentValue(45).suffix("%");
        speed=new SliderSetting(owner,"Wind Speed",()->owner.hideTerrain()||!enabled.isEnabled()).min(0).max(100).step(5).currentValue(50).suffix("%");
        gusts=new BooleanSetting(owner,"Wind Gusts",()->owner.hideTerrain()||!enabled.isEnabled()).enable();
    }
    public static ShaderProgram program(RenderLayer layer,ShaderProgram original){
        int key=layer==RenderLayer.getSolid()?0:layer==RenderLayer.getCutout()?1:layer==RenderLayer.getCutoutMipped()?2:-1;
        if(key<0||System.nanoTime()<retryAt)return original;
        try{
            var mc=MinecraftClient.getInstance();
            var host=BKClientInit.getInstance().getModuleManager().getModule(Ambience.class);
            var wind=host==null?null:host.wind;
            boolean active=host!=null&&host.isEnabled()&&wind.enabled.isEnabled();
            // With wind and stars off the custom program only costs uniform uploads; vanilla draws the same picture.
            if(!active&&!(host!=null&&host.isEnabled()&&host.stars.enabled.isEnabled()))return original;
            var p=mc.getShaderLoader().getOrCreateProgram(KEYS[key]);
            if(p==null)return original;
            var u=p.getUniform("WaveData");
            if(u!=null)u.set((System.nanoTime()-START)/1_000_000_000f*(wind==null?1:.25f+wind.speed.getCurrentValue()*.015f),active?wind.grass.getCurrentValue()*.02f:0,active?wind.leaves.getCurrentValue()*.02f:0,active&&wind.gusts.isEnabled()?.04f:0);
            var camera=mc.gameRenderer.getCamera().getPos();u=p.getUniform("CameraOrigin");
            if(u!=null)u.set((float)(camera.x%50.26548245743669),(float)(camera.y%50.26548245743669),(float)(camera.z%50.26548245743669));
            TerrainStars.apply(p,host,camera);
            return p;
        }catch(RuntimeException e){retryAt=System.nanoTime()+5_000_000_000L;BKClientInit.LOGGER.warn("Terrain effects shader unavailable",e);return original;}
    }
}
