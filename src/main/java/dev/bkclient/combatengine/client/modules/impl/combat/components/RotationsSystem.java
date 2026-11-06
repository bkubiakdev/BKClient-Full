package dev.bkclient.combatengine.client.modules.impl.combat.components;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.utils.combat.PredictUtils;
import dev.bkclient.combatengine.client.modules.impl.combat.components.gcd.GCDUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public abstract class RotationsSystem implements QClient {
   public Vec2f rotate = Vec2f.ZERO;

   public abstract void updateRotations(LivingEntity var1);

   public static Vec2f correctRotation(float yaw, float pitch) {
      if ((yaw != -90.0F || pitch != 90.0F) && yaw != -180.0F) {
         float var2 = GCDUtil.getGCD();
         yaw -= yaw % var2;
         pitch -= pitch % var2;
         return new Vec2f(yaw, pitch);
      } else {
         return new Vec2f(mc.player.getYaw(), mc.player.getPitch());
      }
   }

   protected boolean shouldUseElytraPredict(LivingEntity target) {
      return mc.player != null && target != null && mc.player.isGliding() && target.isGliding();
   }

   protected Vec3d getPredictedPoint(LivingEntity target, Vec3d point) {
      return !this.shouldUseElytraPredict(target) ? point : PredictUtils.bypasselytrahacking(target);
   }

   protected Box getPredictedBox(LivingEntity target) {
      Box var2 = target.getBoundingBox();
      if (!this.shouldUseElytraPredict(target)) {
         return var2;
      }

      Vec3d var3 = var2.getCenter();
      Vec3d var4 = this.getPredictedPoint(target, var3);
      return var2.offset(var4.subtract(var3));
   }
}
