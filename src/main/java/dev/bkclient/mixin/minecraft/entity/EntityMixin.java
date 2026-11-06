package dev.bkclient.mixin.minecraft.entity;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.ESP;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.bkclient.systems.modules.modules.combat.Hitboxes;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.RotationHandler;
import dev.bkclient.utility.rotations.RotationTask;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin implements IMinecraft {

   @Shadow
   private Box field_6005;

   @ModifyExpressionValue(
      method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z")
   )
   public boolean fixFalldistanceValue(boolean original) {
      return (Object)this == mc.player ? false : original;
   }

   @Inject(method = "getBoundingBox()Lnet/minecraft/util/math/Box;", at = @At("HEAD"), cancellable = true)
   public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
      Entity entity = (Entity)(Object)this;
      // Cheap rejections first; the expensive target check only runs while Hitboxes is enabled.
      if (!(entity instanceof LivingEntity livingEntity)) return;
      Hitboxes hitbox = BKClientInit.getInstance().getModuleManager().getModule(Hitboxes.class);
      if (hitbox != null
         && hitbox.isEnabled()
         && hitbox.shouldModifyHitbox(livingEntity)
         && entity.getId() != mc.player.getId()) {
         Box box = this.field_6005;
         // getCurrentValue() widened to double once per side, as before.
         double scale = hitbox.getScale().getCurrentValue();
         cir.setReturnValue(new Box(box.minX - scale, box.minY, box.minZ - scale, box.maxX + scale, box.maxY, box.maxZ + scale));
      }
   }

   @Redirect(method = "updateVelocity(FLnet/minecraft/util/math/Vec3d;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw()F"))
   public float movementCorrection(Entity instance) {
      RotationHandler rotationHandler = BKClientInit.INSTANCE.getRotationHandler();
      RotationTask currentTask = rotationHandler.getCurrentTask();
      return currentTask != null && currentTask.getMoveCorrection() != MoveCorrection.NONE && instance instanceof ClientPlayerEntity
         ? rotationHandler.getCurrentRotation().getYaw()
         : instance.getYaw();
   }

    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    public void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity)(Object)this;
        if (dev.bkclient.systems.modules.modules.visuals.ShaderESP.targets(entity) || ESP.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("RETURN"), cancellable = true)
    public void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity)(Object)this;
        if (dev.bkclient.systems.modules.modules.visuals.ShaderESP.targets(entity)) {
            cir.setReturnValue(dev.bkclient.systems.modules.modules.visuals.ShaderESP.maskColor(entity));
        } else if (ESP.shouldGlow(entity)) {
            cir.setReturnValue(ESP.getGlowColor((Entity)(Object)this));
        }
    }
}
