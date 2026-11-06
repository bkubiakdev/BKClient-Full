package dev.bkclient.systems.modules.modules.visuals.esp;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.Utils;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Additive bloom and bottom-to-top fading ESP boxes. */
public final class GradientBoxRenderer {
    private static final Identifier BLOOM = Identifier.of("bkclient", "textures/esp/box_bloom.png");
    private static final int[][] FACES = {
        {0, 1, 2, 3}, {4, 5, 6, 7}, {3, 2, 6, 7},
        {0, 1, 5, 4}, {0, 3, 7, 4}, {1, 2, 6, 5}
    };
    private static final int[][] EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6},
        {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private GradientBoxRenderer() {}

    public static void render(Render3DEvent event, List<Entity> targets, ColorRGBA color,
                              boolean fillGradient, boolean outlineGradient) {
        if (targets.isEmpty()) return;
        MatrixStack matrices = event.getMatrices();
        Vec3d camera = event.getCamera().getPos();
        RenderUtility.setupRender3D(true);
        try {
            RenderSystem.setShaderTexture(0, BLOOM);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder bloom = RenderSystem.renderThreadTesselator()
                .begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (Entity entity : targets) {
                Vec3d pos = Utils.getInterpolatedPos(entity, event.getTickDelta()).subtract(camera);
                float size = (entity.getWidth() + entity.getHeight()) * 1.2F;
                if (fillGradient) {
                    bloom(matrices, bloom, event, pos, entity.getHeight() / 4.0F, size, color);
                } else {
                    bloom(matrices, bloom, event, pos, entity.getHeight() / 3.0F, size, color);
                    bloom(matrices, bloom, event, pos, entity.getHeight() / 1.5F, size, color);
                }
            }
            RenderUtility.buildBuffer(bloom);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            // The choices control fading, not whether the faces/edges are drawn.
            ColorRGBA fill = color.mulAlpha(fillGradient && !outlineGradient ? 0.35F : 0.1F);
            BufferBuilder faces = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (Entity entity : targets) {
                box(matrices, faces, entity, event, camera, fill, fillGradient, FACES);
            }
            RenderUtility.buildBuffer(faces);
            BufferBuilder edges = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (Entity entity : targets) {
                box(matrices, edges, entity, event, camera, color, outlineGradient, EDGES);
            }
            RenderUtility.buildBuffer(edges);
        } finally {
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.defaultBlendFunc();
            RenderUtility.endRender3D();
        }
    }

    /** Editor uses the same geometry and gradient rules in its model-local matrix. */
    public static void preview(MatrixStack matrices, Entity entity, ColorRGBA color, boolean fillGradient, boolean outlineGradient) {
        float half = entity.getWidth()/2;
        preview(matrices,new net.minecraft.util.math.Box(-half,0,-half,half,entity.getHeight(),half),color,fillGradient,outlineGradient);
    }

    public static void preview(MatrixStack matrices, net.minecraft.util.math.Box bounds, ColorRGBA color, boolean fillGradient, boolean outlineGradient) {
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull(); RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        try {
            BufferBuilder faces=RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS,VertexFormats.POSITION_COLOR);
            vertices(matrices,faces,(float)bounds.minX,(float)bounds.maxX,(float)bounds.minY,(float)bounds.maxY,(float)bounds.minZ,(float)bounds.maxZ,
                color.mulAlpha(fillGradient&&!outlineGradient?.35F:.1F),fillGradient,FACES);
            RenderUtility.buildBuffer(faces);
            BufferBuilder edges=RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES,VertexFormats.POSITION_COLOR);
            vertices(matrices,edges,(float)bounds.minX,(float)bounds.maxX,(float)bounds.minY,(float)bounds.maxY,(float)bounds.minZ,(float)bounds.maxZ,color,outlineGradient,EDGES);
            RenderUtility.buildBuffer(edges);
        } finally {RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();}
    }

    private static void bloom(MatrixStack matrices, BufferBuilder buffer, Render3DEvent event,
                              Vec3d pos, float height, float size, ColorRGBA color) {
        matrices.push();
        try {
            matrices.translate(pos.x, pos.y + height, pos.z);
            matrices.multiply(event.getCamera().getRotation());
            DrawUtility.drawImage(matrices, buffer, -size / 2.0F, -size / 2.0F, 0, size, size, color.mulAlpha(0.5F));
        } finally {
            matrices.pop();
        }
    }

    private static void box(MatrixStack matrices, BufferBuilder buffer, Entity entity, Render3DEvent event,
                            Vec3d camera, ColorRGBA color, boolean gradient, int[][] primitives) {
        Vec3d pos = Utils.getInterpolatedPos(entity, event.getTickDelta()).subtract(camera);
        float halfWidth = entity.getWidth() / 2.0F;
        float minX = (float) pos.x - halfWidth, maxX = (float) pos.x + halfWidth;
        float minY = (float) pos.y, maxY = minY + entity.getHeight();
        float minZ = (float) pos.z - halfWidth, maxZ = (float) pos.z + halfWidth;
        vertices(matrices, buffer, minX, maxX, minY, maxY, minZ, maxZ, color, gradient, primitives);
    }

    private static void vertices(MatrixStack matrices, BufferBuilder buffer, float minX, float maxX,
                                 float minY, float maxY, float minZ, float maxZ,
                                 ColorRGBA color, boolean gradient, int[][] primitives) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int bottom = color.getRGB();
        int top = gradient ? color.mulAlpha(0).getRGB() : bottom;
        for (int[] primitive : primitives) {
            for (int vertex : primitive) {
                int corner = vertex & 3;
                buffer.vertex(matrix, corner == 1 || corner == 2 ? maxX : minX,
                    vertex >= 4 ? maxY : minY, corner >= 2 ? maxZ : minZ)
                    .color(vertex >= 4 ? top : bottom);
            }
        }
    }
}
