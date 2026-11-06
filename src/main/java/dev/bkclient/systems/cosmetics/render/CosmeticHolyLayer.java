package dev.bkclient.systems.cosmetics.render;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;

import java.util.HashMap;
import java.util.Map;

/** Animated Holy blade material; Minecraft loads and reloads the program through its shader key. */
public final class CosmeticHolyLayer {
    private static final ShaderProgramKey PROGRAM = new ShaderProgramKey(
        Identifier.of("bkclient", "core/cosmetics_holy"),
        VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, Defines.EMPTY);
    private static final Map<Identifier, RenderLayer> LAYERS = new HashMap<>();

    private CosmeticHolyLayer() {}

    public static RenderLayer get(Identifier texture) {
        return LAYERS.computeIfAbsent(texture, id -> RenderLayer.of("bkclient_cosmetics_holy",
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS,
            1536, false, true, RenderLayer.MultiPhaseParameters.builder()
                .program(new RenderPhase.ShaderProgram(PROGRAM))
                .texture(new RenderPhase.Texture(id, TriState.FALSE, false))
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .cull(RenderPhase.ENABLE_CULLING)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                .writeMaskState(RenderPhase.ALL_MASK)
                .build(false)));
    }
}
