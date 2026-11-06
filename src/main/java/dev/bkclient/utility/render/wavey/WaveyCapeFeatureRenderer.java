package dev.bkclient.utility.render.wavey;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Capes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * 1:1 port of WaveyCapes CustomCapeRenderer.renderSmoothCape
 * Pre-computes per-segment Matrix4f, then emits vertices with blended normals.
 */
public class WaveyCapeFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private static final int   PART_COUNT = 16;
    private static final float W = 10f / 16f;  // cape width (Â±W/2)
    private static final float H = 1f;          // total height
    private static final float D = 1f / 16f;    // depth/thickness

    public WaveyCapeFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> ctx) {
        super(ctx);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {

        if (state.invisible || !state.capeVisible) return;
        if (state.equippedChestStack != null
                && state.equippedChestStack.getItem() == net.minecraft.item.Items.ELYTRA) return;

        Capes capesModule = BKClientInit.getInstance().getModuleManager().getModule(Capes.class);
        if (capesModule != null && !capesModule.waveyAnimation.isEnabled()) return;

        Identifier capeTexture = null;
        if (capesModule != null && capesModule.isEnabled()) {
            capeTexture = capesModule.getCapeIdentifier();
        } else if (state.skinTextures != null) {
            capeTexture = state.skinTextures.capeTexture();
        }
        if (capeTexture == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        // Only render wavy cape for our own player
        if (!mc.player.getGameProfile().getName().equals(state.name)) return;
        if (!(mc.player instanceof WaveyCapable wavey)) return;
        WaveySimulation sim = wavey.bkclient$getWaveySimulation();
        if (sim == null || sim.isEmpty()) return;

        float delta = mc.getRenderTickCounter().getTickDelta(false);
        VertexConsumer buf = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(capeTexture));

        matrices.push();
        getContextModel().body.rotate(matrices);

        Matrix4f[]   pos       = new Matrix4f[PART_COUNT];
        Vector3f[]   frontN    = new Vector3f[PART_COUNT];
        Vector3f[]   backN     = new Vector3f[PART_COUNT];

        for (int part = 0; part < PART_COUNT; part++) {
            modifyPoseStack(matrices, sim, delta, part);   // pushes
            pos[part] = new Matrix4f(matrices.peek().getPositionMatrix());
            matrices.pop();
        }

        // pre-compute per-segment normals (1:1 WaveyCapes frontNormalVecs / backNormalVecs)
        for (int part = 0; part < PART_COUNT; part++) {
            int prev = Math.max(part - 1, 0);
            float y0 = part       * (H / PART_COUNT);
            float y1 = (part + 1) * (H / PART_COUNT);
            frontN[part] = getNormal(pos[prev], pos[prev], pos[part],
                     W/2, y0, -D,   -W/2, y0, -D,   W/2, y1, -D);
            backN[part]  = getNormal(pos[prev], pos[prev], pos[part],
                     W/2, y1,  0,   -W/2, y1,  0,   W/2, y0,  0);
        }

        for (int part = 0; part < PART_COUNT; part++) {
            float y0   = part       * (H / PART_COUNT);
            float y1   = (part + 1) * (H / PART_COUNT);
            int   prev = Math.max(part - 1, 0);
            int   next = Math.min(part + 1, PART_COUNT - 1);

            float vTop = (1f / 32f) * (part + 1);
            float vBot = vTop + 1f / 32f;

            if (part == 0) {
                Vector3f n = getNormal(pos[0], pos[0], pos[0],
                         W/2, 0, 0,  -W/2, 0, 0,  W/2, 0, D);
                vtx(buf, pos[0],  W/2, 0,  0,  11/64f, 1/32f, light, n);
                vtx(buf, pos[0], -W/2, 0,  0,   1/64f, 1/32f, light, n);
                vtx(buf, pos[0], -W/2, 0, -D,   1/64f,     0, light, n);
                vtx(buf, pos[0],  W/2, 0, -D,  11/64f,     0, light, n);
            }

            if (part == PART_COUNT - 1) {
                Vector3f n = getNormal(pos[part], pos[part], pos[part],
                         W/2, H, -D,  -W/2, H, -D,  W/2, H, 0);
                vtx(buf, pos[part],  W/2, H, -D,  21/64f,     0, light, n);
                vtx(buf, pos[part], -W/2, H, -D,  11/64f,     0, light, n);
                vtx(buf, pos[part], -W/2, H,  0,  11/64f, 1/32f, light, n);
                vtx(buf, pos[part],  W/2, H,  0,  21/64f, 1/32f, light, n);
            }

            {
                Vector3f n = getNormal(pos[part], pos[part], pos[prev],
                        -W/2, y1, 0,  -W/2, y1, -D,  -W/2, y0, 0);
                vtx(buf, pos[part], -W/2, y1,  0,      0, vBot, light, n);
                vtx(buf, pos[part], -W/2, y1, -D,  1/64f, vBot, light, n);
                vtx(buf, pos[prev], -W/2, y0, -D,  1/64f, vTop, light, n);
                vtx(buf, pos[prev], -W/2, y0,  0,      0, vTop, light, n);
            }

            {
                Vector3f n = getNormal(pos[part], pos[part], pos[prev],
                         W/2, y1, -D,  W/2, y1, 0,  W/2, y0, -D);
                vtx(buf, pos[part],  W/2, y1, -D,  11/64f, vBot, light, n);
                vtx(buf, pos[part],  W/2, y1,  0,  12/64f, vBot, light, n);
                vtx(buf, pos[prev],  W/2, y0,  0,  12/64f, vTop, light, n);
                vtx(buf, pos[prev],  W/2, y0, -D,  11/64f, vTop, light, n);
            }

            {
                Vector3f nTop = blend(frontN[part], frontN[prev]);
                Vector3f nBot = blend(frontN[part], frontN[next]);
                vtx(buf, pos[prev],  W/2, y0, -D,  11/64f, vTop, light, nTop);
                vtx(buf, pos[prev], -W/2, y0, -D,   1/64f, vTop, light, nTop);
                vtx(buf, pos[part], -W/2, y1, -D,   1/64f, vBot, light, nBot);
                vtx(buf, pos[part],  W/2, y1, -D,  11/64f, vBot, light, nBot);
            }

            {
                Vector3f nTop = blend(backN[part], backN[prev]);
                Vector3f nBot = blend(backN[part], backN[next]);
                vtx(buf, pos[prev],  W/2, y0, 0,  12/64f, vTop, light, nTop);
                vtx(buf, pos[prev], -W/2, y0, 0,  22/64f, vTop, light, nTop);
                vtx(buf, pos[part], -W/2, y1, 0,  22/64f, vBot, light, nBot);
                vtx(buf, pos[part],  W/2, y1, 0,  12/64f, vBot, light, nBot);
            }
        }

        matrices.pop();
    }

    /** modifyPoseStack does pushPose internally — 1:1 WaveyCapes modifyPoseStackSimulation */
    private void modifyPoseStack(MatrixStack matrices, WaveySimulation sim, float delta, int part) {
        matrices.push();
        matrices.translate(0.0, 0.0, 0.125);

        float x = sim.points.get(part).lerpX(delta) - sim.points.get(0).lerpX(delta);
        if (x > 0) x = 0;
        float y = sim.points.get(0).lerpY(delta) - part - sim.points.get(part).lerpY(delta);
        float z = sim.points.get(0).lerpZ(delta) - sim.points.get(part).lerpZ(delta);

        float rot  = getRotation(delta, part, sim);
        float wind = naturalWind(part);

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0f + wind));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

        matrices.translate(-z / PART_COUNT, y / PART_COUNT, x / PART_COUNT);

        matrices.translate(0,  (0.48 / 16), -(0.48 / 16));
        matrices.translate(0,  part * 1f / PART_COUNT, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-rot));
        matrices.translate(0, -part * 1f / PART_COUNT, 0);
        matrices.translate(0, -(0.48 / 16),  (0.48 / 16));
    }

    private float getRotation(float delta, int part, WaveySimulation sim) {
        if (part == PART_COUNT - 1) return getRotation(delta, part - 1, sim);
        WaveySimulation.Point a = sim.points.get(part);
        WaveySimulation.Point b = sim.points.get(part + 1);
        float dx = b.lerpX(delta) - a.lerpX(delta);
        float dy = b.lerpY(delta) - a.lerpY(delta);
        return (float)(Math.toDegrees(Math.atan2(dx, dy)) + 180);
    }

    private float naturalWind(int part) {
        long t = (System.currentTimeMillis() / 3) % 360;
        float rel = (float)(part + 1) / PART_COUNT;
        return (float)(Math.sin(Math.toRadians(rel * 360 - t)) * 3);
    }


    /** Average of two normals (for smooth shading across segment boundaries) */
    private static Vector3f blend(Vector3f a, Vector3f b) {
        return new Vector3f((a.x+b.x)/2f, (a.y+b.y)/2f, (a.z+b.z)/2f);
    }

    /**
     * Compute face normal from 3 points transformed by 3 different matrices.
     * 1:1 WaveyCapes getNormalVec logic.
     */
    private static Vector3f getNormal(Matrix4f m0, Matrix4f m1, Matrix4f m2,
                                      float ax, float ay, float az,
                                      float bx, float by, float bz,
                                      float cx, float cy, float cz) {
        Vector4f a = m0.transform(new Vector4f(ax, ay, az, 1f));
        Vector4f b = m1.transform(new Vector4f(bx, by, bz, 1f));
        Vector4f c = m2.transform(new Vector4f(cx, cy, cz, 1f));
        float ux = b.x-a.x, uy = b.y-a.y, uz = b.z-a.z;
        float vx = c.x-a.x, vy = c.y-a.y, vz = c.z-a.z;
        float nx = uy*vz - uz*vy;
        float ny = uz*vx - ux*vz;
        float nz = ux*vy - uy*vx;
        float len = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len < 1e-5f) return new Vector3f(0, 1, 0);
        return new Vector3f(nx/len, ny/len, nz/len);
    }

    /** Emit one vertex (world-space normal passed directly, as in WaveyCapes addVertex) */
    private static void vtx(VertexConsumer buf, Matrix4f m,
                             float x, float y, float z,
                             float u, float v,
                             int light, Vector3f n) {
        buf.vertex(m, x, y, z)
           .color(255, 255, 255, 255)
           .texture(u, v)
           .overlay(OverlayTexture.DEFAULT_UV)
           .light(light)
           .normal(n.x, n.y, n.z);
    }
}
