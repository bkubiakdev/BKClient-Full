package dev.bkclient.systems.modules.modules.visuals.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Draws the sparks as camera-facing billboards, one batch per shape. */
public final class ParticleBillboardRenderer {

    /** Lifts every billboard by this much, so a burst reads as coming out of a body rather than off the floor. */
    private double heightBias;

    @FunctionalInterface
    public interface Tint {
        int argb(ClientParticle particle, float alpha);
    }

    /**
     * Sparks grouped by shape.
     *
     * <p>A texture change forces a flush, so a mixed burst drawn in spawn order would cost one draw
     * call per spark. Kept as a field and cleared rather than reallocated, since this runs every
     * frame the module has anything alive.
     */
    private final Map<Identifier, List<ClientParticle>> batches = new LinkedHashMap<>();

    public void render(MatrixStack matrices, Camera camera, float tickDelta, float worldSize,
                       boolean scaleWithAlpha, boolean glow, double heightBias,
                       List<ClientParticle> particles, Tint tint) {
        this.heightBias = heightBias;
        if (particles.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Vec3d camPos = camera.getPos();
        Vector3f forward = camera.getHorizontalPlane();

        for (List<ClientParticle> batch : this.batches.values()) {
            batch.clear();
        }

        for (ClientParticle particle : particles) {
            if (particle.alpha01(now) <= 0.004F) {
                continue;
            }
            Vec3d pos = particle.renderPos(tickDelta);
            double dx = pos.x - camPos.x;
            double dy = pos.y + this.heightBias - camPos.y;
            double dz = pos.z - camPos.z;
            // Anything meaningfully behind the camera is dropped before it reaches a buffer; the
            // billboard would still be built and transformed otherwise, just to be clipped away.
            if (dx * forward.x() + dy * forward.y() + dz * forward.z() < -worldSize * 2.0F) {
                continue;
            }
            this.batches.computeIfAbsent(particle.texture, key -> new ArrayList<>()).add(particle);
        }

        RenderSystem.enableBlend();
        if (glow) {
            RenderSystem.blendFuncSeparate(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE, GL11C.GL_ZERO);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (Map.Entry<Identifier, List<ClientParticle>> entry : this.batches.entrySet()) {
            List<ClientParticle> batch = entry.getValue();
            if (batch.isEmpty()) {
                continue;
            }
            RenderSystem.setShaderTexture(0, entry.getKey());
            BufferBuilder buffer = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (ClientParticle particle : batch) {
                float alpha = particle.alpha01(now);
                int color = tint.argb(particle, alpha);
                if ((color >>> 24) == 0) {
                    continue;
                }
                float size = scaleWithAlpha ? worldSize * alpha : worldSize;
                emit(buffer, matrices, camera, camPos, particle, tickDelta, size, color);
            }
            BuiltBuffer built = buffer.endNullable();
            if (built != null) {
                BufferRenderer.drawWithGlobalProgram(built);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public void clear() {
        this.batches.clear();
    }

    private void emit(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d camPos,
                             ClientParticle particle, float tickDelta, float size, int color) {
        if (size <= 0.0001F) {
            return;
        }
        Vec3d pos = particle.renderPos(tickDelta);
        matrices.push();
        matrices.translate(pos.x - camPos.x, pos.y + this.heightBias - camPos.y, pos.z - camPos.z);
        matrices.multiply(camera.getRotation());
        // Spin happens after the billboard is already facing the camera, so it reads as the shape
        // turning in place rather than tumbling through the world.
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(particle.rotationDeg));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size / 2.0F;
        buffer.vertex(matrix, -half, -half, 0.0F).texture(0.0F, 0.0F).color(color);
        buffer.vertex(matrix, half, -half, 0.0F).texture(1.0F, 0.0F).color(color);
        buffer.vertex(matrix, half, half, 0.0F).texture(1.0F, 1.0F).color(color);
        buffer.vertex(matrix, -half, half, 0.0F).texture(0.0F, 1.0F).color(color);
        matrices.pop();
    }
}
