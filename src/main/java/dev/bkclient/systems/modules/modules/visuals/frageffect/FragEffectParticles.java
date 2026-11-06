package dev.bkclient.systems.modules.modules.visuals.frageffect;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class FragEffectParticles {
    private static final Identifier GLOW_TEXTURE = BKClientInit.id("textures/features/dashtrail/dashbloom.png");
    private final List<Particle> particles = new ArrayList<>();

    public void spawnBurst(Vec3d center, int lifetimeMin, int lifetimeMax, float maxFlight, int count, float gravity) {
        if (center == null) return;
        int safeLifetimeMin = Math.max(1, lifetimeMin);
        float lifetimeTicks = safeLifetimeMin / 50.0f;
        float maxXzMotion = maxFlight / lifetimeTicks * 1.12f;
        float maxYMotion = maxFlight / lifetimeTicks * 0.31f;
        for (int i = 0; i < count; i++) {
            int lifetime = Math.max(1, randomInt(lifetimeMin, lifetimeMax));
            particles.add(new Particle(center, lifetime, maxXzMotion, maxYMotion, gravity));
        }
    }

    public void tick() {
        particles.removeIf(Particle::update);
    }

    public boolean isEmpty() {
        return particles.isEmpty();
    }

    public void clear() {
        particles.clear();
    }

    public void render(MatrixStack matrices, float tickDelta, int tintColor) {
        if (particles.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null) return;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(org.lwjgl.opengl.GL11C.GL_SRC_ALPHA, org.lwjgl.opengl.GL11C.GL_ONE,
                org.lwjgl.opengl.GL11C.GL_ONE, org.lwjgl.opengl.GL11C.GL_ZERO);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (Particle particle : particles) {
            float alpha = particle.alpha();
            if (alpha <= 0.01f) continue;
            double x = particle.renderX(tickDelta);
            double y = particle.renderY(tickDelta);
            double z = particle.renderZ(tickDelta);
            emit(buf, matrices, camera, camPos, x, y, z, particle.size() * 3.0f,
                    particle.color(tintColor, alpha * 0.38f));
            emit(buf, matrices, camera, camPos, x, y, z, particle.size() * 0.66f,
                    particle.color(tintColor, alpha * 0.90f));
        }
        var built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void emit(BufferBuilder buf, MatrixStack matrices, Camera camera, Vec3d camPos,
                      double x, double y, double z, float size, int color) {
        if (size <= 0.0001f || ((color >>> 24) & 0xFF) <= 0) return;
        matrices.push();
        matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        matrices.multiply(camera.getRotation());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float h = size / 2.0f;
        buf.vertex(matrix, -h, -h, 0.0f).texture(0.0f, 0.0f).color(color);
        buf.vertex(matrix, h, -h, 0.0f).texture(1.0f, 0.0f).color(color);
        buf.vertex(matrix, h, h, 0.0f).texture(1.0f, 1.0f).color(color);
        buf.vertex(matrix, -h, h, 0.0f).texture(0.0f, 1.0f).color(color);
        matrices.pop();
    }

    private static int randomInt(int min, int max) {
        return max <= min ? min : ThreadLocalRandom.current().nextInt(min, max);
    }

    private static final class Particle {
        private double prevX, prevY, prevZ;
        private double posX, posY, posZ;
        private double motionX, motionY, motionZ;
        private final int maxTime;
        private final float gravity;
        private final float baseLuma;
        private final float baseSize;
        private final long createdMillis = System.currentTimeMillis();

        private Particle(Vec3d spawn, int maxTime, float motionXzMax, float motionYMax, float gravity) {
            posX = prevX = spawn.x;
            posY = prevY = spawn.y;
            posZ = prevZ = spawn.z;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            motionX = random.nextDouble(-motionXzMax, motionXzMax);
            motionY = random.nextDouble(-motionYMax / 4.0f, motionYMax);
            motionZ = random.nextDouble(-motionXzMax, motionXzMax);
            this.maxTime = maxTime;
            this.gravity = gravity;
            this.baseLuma = FragEffectEasing.expoInOut(random.nextFloat());
            this.baseSize = (float) random.nextDouble(0.035D, 0.08D);
        }

        private boolean update() {
            prevX = posX;
            prevY = posY;
            prevZ = posZ;
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            motionX *= 0.99D;
            motionY = (motionY - gravity) * 0.994D;
            motionZ *= 0.99D;
            return System.currentTimeMillis() - createdMillis >= maxTime;
        }

        private double renderX(float t) { return MathHelper.lerp(clampDelta(t), prevX, posX); }
        private double renderY(float t) { return MathHelper.lerp(clampDelta(t), prevY, posY); }
        private double renderZ(float t) { return MathHelper.lerp(clampDelta(t), prevZ, posZ); }
        private static float clampDelta(float t) { return MathHelper.clamp(t, 0.0f, 1.0f); }
        private float alpha() { return 1.0f - Math.min((float) (System.currentTimeMillis() - createdMillis) / maxTime, 1.0f); }
        private float size() { return baseSize; }

        private int color(int tint, float alpha) {
            int r = (int) (((tint >> 16) & 0xFF) * (0.35f + baseLuma * 0.65f));
            int g = (int) (((tint >> 8) & 0xFF) * (0.35f + baseLuma * 0.65f));
            int b = (int) ((tint & 0xFF) * (0.35f + baseLuma * 0.65f));
            int a = (int) MathHelper.clamp(255.0f * alpha, 0f, 255f);
            return (a << 24) | (Math.min(r, 255) << 16) | (Math.min(g, 255) << 8) | Math.min(b, 255);
        }
    }
}
