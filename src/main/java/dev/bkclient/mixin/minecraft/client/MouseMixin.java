package dev.bkclient.mixin.minecraft.client;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.other.WindowScreen;
import dev.bkclient.systems.event.impl.window.MouseEvent;
import dev.bkclient.systems.event.impl.window.MouseScrollEvent;
import dev.bkclient.systems.event.impl.window.ChatReleaseEvent;
import dev.bkclient.systems.modules.modules.other.tinytask.TinyTaskBridge;
import dev.bkclient.ui.hud.impl.CustomChat;
import dev.bkclient.ui.hud.impl.DirectMessageChat;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin implements IMinecraft {
   @org.spongepowered.asm.mixin.injection.ModifyArgs(method = "updateMouse", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 0)
   private void bkclient$freecamModifyLookArgs(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
      if (dev.bkclient.systems.modules.modules.visuals.FreeLook.active()) {
         dev.bkclient.systems.modules.modules.visuals.FreeLook.look((Double)args.get(0),(Double)args.get(1));
         args.set(0,0.0);args.set(1,0.0);return;
      }
      if (!dev.bkclient.systems.modules.modules.player.FreecamUtil.isActive()) return;
      dev.bkclient.systems.modules.modules.player.FreecamUtil.applyLookDelta((Double) args.get(0), (Double) args.get(1));
      args.set(0, 0.0);
      args.set(1, 0.0);
   }
   @org.spongepowered.asm.mixin.Shadow private double cursorDeltaX;
   @org.spongepowered.asm.mixin.Shadow private double cursorDeltaY;
   @org.spongepowered.asm.mixin.Shadow private net.minecraft.util.math.Smoother cursorXSmoother;
   @org.spongepowered.asm.mixin.Shadow private net.minecraft.util.math.Smoother cursorYSmoother;

   @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
   private void aura$mouse(double timeDelta, CallbackInfo ci) {
      dev.bkclient.systems.modules.modules.visuals.FreeLook.refresh();
      if(dev.bkclient.systems.modules.modules.visuals.FreeLook.active()) return;
      if (dev.bkclient.systems.modules.modules.player.FreecamUtil.isActive()) return;
      // Perspektive consumes vanilla changeLookDirection; do not cancel it upstream.
      if (dev.bkclient.compat.PerspektiveCompatibility.isFreeLookActive()) return;
      if (!dev.bkclient.combatengine.bridge.AuraRuntime.active() || mc.player == null) return;
      double sensitivity = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
      double scaled = sensitivity * sensitivity * sensitivity * 8.0;
      double yaw, pitch;
      if (mc.options.smoothCameraEnabled) {
         yaw = cursorXSmoother.smooth(cursorDeltaX * scaled, timeDelta * scaled);
         pitch = cursorYSmoother.smooth(cursorDeltaY * scaled, timeDelta * scaled);
      } else if (mc.options.getPerspective().isFirstPerson() && mc.player.isUsingSpyglass()) {
         cursorXSmoother.clear(); cursorYSmoother.clear();
         yaw = cursorDeltaX * sensitivity * sensitivity * sensitivity;
         pitch = cursorDeltaY * sensitivity * sensitivity * sensitivity;
      } else {
         cursorXSmoother.clear(); cursorYSmoother.clear();
         yaw = cursorDeltaX * scaled; pitch = cursorDeltaY * scaled;
      }
      var event = new dev.bkclient.combatengine.api.events.implement.EventLook(yaw, pitch * (mc.options.getInvertYMouse().getValue() ? -1 : 1));
      dev.bkclient.combatengine.bridge.AuraRuntime.emit(event);
      if (!event.isCancelled()) {
         mc.getTutorialManager().onUpdateMouse(event.getYaw(), event.getPitch());
         mc.player.changeLookDirection(event.getYaw(), event.getPitch());
      }
      cursorDeltaX = cursorDeltaY = 0.0;
      ci.cancel();
   }

   @Inject(method = "tick()V", at = @At("RETURN"))
   private void tick(CallbackInfo ci) {
      if (CursorUtility.getCurrentType() != CursorUtility.getPrev()) {
         GLFW.glfwSetCursor(mc.getWindow().getHandle(), CursorUtility.getCurrentType().getCode());
      }

      CursorUtility.setPrev(CursorUtility.getCurrentType());
      CursorUtility.set(CursorType.DEFAULT);
   }

   @Inject(method = "onMouseButton(JIII)V", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
       MouseEvent event = new MouseEvent(button, action);
       BKClientInit.getInstance().getEventManager().triggerEvent(event);

       if (action == GLFW.GLFW_RELEASE
             && mc.currentScreen instanceof ChatScreen) {
          if (CustomChat.active()) DirectMessageChat.release();
          double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
          double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
          BKClientInit.getInstance().getEventManager().triggerEvent(
                new ChatReleaseEvent((float) mouseX, (float) mouseY, button));
       }

       if (event.isCancelled()) {
          ci.cancel();
       }
    }

   /**
    * Sends the click to the embedded window instead of the world when the crosshair is on it.
    *
    * <p>Cancelling here rather than reacting afterwards is the point: it is what stops the same
    * click from also swinging the arm or placing a block behind the panel.
    */
   @Inject(method = "onMouseButton(JIII)V", at = @At("HEAD"), cancellable = true)
   private void bkClient$windowScreenClick(long window, int button, int action, int mods, CallbackInfo ci) {
      // Every click in the game passes through here. Anything thrown from this handler would
      // take the mouse with it, so a fault in the panel code costs one lost click rather than
      // the ability to click at all.
      try {
         if (WindowScreen.handleMouseButton(button, action)) {
            ci.cancel();
         }
      } catch (Throwable throwable) {
         BKClientInit.LOGGER.error("[WindowScreen] Mouse handler failed", throwable);
      }
   }

   @Inject(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;mouseClicked(DDI)Z"), require = 0)
   private void bkClient$recordTinyTaskClick(long window, int button, int action, int mods, CallbackInfo ci) {
      if (action == GLFW.GLFW_PRESS && mc.currentScreen != null) {
         double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
         double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
         TinyTaskBridge.recordGuiClick(mc.currentScreen.getClass().getName(),
            mouseX - mc.currentScreen.width / 2.0, mouseY - mc.currentScreen.height / 2.0, button);
      }
   }

   @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"), cancellable = true)
   private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (vertical != 0.0) {
         if(dev.bkclient.systems.modules.modules.visuals.Zoom.scroll(vertical)) {ci.cancel();return;}
         MouseScrollEvent event = new MouseScrollEvent(vertical);
         BKClientInit.getInstance().getEventManager().triggerEvent(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
