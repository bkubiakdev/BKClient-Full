package dev.bkclient.systems.modules.modules.visuals.particles;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * What a spark does when it runs into the world.
 *
 * <p>Each axis is tested separately against the block the spark is about to enter, which is what
 * lets a bounce off a wall keep its vertical travel and a slide along the floor keep its heading.
 * Testing the three together would only ever give one answer for the whole move.
 */
public final class ParticleCollision {

    /** Sparks are clamped to this per axis so a bad bounce cannot fling one across the map. */
    private static final double SPEED_LIMIT = 5.0;

    /** Below this the spark is treated as settled and stops being integrated at all. */
    private static final double REST_SPEED = 0.008;

    private ParticleCollision() {
    }

    /**
     * Declaration order matters: the module's mode setting is matched to these by index, so the
     * setting values must be declared in the same order.
     */
    public enum Mode {
        BOUNCE(1.0),
        STRONG_BOUNCE(1.5),
        VERY_STRONG_BOUNCE(2.0),
        WEAK_BOUNCE(0.75),
        VERY_WEAK_BOUNCE(0.5),
        IGNORE(0.0),
        STICK(0.0),
        VANISH(0.0),
        SLIDE(0.0),
        SLOW_SLIDE(0.0);

        private final double restitution;

        Mode(double restitution) {
            this.restitution = restitution;
        }

        boolean isBounce() {
            return this.ordinal() <= VERY_WEAK_BOUNCE.ordinal();
        }
    }

    /**
     * Resolves one step of collision.
     *
     * @return true when the caller must not apply the motion itself, because the spark is pinned
     */
    public static boolean apply(MinecraftClient mc, ClientParticle particle, Mode mode, float gravity) {
        boolean pinned = false;

        if (mode == Mode.VANISH || mode == Mode.STICK) {
            if (particle.holdCollide || collidesX(mc, particle) || collidesY(mc, particle) || collidesZ(mc, particle)) {
                particle.motionX = 0.0;
                particle.motionY = 0.0;
                particle.motionZ = 0.0;
                particle.holdCollide = true;
                if (mode == Mode.VANISH) {
                    particle.dead = true;
                }
                pinned = true;
            }
        } else if (mode != Mode.IGNORE) {
            boolean bounce = mode.isBounce();
            double restitution = mode.restitution;
            double friction = !bounce && mode == Mode.SLOW_SLIDE ? 0.875 : 1.0;
            boolean touched = false;

            if (particle.holdCollide || collidesY(mc, particle)) {
                if (particle.motionY != 0.0) {
                    // Softened by gravity and by how hard the spark was falling, so a spark that
                    // only just reached the floor settles instead of jittering there forever.
                    particle.motionY = bounce
                            ? -particle.motionY * restitution / (Math.max(gravity, 0.0F) + 1.0)
                                    * Math.min(Math.abs(particle.motionY) / 0.2, 1.0)
                            : 0.0;
                    if (bounce && particle.motionY > 0.0 && particle.motionY < 0.02) {
                        particle.motionY = 0.0;
                    }
                }
                if (!bounce) {
                    particle.motionX *= friction;
                    particle.motionZ *= friction;
                }
                touched = true;
            }

            if (particle.holdCollide || collidesX(mc, particle)) {
                particle.motionX = bounce ? -particle.motionX * restitution : 0.0;
                if (!bounce) {
                    particle.motionZ *= friction;
                }
                touched = true;
            }

            if (particle.holdCollide || collidesZ(mc, particle)) {
                particle.motionZ = bounce ? -particle.motionZ * restitution : 0.0;
                if (!bounce) {
                    particle.motionX *= friction;
                }
                touched = true;
            }

            if (touched) {
                double speed = Math.sqrt(particle.motionX * particle.motionX
                        + particle.motionY * particle.motionY
                        + particle.motionZ * particle.motionZ);
                particle.holdCollide = speed < REST_SPEED;
            }
        }

        particle.motionX = MathHelper.clamp(particle.motionX, -SPEED_LIMIT, SPEED_LIMIT);
        particle.motionY = MathHelper.clamp(particle.motionY, -SPEED_LIMIT, SPEED_LIMIT);
        particle.motionZ = MathHelper.clamp(particle.motionZ, -SPEED_LIMIT, SPEED_LIMIT);
        return pinned;
    }

    private static boolean collidesX(MinecraftClient mc, ClientParticle particle) {
        return isSolid(mc, particle.posX + particle.motionX, particle.posY, particle.posZ);
    }

    private static boolean collidesY(MinecraftClient mc, ClientParticle particle) {
        return isSolid(mc, particle.posX, particle.posY + particle.motionY, particle.posZ);
    }

    private static boolean collidesZ(MinecraftClient mc, ClientParticle particle) {
        return isSolid(mc, particle.posX, particle.posY, particle.posZ + particle.motionZ);
    }

    /**
     * Whether a point is inside something a spark should not pass through.
     *
     * <p>The collision shape is what matters rather than the block being present: sparks fall
     * through grass, torches and open doors, and only stop on the parts you could stand on.
     */
    public static boolean isSolid(MinecraftClient mc, double x, double y, double z) {
        if (mc.world == null) {
            return false;
        }
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        return !state.getCollisionShape(mc.world, pos).isEmpty();
    }
}
