package dev.bkclient.mixin.minecraft.world;

import dev.bkclient.BKClientInit;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.bkclient.systems.modules.modules.visuals.Ambience;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.LightmapTextureManager;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public class MixinLightmapTextureManager {
   @org.spongepowered.asm.mixin.Shadow private boolean dirty;
   @org.spongepowered.asm.mixin.Unique private boolean bk$nightWasActive;
   @org.spongepowered.asm.mixin.Unique private float bk$nightR, bk$nightG, bk$nightB, bk$nightStrength;

   @Inject(method = "update(F)V", at = @At("HEAD"))
   private void bk$refreshNightTint(float tickDelta, CallbackInfo ci) {
      var manager = BKClientInit.getInstance().getModuleManager();
      var ambience = manager == null ? null : manager.getModule(Ambience.class);
      boolean active = ambience != null && ambience.isNightModeActive();
      // Vanilla already rebuilds the lightmap every tick; force a rebuild only when the tint or strength changed
      // (theme-synced colours animate), or when night mode was just switched on or off.
      if (active) {
         Vector3f tint = ambience.getNightModeTint();
         float strength = ambience.getNightModeStrengthValue();
         if (!bk$nightWasActive || tint.x != bk$nightR || tint.y != bk$nightG || tint.z != bk$nightB || strength != bk$nightStrength) {
            dirty = true; bk$nightR = tint.x; bk$nightG = tint.y; bk$nightB = tint.z; bk$nightStrength = strength;
         }
      } else if (bk$nightWasActive) dirty = true;
      bk$nightWasActive = active;
   }


   // Gamma brightness only replaces the value the lightmap reads, so the Brightness option
   // keeps its real value in the options screen and in options.txt.
   @ModifyExpressionValue(
      method = "update(F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 0),
      slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getGamma()Lnet/minecraft/client/option/SimpleOption;"))
   )
   private Object bk$gammaBrightness(Object gamma) {
      var manager = BKClientInit.getInstance().getModuleManager();
      var ambience = manager == null ? null : manager.getModule(Ambience.class);
      return ambience != null && ambience.shouldUseGammaBrightness() ? (Object)1337.0 : gamma;
   }

   @Inject(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/SimpleFramebuffer;beginWrite(Z)V"))
   private void bkclient$applyNightModeUniforms(float tickDelta, CallbackInfo info, @Local ShaderProgram shaderProgram) {
      Ambience ambience = BKClientInit.getInstance().getModuleManager() == null ? null : BKClientInit.getInstance().getModuleManager().getModule(Ambience.class);
      if (ambience != null && ambience.isNightModeActive()) {
         this.setNightModeUniforms(shaderProgram, ambience.getNightModeTint(), ambience.getNightModeStrengthValue());
      } else {
         this.setNightModeUniforms(shaderProgram, new Vector3f(1.0F, 1.0F, 1.0F), 0.0F);
      }
   }

   private void setNightModeUniforms(ShaderProgram shaderProgram, Vector3f tint, float strength) {
      GlUniform tintUniform = shaderProgram.getUniform("BKClientNightTint");
      GlUniform strengthUniform = shaderProgram.getUniform("BKClientNightStrength");
      if (tintUniform != null) {
         tintUniform.set(tint);
      }

      if (strengthUniform != null) {
         strengthUniform.set(strength);
      }
   }
}
