package dev.bkclient.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.CustomFog;
import dev.bkclient.utility.interfaces.IMinecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Translucent fog must not make Sodium discard terrain still visible through it. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
public class RenderSectionManagerMixin implements IMinecraft {
   @ModifyReturnValue(method = {
      "getEffectiveRenderDistance(Lnet/minecraft/client/render/Fog;)F",
      "getEffectiveRenderDistance(Lnet/minecraft/class_9958;)F"
   }, at = @At("RETURN"), require = 0)
   private float keepFogRenderDistance(float distance) {
      if (mc.options == null || mc.gameRenderer == null || BKClientInit.getInstance() == null
          || BKClientInit.getInstance().getModuleManager() == null) return distance;
      CustomFog fog = BKClientInit.getInstance().getModuleManager().getModule(CustomFog.class);
      if (fog == null || !fog.shouldModifyFog(mc.gameRenderer.getCamera())) return distance;
      return Math.max(distance, mc.options.getClampedViewDistance() * 16.0F);
   }
}
