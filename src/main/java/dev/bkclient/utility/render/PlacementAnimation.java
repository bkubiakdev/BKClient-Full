package dev.bkclient.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.colors.ColorRGBA;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Compact web pulse or a bottom-up trap reveal with a surface outline and fading sweep. */
public final class PlacementAnimation {
    private final Map<BlockPos, Long> placements = new LinkedHashMap<>();
    private final boolean trapReveal;
    private ClientWorld world;

    public PlacementAnimation() { this(false); }

    public PlacementAnimation(boolean trapReveal) { this.trapReveal = trapReveal; }

    public void add(BlockPos pos) {
        checkWorld();
        // Retried interactions must not repeatedly restart the same visible pulse.
        long now = System.nanoTime();
        placements.values().removeIf(start -> now - start >= lifetimeNanos());
        placements.putIfAbsent(pos.toImmutable(), now);
    }

    public void clear() {
        placements.clear();
        world = null;
    }

    private void checkWorld() {
        ClientWorld current = MinecraftClient.getInstance().world;
        if (world != current) {
            placements.clear();
            world = current;
        }
    }

    private long lifetimeNanos() { return trapReveal ? 700_000_000L : 400_000_000L; }

    private static float smooth(float value) {
        float t = Math.max(0f, Math.min(1f, value));
        return t * t * (3f - 2f * t);
    }

    public void render(Render3DEvent event, ColorRGBA color) {
        checkWorld();
        long now = System.nanoTime();
        placements.values().removeIf(start -> now - start >= lifetimeNanos());
        if (world == null || placements.isEmpty()) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        var matrices = event.getMatrices();
        try {
            for (var entry : placements.entrySet()) {
                float age = (now - entry.getValue()) / 1_000_000f;
                float t = age < 200f ? age / 200f : (age - 200f) / 200f;
                float ease = 1f - (float) Math.pow(1f - t, 3);
                float scale = age < 200f ? ease : 1f - ease;
                float opacity = trapReveal ? smooth(age / 90f) * (1f - smooth((age - 230f) / 470f)) : scale;
                if (opacity < 0.01f) continue;
                double radius = trapReveal ? 0.503 : scale * 0.5;
                float reveal = 1f - (float) Math.pow(1f - Math.min(1f, age / 190f), 3);
                Box box = trapReveal
                    ? new Box(-radius, -radius, -radius, radius, -radius + 2 * radius * reveal, radius)
                    : new Box(-radius, -radius, -radius, radius, radius, radius);
                matrices.push();
                try {
                    RenderUtility.prepareMatrices(matrices, Vec3d.ofCenter(entry.getKey()));
                    var fill = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                    Draw3DUtility.renderFilledBox(matrices, fill, box, color.withAlpha((trapReveal ? 22f : 30f) * opacity));
                    Box sweep = null;
                    float sweepAlpha = 0f;
                    if (trapReveal) {
                        // A thin illuminated band travels upwards while the full outline settles.
                        double y = -radius + 2 * radius * smooth(age / 420f);
                        sweep = new Box(-radius - 0.008, y - 0.012, -radius - 0.008,
                            radius + 0.008, Math.min(radius + 0.012, y + 0.012), radius + 0.008);
                        sweepAlpha = opacity * (1f - smooth((age - 280f) / 170f));
                        Draw3DUtility.renderFilledBox(matrices, fill, sweep, color.withAlpha(45f * sweepAlpha));
                        Draw3DUtility.renderFilledBox(matrices, fill, box.expand(0.012), color.withAlpha(4f * opacity));
                    }
                    RenderUtility.buildBuffer(fill);
                    var outline = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                    Draw3DUtility.renderOutlinedBox(matrices, outline, box, color.withAlpha(210f * opacity));
                    if (sweep != null) {
                        Draw3DUtility.renderOutlinedBox(matrices, outline, sweep, color.withAlpha(235f * sweepAlpha));
                    }
                    RenderUtility.buildBuffer(outline);
                } finally {
                    matrices.pop();
                }
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }
}
