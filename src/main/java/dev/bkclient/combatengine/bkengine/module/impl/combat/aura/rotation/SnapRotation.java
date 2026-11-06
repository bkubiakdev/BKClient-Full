package dev.bkclient.combatengine.bkengine.module.impl.combat.aura.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import dev.bkclient.combatengine.api.storages.implement.FreeLookStorage;
import dev.bkclient.combatengine.bkengine.manager.rotation.Rotation;
import dev.bkclient.combatengine.bkengine.manager.rotation.RotationProcess;
import dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura;
import dev.bkclient.combatengine.bkengine.module.impl.combat.aura.RotationAura;
import dev.bkclient.combatengine.bkengine.utils.aura.AuraUtil;
import dev.bkclient.combatengine.bkengine.utils.aura.UAttack;
import dev.bkclient.combatengine.bkengine.utils.aura.UBoxPoints;
import dev.bkclient.combatengine.bkengine.utils.math.MathUtil;

public class SnapRotation implements RotationAura {

    @Override
    
    public void onRotation(AttackAura aura, LivingEntity target, float[] ranges, boolean canAttack) {
        if (aura.typeSnap.is("FOV")) {
            fov(aura, target, canAttack);
            return;
        }
        snap360(aura, target, ranges);
    }

    private void snap360(AttackAura aura, LivingEntity target, float[] ranges) {
        boolean canAttack = UAttack.shouldAttack(target, false, true, true, -100L, ranges);

        Vec3d vec3d = AuraUtil.getVector3(target);

        float yaw = (float) Math.toDegrees(Math.atan2(-vec3d.x, vec3d.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec3d.y, Math.hypot(vec3d.x, vec3d.z))), -90, 90);

        float speedYAW = MathUtil.randomLerp(80, 90);
        float speedPit = MathUtil.randomLerp(80, 90);
        float waveA = (float) Math.cos(System.currentTimeMillis() / 30D);
        float waveB = (float) Math.sin(System.currentTimeMillis() / 30D);

        float yawJitter = waveA * MathUtil.randomLerp(8, 16);
        float pitchJitter = waveB * MathUtil.randomLerp(8, 16);


        boolean attack = false;
        if (canAttack) {
            aura.tick   = 1;
        }
        if(aura.tick > 0) {

            attack = true;

            aura.tick --;
        }


        if (!attack) {
            yawJitter = 0;
            pitchJitter = 0;
            yaw = FreeLookStorage.getFreeYaw();
            pitch = FreeLookStorage.getFreePitch();
        }
        Rotation newRotation = new Rotation(yaw + yawJitter, pitch + pitchJitter);
        RotationProcess.update(newRotation, speedYAW, speedPit,
                MathUtil.randomInt(90, 180), MathUtil.randomInt(90, 180), MathUtil.randomInt(0, 3), 15, false);
    }

    private void fov(AttackAura aura, LivingEntity target, boolean canAttack) {


        Vec3d vec = UBoxPoints.getBestVector3dOnEntityBox(target.getBoundingBox(), false).add(
                0.3F * Math.sin(System.currentTimeMillis() / 50D),
                0.1F * Math.sin(System.currentTimeMillis() / 50D) + 0.3F * Math.cos(System.currentTimeMillis() / 50D),
                0.3F * Math.cos(System.currentTimeMillis() / 50D)
        ).subtract(mc.player.getEyePos());

        float yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90F, 90F);

        float waveA = (float) Math.cos(System.currentTimeMillis() / 40D) * 4;
        float waveB = (float) Math.sin(System.currentTimeMillis() / 70D) * 4;

        if (!canAttack || (aura.getTargetFov(target) >= aura.fov.get())) {

            waveA = 0;
            waveB = 0;

                yaw = FreeLookStorage.getFreeYaw();
                pitch = FreeLookStorage.getFreePitch();

        }
        RotationProcess.update(new Rotation(yaw + waveA , pitch + waveB ),
                30,30, 30, 30, 1, 15, false);
    }
}
