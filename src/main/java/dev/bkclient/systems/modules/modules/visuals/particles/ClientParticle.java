package dev.bkclient.systems.modules.modules.visuals.particles;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Client-managed hit particle with millisecond lifetime and ParticleCollision physics.
 */
public final class ClientParticle {

    /** Fraction of the life spent fading in; the rest fades out. */
    private static final float FADE_IN = 0.15F;

    public final Identifier texture;
    public final float rotationDeg;

    /** Where this spark sits in its burst, 0 to 1, so a burst can be coloured as a gradient. */
    public final float gradient;

    public double posX;
    public double posY;
    public double posZ;
    public double motionX;
    public double motionY;
    public double motionZ;

    /** Non-zero for the twist preset: which way round the target this spark spirals. */
    public float twist;

    /** Set once the spark has settled against a block, so it stops being pushed around. */
    public boolean holdCollide;
    public boolean dead;

    private double prevX;
    private double prevY;
    private double prevZ;

    private final long bornMillis = System.currentTimeMillis();
    private final float lifetimeMs;

    public ClientParticle(Vec3d position, Vec3d motion, float rotationDeg, float lifetimeMs, Identifier texture, float gradient) {
        this.posX = this.prevX = position.x;
        this.posY = this.prevY = position.y;
        this.posZ = this.prevZ = position.z;
        this.motionX = motion.x;
        this.motionY = motion.y;
        this.motionZ = motion.z;
        this.rotationDeg = rotationDeg;
        this.lifetimeMs = Math.max(1.0F, lifetimeMs);
        this.texture = texture;
        this.gradient = gradient;
    }

    /** Snapshots the current position so the frames between ticks have something to interpolate. */
    public void beginStep() {
        this.prevX = this.posX;
        this.prevY = this.posY;
        this.prevZ = this.posZ;
    }

    public Vec3d renderPos(float tickDelta) {
        float t = MathHelper.clamp(tickDelta, 0.0F, 1.0F);
        return new Vec3d(
                MathHelper.lerp(t, this.prevX, this.posX),
                MathHelper.lerp(t, this.prevY, this.posY),
                MathHelper.lerp(t, this.prevZ, this.posZ)
        );
    }

    public float alpha01(long now) {
        float age = now - this.bornMillis;
        if (age <= 0.0F || age >= this.lifetimeMs) {
            return 0.0F;
        }
        float progress = age / this.lifetimeMs;
        return progress < FADE_IN
                ? progress / FADE_IN
                : 1.0F - (progress - FADE_IN) / (1.0F - FADE_IN);
    }

    public boolean isDead(long now) {
        return this.dead || now - this.bornMillis >= this.lifetimeMs;
    }
}
