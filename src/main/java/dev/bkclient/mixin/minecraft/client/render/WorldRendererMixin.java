package dev.bkclient.mixin.minecraft.client.render;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.modules.visuals.CustomFog;
import dev.bkclient.systems.modules.modules.visuals.GlassVapor;
import dev.bkclient.systems.modules.modules.visuals.JumpCircles;
import dev.bkclient.systems.modules.modules.visuals.ViewModel;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.DynamicLightUtility;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.BlockRenderView;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements IMinecraft {
   @Inject(
      method = "render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
      at = @At("HEAD")
   )
   private void bkclient$beginBlizzard(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean outline,
      Camera camera, GameRenderer renderer, Matrix4f view, Matrix4f projection, CallbackInfo ci) {
      var blizzard = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.Blizzard.class);
      if (blizzard != null) blizzard.beginFrame(camera, tickCounter.getTickDelta(false));
   }
   // Cyberspace supplies its own sky layers. Keep the user's cloud option intact
   // so switching the mode or disabling Sky restores vanilla clouds immediately.
   @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
   private void bkclient$cyberspaceClouds(CallbackInfo ci) {
      var manager = BKClientInit.getInstance().getModuleManager();
      if (manager == null) return;
      var sky = manager.getModule(dev.bkclient.systems.modules.modules.visuals.Sky.class);
      if (sky != null && sky.isEnabled() && "Cyberspace".equals(sky.getSkyEffectMode())) ci.cancel();
   }

   @Inject(
      method = "render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
      at = @At("RETURN")
   )
   private void render(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      CallbackInfo ci
   ) {
      Profilers.get().swap(BKClientInit.MOD_ID + "_renderWorld");
      dev.bkclient.utility.render.ambience.FogDebugCapture.beginFrame(positionMatrix, projectionMatrix);
      ViewModel viewModel = ViewModel.getInstance();
      if (viewModel != null) {
         viewModel.beginHandFrame();
      }
      // Snow is world geometry: atmosphere and distortion must affect it together with terrain.
      var blizzard = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.Blizzard.class);
      if (blizzard != null) blizzard.renderWorld(positionMatrix, projectionMatrix, camera);
      var ambience = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.Ambience.class);
      if (ambience != null) ambience.renderAtmosphere(positionMatrix, projectionMatrix, camera, tickCounter.getTickDelta(false));
      var sky = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.Sky.class);
      if (sky != null) sky.renderWorldFlash();
      this.applyCustomFogDepthBlur(camera);
      this.applyJumpDistortPost(positionMatrix, projectionMatrix, camera);
      this.applyGlassVaporPost(positionMatrix, projectionMatrix, camera);
      MatrixStack matrices = new MatrixStack();
      matrices.multiplyPositionMatrix(positionMatrix);
      BKClientInit.getInstance()
         .getEventManager()
         .triggerEvent(new Render3DEvent(matrices, positionMatrix, projectionMatrix, camera, tickCounter.getTickDelta(false)));
      dev.bkclient.utility.render.ambience.FogDebugCapture.endFrame();
   }

   // Hot path of chunk building and entity lighting: ModifyReturnValue avoids allocating a CallbackInfoReturnable per call.
   @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At("RETURN"))
   private static int applyDynamicFullbright(int original, BlockRenderView world, BlockState state, BlockPos pos) {
      return DynamicLightUtility.apply(pos, original);
   }

   private void applyCustomFogDepthBlur(Camera camera) {
      CustomFog customFog = BKClientInit.getInstance().getModuleManager().getModule(CustomFog.class);
      if (customFog != null && DrawUtility.fogBlurPost != null && customFog.shouldApplyDepthBlur(camera)) {
         float fogStart = customFog.getDistance().getFirstValue();
         float fogEnd = customFog.getDistance().getSecondValue();
         if (!(fogEnd <= fogStart)) {
            DrawUtility.fogBlurPost
               .apply(
                  fogStart,
                  fogEnd,
                  customFog.getBlurStrength().getCurrentValue(),
                  8,
                  customFog.getBlurOffset().getCurrentValue(),
                  0.0F,
                  customFog.getNoSkyBlur().isEnabled()
               );
         }
      }
   }


   
   private void applyGlassVaporPost(Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera) {
      GlassVapor glassVapor = BKClientInit.getInstance().getModuleManager().getModule(GlassVapor.class);
      if (glassVapor != null) {
         glassVapor.onRenderWorldPost(positionMatrix, projectionMatrix, camera);
      }
   }

   private void applyJumpDistortPost(Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera) {
      JumpCircles jumpCircles = BKClientInit.getInstance().getModuleManager().getModule(JumpCircles.class);
      if (jumpCircles != null) {
         jumpCircles.onRenderWorldPost(positionMatrix, projectionMatrix, camera);
      }
   }
}
