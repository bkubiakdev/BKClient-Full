package dev.bkclient.utility.render.ambience;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.shader.GlProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.util.TriState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.client.render.Camera;
import java.util.Random;

/** Seeded GPU rain: a stable cell of falling streaks, clipped by the terrain heightmap. */
public final class RainRenderer {
    private final GlProgram shader = new GlProgram(BKClientInit.id("ambient_rain/data"), VertexFormats.POSITION_TEXTURE_COLOR);
    private VertexBuffer vertices;
    private RenderLayer layer;
    private int count;

    public void draw(Camera camera, Matrix4f viewProjection, RainHeightmap roof, float density, float opacity, float time) {
        int requested = Math.max(1, Math.round(4000 * density));
        if (vertices == null || requested != count) {
            close();
            count = requested;
            vertices = VertexBuffer.createAndUpload(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR, consumer -> {
                Random random = new Random(1592635409L);
                for (int i = 0; i < requested; i++) {
                    float x = random.nextFloat()*42, y = random.nextFloat()*30, z = random.nextFloat()*42;
                    int r = (int)(random.nextFloat()*255), g = (int)(random.nextFloat()*255), b = (int)(random.nextFloat()*255), a = 150+(int)(random.nextFloat()*105);
                    consumer.vertex(x,y,z).texture(0,0).color(r,g,b,a);
                    consumer.vertex(x,y,z).texture(0,1).color(r,g,b,a);
                    consumer.vertex(x,y,z).texture(1,1).color(r,g,b,a);
                    consumer.vertex(x,y,z).texture(1,0).color(r,g,b,a);
                }
            });
        }
        if (layer == null) layer = RenderLayer.of("bk_ambient_rain", VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormat.DrawMode.QUADS, 1536, false, true, RenderLayer.MultiPhaseParameters.builder()
                .program(shader.renderPhaseProgram()).texture(new RenderPhase.Texture(roof.id(), TriState.FALSE, false))
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).cull(RenderPhase.DISABLE_CULLING)
                .writeMaskState(RenderPhase.COLOR_MASK).build(false));
        var pos = camera.getPos();
        Vector3f right = camera.getRotation().transform(new Vector3f(1,0,0));
        Vector3f up = camera.getRotation().transform(new Vector3f(0,1,0));
        shader.findUniform("ViewProj").set(viewProjection);
        shader.findUniform("CamPos").set((float)pos.x,(float)pos.y,(float)pos.z);
        shader.findUniform("CamRight").set(right.x,right.y,right.z);
        shader.findUniform("CamUp").set(up.x,up.y,up.z);
        shader.findUniform("Motion").set(0,-24f,0);
        shader.findUniform("Cell").set(42f,30f,42f);
        shader.findUniform("Tint").set(178/255f,196/255f,214/255f,opacity);
        shader.findUniform("Time").set(time);
        shader.findUniform("Size").set(0.034f);
        shader.findUniform("Drift").set(0.05f);
        shader.findUniform("Flicker").set(0f);
        shader.findUniform("Stretch").set(17f);
        shader.findUniform("Roof").set(roof.x(),roof.z(),roof.y(),roof.isReady()?1f:0f);
        shader.findUniform("RoofSpan").set(192f);
        vertices.draw(layer);
    }

    public void close() {
        if (vertices != null && !vertices.isClosed()) vertices.close();
        vertices = null;
        count = 0;
    }
}
