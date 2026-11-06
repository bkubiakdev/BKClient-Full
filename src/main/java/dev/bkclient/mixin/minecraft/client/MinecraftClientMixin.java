package dev.bkclient.mixin.minecraft.client;

import dev.bkclient.BKClientInit;
import dev.bkclient.protection.client.MinecraftClientMixinProtection;
import dev.bkclient.systems.event.impl.game.GameTickEvent;
import dev.bkclient.systems.modules.modules.player.NoDelay;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.update.UpdateChecker;
import dev.bkclient.utility.render.sprite.SpriteAnimationAtlas;
import dev.bkclient.systems.modules.modules.other.tinytask.TinyTaskBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
   @Inject(method = "setWorld", at = @At("HEAD"))
   private void bkclient$releaseWorldResources(net.minecraft.client.world.ClientWorld world, CallbackInfo ci) {
      // Vanilla assigns MinecraftClient.world before calling setWorld on both
      // join and disconnect. Comparing that field to the argument always skipped cleanup.
      BKClientInit.INSTANCE.releaseWorldResources();
   }
   @Inject(method = "tick()V", at = @At("TAIL"))
   private void aura$bkengineTickEnd(CallbackInfo ci) {
      dev.bkclient.combatengine.bridge.AuraRuntime.clientTickEnd();
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void aura$render(boolean tick, CallbackInfo ci) {
      dev.bkclient.combatengine.bridge.AuraRuntime.render();
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   private void bkclient$blockPanicScreens(net.minecraft.client.gui.screen.Screen screen, CallbackInfo ci) {
      if (BKClientInit.INSTANCE.isPanic() && screen != null
          && screen.getClass().getName().startsWith("dev.bkclient.")) ci.cancel();
   }

   @Shadow
   private int field_1752;

   @Inject(method = "tick()V", at = @At("HEAD"))
   public void tick(CallbackInfo ci) {
      if (BKClientInit.INSTANCE.isPanic()) return;
      TinyTaskBridge.tick();
      BKClientInit.getInstance().getEventManager().triggerEvent(new GameTickEvent());
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null && client.currentScreen == null) {
         UpdateChecker.checkOnce();

      }
      try {
         AutoMarket autoMarket = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
         if (autoMarket != null) {
            autoMarket.getReconnectController().setDelaySeconds(
               Math.round(autoMarket.reconnectDelay.getCurrentValue()));
            autoMarket.getReconnectController().globalTick(autoMarket.autoReconnect.isEnabled());
         }
      } catch (RuntimeException ignored) {
      }
   }

   @Inject(method = "isWindowFocused", at = @At("HEAD"), cancellable = true)
   private void bkClient$keepTinyTaskFocused(CallbackInfoReturnable<Boolean> cir) {
      if (TinyTaskBridge.isPlaying()) cir.setReturnValue(true);
   }

   @Inject(method = "<init>(Lnet/minecraft/client/RunArgs;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;onResolutionChanged()V"))
   public void initializeClient(RunArgs args, CallbackInfo ci) {
      MinecraftClientMixinProtection.init();
   }

    @Inject(method = "<init>(Lnet/minecraft/client/RunArgs;)V", at = @At("RETURN"))
    public void endInitialize(RunArgs args, CallbackInfo ci) {
       SpriteAnimationAtlas atlas = SpriteAnimationAtlas.getOrCreateAtlasFor(16, 16);
      atlas.registerAnimationArchive(BKClientInit.id("animations/combat.bkanim"));
      atlas.registerAnimationArchive(BKClientInit.id("animations/movement.bkanim"));
      atlas.registerAnimationArchive(BKClientInit.id("animations/visuals.bkanim"));
      atlas.registerAnimationArchive(BKClientInit.id("animations/player.bkanim"));
      atlas.registerAnimationArchive(BKClientInit.id("animations/other.bkanim"));
      atlas.registerAnimationArchive(BKClientInit.id("animations/search.bkanim"));
      atlas.buildAtlas();
      SpriteAnimationAtlas atlas12 = SpriteAnimationAtlas.getOrCreateAtlasFor(12, 12);
      atlas12.registerAnimationArchive(BKClientInit.id("animations/check_enable.bkanim"));
      atlas12.registerAnimationArchive(BKClientInit.id("animations/check_disable.bkanim"));
      atlas12.buildAtlas();
   }

   @Inject(method = "stop()V", at = @At("HEAD"))
   public void shutdownClient(CallbackInfo ci) {
      MinecraftClientMixinProtection.shutdown();
   }

   @Inject(method = "getWindowTitle()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
   public void changeWindowTitle(CallbackInfoReturnable<String> cir) {
      MinecraftClientMixinProtection.updateTitle(cir);
   }

   @Inject(method = "doItemUse()V", at = @At("TAIL"))
   private void resetItemUseCooldown(CallbackInfo ci) {
      NoDelay noDelayModule = BKClientInit.getInstance().getModuleManager().getModule(NoDelay.class);
      if (noDelayModule.isEnabled() && noDelayModule.getRightClick().isEnabled()) {
         this.field_1752 = 0;
      }
   }
}
