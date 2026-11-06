package dev.bkclient.mixin.minecraft.client.render;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Removals;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WeatherRendererMixin {
   @Inject(method = "renderWeather(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/util/math/Vec3d;FLnet/minecraft/client/render/Fog;)V", at = @At("HEAD"), cancellable = true)
   private void onRenderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog, CallbackInfo ci) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      var ambience = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.Ambience.class);
      var blizzard = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.Blizzard.class);
      if ((removals.isEnabled() && removals.getWeather().isSelected()) || (ambience != null && ambience.hasCustomRain())
            || (blizzard != null && blizzard.hidesVanillaWeather())) {
         ci.cancel();
      }
   }
}
