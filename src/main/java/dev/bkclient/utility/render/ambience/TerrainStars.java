package dev.bkclient.utility.render.ambience;

import dev.bkclient.systems.modules.modules.visuals.Ambience;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.util.math.Vec3d;

/** Stars attached to terrain surfaces; independent of sky and vegetation animation. */
public final class TerrainStars {
    public final BooleanSetting enabled;
    public final SliderSetting strength, size, density, speed;
    public final ColorSetting color;
    private static final long START = System.nanoTime();
    public TerrainStars(Ambience owner) {
        enabled = new BooleanSetting(owner, "Stars", owner::hideTerrain);
        strength = new SliderSetting(owner, "Stars Brightness", () -> owner.hideTerrain() || !enabled.isEnabled()).min(0).max(200).step(5).currentValue(80).suffix("%");
        size = new SliderSetting(owner, "Stars Size", () -> owner.hideTerrain() || !enabled.isEnabled()).min(25).max(200).step(5).currentValue(100).suffix("%");
        density = new SliderSetting(owner, "Stars Density", () -> owner.hideTerrain() || !enabled.isEnabled()).min(1).max(6).step(1).currentValue(2);
        speed = new SliderSetting(owner, "Stars Speed", () -> owner.hideTerrain() || !enabled.isEnabled()).min(0).max(3).step(.1f).currentValue(1);
        color = new ColorSetting(owner, "Stars Color", () -> owner.hideTerrain() || !enabled.isEnabled()).color(ColorRGBA.fromInt(0xffb4d5ff)).alpha(false);
    }
    public static void apply(ShaderProgram shader, Ambience owner, Vec3d camera) {
        TerrainStars stars = owner == null ? null : owner.stars;
        boolean active = owner != null && owner.isEnabled() && stars.enabled.isEnabled();
        var params = shader.getUniform("StarsParams");
        if (params != null) params.set((System.nanoTime()-START)/1_000_000_000f*(stars==null?1:stars.speed.getCurrentValue()),
            active ? stars.strength.getCurrentValue()/100f : 0,
            stars==null?1:stars.size.getCurrentValue()/100f, stars==null?2:stars.density.getCurrentValue());
        var origin = shader.getUniform("StarsOrigin");
        // Integer density and a whole-block period preserve continuity across origin shifts.
        if (origin != null) origin.set((float)(camera.x%4096), (float)(camera.y%4096), (float)(camera.z%4096));
        var tint = shader.getUniform("StarsTint");
        if (tint != null && stars != null) {
            var c = stars.color.getColor(); tint.set(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f);
        }
    }
}
