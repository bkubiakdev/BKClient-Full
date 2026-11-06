package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.systems.event.impl.game.AttackEvent;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.render.bkengine.Animation;
import dev.bkclient.utility.render.bkengine.Easings;
import dev.bkclient.utility.render.bkengine.ParticlePortMath;
import dev.bkclient.utility.render.bkengine.ParticlePortColor;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@dev.bkclient.systems.modules.api.ModuleInfo(name="Particles",category=dev.bkclient.systems.modules.api.ModuleCategory.VISUALS)
public class Particles extends dev.bkclient.systems.modules.impl.BaseModule {



    public BooleanSetting onAttack = new BooleanSetting(this,"On Attack").enabled(true);
    public SliderSetting onAtSizs = new SliderSetting(this,"Attack Count").min(5F).max(50F).step(1F).currentValue(20F);
    public BooleanSetting totem = new BooleanSetting(this,"On Totem").enabled(true);
    public SliderSetting onTtSizs = new SliderSetting(this,"Totem Count").min(20F).max(150F).step(1F).currentValue(75F);
    public BooleanSetting perka = new BooleanSetting(this,"Projectile Trails").enabled(true);
    public SliderSetting onPerkSizs = new SliderSetting(this,"Trail Count").min(1F).max(8F).step(1F).currentValue(3F);
    public BooleanSetting worlds = new BooleanSetting(this,"World Particles").enabled(true);
    public SliderSetting onworldsSizs = new SliderSetting(this,"World Count").min(2F).max(60F).step(1F).currentValue(10F);
    public BooleanSetting move = new BooleanSetting(this,"On Movement").enabled(true);
    private final ModeSetting particleMode = new ModeSetting(this,"Particle Type");
    { for (String name : new String[]{"Client","Circle","Bloom","Star","Heart","Snow","Dollar","Pumpkin"}) {
        var value = new ModeSetting.Value(particleMode,name); if (name.equals("Client")) value.select();
    } }
    private final SliderSetting size = new SliderSetting(this,"Size").min(0.0F).max(1F).step(0.1F).currentValue(0.5F);
    public BooleanSetting glows = new BooleanSetting(this,"Glow").enabled(true);
    private final SliderSetting glowsiz = new SliderSetting(this,"Glow Strength").min(0.0F).max(1F).step(0.1F).currentValue(0.5F);
    public BooleanSetting rotations = new BooleanSetting(this,"Rotation").enabled(true);




