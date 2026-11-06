package dev.bkclient.mixin.minecraft.client.gui.overlay;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.bkclient.systems.modules.modules.visuals.Beautifully;
import dev.bkclient.ui.hud.impl.CustomChat;
import dev.bkclient.utility.render.ScissorUtility;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class BeautifulChatHudMixin {
   @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
   @Shadow private int scrolledLines;
   @Shadow public abstract double getChatScale();
   @Shadow private int getLineHeight() { throw new AssertionError(); }
   @Unique private int bk$before, bk$newLines;
   @Unique private long bk$slideStart;
   @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
   private void bk$before(Text text, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
      bk$before = visibleMessages.size();
   }
   @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("TAIL"))
   private void bk$after(Text text, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
      if (!Beautifully.chatAnimation() || CustomChat.active() || scrolledLines > 0) return;
      bk$newLines = Math.clamp(visibleMessages.size() - bk$before, 1, 4);
      bk$slideStart = System.currentTimeMillis();
   }
   @WrapMethod(method = "render")
   private void bk$slide(DrawContext context, int tick, int mx, int my, boolean focused, Operation<Void> original) {
      float p = Beautifully.progress(bk$slideStart);
      if (!Beautifully.chatAnimation() || CustomChat.active() || scrolledLines > 0 || p >= 1) {
         original.call(context, tick, mx, my, focused); return;
      }
      float shift = (1 - Beautifully.OUT_CUBIC.ease(p, 0, 1, 1)) * bk$newLines * getLineHeight() * (float)getChatScale();
      context.draw();
      ScissorUtility.push(context.getMatrices(), 0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight() - 40);
      context.getMatrices().push();
      try {
         context.getMatrices().translate(0, shift, 0);
         original.call(context, tick, mx, (int)(my - shift), focused);
         context.draw();
      } finally {
         context.getMatrices().pop();
         ScissorUtility.pop();
      }
   }
}
