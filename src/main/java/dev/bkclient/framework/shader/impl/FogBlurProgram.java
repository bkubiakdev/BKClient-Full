package dev.bkclient.framework.shader.impl;

import dev.bkclient.framework.shader.GlProgram;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class FogBlurProgram extends GlProgram {
   private GlUniform fogParamsUniform;
   private GlUniform blurStrengthUniform;
   private GlUniform blurFloorUniform;
   private GlUniform skipSkyUniform;

   public FogBlurProgram(Identifier identifier) {
      super(identifier, VertexFormats.POSITION_TEXTURE_COLOR);
   }

   public void updateUniforms(float fogStart, float fogEnd, float nearPlane, float farPlane, float blurStrength, float blurFloor, boolean skipSky) {
      if (this.fogParamsUniform != null) {
         this.fogParamsUniform.set(fogStart, fogEnd, nearPlane, farPlane);
      }

      if (this.blurStrengthUniform != null) {
         this.blurStrengthUniform.set(blurStrength);
      }

      if (this.blurFloorUniform != null) {
         this.blurFloorUniform.set(blurFloor);
      }

      if (this.skipSkyUniform != null) {
         this.skipSkyUniform.set(skipSky ? 1.0F : 0.0F);
      }
   }

   @Override
   protected void setup() {
      this.fogParamsUniform = this.findUniform("FogParams");
      this.blurStrengthUniform = this.findUniform("BlurStrength");
      this.blurFloorUniform = this.findUniform("BlurFloor");
      this.skipSkyUniform = this.findUniform("SkipSky");
      super.setup();
   }
}
