package dev.bkclient.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Vega-style dash particle trail, ported from BKClient 1.21.11's DashTrail module.
 *
 * <p>The original ran on the 1.21.5+ RenderPipeline API; this rewrite targets
 * 1.21.4 and draws through immediate-mode {@link Tessellator} batches instead,
 * grouping quads per texture so a frame costs one draw call per distinct sprite.
 */
public final class DashTrailRenderer {

    private static final String ROOT = "textures/features/dashtrail/";
    private static final Identifier BLOOM_TEXTURE = texture("dashbloomsample.png");
    private static final Identifier DOT_TEXTURE = texture("dashbloom.png");
    private static final float BASE_PIXEL_SCALE = 0.0033f;
    private static final double MIN_DASH_SPEED = 0.08D;
    // Raised from BKClient's 420: at the top of the extended lifetime slider a
    // sprinting player keeps far more pieces alive at once, and the old cap
    // trimmed the tail of the trail away mid-flight.
    private static final int MAX_CUBICS = 1400;

    private static final TextureInfo[] STATIC_TEXTURES = new TextureInfo[]{
            cubic(1, 48, 48), cubic(2, 48, 48), cubic(3, 48, 48),
            cubic(4, 96, 48), cubic(5, 96, 48), cubic(6, 96, 48),
            cubic(7, 96, 48), cubic(8, 96, 48), cubic(9, 96, 48),
            cubic(10, 96, 48), cubic(11, 96, 48), cubic(12, 96, 48),
            cubic(13, 96, 96), cubic(14, 96, 96), cubic(15, 96, 96),
            cubic(16, 96, 96), cubic(17, 44, 48), cubic(18, 44, 48),
            cubic(19, 44, 48), cubic(20, 44, 48), cubic(21, 44, 48)
    };

    private static final TextureInfo[][] ANIMATED_TEXTURES = new TextureInfo[][]{
            group(1, 11, 144, 48),
            group(2, 23, 48, 48),
            group(3, 32, 96, 48),
            group(4, 16, 48, 48),
            group(5, 32, 48, 48)
    };

    private final Random random = new Random(1234567891L);
    private final List<DashCubic> dashCubics = new ArrayList<>();
    private final Map<Integer, Vec3d> lastPositions = new HashMap<>();

    // Per-frame batching scratch, reused so rendering allocates nothing steady-state.
    // batchPool keeps one growable batch per texture across frames; batchOrder holds the
    // batches used this frame in first-queued order, which is the order the old per-frame
    // LinkedHashMap iterated in, so draw order (and therefore the blended result) is unchanged.
    private final Map<Identifier, QuadBatch> batchPool = new HashMap<>();
    private final List<QuadBatch> batchOrder = new ArrayList<>();

    // Matrix scratch for drawBatches (render thread only).
    private final Matrix4f billboardMatrix = new Matrix4f();
    private final Matrix4f translatedMatrix = new Matrix4f();
    private final Matrix4f rotatedMatrix = new Matrix4f();
    private final Quaternionf rollQuat = new Quaternionf();

    /** Tunables pushed in from the module before each update/render. */
    private boolean motionSmoothing;
    private boolean dashSegments;
    private boolean dashDots;
    private float dashLength = 0.75f;

    public void configure(boolean motionSmoothing, boolean dashSegments, boolean dashDots,
                          float dashLength) {
        this.motionSmoothing = motionSmoothing;
        this.dashSegments = dashSegments;
        this.dashDots = dashDots;
        this.dashLength = dashLength;
    }

    public void clear() {
        dashCubics.clear();
        lastPositions.clear();
        resetBatches();
        batchPool.clear();
    }

    private void resetBatches() {
        for (int i = 0; i < batchOrder.size(); i++) batchOrder.get(i).size = 0;
        batchOrder.clear();
    }

    /** Drops tracking state for players that are no longer valid targets. */
    public void retainOnly(java.util.Set<Integer> liveIds) {
        lastPositions.keySet().removeIf(id -> !liveIds.contains(id));
    }

    public void trackPlayer(PlayerEntity player, ColorRGBA color) {
        Vec3d current = player.getPos();
        Vec3d previous = lastPositions.put(player.getId(), current);
        if (previous == null) return;

        Vec3d delta = current.subtract(previous);
        double speedXZ = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (speedXZ < MIN_DASH_SPEED || delta.lengthSquared() > 36.0D) return;

        int count = MathHelper.clamp((int) (delta.length() / MIN_DASH_SPEED), 1, 16);
        boolean addSparks = dashSegments || dashDots;
        for (int i = 0; i < count; i++) {
            float offset = count == 1 ? 0.0f : (float) i / (float) count;
            spawnCubic(player, current, delta, offset, addSparks, color);
        }
        trimCubics();
    }

