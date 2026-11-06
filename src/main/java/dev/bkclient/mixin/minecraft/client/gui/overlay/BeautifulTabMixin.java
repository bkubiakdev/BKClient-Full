package dev.bkclient.mixin.minecraft.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.modules.modules.visuals.Beautifully;
import dev.bkclient.utility.animation.base.Animation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class BeautifulTabMixin {
   @Shadow @Final private PlayerListHud playerListHud;
   @Unique private final Animation bk$tab = new Animation(200L, Beautifully.OUT_CUBIC);
   @Unique private Object bk$world;
   @Inject(method = "renderPlayerList", at = @At("HEAD"), cancellable = true)
   private void bk$renderTab(DrawContext context, RenderTickCounter ticks, CallbackInfo ci) {
      var mc = MinecraftClient.getInstance();
      if (!Beautifully.tabAnimation() || mc.world == null || mc.player == null) {
         bk$tab.setValue(0);
         bk$world = null;
         return;
      }
      if (bk$world != mc.world) { bk$tab.setValue(0); bk$world = mc.world; }
      var board = mc.world.getScoreboard();
      var objective = board.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
      boolean open = mc.options.playerListKey.isPressed() && (!mc.isInSingleplayer()
         || mc.player.networkHandler.getListedPlayerListEntries().size() > 1 || objective != null);
      ci.cancel();
      bk$tab.setEasing(open ? Beautifully.OPEN : Beautifully.IN_CUBIC);
      float p = bk$tab.update(open ? 1 : 0);
      playerListHud.setVisible(open);
      if (p <= 0.005F) return;
      context.draw();
      float[] color = RenderSystem.getShaderColor().clone();
      context.getMatrices().push();
      try {
         RenderSystem.setShaderColor(1, 1, 1, Math.clamp(p, 0, 1));
         float center = context.getScaledWindowWidth() / 2.0F;
         float scale = 0.96F + 0.04F * p;
         context.getMatrices().translate(center, 0, 0);
         context.getMatrices().scale(scale, scale, 1);
         context.getMatrices().translate(-center, (p - 1) * 10, 0);
         playerListHud.render(context, context.getScaledWindowWidth(), board, objective);
         context.draw();
      } finally {
         context.getMatrices().pop();
         RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
      }
   }
}
