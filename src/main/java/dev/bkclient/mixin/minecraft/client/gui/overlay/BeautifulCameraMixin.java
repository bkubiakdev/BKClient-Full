package dev.bkclient.mixin.minecraft.client.gui.overlay;

import dev.bkclient.systems.modules.modules.visuals.Beautifully;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class BeautifulCameraMixin {
   @Shadow private boolean thirdPerson;
   @Shadow private float cameraY;
   @Shadow private float lastCameraY;
   @Shadow protected abstract void setPos(Vec3d pos);
   @Shadow protected abstract void setRotation(float yaw, float pitch);
   @Shadow protected abstract void moveBy(float x, float y, float z);
   @Shadow private float clipToSpace(float distance) { throw new AssertionError(); }
   @Unique private final Animation bk$distance = new Animation(400L, Easing.SMOOTH_STEP);
   @Unique private final Animation bk$front = new Animation(300L, Easing.generate(0.31F, 0.87F, 0.43F, 0.94F));
   @Unique private Entity bk$lastEntity;
   @Unique private BlockView bk$lastWorld;
   @Unique private boolean bk$active;

   @Inject(method = "update", at = @At("TAIL"))
   private void bk$smoothF5(BlockView world, Entity entity, boolean third, boolean inverse, float delta, CallbackInfo ci) {
      boolean enabled = Beautifully.cameraAnimation()
            && !dev.bkclient.systems.modules.modules.player.FreecamUtil.isActive()
            && !dev.bkclient.compat.PerspektiveCompatibility.isFreeLookActive()
            && !(entity instanceof LivingEntity living && living.isSleeping());
      if (!enabled || !bk$active || entity != bk$lastEntity || world != bk$lastWorld) {
         bk$distance.setValue(third ? 1 : 0);
         bk$front.setValue(third && inverse ? 1 : 0);
         bk$lastEntity = entity;
         bk$lastWorld = world;
         bk$active = enabled;
         return;
      }
      bk$distance.setEasing(third ? Easing.generate(0.31F, 0.87F, 0.41F, 1.3F)
         : Easing.generate(0.17F, 0.85F, 0.29F, 0.99F));
      float distance = bk$distance.update(third ? 1 : 0);
      float front = bk$front.update(third && inverse ? 1 : 0);
      if (bk$distance.isDone() && bk$front.isDone()) return;
      Camera camera = (Camera)(Object)this;
      // Reuse the final vanilla/rotation-module orientation so silent rotations remain intact.
      float yaw = camera.getYaw() - (third && inverse ? 180 : 0);
      float pitch = camera.getPitch() * (third && inverse ? -1 : 1);
      setPos(entity.getLerpedPos(delta).add(0, MathHelper.lerp(delta, lastCameraY, cameraY), 0));
      setRotation(yaw + 180 * front, MathHelper.lerp(front, pitch, -pitch));
      thirdPerson = third || distance > 0.01F;
      float scale = entity instanceof LivingEntity living ? living.getScale() : 1;
      if (distance > 0.001F) moveBy(-clipToSpace(4 * scale * distance), 0, 0);
   }
}
