package dev.bkclient.mixin.minecraft.client.input;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.player.InputEvent;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.modules.modules.other.tinytask.TinyTaskBridge;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
   @Inject(method = "tick()V", at = @At("TAIL"))
   private void onTick(CallbackInfo ci) {
      Input input = (Input)(Object)this;
      InputAccessor accessor = (InputAccessor)input;
      PlayerInput keys = accessor.getInput();
      float movementForward = accessor.getMovementForward();
      float movementSideways = accessor.getMovementSideways();
      boolean jumping = accessor.getInput().jump();
      boolean sneaking = accessor.getInput().sneak();
      boolean sprint = accessor.getInput().sprint();
      InputEvent event = new InputEvent(movementForward, movementSideways, jumping, sneaking, sprint);
      BKClientInit.getInstance().getEventManager().triggerEvent(event);
      var sourceInput=new dev.bkclient.combatengine.api.events.implement.EventMoveInput(event.getForward(),event.getStrafe(),event.isJump(),event.isSneak(),event.isSprint());
      dev.bkclient.combatengine.bridge.AuraRuntime.emit(sourceInput);
      event.setForward(sourceInput.getForward());event.setStrafe(sourceInput.getStrafe());event.setJump(sourceInput.isJump());event.setSneak(sourceInput.isSneak());event.setSprint(sourceInput.isSprint());
      accessor.setMovementForward(event.getForward());
      accessor.setMovementSideways(event.getStrafe());
      boolean forwardKey = event.getForward() > 0.0F;
      boolean backwardKey = event.getForward() < 0.0F;
      boolean leftKey = event.getStrafe() > 0.0F;
      boolean rightKey = event.getStrafe() < 0.0F;
      accessor.setInput(new PlayerInput(forwardKey, backwardKey, leftKey, rightKey, event.isJump(), event.isSneak(), event.isSprint()));
      TinyTaskBridge.applyInputOverride(input);

      // LobbyController runs independently from the physical keyboard. Writing only
      // KeyBinding#setPressed is not enough because this mixin rebuilds PlayerInput
      // every tick (and TinyTask may rebuild it once more). Apply lobby movement last.
      try {
         AutoMarket autoMarket = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
         if (autoMarket != null && autoMarket.isEnabled()
            && autoMarket.getLobbyController().shouldForceForward()) {
            PlayerInput current = accessor.getInput();
            accessor.setMovementForward(1.0F);
            accessor.setInput(new PlayerInput(
               true,
               false,
               current.left(),
               current.right(),
               current.jump(),
               current.sneak(),
               current.sprint()
            ));
         }
      } catch (RuntimeException ignored) {
         // Client/module initialization can still be incomplete on the first input tick.
      }
      dev.bkclient.combatengine.bridge.AuraRuntime.captureInputYaw();
   }
}
