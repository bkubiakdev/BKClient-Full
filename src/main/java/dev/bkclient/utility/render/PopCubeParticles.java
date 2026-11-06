package dev.bkclient.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.mixin.accessors.LivingEntityRendererAccessor;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Samples the posed body once; each voxel then moves independently, without ticking entities. */
public final class PopCubeParticles {
    private static final int MAX_CUBES = 900;
    private static final float[][] VERTICES = {
        {-1,-1,-1}, {1,-1,-1}, {1,1,-1}, {-1,1,-1},
        {-1,-1,1}, {1,-1,1}, {1,1,1}, {-1,1,1}
    };
    private static final int[][] FACES = {
        {0,3,2,1}, {4,5,6,7}, {0,4,7,3}, {1,2,6,5}, {0,1,5,4}, {3,7,6,2}
    };
    private static final float[] LIGHT = {0.72F,0.88F,0.65F,0.82F,0.55F,1};
    private final List<Cube> cubes = new ArrayList<>();
    private final float size;
    // Render-thread scratch for render(): the 8 transformed corners of the current cube.
    private final float[] cornerScratch = new float[VERTICES.length * 3];
    private final Vector3f cornerVec = new Vector3f();

    private PopCubeParticles(float size) { this.size = size; }

    public static PopCubeParticles capture(PlayerEntityRenderer renderer, PlayerEntityRenderState state, float size) {
        PopCubeParticles result = new PopCubeParticles(size);
        MatrixStack matrices = new MatrixStack();
        matrices.scale(state.baseScale, state.baseScale, state.baseScale);
        var transforms = (LivingEntityRendererAccessor)renderer;
        transforms.bkclient$setupTransforms(state, matrices, state.bodyYaw, state.baseScale);
        matrices.scale(-1, -1, 1);
        transforms.bkclient$scale(state, matrices);
        matrices.translate(0, -1.501F, 0);
        var model = renderer.getModel();
        model.setAngles(state);
        Random random = new Random();
        float spacing = Math.max(0.09F, size * 1.3F);
        // Sample base parts only: jacket/hat layers would duplicate the same voxels.
        for (ModelPart part : new ModelPart[]{model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg}) {
            part.forEachCuboid(matrices, (entry, path, index, box) -> {
                int nx = Math.max(1, (int)Math.ceil((box.maxX - box.minX) / 16F / spacing));
                int ny = Math.max(1, (int)Math.ceil((box.maxY - box.minY) / 16F / spacing));
                int nz = Math.max(1, (int)Math.ceil((box.maxZ - box.minZ) / 16F / spacing));
                for (int x = 0; x < nx; x++) for (int y = 0; y < ny; y++) for (int z = 0; z < nz; z++) {
                    if (result.cubes.size() >= MAX_CUBES) return;
                    Vector3f point = entry.getPositionMatrix().transformPosition(new Vector3f(
                        MathHelper.lerp((x + 0.5F) / nx, box.minX, box.maxX) / 16F,
                        MathHelper.lerp((y + 0.5F) / ny, box.minY, box.maxY) / 16F,
                        MathHelper.lerp((z + 0.5F) / nz, box.minZ, box.maxZ) / 16F));
                    Vector3f direction = new Vector3f(point.x, (point.y - state.height * 0.45F) * 0.2F, point.z);
                    direction.add(random.nextFloat() - 0.5F, random.nextFloat() * 0.45F, random.nextFloat() - 0.5F);
                    if (direction.lengthSquared() > 0.0001F) direction.normalize();
                    direction.mul(0.3F + random.nextFloat() * 0.7F);
                    result.cubes.add(new Cube(point, direction, random.nextFloat() * 0.16F,
                        0.75F + random.nextFloat() * 0.35F, random.nextFloat() * 6.28F));
                }
            });
        }
        return result;
    }

    public void render(Render3DEvent event, Vec3d origin, float progress, float rise, float spread, float alpha, ColorRGBA accent) {
        if (cubes.isEmpty()) return;
        Vec3d relative = origin.subtract(event.getCamera().getPos());
        MatrixStack matrices = event.getMatrices();
        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        try {
            matrices.translate(relative.x, relative.y, relative.z);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            Matrix4f transform = new Matrix4f();
            Matrix4f base = matrices.peek().getPositionMatrix();
            // Each cube has 8 corners but 24 emitted vertices; vertex(Matrix4f, x, y, z) is exactly
            // transform.transformPosition(x, y, z, new Vector3f()) followed by vertex(x', y', z'), so the
            // corners are transformed once into this table and emitted directly: bit-identical output.
            float[] corners = this.cornerScratch;
            Vector3f corner = this.cornerVec;
            for (Cube cube : cubes) {
                float motion = MathHelper.clamp((progress - 0.12F - cube.delay) / (0.88F - cube.delay), 0, 1);
                float travel = motion * motion * (3 - 2 * motion);
                float shrink = 1 - MathHelper.clamp((progress - 0.5F) / 0.5F, 0, 1);
                float half = size * cube.scale * (0.25F + 0.75F * shrink) * 0.5F;
                float x = cube.position.x + cube.direction.x * spread * travel;
                float y = cube.position.y + (cube.direction.y * spread + rise) * travel;
                float z = cube.position.z + cube.direction.z * spread * travel;
                transform.set(base).translate(x, y, z)
                    .rotateXYZ(motion * cube.spin, motion * cube.spin * 0.7F, motion * cube.spin * 0.4F);
                for (int i = 0; i < VERTICES.length; i++) {
                    float[] v = VERTICES[i];
                    transform.transformPosition(v[0] * half, v[1] * half, v[2] * half, corner);
                    corners[i * 3] = corner.x;
                    corners[i * 3 + 1] = corner.y;
                    corners[i * 3 + 2] = corner.z;
                }
                float tint = MathHelper.clamp(cube.position.y / 2, 0, 1) * 0.4F;
                float r = MathHelper.lerp(tint, accent.getRed(), 255);
                float g = MathHelper.lerp(tint, accent.getGreen(), 255);
                float b = MathHelper.lerp(tint, accent.getBlue(), 255);
                int a = Math.round(255 * alpha * shrink);
                for (int face = 0; face < FACES.length; face++) {
                    int color = a << 24 | Math.round(r * LIGHT[face]) << 16 | Math.round(g * LIGHT[face]) << 8 | Math.round(b * LIGHT[face]);
                    for (int vertex : FACES[face]) {
                        int c = vertex * 3;
                        buffer.vertex(corners[c], corners[c + 1], corners[c + 2]).color(color);
                    }
                }
            }
            BuiltBuffer built = buffer.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        } finally {
            matrices.pop();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private record Cube(Vector3f position, Vector3f direction, float delay, float scale, float spin) { }
}
