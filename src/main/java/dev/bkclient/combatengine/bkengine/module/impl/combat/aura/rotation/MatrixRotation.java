package dev.bkclient.combatengine.bkengine.module.impl.combat.aura.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import dev.bkclient.combatengine.bkengine.manager.rotation.Rotation;
import dev.bkclient.combatengine.bkengine.manager.rotation.RotationProcess;
import dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura;
import dev.bkclient.combatengine.bkengine.module.impl.combat.aura.RotationAura;
import dev.bkclient.combatengine.bkengine.utils.aura.RayTraceUtil;
import dev.bkclient.combatengine.bkengine.utils.aura.UBoxPoints;
import dev.bkclient.combatengine.bkengine.utils.math.MathUtil;


public class MatrixRotation implements RotationAura {

    private float currentSpeedYaw = 34f;
    private float currentSpeedPitch = 12F;


    @Override
    
    public void onRotation(AttackAura aura, LivingEntity target, float[] ranges, boolean canAttack) {
        if (mc.player == null || target == null) return;


            Vec3d vec = UBoxPoints.getBestVector3dOnEntityBox(target.getBoundingBox()).subtract(mc.player.getEyePos()).normalize();


            float rawYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
            float rawPitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90F, 90F);

            float targetSpeedYaw = MathUtil.randomLerp(50, 80);
            float targetSpeedPitch = MathUtil.randomLerp(7, 12);


            if (RayTraceUtil.rayTraceEntity(mc.player.getYaw(), mc.player.getPitch(), aura.attackRange.get(), target)) {
                targetSpeedYaw = MathUtil.randomLerp(3, 4);
                targetSpeedPitch = MathUtil.randomLerp(1, 2);
            }


            float smoothFactor = 0.3f;

            currentSpeedYaw += (targetSpeedYaw - currentSpeedYaw) * smoothFactor;

            currentSpeedPitch += (targetSpeedPitch - currentSpeedPitch) * smoothFactor;
            RotationProcess.update(new Rotation((float) (rawYaw   ), (float) (rawPitch   )),
                    currentSpeedYaw,   currentSpeedPitch, MathUtil.random(360, 390), MathUtil.random(360, 390), (int)
                            MathUtil.random(3,5), 1, false);


    }

}
