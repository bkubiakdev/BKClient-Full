package dev.bkclient.systems.modules.modules.visuals.frageffect;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.Random;

/** A glowing silhouette that either disperses or falls and bounces. */
public final class BodyDissolveParticles implements IMinecraft {
    private static final int MAX_PARTICLES = 2048;
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private long lastFrame;

    public void clear() { particles.clear(); lastFrame = 0; }

    public void spawn(LivingEntity entity, ColorRGBA color, boolean gravity) {
        long now = System.nanoTime();
        if (particles.isEmpty()) lastFrame = now;
        particles.removeIf(p -> now - p.born >= p.lifetime);
        // Keep complete silhouettes even during rapid multi-kills.
        int excess = particles.size() + 231 - MAX_PARTICLES;
        if (excess > 0) particles.subList(0, excess).clear();
        Vec3d origin = entity.getPos();
        float height = entity.getHeight(), width = entity.getWidth();
        double yaw = Math.toRadians(-entity.bodyYaw + 90);
        for (int i = 0; i < 25; i++) {
            double azimuth = random.nextDouble() * Math.PI * 2;
            double polar = Math.acos(2 * random.nextDouble() - 1);
            double radius = width * 0.4 * Math.cbrt(random.nextDouble());
            add(origin.add(radius * Math.sin(polar) * Math.cos(azimuth),
                    height - 0.2 + radius * Math.sin(polar) * Math.sin(azimuth), radius * Math.cos(polar)), color, gravity, now);
        }
        for (int i = 0; i < 62; i++) {
            double x = centered(width * 0.8), z = centered(width * 0.4);
            add(origin.add(x * Math.cos(yaw) - z * Math.sin(yaw),
                    height * (0.4 + random.nextDouble() * 0.45), x * Math.sin(yaw) + z * Math.cos(yaw)), color, gravity, now);
        }
        for (int side : new int[]{-1, 1}) {
            Vec3d shoulder = origin.add(Math.sin(yaw) * width * 0.5 * side, height * 0.75, Math.cos(yaw) * width * 0.5 * side);
            limb(shoulder, height * 0.4, width * 0.15, 31, color, gravity, now);
            Vec3d hip = origin.add(Math.sin(yaw) * width * 0.15 * side, height * 0.4, Math.cos(yaw) * width * 0.15 * side);
            limb(hip, height * 0.45, width * 0.15, 41, color, gravity, now);
        }
    }

    private double centered(double range) { return (random.nextDouble() - 0.5) * range; }

    private void limb(Vec3d origin, double length, double radius, int count, ColorRGBA color, boolean gravity, long now) {
        for (int i = 0; i < count; i++) {
            add(origin.add(centered(radius * 2), -random.nextDouble() * length, centered(radius * 2)), color, gravity, now);
        }
    }

    private void add(Vec3d position, ColorRGBA color, boolean gravity, long now) {
        double spread = gravity ? 0.04 : 0.03;
        particles.add(new Particle(position, centered(spread), gravity ? 0.025 + random.nextDouble() * 0.035 : centered(0.03),
                centered(spread), 0.005 + random.nextDouble() * 0.005, gravity, color, now,
                (1500L + random.nextInt(1500)) * 1_000_000L));
    }

