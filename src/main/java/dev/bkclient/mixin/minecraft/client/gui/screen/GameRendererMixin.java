package dev.bkclient.mixin.minecraft.client.gui.screen;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.bkclient.utility.render.shader.FlameHandsRenderer;
import dev.bkclient.systems.cosmetics.render.CosmeticGlowRenderer;
import dev.bkclient.systems.cosmetics.render.MeleeCosmeticRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.systems.modules.modules.visuals.Removals;
import dev.bkclient.systems.modules.modules.visuals.HandsShader;
import dev.bkclient.systems.modules.modules.visuals.AspectRatio;
import dev.bkclient.systems.modules.modules.visuals.ViewModel;
import dev.bkclient.utility.render.Utils;
import dev.bkclient.utility.render.shader.HandShaderHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
   @ModifyReturnValue(method="getFov", at=@At("RETURN"))
   private float bkclient$zoom(float original, net.minecraft.client.render.Camera camera, float tickDelta, boolean changingFov) {
      return changingFov ? dev.bkclient.systems.modules.modules.visuals.Zoom.apply(original) : original;
   }

   // Composite after world post-processing (including Iris), before the HUD.
   @Inject(method = "renderWorld", at = @At("RETURN"))
   private void bkclient$finishShaderEsp(RenderTickCounter counter, CallbackInfo ci) {
      dev.bkclient.utility.render.BKEngineShaderEspRenderer.finishFrame();
      dev.bkclient.ui.mainmenu.WorldCarouselPreview.captureIfReady();
   }

   @WrapOperation(method = "renderHand", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"))
   private void bkclient$captureFlameHands(HeldItemRenderer renderer, float tickDelta, MatrixStack matrices,
      VertexConsumerProvider.Immediate consumers, ClientPlayerEntity player, int light, Operation<Void> original) {
      HandsShader hands = HandsShader.getInstance();
      if (hands == null || !hands.isEnabled() || !hands.isFlame()) {
         original.call(renderer, tickDelta, matrices, consumers, player, light);
         if (CosmeticGlowRenderer.hasPendingGeometry()) {
            consumers.draw();
            CosmeticGlowRenderer.capture();
         }
         return;
      }
      consumers.draw();
      MatrixStack captureMatrices = new MatrixStack();
      captureMatrices.peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
      captureMatrices.peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());
      original.call(renderer, tickDelta, matrices, consumers, player, light);
      consumers.draw();
      CosmeticGlowRenderer.capture();
      FlameHandsRenderer.capture(() -> {
         try {
            original.call(renderer, tickDelta, captureMatrices, consumers, player, light);
         } finally {
            consumers.draw();
         }
      });
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void bkclient$profileFrame(RenderTickCounter counter, boolean tick, CallbackInfo ci) {
      MeleeCosmeticRenderer.beginVisualFrame();
      CosmeticGlowRenderer.beginFrame();
      dev.bkclient.systems.modules.modules.visuals.FreeLook.refresh();
      dev.bkclient.systems.modules.modules.other.PerformanceProfiler.frame();
   }

   /**
    * Stretches the world projection for the Aspect Ratio module.
    *
    * <p>This one matrix feeds both the world pass and the held-item pass, so the hand follows the
    * stretch without a second hook. Modified in place because vanilla hands out the same instance
    * it keeps.
    */
   @ModifyReturnValue(method = "getBasicProjectionMatrix(F)Lorg/joml/Matrix4f;", at = @At("RETURN"))
   private Matrix4f bkclient$stretchProjection(Matrix4f original) {
      AspectRatio.apply(original);
      return original;
   }

   @Inject(
      method = "renderWorld",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V")
   )
   private void beforeRenderHand(RenderTickCounter tickCounter, CallbackInfo ci) {
      HandsShader hands = HandsShader.getInstance();
      if (hands == null || !hands.isEnabled()) {
         return;
      }

      if (!HandShaderHelper.isInitialized()) {
         HandShaderHelper.initIfNeeded();
      }
      if (hands.isGlassHideHand()) {
         RenderSystem.colorMask(false, false, false, false);
      }
      hands.captureBefore();
   }

   @Inject(
      method = "renderWorld",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V",
         shift = At.Shift.AFTER
      )
   )
   private void afterRenderHand(RenderTickCounter tickCounter, CallbackInfo ci) {
      HandsShader hands = HandsShader.getInstance();
      if (hands != null && hands.isEnabled()) {
         if (hands.isGlassHideHand()) {
            RenderSystem.colorMask(true, true, true, true);
         }
         HandShaderHelper.checkFramebuffers();
         hands.render();
      }

      CosmeticGlowRenderer.composite();
      ViewModel viewModel = ViewModel.getInstance();
      if (viewModel != null) {
         viewModel.renderHandOutline();
      }
   }

   @Inject(
      method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand")
   )
   private void onRenderWorld(
      RenderTickCounter tickCounter,
      CallbackInfo ci,
      @Local(ordinal = 0) Matrix4f projection,
      @Local(ordinal = 2) Matrix4f view,
      @Local(ordinal = 1) float tickDelta,
      @Local MatrixStack matrices
   ) {
      Utils.onRender(view, projection);
   }

   @Inject(method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), cancellable = true)
   private void tiltViewWhenHurtHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getHurtCam().isSelected()) {
         ci.cancel();
      }
   }

   @Redirect(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
   private float renderWorldHook(float delta, float first, float second) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      return removals.isEnabled() && removals.getNausea().isSelected() ? 0.0F : MathHelper.lerp(delta, first, second);
   }
}
