package dev.bkclient.mixin.minecraft.entity;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.game.BlockBreakEvent;
import dev.bkclient.systems.event.impl.game.InternalAttackEvent;
import dev.bkclient.systems.event.impl.game.StartBreakBlockEvent;
import dev.bkclient.systems.modules.modules.player.NoInteract;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Shadow
    private MinecraftClient field_3712;

   @Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void bkclient$critPre(PlayerEntity player, Entity target, CallbackInfo ci) {
      var displacement = BKClientInit.getInstance().getModuleManager().getModule(
            dev.bkclient.systems.modules.modules.combat.KnockbackDisplacement.class);
      if (displacement.deferAttack(player, target)) { ci.cancel(); return; }
      var sourceEvent=new dev.bkclient.combatengine.api.events.implement.EventAttackEntity(player,target);
      dev.bkclient.combatengine.bridge.AuraRuntime.emit(sourceEvent);
      if(sourceEvent.isCancelled()){ci.cancel();return;}
      var aura=BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.combat.Aura.class);
      if(aura!=null && aura.isEnabled()) aura.noteAttack();
      InternalAttackEvent event = new InternalAttackEvent(target);
      BKClientInit.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
         return;
      }
      displacement.attackAccepted();
   }

   @Inject(method = "breakBlock(Lnet/minecraft/util/math/BlockPos;)Z", at = @At("RETURN"), cancellable = true)
   public void breakBlockHook(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      BlockBreakEvent event = new BlockBreakEvent(pos);
      BKClientInit.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "attackBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z", at = @At("HEAD"), cancellable = true)
   private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
      StartBreakBlockEvent event = new StartBreakBlockEvent(blockPos);
      BKClientInit.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(
      method = "interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
      at = @At("HEAD"),
      cancellable = true
   )
   public void preventInteraction(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
      if (this.field_3712 != null && this.field_3712.world != null) {
         NoInteract noInteractModule = BKClientInit.getInstance().getModuleManager().getModule(NoInteract.class);
         Block block = this.field_3712.world.getBlockState(hitResult.getBlockPos()).getBlock();
         if (noInteractModule.isEnabled() && noInteractModule.shouldPreventInteract(block)) {
            cir.setReturnValue(ActionResult.PASS);
            return;
         }
         var dripstone = BKClientInit.getInstance().getModuleManager().getModule(
             dev.bkclient.systems.modules.modules.combat.AutoDripstone.class);
         ActionResult result = dripstone.onUseBlock(player, hand, hitResult);
         if (result != null) cir.setReturnValue(result);
      }
   }

   // Brewing Overlay deferred: no tracking while disabled.
   //    @Inject(method = "interactBlock", at = @At("RETURN"))
   //    private void bk$rememberBrewingStand(ClientPlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
   //       var overlay = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.BrewingOverlay.class);
   //       if (overlay != null && cir.getReturnValue().isAccepted()) overlay.noteInteraction(hit.getBlockPos());
   //    }
   //

   @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
   private void preventEntityInteraction(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      NoInteract noInteract = BKClientInit.getInstance().getModuleManager().getModule(NoInteract.class);
      if (noInteract.isEnabled() && noInteract.shouldPreventInteractEntity(entity)) {
         cir.setReturnValue(ActionResult.PASS);
      }
   }

}