    public void render(Render3DEvent event) {
        if (mc.world == null || mc.player == null) { clear(); return; }
        if (particles.isEmpty()) return;
        long now = System.nanoTime();
        double delta = Math.min(5, Math.max(0, (now - lastFrame) / 16_666_667.0));
        lastFrame = now;
        particles.removeIf(p -> now - p.born >= p.lifetime);
        if (particles.isEmpty()) return;
        int steps = Math.max(1, (int)Math.ceil(delta));
        BLOCKED_CACHE.clear();
        for (Particle p : particles) for (int step = 0; step < steps; step++) p.advance(delta / steps);

        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, BKClientInit.id("textures/bloom.png"));
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            var buffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            var matrices = event.getMatrices();
            Vec3d camera = event.getCamera().getPos();
            // Each particle used to push, translate, rotate by the camera and pop. The camera rotation is
            // shared: JOML's rotate reads only the 3x3 block and translate changes neither it nor the property
            // path rotate takes, so the rotated block is built once and each particle copies in the
            // translation column the same translate((float) dx, ...) produces. Bit-identical positions.
            Matrix4f base = matrices.peek().getPositionMatrix();
            Matrix4f billboard = billboardScratch.set(base).translate(0.0F, 0.0F, 0.0F)
                    .rotate(event.getCamera().getRotation());
            Matrix4f translated = translatedScratch;
            for (Particle p : particles) {
                float fade = 1 - (float)(now - p.born) / p.lifetime;
                translated.set(base).translate((float) (p.x - camera.x), (float) (p.y - camera.y), (float) (p.z - camera.z));
                billboard.m30(translated.m30()).m31(translated.m31()).m32(translated.m32());
                drawImage(billboard, buffer, -0.05, -0.05, 0, 0.1, 0.1, p.color.mulAlpha(0.9F * fade).getRGB());
                drawImage(billboard, buffer, -0.25, -0.25, 0, 0.5, 0.5, p.color.mulAlpha(0.15F * fade).getRGB());
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            RenderSystem.depthMask(mask);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            RenderSystem.setShaderTexture(0, 0);
        }
    }

    private final Matrix4f billboardScratch = new Matrix4f();
    private final Matrix4f translatedScratch = new Matrix4f();

    /** DrawUtility.drawImage(MatrixStack, BufferBuilder, ...) with the matrix passed directly; same casts. */
    private static void drawImage(Matrix4f matrix, BufferBuilder builder, double x, double y, double z,
                                  double width, double height, int rgb) {
        builder.vertex(matrix, (float)x, (float)(y + height), (float)z).texture(0.0F, 1.0F).color(rgb);
        builder.vertex(matrix, (float)(x + width), (float)(y + height), (float)z).texture(1.0F, 1.0F).color(rgb);
        builder.vertex(matrix, (float)(x + width), (float)y, (float)z).texture(1.0F, 0.0F).color(rgb);
        builder.vertex(matrix, (float)x, (float)y, (float)z).texture(0.0F, 0.0F).color(rgb);
    }

    // Per-frame collision cache: render() runs on the render thread, which is also the only thread that
    // mutates the client world, so a block's collision shape cannot change while one render() call runs.
    // Cleared at the start of every render(); key = BlockPos.asLong of the floored position.
    private static final Long2ByteOpenHashMap BLOCKED_CACHE = new Long2ByteOpenHashMap();
    private static final BlockPos.Mutable BLOCKED_PROBE = new BlockPos.Mutable();
    static { BLOCKED_CACHE.defaultReturnValue((byte) -1); }

    private static boolean blocked(double x, double y, double z) {
        BlockPos.Mutable pos = BLOCKED_PROBE.set(x, y, z); // floors exactly like BlockPos.ofFloored
        long key = pos.asLong();
        byte cached = BLOCKED_CACHE.get(key);
        if (cached >= 0) return cached != 0;
        boolean result = !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
        BLOCKED_CACHE.put(key, result ? (byte) 1 : (byte) 0);
        return result;
    }

    private static final class Particle {
        double x, y, z, vx, vy, vz;
        final double acceleration;
        final boolean gravity;
        final ColorRGBA color;
        final long born, lifetime;

        Particle(Vec3d pos, double vx, double vy, double vz, double acceleration, boolean gravity, ColorRGBA color, long born, long lifetime) {
            x = pos.x; y = pos.y; z = pos.z;
            this.vx = vx; this.vy = vy; this.vz = vz;
            this.acceleration = acceleration; this.gravity = gravity;
            this.color = color; this.born = born; this.lifetime = lifetime;
        }

        void advance(double delta) {
            if (gravity) vy -= acceleration * delta;
            double damping = Math.pow(gravity ? 0.999 : 0.995, delta);
            vx *= damping; vy *= damping; vz *= damping;
            double nx = x + vx * delta, ny = y + vy * delta, nz = z + vz * delta;
            if (gravity) {
                if (blocked(x, ny - 0.05, z)) { vy *= -0.5; ny = y; }
                if (blocked(nx, y, z)) { vx *= -0.5; nx = x; }
                if (blocked(x, y, nz)) { vz *= -0.5; nz = z; }
            }
            x = nx; y = ny; z = nz;
        }
    }
}