    private void spawnCubic(PlayerEntity player, Vec3d current, Vec3d delta,
                            float offset, boolean addSparks, ColorRGBA color) {
        double height = player.getHeight();
        double x = current.x - delta.x * offset + random.nextDouble() * 0.175D - 0.0875D;
        double y = current.y - delta.y * offset + height / 3.0D + height / 4.0D * random.nextDouble() * 0.7D;
        double z = current.z - delta.z * offset + random.nextDouble() * 0.175D - 0.0875D;
        Vec3d position = new Vec3d(x, y, z);
        Vec3d motion = delta.multiply(0.04D);
        int lifetime = randomLifetime();
        DashTexture dashTexture = DashTexture.random(random, lifetime);
        float scale = 0.86f + random.nextFloat() * 0.34f;
        dashCubics.add(new DashCubic(player.getId(), position, motion, dashTexture,
                lifetime, color.getRGB(), scale, addSparks, random));
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < dashCubics.size(); i++) {
            DashCubic current = dashCubics.get(i);
            DashCubic next = motionSmoothing && i + 1 < dashCubics.size()
                    && dashCubics.get(i + 1).entityId == current.entityId
                    ? dashCubics.get(i + 1)
                    : null;
            current.tick(next, random, dashSegments, dashDots, now);
        }
        dashCubics.removeIf(cubic -> cubic.isDead(now));
        trimCubics();
    }

    public boolean isEmpty() {
        return dashCubics.isEmpty();
    }

    public void render(MatrixStack matrices, float tickDelta) {
        long now = System.currentTimeMillis();
        dashCubics.removeIf(cubic -> cubic.isDead(now));
        MinecraftClient mc = MinecraftClient.getInstance();
        if (dashCubics.isEmpty() || mc.gameRenderer == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        resetBatches();
        // Same value Vec3d#lerp received (float clamp widened to double).
        double lerpDelta = MathHelper.clamp(tickDelta, 0.0f, 1.0f);

        for (DashCubic cubic : dashCubics) {
            float progress = cubic.progress(now);
            float alpha = dashAlpha(progress);
            if (alpha <= 0.02f) continue;

            // Inlined DashCubic#renderPos: Vec3d#lerp is MathHelper.lerp per component.
            double px = MathHelper.lerp(lerpDelta, cubic.prevPos.x, cubic.pos.x);
            double py = MathHelper.lerp(lerpDelta, cubic.prevPos.y, cubic.pos.y);
            double pz = MathHelper.lerp(lerpDelta, cubic.prevPos.z, cubic.pos.z);
            TextureInfo info = cubic.texture.get(now);
            float sizeAlpha = easeOutQuad(alpha);
            float width = info.width * BASE_PIXEL_SCALE * cubic.scale * sizeAlpha;
            float height = info.height * BASE_PIXEL_SCALE * cubic.scale * sizeAlpha;

            int mainColor = multAlpha(lerpColor(cubic.color, 0xFFFFFFFF, 0.34f), alpha * 0.92f);
            queue(info.id, px, py, pz, width, height, cubic.rotation, mainColor);

            queueBloom(px, py, pz, width, height, cubic.color, alpha);

            if (dashDots || dashSegments) {
                queueSparks(cubic, px, py, pz, lerpDelta, alpha);
            }
        }

        if (batchOrder.isEmpty()) return;
        flush(matrices, camera, camPos);
    }

    private void queueBloom(double x, double y, double z, float width, float height, int color, float alpha) {
        float diagonal = (float) Math.sqrt(width * width + height * height);
        int softColor = multAlpha(lerpColor(color, 0xFFFFFFFF, 0.18f), alpha * 0.13f);
        queue(BLOOM_TEXTURE, x, y, z, diagonal * 0.58f, diagonal * 0.58f, 0.0f, softColor);
    }

    private void queueSparks(DashCubic cubic, double cx, double cy, double cz, double lerpDelta, float cubicAlpha) {
        long now = System.currentTimeMillis();
        for (DashSpark spark : cubic.sparks) {
            float sparkAlpha = easeInOutWave((float) spark.alpha(now) * cubicAlpha);
            if (sparkAlpha <= 0.01f) continue;

            // Same arithmetic as the old Vec3d chain: renderPos = per-component MathHelper.lerp,
            // first = center.add(offset), second = center.subtract(offset) (Vec3d adds the negated offset).
            double ox = MathHelper.lerp(lerpDelta, spark.prevPos.x, spark.pos.x);
            double oy = MathHelper.lerp(lerpDelta, spark.prevPos.y, spark.pos.y);
            double oz = MathHelper.lerp(lerpDelta, spark.prevPos.z, spark.pos.z);
            double fx = cx + ox, fy = cy + oy, fz = cz + oz;
            double sx = cx + -ox, sy = cy + -oy, sz = cz + -oz;
            int sparkColor = multAlpha(lerpColor(cubic.color, 0xFFFFFFFF, 0.45f), sparkAlpha * 0.72f);

            if (dashDots) {
                float size = 0.052f + 0.07f * sparkAlpha;
                queue(DOT_TEXTURE, fx, fy, fz, size, size, 0.0f, sparkColor);
                queue(DOT_TEXTURE, sx, sy, sz, size, size, 0.0f, sparkColor);
            }
            if (dashSegments) {
                // Segments are drawn as a thin dot-textured quad stretched between
                // the two spark points; 1.21.4 has no cheap gradient line primitive here.
                // mid = first.add(second).multiply(0.5); len = first.distanceTo(second).
                double mx = (fx + sx) * 0.5D, my = (fy + sy) * 0.5D, mz = (fz + sz) * 0.5D;
                double dx = sx - fx, dy = sy - fy, dz = sz - fz;
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                queue(DOT_TEXTURE, mx, my, mz, len, 0.012f, 0.0f, multAlpha(sparkColor, 0.55f));
            }
        }
    }

    private void queue(Identifier texture, double x, double y, double z,
                       float width, float height, float rotation, int color) {
        if (width <= 0.001f || height <= 0.001f || ((color >>> 24) & 0xFF) <= 0) return;
        QuadBatch batch = batchPool.computeIfAbsent(texture, QuadBatch::new);
        if (batch.size == 0) batchOrder.add(batch);
        batch.add(x, y, z, width, height, rotation, color);
    }

    private void flush(MatrixStack matrices, Camera camera, Vec3d camPos) {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        // Additive is mandatory here, not a style choice: the dash textures are stored
        // as RGB with no alpha channel (PNG colour type 2), so their black background is
        // fully opaque. Under straight alpha blending every quad paints a black square.
        // With additive, black adds nothing and the luminance acts as the mask.
        drawBatches(matrices, camera, camPos);

        resetBatches();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawBatches(MatrixStack matrices, Camera camera, Vec3d camPos) {
        RenderSystem.blendFuncSeparate(
                org.lwjgl.opengl.GL11C.GL_SRC_ALPHA, org.lwjgl.opengl.GL11C.GL_ONE,
                org.lwjgl.opengl.GL11C.GL_ONE, org.lwjgl.opengl.GL11C.GL_ZERO);

        // Every quad used to push, translate, rotate by the camera (optionally roll) and pop. The camera
        // rotation is the same for all quads; JOML's rotate reads only the 3x3 block, and translate
        // changes neither that block nor the property path rotate takes, so the rotated block is built
        // once and each quad only copies in the translation column that the same
        // translate((float) dx, (float) dy, (float) dz) produces. Positions are bit-identical. Only the
        // position matrix is consumed here (no normals), so skipping the normal-matrix update is moot.
        Matrix4f base = matrices.peek().getPositionMatrix();
        Matrix4f billboard = billboardMatrix.set(base).translate(0.0f, 0.0f, 0.0f).rotate(camera.getRotation());
        Matrix4f translated = translatedMatrix;

        for (int b = 0; b < batchOrder.size(); b++) {
            QuadBatch batch = batchOrder.get(b);
            RenderSystem.setShaderTexture(0, batch.texture);
            BufferBuilder buf = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            double[] pos = batch.pos;
            float[] dims = batch.dims;
            int[] colors = batch.colors;
            for (int q = 0; q < batch.size; q++) {
                int p = q * 3;
                translated.set(base).translate((float) (pos[p] - camPos.x), (float) (pos[p + 1] - camPos.y),
                        (float) (pos[p + 2] - camPos.z));
                billboard.m30(translated.m30()).m31(translated.m31()).m32(translated.m32());
                Matrix4f matrix = billboard;
                float rotation = dims[p + 2];
                if (rotation != 0.0f) {
                    // RotationAxis.POSITIVE_Z.rotationDegrees(r) == new Quaternionf().rotationZ(r * 0.017453292f).
                    matrix = rotatedMatrix.set(billboard).rotate(rollQuat.rotationZ(rotation * 0.017453292f));
                }
                int color = colors[q];
                float hw = dims[p] / 2.0f;
                float hh = dims[p + 1] / 2.0f;
                buf.vertex(matrix, -hw, -hh, 0.0f).texture(0.0f, 0.0f).color(color);
                buf.vertex(matrix, hw, -hh, 0.0f).texture(1.0f, 0.0f).color(color);
                buf.vertex(matrix, hw, hh, 0.0f).texture(1.0f, 1.0f).color(color);
                buf.vertex(matrix, -hw, hh, 0.0f).texture(0.0f, 1.0f).color(color);
            }

            var built = buf.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        }
    }

    private void trimCubics() {
        int overflow = dashCubics.size() - MAX_CUBICS;
        if (overflow > 0) dashCubics.subList(0, overflow).clear();
    }

    private int randomLifetime() {
        return Math.round((550 + random.nextInt(300)) * dashLength);
    }


    private static int multAlpha(int color, float factor) {
        int alpha = (int) MathHelper.clamp(((color >>> 24) & 0xFF) * factor, 0f, 255f);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static int lerpColor(int from, int to, float t) {
        float clamped = MathHelper.clamp(t, 0f, 1f);
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * clamped);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * clamped);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * clamped);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float dashAlpha(float progress) {
        float clamped = MathHelper.clamp(progress, 0.0f, 1.0f);
        if (clamped < 0.08f) return clamped / 0.08f;
        if (clamped > 0.72f) return 1.0f - (clamped - 0.72f) / 0.28f;
        return 1.0f;
    }

    private static float easeOutQuad(float value) {
        float clamped = MathHelper.clamp(value, 0.0f, 1.0f);
        return 1.0f - (1.0f - clamped) * (1.0f - clamped);
    }

    private static float easeInOutWave(float value) {
        float clamped = MathHelper.clamp(value, 0.0f, 1.0f);
        return clamped <= 0.5f
                ? 2.0f * clamped * clamped
                : 1.0f - (float) Math.pow(-2.0f * clamped + 2.0f, 2.0f) / 2.0f;
    }

    private static Identifier texture(String path) {
        return Identifier.of("bkclient", ROOT + path);
    }

    private static TextureInfo cubic(int number, int width, int height) {
        return new TextureInfo(texture("dashcubics/dashcubic" + number + ".png"), width, height);
    }

    private static TextureInfo[] group(int group, int count, int width, int height) {
        TextureInfo[] textures = new TextureInfo[count];
        for (int i = 0; i < count; i++) {
            textures[i] = new TextureInfo(
                    texture("dashcubics/group_dashs/group" + group + "/dashcubic" + (i + 1) + ".png"),
                    width, height);
        }
        return textures;
    }

    private record TextureInfo(Identifier id, int width, int height) {
    }

    /** Growable struct-of-arrays quads of one texture: pos x/y/z, dims width/height/rotation, color. */
    private static final class QuadBatch {
        private final Identifier texture;
        private int size;
        private double[] pos = new double[3 * 64];
        private float[] dims = new float[3 * 64];
        private int[] colors = new int[64];

        private QuadBatch(Identifier texture) {
            this.texture = texture;
        }

        private void add(double x, double y, double z, float width, float height, float rotation, int color) {
            if (size == colors.length) {
                int cap = size * 2;
                pos = Arrays.copyOf(pos, cap * 3);
                dims = Arrays.copyOf(dims, cap * 3);
                colors = Arrays.copyOf(colors, cap);
            }
            int p = size * 3;
            pos[p] = x;
            pos[p + 1] = y;
            pos[p + 2] = z;
            dims[p] = width;
            dims[p + 1] = height;
            dims[p + 2] = rotation;
            colors[size] = color;
            size++;
        }
    }

    private static final class DashTexture {
        private final TextureInfo[] textures;
        private final long startTime;
        private final long animationTime;

        private DashTexture(TextureInfo[] textures, long startTime, long animationTime) {
            this.textures = textures;
            this.startTime = startTime;
            this.animationTime = Math.max(1L, animationTime);
        }

        private static DashTexture random(Random random, long animationTime) {
            if (random.nextInt(100) > 40) {
                return new DashTexture(ANIMATED_TEXTURES[random.nextInt(ANIMATED_TEXTURES.length)],
                        System.currentTimeMillis(), animationTime);
            }
            return new DashTexture(new TextureInfo[]{STATIC_TEXTURES[random.nextInt(STATIC_TEXTURES.length)]},
                    System.currentTimeMillis(), animationTime);
        }

        private TextureInfo get(long now) {
            if (textures.length <= 1) return textures[0];
            long elapsed = Math.floorMod(now - startTime, animationTime);
            int index = MathHelper.clamp((int) ((elapsed / (float) animationTime) * textures.length),
                    0, textures.length - 1);
            return textures[index];
        }
    }

    private static final class DashCubic {
        private final int entityId;
        private final DashTexture texture;
        private final long startTime = System.currentTimeMillis();
        private final int lifetime;
        private final int color;
        private final float scale;
        private final boolean addSparks;
        private final List<DashSpark> sparks = new ArrayList<>();
        private Vec3d pos;
        private Vec3d prevPos;
        private Vec3d motion;
        private float rotation;

        private DashCubic(int entityId, Vec3d pos, Vec3d motion, DashTexture texture,
                          int lifetime, int color, float scale, boolean addSparks, Random random) {
            this.entityId = entityId;
            this.pos = pos;
            this.prevPos = pos;
            this.motion = motion;
            this.texture = texture;
            this.lifetime = Math.max(1, lifetime);
            this.color = color;
            this.scale = scale;
            this.addSparks = addSparks;
            updateRotation(random);
        }

        private void tick(DashCubic next, Random random, boolean addSegments, boolean addDots, long now) {
            prevPos = pos;
            Vec3d sourceMotion = next != null ? next.motion : motion;
            motion = sourceMotion.multiply(1.0D / 1.05D);
            double yDivider = motion.y < 0.0D ? 1.0D : 3.5D;
            pos = pos.add(motion.x * 5.0D, motion.y * 5.0D / yDivider, motion.z * 5.0D);
            updateRotation(random);

            if (addSparks && (addSegments || addDots) && progress(now) < 0.3f && random.nextInt(12) > 5) {
                int amount = addSegments ? 1 : 2;
                for (int i = 0; i < amount; i++) {
                    sparks.add(new DashSpark(random));
                }
            }
            if (addSparks && (addSegments || addDots)) {
                for (DashSpark spark : sparks) spark.tick();
                sparks.removeIf(DashSpark::isDead);
            } else {
                sparks.clear();
            }
        }

        private Vec3d renderPos(float tickDelta) {
            return prevPos.lerp(pos, MathHelper.clamp(tickDelta, 0.0f, 1.0f));
        }

        private float progress(long now) {
            return MathHelper.clamp((now - startTime) / (float) lifetime, 0.0f, 1.0f);
        }

        private boolean isDead(long now) {
            if (progress(now) < 1.0f) return false;
            for (DashSpark spark : sparks) {
                if (!spark.isDead()) return false;
            }
            return true;
        }

        private void updateRotation(Random random) {
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            if (horizontal < 5.0E-4D) {
                rotation = random.nextFloat() * 360.0f;
                return;
            }
            float motionYaw = (float) Math.toDegrees(Math.atan2(motion.z, motion.x) - Math.PI / 2.0D);
            if (motionYaw < 0.0f) motionYaw += 360.0f;
            rotation = motionYaw - 60.0f;
        }
    }

    private static final class DashSpark {
        private final double speed;
        private final double yaw;
        private final double pitch;
        private final long startTime = System.currentTimeMillis();
        private Vec3d pos = Vec3d.ZERO;
        private Vec3d prevPos = Vec3d.ZERO;

        private DashSpark(Random random) {
            speed = random.nextDouble() / 50.0D;
            yaw = Math.toRadians(random.nextDouble() * 360.0D);
            pitch = Math.toRadians(-180.0D + random.nextDouble() * 180.0D);
        }

        private void tick() {
            prevPos = pos;
            pos = pos.add(Math.sin(yaw) * speed, Math.cos(pitch) * speed, Math.cos(yaw) * speed);
        }

        private Vec3d renderPos(float tickDelta) {
            return prevPos.lerp(pos, MathHelper.clamp(tickDelta, 0.0f, 1.0f));
        }

        private double alpha(long now) {
            return 1.0D - MathHelper.clamp((now - startTime) / 1000.0D, 0.0D, 1.0D);
        }

        private boolean isDead() {
            return alpha(System.currentTimeMillis()) <= 0.0D;
        }
    }
}
