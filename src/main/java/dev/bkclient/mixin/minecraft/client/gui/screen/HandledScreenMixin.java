package dev.bkclient.mixin.minecraft.client.gui.screen;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.systems.event.impl.render.ScreenRenderEvent;
import dev.bkclient.systems.event.impl.window.ContainerClickEvent;
import dev.bkclient.systems.event.impl.window.ContainerReleaseEvent;
import dev.bkclient.systems.modules.modules.player.InvUtils;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.time.Timer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin implements IMinecraft {

@Unique
   private final Timer timer = new Timer();

   @Unique
   private final dev.bkclient.protection.client.HandledScreenLogic bkclient$logic = new dev.bkclient.protection.client.HandledScreenLogic();

   @Shadow
   protected int x;

   @Shadow
   protected int y;

   @Shadow
   protected abstract boolean method_2387(Slot var1, double var2, double var4);

   @Shadow
   protected abstract void method_2383(Slot var1, int var2, int var3, SlotActionType var4);

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      CustomDrawContext customDrawContext = CustomDrawContext.of(context);
      BKClientInit.getInstance().getEventManager().triggerEvent(new ScreenRenderEvent(customDrawContext, delta));

      this.bkclient$logic.render(context, ((HandledScreen<?>)(Object)this).getTitle(), this.x, this.y);

      if (mc.player == null || mc.player.currentScreenHandler == null) return;
      for (Slot slot : mc.player.currentScreenHandler.slots) {
         InvUtils invUtils = BKClientInit.getInstance().getModuleManager().getModule(InvUtils.class);
         if (invUtils != null
            && this.method_2387(slot, mouseX, mouseY)
            && slot.isEnabled()
            && invUtils.isEnabled()
            && invUtils.getScroller().isSelected()
            && this.timer.finished((long)invUtils.getScrollDelay().getCurrentValue())
            && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)
            && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
            this.method_2383(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
            this.timer.reset();
         }
      }
   }

   @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
   private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button == 1 && this.bkclient$logic.consumeClick(slot -> this.method_2387(slot, mouseX, mouseY))) {
         cir.setReturnValue(true);
         return;
      }
      BKClientInit.getInstance().getEventManager().triggerEvent(new ContainerClickEvent((float)mouseX, (float)mouseY, button));
   }

   @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"))
   public void mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      BKClientInit.getInstance().getEventManager().triggerEvent(new ContainerReleaseEvent((float)mouseX, (float)mouseY, button));
   }

}
