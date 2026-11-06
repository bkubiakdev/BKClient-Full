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

/** Fullbright cosmetic strips, with a depth-read-only variant for the emission mask. */
final class CosmeticEmissiveLayer {
    private static final ShaderProgramKey PROGRAM = new ShaderProgramKey(
            Identifier.of("bkclient", "core/cosmetics_emissive"),
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, Defines.EMPTY);
    // MAIN_TARGET and item targets can rebind Minecraft's framebuffer during a buffered draw.
    private static final RenderPhase.Target CURRENT_TARGET = new RenderPhase.Target(
            "bkclient_cosmetic_current_target", () -> {}, () -> {});
    private static final Map<Identifier, RenderLayer> SURFACES = new HashMap<>();
    private static final Map<Identifier, RenderLayer> MASKS = new HashMap<>();

    private CosmeticEmissiveLayer() {}

    static RenderLayer get(Identifier texture, boolean mask) {
        return (mask ? MASKS : SURFACES).computeIfAbsent(texture, id -> RenderLayer.of(
                mask ? "bkclient_cosmetics_emission_mask" : "bkclient_cosmetics_emissive",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS,
                1536, false, true, RenderLayer.MultiPhaseParameters.builder()
                        .program(new RenderPhase.ShaderProgram(PROGRAM))
                        .texture(new RenderPhase.Texture(id, TriState.FALSE, false))
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .cull(RenderPhase.ENABLE_CULLING)
                        .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                        .writeMaskState(mask ? RenderPhase.COLOR_MASK : RenderPhase.ALL_MASK)
                        .target(CURRENT_TARGET)
                        .build(false)));
    }

    static void clear() {
        SURFACES.clear();
        MASKS.clear();
    }
}
