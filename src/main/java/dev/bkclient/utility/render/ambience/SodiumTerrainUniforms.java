package dev.bkclient.utility.render.ambience;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Ambience;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL20;

/** One instance per Sodium shader interface, recreated with shaders on resource reload. */
public final class SodiumTerrainUniforms {
    private static final long START = System.nanoTime();
    private int program = -1, wave, origin, stars, starsOrigin, tint;
    /** True once zero strength was uploaded to {@link #program}; disabled effects then skip the uploads. */
    private boolean zeroed;
    public void upload() {
        int current = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (current <= 0) return;
        if (program != current) {
            program=current;
            wave=GL20.glGetUniformLocation(current,"bkWaveData");
            origin=GL20.glGetUniformLocation(current,"bkCameraOrigin");
            stars=GL20.glGetUniformLocation(current,"bkStarsParams");
            starsOrigin=GL20.glGetUniformLocation(current,"bkStarsOrigin");
            tint=GL20.glGetUniformLocation(current,"bkStarsTint");
            zeroed=false;
        }
        if (wave<0 && stars<0) return; // e.g. an Iris shaderpack's own program
        var mc=MinecraftClient.getInstance();
        var manager=BKClientInit.getInstance().getModuleManager();
        var host=manager==null?null:manager.getModule(Ambience.class);
        var wind=host==null?null:host.wind;
        var surface=host==null?null:host.stars;
        boolean enabled=host!=null && host.isEnabled();
        boolean windEnabled=enabled && wind!=null && wind.enabled.isEnabled();
        boolean starsEnabled=enabled && surface!=null && surface.enabled.isEnabled();
        if (!windEnabled && !starsEnabled) { if (zeroed) return; zeroed=true; } else zeroed=false;
        float time=(System.nanoTime()-START)/1_000_000_000f;
        GL20.glUniform4f(wave,time*(wind==null?1:.25f+wind.speed.getCurrentValue()*.015f),
            windEnabled?wind.grass.getCurrentValue()*.02f:0,windEnabled?wind.leaves.getCurrentValue()*.02f:0,
            windEnabled&&wind.gusts.isEnabled()?.04f:0);
        var camera=mc.gameRenderer.getCamera().getPos();
        GL20.glUniform3f(origin,(float)(camera.x%50.26548245743669),(float)(camera.y%50.26548245743669),(float)(camera.z%50.26548245743669));
        GL20.glUniform4f(stars,time*(surface==null?1:surface.speed.getCurrentValue()),starsEnabled?surface.strength.getCurrentValue()/100f:0,
            surface==null?1:surface.size.getCurrentValue()/100f,surface==null?2:surface.density.getCurrentValue());
        GL20.glUniform3f(starsOrigin,(float)(camera.x%4096),(float)(camera.y%4096),(float)(camera.z%4096));
        if (surface!=null) {
            var c=surface.color.getColor(); GL20.glUniform3f(tint,c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f);
        }
    }
}