    private final dev.bkclient.systems.event.EventListener<AttackEvent> attack = this::onEvent;
    private final dev.bkclient.systems.event.EventListener<ReceivePacketEvent> packet = e -> {
        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 35) {
            this.pendingTotems.offer(new PendingTotem(status, this.particleGeneration));
        }
    };
    private final dev.bkclient.systems.event.EventListener<ClientPlayerTickEvent> tickEvent = e -> {
        if (mc.player != null && mc.world != null) {
            PendingTotem pending;
            while ((pending = this.pendingTotems.poll()) != null) {
                if (pending.generation() == this.particleGeneration) handleTotem(pending.packet());
            }
            spawnTrails(e); spawnAmbient(e);
        }
    };
    private final dev.bkclient.systems.event.EventListener<WorldChangeEvent> worldEvent = e -> onWorldChanged();
    private final dev.bkclient.systems.event.EventListener<Render3DEvent> renderEvent = e -> {
        if (mc.player != null && mc.world != null) onEvent(e);
    };

    public final List<Particle> targetParticles = new ArrayList<>();
    public final List<Particle> totemParticles = new ArrayList<>();
    public final List<Particle> frameParticles = new ArrayList<>();
    public final List<Particle> worldParticles = new ArrayList<>();

    private long totemSpawnStartTime = 0;
    private Entity totemTargetEntity = null;
    private int totemParticlesLeftToSpawn = 0;
    private int totalTotemParticlesToSpawn = 0;

    @Override public void onEnable() { clear(); lastUpdateTime = System.nanoTime(); }
    @Override public void onDisable() { onWorldChanged(); }

    private record PendingTotem(EntityStatusS2CPacket packet, long generation) { }
    private final java.util.concurrent.ArrayBlockingQueue<PendingTotem> pendingTotems = new java.util.concurrent.ArrayBlockingQueue<>(64);
    private volatile long particleGeneration;

    public void onWorldChanged() {
        ++particleGeneration;
        pendingTotems.clear();
        clear();
        if (boxAllocator != null) boxAllocator.close();
        boxAllocator = null;
        particleBuffers = null;
        lastUpdateTime = System.nanoTime();
    }

    public void onEvent(WorldChangeEvent event) {
        clear();
    }

    private void spawnParticle(List<Particle> particles, Vec3d position, Vec3d velocity, boolean isWorld) {
        float particleSize = 0.05F + (this.size.getCurrentValue() * 0.2F);
        int color = ParticlePortColor.fade(particles.size() * 100);

        ParticleType type = switch (this.particleMode.getValue().getName()) {
            case "Heart" -> ParticleType.HEART;
            case "Star" -> ParticleType.STAR;
            case "Snow" -> ParticleType.SNOOW;
            case "Bloom" -> ParticleType.BLOOM;
            case "Circle" -> ParticleType.CIRCLE;
            case "Client" -> ParticleType.STAR_NEW;
            case "Dollar" -> ParticleType.DOLLAR;
            case "Pumpkin" -> ParticleType.PUMPKIN;
            default ->  ParticleType.BLOOM;
        };

        if (particles.size() >= 4096) particles.remove(0);
        particles.add(new Particle(type,
                position.add(0, particleSize, 0),
                velocity,
                particles.size(),
                (int) ParticlePortMath.step(ParticlePortMath.randomValue(0, 360), 15),
                color,
                particleSize,
                1.1F,
                isWorld,
                false)
        );
    }

    public void spawnParticleTotem(List<Particle> particles, Vec3d position, Vec3d velocity) {
        float particleSize = 0.05F + (this.size.getCurrentValue() * 0.2F);

        ParticleType type = switch (this.particleMode.getValue().getName()) {
            case "Heart" -> ParticleType.HEART;
            case "Star" -> ParticleType.STAR;
            case "Snow" -> ParticleType.SNOOW;
            case "Circle" -> ParticleType.CIRCLE;
            case "Bloom" -> ParticleType.BLOOM;
            case "Client" -> ParticleType.STAR_NEW;
            case "Dollar" -> ParticleType.DOLLAR;
            case "Pumpkin" -> ParticleType.PUMPKIN;
            default ->  ParticleType.BLOOM;
        };

        int[] colors = new int[]{
                ParticlePortColor.getColor(221, 218, 127),
                ParticlePortColor.getColor(127, 221, 144),
                ParticlePortColor.getColor(255, 215, 0),
                ParticlePortColor.getColor(50, 205, 50)
        };
        int col = colors[ParticlePortMath.randomInt(0, colors.length - 1)];
        if (particles.size() >= 4096) particles.remove(0);
        particles.add(new Particle(type,
                position.add(0, particleSize, 0),
                velocity,
                particles.size(),
                (int) ParticlePortMath.step(ParticlePortMath.randomValue(0, 360), 15),
                col,
                particleSize,
                1.1F,
                false,
                true)
        );
    }

    public void onEvent(AttackEvent event) {
        if (onAttack.isEnabled()) spawnAttackBurst(event.getEntity());
    }

    public void spawnAttackBurst(Entity target) {
        if (target == null) return;

        float motion = 1.1F;
        for (int i = 0; i < onAtSizs.getCurrentValue(); i++) {
            spawnParticle(targetParticles,
                    new Vec3d(target.getX() + ParticlePortMath.randomValue(-0.035F, 0.035F),
                            target.getY() + ParticlePortMath.randomValue(0.1F, target.getHeight()),
                            target.getZ() + ParticlePortMath.randomValue(-0.035F, 0.035F)),
                    new Vec3d(ParticlePortMath.randomValue(-motion + 0.2F, motion - 0.2F),
                            ParticlePortMath.randomValue(-motion, motion / 2 ),
                            ParticlePortMath.randomValue(-motion + 0.2F, motion - 0.2F)),
                    false);
        }
    }

    public void spawnTotemBurst(Entity target, int count) {
        if (target == null) return;

        for (int i = 0; i < count; i++) {
            double angle = ParticlePortMath.randomValue(0, Math.PI * 2);
            double speedXZ = ParticlePortMath.randomValue(2.5, 4.0) * 0.1F;

            spawnParticleTotem(totemParticles,
                    new Vec3d(target.getX() + ParticlePortMath.randomValue(-0.2, 0.2),
                            target.getY() + ParticlePortMath.randomValue(0.1F, target.getHeight()),
                            target.getZ() + ParticlePortMath.randomValue(-0.2, 0.2)),
                    new Vec3d(Math.cos(angle) * speedXZ,
                            ParticlePortMath.randomValue(0.0, target.getHeight() / 4),
                            Math.sin(angle) * speedXZ));
        }
    }

    public void spawnThrowBurst(Entity target) {
        if (target == null) return;

        Vec3d pos = target.getPos().add(0, target.getHeight() * 0.6F, 0);
        for (int i = 0; i < onPerkSizs.getCurrentValue() * 8; i++) {
            spawnParticle(frameParticles,
                    pos.add(ParticlePortMath.randomValue(-0.1, 0.1),
                            ParticlePortMath.randomValue(-0.1, 0.1),
                            ParticlePortMath.randomValue(-0.1, 0.1)),
                    new Vec3d(ParticlePortMath.randomValue(-0.15, 0.15),
                            ParticlePortMath.randomValue(-0.15, 0.15),
                            ParticlePortMath.randomValue(-0.15, 0.15)),
                    true);
        }
    }

    public void onPacket(ReceivePacketEvent e) {
        if (!(e.getPacket() instanceof EntityStatusS2CPacket packet)) return;
        handleTotem(packet);
    }

    private void handleTotem(EntityStatusS2CPacket packet) {
        if (packet.getStatus() == 35) {
            Entity entity = packet.getEntity(mc.world);
            if (entity == null) return;

            if (totem.isEnabled()) {
                this.totemTargetEntity = entity;
                this.totalTotemParticlesToSpawn = (int) (int) onTtSizs.getCurrentValue();
                this.totemParticlesLeftToSpawn = this.totalTotemParticlesToSpawn;
                this.totemSpawnStartTime = System.currentTimeMillis();
            }
        }
    }

    public void spawnTrails(ClientPlayerTickEvent e) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        if (perka.isEnabled()) {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof EnderPearlEntity || entity instanceof ArrowEntity || entity instanceof TridentEntity)) {
                    continue;
                }
                if (entity instanceof TridentEntity trident && trident.isOnGround()) {
                    continue;
                }

                boolean isMoving = entity.lastRenderX != entity.getX() || entity.lastRenderY != entity.getY() || entity.lastRenderZ != entity.getZ();
                if (!isMoving) continue;

                Vec3d pos = entity.getPos();

                for (int i = 0; i < onPerkSizs.getCurrentValue(); i++) {
                    spawnParticle(
                            frameParticles,
                            new Vec3d(
                                    pos.x + MathHelper.nextDouble(Random.create(), -0.1, 0.1),
                                    pos.y + MathHelper.nextDouble(Random.create(), -0.1, 0.1),
                                    pos.z + MathHelper.nextDouble(Random.create(), -0.1, 0.1)
                            ),
                            new Vec3d(
                                    MathHelper.nextDouble(Random.create(), -0.15, 0.15),
                                    MathHelper.nextDouble(Random.create(), -0.15, 0.15 ),
                                    MathHelper.nextDouble(Random.create(), -0.15, 0.15)
                            ),
                            true
                    );
                }
            }
        }
    }

    public void spawnAmbient(ClientPlayerTickEvent e) {
        if(move.isEnabled() && hasPlayerMoved()) {
            spawnParticle(frameParticles,
                    new Vec3d(mc.player.getX() + ParticlePortMath.randomValue(-0.1, 0.1),
                            mc.player.getY() + ParticlePortMath.randomValue(0, mc.player.getHeight()),
                            mc.player.getZ() + ParticlePortMath.randomValue(-0.1, 0.1)),
                    new Vec3d(ParticlePortMath.randomValue(-0.3, 0.3),
                            ParticlePortMath.randomValue(-0.03, 0.3),
                            ParticlePortMath.randomValue(-0.3, 0.3)),
                    true);
        }

        if (worlds.isEnabled()) {
            if (mc.world == null || mc.player == null) return;

            int r = 24;
            for (int i = 0; i < onworldsSizs.getCurrentValue(); i++) {
                Vec3d additional = mc.player.getPos().add(
                        ParticlePortMath.randomValue(-r, r),
                        0,
                        ParticlePortMath.randomValue(-r, r)
                );

                BlockPos topPos = mc.world.getTopPosition(
                        Heightmap.Type.MOTION_BLOCKING,
                        BlockPos.ofFloored(additional)
                );

                double x = topPos.getX() + ParticlePortMath.randomValue(-3, 3);
                double z = topPos.getZ() + ParticlePortMath.randomValue(-3, 3);
                double y = mc.player.getY() + ParticlePortMath.randomValue(mc.player.getHeight(), r);

                Vec3d spawnPos = new Vec3d(x, y, z);

                while (!mc.world.isAir(BlockPos.ofFloored(spawnPos)) && spawnPos.y < mc.world.getTopYInclusive()) {
                    spawnPos = spawnPos.add(0, 1, 0);
                }

                spawnParticle(
                        worldParticles,
                        spawnPos,
                        new Vec3d(
                                mc.player.getVelocity().x + ParticlePortMath.randomValue(-0.6F, 0.6F),
                                ParticlePortMath.randomValue(-0.01, 0.44),
                                mc.player.getVelocity().z + ParticlePortMath.randomValue(-0.6F, 0.6F)
                        ),
                        true
                );
            }
        }


        if (totemParticlesLeftToSpawn > 0 && totemTargetEntity != null) {
            if (totemTargetEntity.isAlive() || mc.world.getEntityById(totemTargetEntity.getId()) != null) {

                long elapsed = System.currentTimeMillis() - totemSpawnStartTime;
                long totalDuration = 1500;

                if (elapsed < totalDuration) {
                    int expectedSpawnedSoFar = (int) ((double) elapsed / totalDuration * totalTotemParticlesToSpawn);
                    int currentSpawned = totalTotemParticlesToSpawn - totemParticlesLeftToSpawn;
                    int toSpawnThisTick = expectedSpawnedSoFar - currentSpawned;

                    if (toSpawnThisTick > totemParticlesLeftToSpawn) {
                        toSpawnThisTick = totemParticlesLeftToSpawn;
                    }

                    for (int i = 0; i < toSpawnThisTick; i++) {
                        double angle = ParticlePortMath.randomValue(0, Math.PI * 2);
                        double speedXZ = ParticlePortMath.randomValue(2.5, 4.0) * 0.1F;
                        double motionX = Math.cos(angle) * speedXZ;
                        double motionZ = Math.sin(angle) * speedXZ;
                        double motionY = ParticlePortMath.randomValue(0.0, totemTargetEntity.getHeight() / 4);

                        spawnParticleTotem(totemParticles,
                                new Vec3d(totemTargetEntity.getX() + ParticlePortMath.randomValue(-0.2, 0.2),
                                        totemTargetEntity.getY() + ParticlePortMath.randomValue(0.1F, totemTargetEntity.getHeight()),
                                        totemTargetEntity.getZ() + ParticlePortMath.randomValue(-0.2, 0.2)),
                                new Vec3d(motionX, motionY, motionZ));

                        totemParticlesLeftToSpawn--;
                    }
                } else {
                    for (int i = 0; i < totemParticlesLeftToSpawn; i++) {
                        double angle = ParticlePortMath.randomValue(0, Math.PI * 2);
                        double speedXZ = ParticlePortMath.randomValue(2.5, 4.0) * 0.1F;
                        double motionX = Math.cos(angle) * speedXZ;
                        double motionZ = Math.sin(angle) * speedXZ;
                        double motionY = ParticlePortMath.randomValue(0.0, totemTargetEntity.getHeight() / 4);

                        spawnParticleTotem(totemParticles,
                                new Vec3d(totemTargetEntity.getX() + ParticlePortMath.randomValue(-0.2, 0.2),
                                        totemTargetEntity.getY() + ParticlePortMath.randomValue(0.1F, totemTargetEntity.getHeight()),
                                        totemTargetEntity.getZ() + ParticlePortMath.randomValue(-0.2, 0.2)),
                                new Vec3d(motionX, motionY, motionZ));
                    }
                    totemParticlesLeftToSpawn = 0;
                    totemTargetEntity = null;
                }
            } else {
                totemParticlesLeftToSpawn = 0;
                totemTargetEntity = null;
            }
        }

        removeExpiredParticles(frameParticles, 3000);
        removeExpiredParticles(totemParticles, 3500);
        removeExpiredParticles(worldParticles, 3500);
        removeExpiredParticles(targetParticles, 6000);
    }

    private boolean hasPlayerMoved() {
        return mc.player.lastRenderX != mc.player.getX()
                || mc.player.lastRenderY != mc.player.getY()
                || mc.player.lastRenderZ != mc.player.getZ();
    }

    private void removeExpiredParticles(List<Particle> particles, long lifespan) {
        particles.removeIf(particle -> particle.time().finished(lifespan));
    }

    private long lastUpdateTime = System.nanoTime();
    private BufferAllocator boxAllocator;
    private VertexConsumerProvider.Immediate particleBuffers;

    public void onWorldLoad(WorldChangeEvent e) {
        onWorldChanged();
    }

    public void onEvent(Render3DEvent event) {
        MatrixStack matrix = event.getMatrices();
        long now = System.nanoTime();
        double deltaTime = Math.min(0.05, (now - lastUpdateTime) / 1_000_000_000.0);
        lastUpdateTime = now;

        if (targetParticles.isEmpty() && totemParticles.isEmpty()
                && worldParticles.isEmpty() && frameParticles.isEmpty()) return;

        matrix.push();
        if (particleBuffers == null) {
            boxAllocator = new BufferAllocator(1 << 12);
            particleBuffers = VertexConsumerProvider.immediate(boxAllocator);
        }
        VertexConsumerProvider.Immediate immediate = particleBuffers;
        for (Particle particle : targetParticles) particle.update(deltaTime);
        for (Particle particle : totemParticles) particle.update(deltaTime);
        for (Particle particle : worldParticles) particle.update(deltaTime);
        for (Particle particle : frameParticles) particle.update(deltaTime);

        if(glows.isEnabled()) {
            renderParticlesGlow(matrix, immediate, targetParticles, 2000, 2500, deltaTime);
            renderParticlesGlow(matrix, immediate, totemParticles, 1000, 1500, deltaTime);
            renderParticlesGlow(matrix, immediate, worldParticles, 1000, 1500, deltaTime);
            renderParticlesGlow(matrix, immediate, frameParticles, 750, 1000, deltaTime);
        }
        renderParticles(matrix, immediate, targetParticles, 2000, 2500, deltaTime);
        renderParticles(matrix, immediate, totemParticles, 1000, 1500, deltaTime);
        renderParticles(matrix, immediate, worldParticles, 1000, 1500, deltaTime);
        renderParticles(matrix, immediate, frameParticles, 750, 1000, deltaTime);

        immediate.draw();
        matrix.pop();
    }

    private void renderParticles(MatrixStack matrix, VertexConsumerProvider.Immediate immediate, List<Particle> particles, long fadeInTime, long fadeOutTime, double deltaTime) {
        if (particles.isEmpty()) return;

        matrix.push();
        beginBillboardPass(matrix);
        for (Particle particle : particles) {

            boolean notFinishedFadeIn = !particle.time().finished(fadeInTime);
            boolean finishedFadeOut = particle.time().finished(fadeOutTime);

            if (notFinishedFadeIn) {
                particle.animation().run(1, 0.5F, Easings.LINEAR, true);
            } else if (finishedFadeOut) {
                particle.animation().run(0, 2, Easings.LINEAR, true);
            }

            if (particle.animation.isAlive()) {
                particle.animation.update();
            }

            float animValue = particle.animation.get();
            int alpha = (int) (animValue * 255);
            if (alpha <= 0) continue;

            int color = ParticlePortColor.replAlpha(particle.color(), alpha);
            Vec3d v = particle.position();
            renderParticle(matrix, immediate, particle, (float) v.x, (float) v.y, (float) v.z, particle.size * animValue, color, alpha);
        }
        matrix.pop();
    }

    // Each particle used to push, translate, rotate by the camera (and optionally by its own Z roll) and pop.
    // The camera rotation is shared by the whole pass: JOML's rotate reads only the 3x3 block, and translate
    // changes neither that block, the normal matrix nor the property path rotate takes. So the rotated
    // position block and normal matrix are built once per pass and each particle only copies in the
    // translation column that the identical translate((float) (x - cam.x), ...) produces; the optional roll
    // is applied to scratch copies exactly as MatrixStack#multiply did. Vertices and normals are bit-identical.
    private Matrix4f passBase;
    private final Matrix4f passBillboard = new Matrix4f();
    private final Matrix4f passTranslated = new Matrix4f();
    private final Matrix4f passRolled = new Matrix4f();
    private final Matrix3f passNormalMatrix = new Matrix3f();
    private final Matrix3f passRolledNormal = new Matrix3f();
    private final org.joml.Quaternionf passRoll = new org.joml.Quaternionf();
    private final Vector3f passNormalVec = new Vector3f();
    private double passCamX, passCamY, passCamZ;
    private boolean passRotations;
    private float passNx, passNy, passNz;
    // getBuffer returns the same pending builder while the same QUADS layer is requested back to back
    // (only a different layer, or draw(), flushes it), so consecutive same-texture particles reuse it.
    private Identifier passLastTexture;
    private VertexConsumer passLastBuffer;

    private void beginBillboardPass(MatrixStack matrix) {
        MatrixStack.Entry entry = matrix.peek();
        passBase = entry.getPositionMatrix();
        org.joml.Quaternionf cameraRotation = mc.gameRenderer.getCamera().getRotation();
        passBillboard.set(passBase).translate(0.0F, 0.0F, 0.0F).rotate(cameraRotation);
        passNormalMatrix.set(entry.getNormalMatrix()).rotate(cameraRotation);
        Vec3d renderPos = mc.getEntityRenderDispatcher().camera.getPos();
        passCamX = renderPos.x;
        passCamY = renderPos.y;
        passCamZ = renderPos.z;
        passRotations = rotations.isEnabled();
        if (!passRotations) computeNormal(passNormalMatrix);
        passLastTexture = null;
        passLastBuffer = null;
    }

    /** Same as the old per-quad REUSABLE_NORMAL computation; stored once per matrix instead of per quad. */
    private void computeNormal(Matrix3f normalMatrix) {
        passNormalVec.set(0, 0, 1);
        normalMatrix.transform(passNormalVec);
        passNormalVec.normalize();
        passNx = passNormalVec.x;
        passNy = passNormalVec.y;
        passNz = passNormalVec.z;
    }

    private Matrix4f billboardAt(Particle particle, float x, float y, float z) {
        // setupOrientationMatrix: translate((double) x - cam.x, ...) which MatrixStack narrows to float.
        passTranslated.set(passBase).translate((float) (x - passCamX), (float) (y - passCamY), (float) (z - passCamZ));
        passBillboard.m30(passTranslated.m30()).m31(passTranslated.m31()).m32(passTranslated.m32());
        if (!passRotations) return passBillboard;
        // RotationAxis.POSITIVE_Z.rotationDegrees(r) == new Quaternionf().rotationZ(r * 0.017453292f).
        passRoll.rotationZ(particle.rotate() * 0.017453292f);
        computeNormal(passRolledNormal.set(passNormalMatrix).rotate(passRoll));
        return passRolled.set(passBillboard).rotate(passRoll);
    }

    private VertexConsumer bufferFor(VertexConsumerProvider.Immediate immediate, Identifier texture) {
        if (passLastBuffer != null && texture == passLastTexture) return passLastBuffer;
        passLastTexture = texture;
        passLastBuffer = immediate.getBuffer(ROMB_ESP.apply(texture));
        return passLastBuffer;
    }

    private void renderParticle(MatrixStack matrix, VertexConsumerProvider.Immediate immediate,
                                Particle particle, float x, float y, float z,
                                float pos, int color, int alpha) {
        Matrix4f matrix4f = billboardAt(particle, x, y, z);

        VertexConsumer buffer = bufferFor(immediate, particle.type().texture());

        drawTexturedQuad(buffer, matrix4f, -pos, -pos, pos * 2, pos * 2, color, alpha);

        if (particle.type == ParticleType.BLOOM && glows.isEnabled()) {
            drawTexturedQuad(buffer, matrix4f, -pos / 2, -pos / 2, pos, pos, color, alpha);
        }
    }

    private void renderParticlesGlow(MatrixStack matrix, VertexConsumerProvider.Immediate immediate, List<Particle> particles, long fadeInTime, long fadeOutTime, double deltaTime) {
        if (particles.isEmpty()) return;

        matrix.push();
        beginBillboardPass(matrix);
        for (Particle particle : particles) {


            float animValue = particle.animation.get();
            int alpha = (int) (animValue * 255 * glowsiz.getCurrentValue());
            if (alpha <= 0) continue;

            int color = ParticlePortColor.replAlpha(particle.color(), alpha);
            Vec3d v = particle.position();
            renderParticle2(matrix, immediate, particle, (float) v.x, (float) v.y, (float) v.z, particle.size  * animValue, color, alpha);
        }
        matrix.pop();
    }

    private void renderParticle2(MatrixStack matrix, VertexConsumerProvider.Immediate immediate,
                                 Particle particle, float x, float y, float z,
                                 float pos, int color, int alpha) {
        Matrix4f matrix4f = billboardAt(particle, x, y, z);

        VertexConsumer buffer = bufferFor(immediate, ParticleType.BLOOM.texture);

        drawTexturedQuad(buffer, matrix4f, -pos * 3, -pos * 3, pos * 6, pos * 6, color, alpha);
    }

    public static final Function<Identifier, RenderLayer> ROMB_ESP = Util.memoize(texture ->
        RenderLayer.of("bkengine_particles", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            VertexFormat.DrawMode.QUADS, 1536, false, true, RenderLayer.MultiPhaseParameters.builder()
                .program(RenderPhase.ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM)
                .texture(new RenderPhase.Texture(texture, net.minecraft.util.TriState.FALSE, false))
                .transparency(RenderPhase.LIGHTNING_TRANSPARENCY).cull(RenderPhase.DISABLE_CULLING)
                .depthTest(RenderPhase.LEQUAL_DEPTH_TEST).writeMaskState(RenderPhase.COLOR_MASK).build(false)));

    private void drawTexturedQuad(VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, int color, int alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Normal comes from the current pass/particle matrix (computeNormal), identical to the old per-quad value.
        float nx = passNx;
        float ny = passNy;
        float nz = passNz;

        float x1 = x;
        float y1 = y;
        float x2 = x + width;
        float y2 = y + height;

        buffer.vertex(matrix, x1, y1, 0.0f).color(r, g, b, alpha).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(nx, ny, nz);
        buffer.vertex(matrix, x2, y1, 0.0f).color(r, g, b, alpha).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(nx, ny, nz);
        buffer.vertex(matrix, x2, y2, 0.0f).color(r, g, b, alpha).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(nx, ny, nz);
        buffer.vertex(matrix, x1, y2, 0.0f).color(r, g, b, alpha).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(nx, ny, nz);
    }

    public static void setupOrientationMatrix(MatrixStack matrix, float x, float y, float z) {
        setupOrientationMatrix(matrix, (double) x, y, z);
    }

    public static void setupOrientationMatrix(MatrixStack matrix, double x, double y, double z) {
        final Vec3d renderPos = mc.getEntityRenderDispatcher().camera.getPos();
        matrix.translate(x - renderPos.x, y - renderPos.y, z - renderPos.z);
    }

    private void clear() {
        targetParticles.clear();
        frameParticles.clear();
        totemParticles.clear();
        worldParticles.clear();
        totemParticlesLeftToSpawn = 0;
        totalTotemParticlesToSpawn = 0;
        totemTargetEntity = null;
        totemSpawnStartTime = 0;
    }

    @Getter
    @Accessors(fluent = true)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum ParticleType {
        HEART("heart",true),
        STAR("star",true),
        BLOOM("glow",true),
        CIRCLE("circle",true),
        STAR_NEW("client",true),
        SNOOW("snowflake",true),
        DOLLAR("dollar",true),
        PUMPKIN("pumpkin",true);;

        Identifier texture;
        boolean rotatable;

        ParticleType(String name, boolean rotatable) {
            this.texture = Identifier.of("bkclient", "textures/particles/bkengine/" + name + ".png");
            this.rotatable = rotatable;
        }
    }

    @Getter
    @Accessors(fluent = true)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class Particle {
        ParticleType type;
        @NonFinal Vec3d position;
        @NonFinal Vec3d velocity;
        int index;
        int color;
        float size;
        private static final double BASE_VELOCITY = 0.05;
        double speedMultiplier;
        boolean isWorldParticle;
        boolean isTotem;

        dev.bkclient.utility.time.Timer time = new dev.bkclient.utility.time.Timer();
        Animation animation = new Animation();

        @NonFinal private float rotate = 0;

        public Particle(ParticleType type, Vec3d position, Vec3d velocity, int index, int rotate, int color, float size,
                        double speedMultiplier, boolean isWorldParticle, boolean isTotem) {
            this.type = type;
            this.position = position;
            this.velocity = isTotem ? velocity.multiply(0.08) : velocity.multiply(BASE_VELOCITY);
            this.index = index;
            this.color = color;
            this.rotate = rotate;
            this.size = size;
            this.speedMultiplier = speedMultiplier;
            this.isWorldParticle = isWorldParticle;
            this.isTotem = isTotem;
            this.time.reset();
        }

        public void update(double deltaTime) {
            double ticks = deltaTime * 120.0;
            if (ticks <= 0) return;

            double bounciness = 0.85;
            double gravity = isWorldParticle ? 0.0001 : 0.0005;

          // if (isTotem) {
          //     gravity = 0.0012;
          //     double dragXZ = Math.pow(0.89, ticks);
          //     velocity = new Vec3d(velocity.x * dragXZ, velocity.y, velocity.z * dragXZ);
          // }

            // Same per-axis logic as before, on doubles: one Vec3d for position and one for velocity per
            // update instead of up to five, and the block probes reuse one BlockPos.Mutable.
            double px = position.x, py = position.y, pz = position.z;
            double vx = velocity.x, vy = velocity.y, vz = velocity.z;
            double nextX = px + vx * ticks;
            double nextY = py + vy * ticks;
            double nextZ = pz + vz * ticks;

            if (isBlockSolidMutable(nextX, py, pz)) {
                vx = -vx * bounciness;
            } else {
                px = nextX;
            }

            if (isBlockSolidMutable(px, nextY, pz)) {
                vy = -vy * bounciness;
            } else {
                py = nextY;
            }

            if (isBlockSolidMutable(px, py, nextZ)) {
                vz = -vz * bounciness;
            } else {
                pz = nextZ;
            }

            position = new Vec3d(px, py, pz);
            // velocity.subtract(0, g, 0) == add(-0.0, -g, -0.0): v + -0.0 is v bit for bit.
            velocity = new Vec3d(vx + -0.0, vy + -(gravity * ticks), vz + -0.0);
            rotate += ( 2.0f) * ticks;
        }
    }

    public static boolean isBlockSolid(final double x, final double y, final double z) {
        if (mc.world == null) return false;
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        return mc.world.getBlockState(pos).isFullCube(mc.world, pos);
    }

    // Render-thread only (Particle#update runs from onEvent(Render3DEvent)).
    private static final BlockPos.Mutable SOLID_PROBE = new BlockPos.Mutable();

    /** isBlockSolid without the BlockPos allocation; Mutable#set(double...) floors exactly like ofFloored. */
    private static boolean isBlockSolidMutable(final double x, final double y, final double z) {
        if (mc.world == null) return false;
        BlockPos.Mutable pos = SOLID_PROBE.set(x, y, z);
        return mc.world.getBlockState(pos).isFullCube(mc.world, pos);
    }
}
