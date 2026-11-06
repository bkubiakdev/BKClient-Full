package dev.bkclient.mixin.minecraft.client.network;

import dev.bkclient.BKClientInit;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.bkclient.systems.event.impl.game.CloseScreenEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEndEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.player.SlowDownEvent;
import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.systems.modules.modules.player.InvUtils;
import dev.bkclient.systems.modules.modules.player.NoPush;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.mixins.ClientPlayerEntityAddition;
import dev.bkclient.utility.rotations.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.utility.rotations.ContinuousYaw;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin implements ClientPlayerEntityAddition, IMinecraft {
   @Unique
   private int groundTicks = 0;
   @Unique
   private Aura aura;
   @Unique
   private boolean bkclient$hasPacketYaw;
   @Unique
   private float bkclient$packetYaw;

   @Inject(method = "<init>*", at = @At("RETURN"))
   private void initAura(CallbackInfo ci) {
      this.aura = BKClientInit.getInstance().getModuleManager().getModule(Aura.class);
   }

   @Redirect(method = "tickMovement()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
   private boolean onIsUsingItemRedirect(ClientPlayerEntity player) {
      if (!BKClientInit.getInstance().getEventManager().hasListeners(SlowDownEvent.class)) {
         return player.isUsingItem() && player.getVehicle() == null;
      }
      SlowDownEvent slowDownEvent = new SlowDownEvent();
      BKClientInit.getInstance().getEventManager().triggerEvent(slowDownEvent);
      return player.isUsingItem() && player.getVehicle() == null && !slowDownEvent.isCancelled();
   }

   @ModifyExpressionValue(method = "tickMovement()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;canSprint()Z"))
   private boolean disallowSprinting(boolean original) {
      if (dev.bkclient.combatengine.bridge.AuraRuntime.preventSprint()) return false;
      if (this.aura == null) return original;
      return this.aura.isEnabled() && this.aura.shouldPreventSprinting() ? false : original;
   }

   @WrapWithCondition(method = "closeScreen()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
   private boolean preventCloseScreen(MinecraftClient instance, Screen screen) {
      BKClientInit.getInstance().getEventManager().triggerEvent(new CloseScreenEvent(screen));
      return true;
   }

   @Inject(method = "pushOutOfBlocks(DD)V", at = @At("HEAD"), cancellable = true)
   public void removePushOutFromBlocks(double x, double z, CallbackInfo ci) {
      NoPush noPush = BKClientInit.getInstance().getModuleManager().getModule(NoPush.class);
      if (noPush.isEnabled() && noPush.getBlocks().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "tick()V", at = @At("HEAD"))
   public void triggerTickEvent(CallbackInfo ci) {
      BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.combat.KnockbackDisplacement.class).flushAttack();
      BKClientInit.getInstance().getEventManager().triggerEvent(new ClientPlayerTickEvent());
      dev.bkclient.combatengine.bridge.AuraRuntime.tick();
   }

   @Inject(method = "tick()V", at = @At("RETURN"))
   public void triggerTickEndEvent(CallbackInfo ci) {
      BKClientInit.getInstance().getEventManager().triggerEvent(new ClientPlayerTickEndEvent());
      dev.bkclient.combatengine.bridge.AuraRuntime.postTick();
   }

   @Inject(method = "tickMovement()V", at = @At("HEAD"))
   public void updateOnGroundTicks(CallbackInfo ci) {
      BKClientInit.getInstance().getModuleManager()
         .getModule(dev.bkclient.systems.modules.modules.player.Scaffold.class).flushPendingPlacement();
      if (mc.player != null && mc.player.isOnGround()) {
         this.groundTicks++;
      } else {
         this.groundTicks = 0;
      }
   }

   @Inject(method = "sendMovementPackets()V", at = @At("RETURN"))
   private void bkclient$displacementMovementSent(CallbackInfo ci) {
      BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.combat.KnockbackDisplacement.class).movementSent();
   }

   @Redirect(method = "sendMovementPackets()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
   public float replaceMovePacketYaw(ClientPlayerEntity instance) {
      RotationHandler rotationHandler = BKClientInit.getInstance().getRotationHandler();
      float yaw = dev.bkclient.combatengine.bridge.AuraRuntime.ownsRotation() || rotationHandler.isIdling() ? instance.getYaw() : rotationHandler.getCurrentRotation().getYaw();
      yaw = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.combat.KnockbackDisplacement.class).movementYaw(yaw);
      // Wrapping the absolute yaw made 179 -> 181 become 179 -> -179.
      // Preserve the direction without introducing a nearly full-turn jump.
      if (this.bkclient$hasPacketYaw) yaw = ContinuousYaw.next(this.bkclient$packetYaw, yaw);
      this.bkclient$packetYaw = yaw;
      this.bkclient$hasPacketYaw = true;
      rotationHandler.getServerRotation().setYaw(yaw);
      return yaw;
   }

   @Redirect(method = "sendMovementPackets()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
   public float replaceMovePacketPitch(ClientPlayerEntity instance) {
      RotationHandler rotationHandler = BKClientInit.getInstance().getRotationHandler();
      float pitch = dev.bkclient.combatengine.bridge.AuraRuntime.ownsRotation() || rotationHandler.isIdling() ? instance.getPitch() : rotationHandler.getCurrentRotation().getPitch();
      rotationHandler.getServerRotation().setPitch(pitch);
      return pitch;
   }

   @Inject(method = "dropSelectedItem(Z)Z", at = @At("HEAD"), cancellable = true)
   private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
      InvUtils slotLock = BKClientInit.getInstance().getModuleManager().getModule(InvUtils.class);
      if (slotLock.isEnabled() && slotLock.getSlotLock().isSelected() && slotLock.isLocked(mc.player.getInventory().selectedSlot)) {
         cir.setReturnValue(false);
         cir.cancel();
      }
   }

   @Override
   public int bkclient$getOnGroundTicks() {
      return this.groundTicks;
   }
}
