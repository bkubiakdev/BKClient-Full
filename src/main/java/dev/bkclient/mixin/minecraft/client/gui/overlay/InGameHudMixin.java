package dev.bkclient.mixin.minecraft.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.render.PostHudRenderEvent;
import dev.bkclient.systems.event.impl.render.PreHudRenderEvent;
import dev.bkclient.systems.modules.modules.visuals.CustomHotbar;
import dev.bkclient.systems.modules.modules.visuals.Removals;
import dev.bkclient.ui.hud.impl.CustomChat;
import dev.bkclient.utility.game.server.ServerUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import dev.bkclient.utility.render.HotbarRendered;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin implements IMinecraft {
   @Inject(method = "render", at = @At("HEAD"))
   private void aura$aimAssist(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
      dev.bkclient.combatengine.bridge.AuraRuntime.emit(new dev.bkclient.combatengine.api.events.implement.EventRender.Default(context,counter.getTickDelta(true)));
   }
   @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
   private void bkclient$bkclientHotbar(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
      if (CustomHotbar.replacesHud()) {
         CustomHotbar.get().renderHotbar(context, counter);
         ci.cancel();
      }
   }

   @WrapMethod(method = {"renderStatusBars", "renderMountHealth"})
   private void bkclient$alignStatusBars(DrawContext context, Operation<Void> original) {
      if (!CustomHotbar.replacesHud()) {
         original.call(context);
         return;
      }
      int vanillaY = context.getScaledWindowHeight() - 39;
      context.getMatrices().push();
      try {
         context.getMatrices().translate(0, HotbarRendered.statusBarY(context.getScaledWindowHeight()) - vanillaY, 0);
         original.call(context);
      } finally {
         context.getMatrices().pop();
      }
   }

   @Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true)
   private void bkclient$customHearts(CallbackInfo ci) {
      if (CustomHotbar.replacesHearts()) ci.cancel();
   }

   @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
   private void bkclient$customFood(CallbackInfo ci) {
      if (CustomHotbar.replacesFood()) ci.cancel();
   }

   @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
   private static void bkclient$customArmor(CallbackInfo ci) {
      if (CustomHotbar.replacesArmor()) ci.cancel();
   }

   @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
   private void bkclient$bkclientExperience(DrawContext context, int x, CallbackInfo ci) {
      if (CustomHotbar.replacesHud()) ci.cancel();
   }

   @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
   private void bkclient$bkclientLevel(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
      if (CustomHotbar.replacesHud()) ci.cancel();
   }

   @Shadow
   private void renderStatusBars(DrawContext context) {
      throw new AssertionError();
   }

   
@Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
   private void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      dev.bkclient.systems.modules.modules.visuals.Crosshair crosshair = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.Crosshair.class);
      if (crosshair != null && crosshair.isEnabled()) {
         ci.cancel();
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static net.minecraft.text.Text bkclient$lastSidebarTitle;

   @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
   private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      // Parsing depends only on the title Text and the server; the same Text instance gives the same result every frame.
      net.minecraft.text.Text title = objective.getDisplayName();
      if (title != bkclient$lastSidebarTitle) {
         bkclient$lastSidebarTitle = title;
         String titleString = title.getString();
         if (titleString.contains("Анархия") && (ServerUtility.isFT() || ServerUtility.isST())) {
            try {
               ServerUtility.ftAn = Integer.parseInt(titleString.split("-")[1].trim());
            } catch (Exception var5) {
            }
         }
      }

      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getScoreboard().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderPortalOverlay(Lnet/minecraft/client/gui/DrawContext;F)V", at = @At("HEAD"), cancellable = true)
   private void renderPortalOverlayHook(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getPortal().isSelected()) {
         ci.cancel();
      }
   }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"))
   public void triggerPreHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomDrawContext customDrawContext = CustomDrawContext.of(context);
      BKClientInit.getInstance().getEventManager().triggerEvent(new PreHudRenderEvent(customDrawContext, tickCounter.getTickDelta(false)));
   }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("RETURN"))
    public void triggerPostHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        BKClientInit.getInstance().getEventManager().triggerEvent(new PostHudRenderEvent(customDrawContext, tickCounter.getTickDelta(false)));
    }

    @Inject(method = "renderMainHud(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
    private void triggerHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        this.bkclient$renderMissingVanillaStatusBars(context);
        CustomDrawContext customDrawContext = CustomDrawContext.of(context);
        DrawUtility.blurProgram.draw();
        // The replacement chat renders the HUD above the screen and below its suggestions.
        // Keep preparing shared effects here, but let that later pass own HUD rendering.
        if (!(mc.currentScreen instanceof ChatScreen && CustomChat.active())) {
            BKClientInit.getInstance().getEventManager().triggerEvent(new HudRenderEvent(customDrawContext, tickCounter.getTickDelta(false)));
        }
    }

    @Inject(
       method = "renderMainHud(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
       at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusBars(Lnet/minecraft/client/gui/DrawContext;)V")
    )
    private void bkclient$beforeVanillaStatusBars(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
       this.bkclient$resetVanillaHudRenderState();
    }

    @Unique
    private void bkclient$renderMissingVanillaStatusBars(DrawContext context) {
       if (CustomHotbar.replacesHud() || mc.player == null || mc.interactionManager == null || mc.player.isSpectator() || !mc.player.isAlive()) {
          return;
       }

       GameMode serverGameMode = this.bkclient$getServerGameMode();
       if (serverGameMode != null && !serverGameMode.isSurvivalLike()) {
          return;
       }

       boolean shouldRenderStatusBars = mc.interactionManager.hasStatusBars()
          || serverGameMode != null && serverGameMode.isSurvivalLike()
          || serverGameMode == null && !mc.player.getAbilities().creativeMode;
       if (!shouldRenderStatusBars) {
          return;
       }

       context.draw();
       this.bkclient$resetVanillaHudRenderState();
       this.renderStatusBars(context);
    }

    @Unique
    private GameMode bkclient$getServerGameMode() {
       if (mc.getNetworkHandler() == null || mc.player == null) {
          return null;
       }

       PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
       return entry != null ? entry.getGameMode() : null;
    }

    @Unique
    private void bkclient$resetVanillaHudRenderState() {
       RenderSystem.colorMask(true, true, true, true);
       RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
       RenderSystem.enableBlend();
       RenderSystem.defaultBlendFunc();
       RenderSystem.disableDepthTest();
    }


}
