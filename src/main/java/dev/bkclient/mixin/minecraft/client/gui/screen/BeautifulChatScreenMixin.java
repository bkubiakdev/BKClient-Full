package dev.bkclient.mixin.minecraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.modules.modules.visuals.Beautifully;
import dev.bkclient.ui.hud.impl.CustomChat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class BeautifulChatScreenMixin {
   @Unique private long bk$openedAt;
   @Unique private boolean bk$shifted;
   @Unique private float[] bk$color;
   @Inject(method = "init", at = @At("TAIL"))
   private void bk$open(CallbackInfo ci) { bk$openedAt = System.currentTimeMillis(); }
   @Inject(method = "render", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V", shift = At.Shift.AFTER))
   private void bk$input(DrawContext context, int mx, int my, float delta, CallbackInfo ci) {
      if (!Beautifully.chatAnimation() || CustomChat.active()) return;
      float p = Beautifully.progress(bk$openedAt);
      if (p >= 1) return;
      float eased = Beautifully.OUT_CUBIC.ease(p, 0, 1, 1);
      context.draw();
      bk$color = RenderSystem.getShaderColor().clone();
      context.getMatrices().push();
      context.getMatrices().translate(0, (1 - eased) * 16, 0);
      RenderSystem.setShaderColor(1, 1, 1, eased);
      bk$shifted = true;
   }
   @Inject(method = "render", at = @At("RETURN"))
   private void bk$restore(DrawContext context, int mx, int my, float delta, CallbackInfo ci) {
      if (!bk$shifted) return;
      context.draw();
      context.getMatrices().pop();
      RenderSystem.setShaderColor(bk$color[0], bk$color[1], bk$color[2], bk$color[3]);
      bk$shifted = false;
   }
}
