package dev.bkclient.framework.shader.impl;

import dev.bkclient.framework.shader.GlProgram;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class SkyShaderProgram extends GlProgram {
   private GlUniform timeUniform;
   private GlUniform accentUniform;

   public SkyShaderProgram(Identifier id) {
      super(id, VertexFormats.POSITION_TEXTURE_COLOR);
   }

   public void updateUniforms(float time, ColorRGBA accent) {
      if (this.timeUniform != null) {
         this.timeUniform.set(time);
      }

      if (this.accentUniform != null && accent != null) {
         this.accentUniform.set(accent.getRed() / 255.0F, accent.getGreen() / 255.0F, accent.getBlue() / 255.0F);
      }
   }

   public boolean isReady() {
      return this.backingProgram != null;
   }

   @Override
   protected void setup() {
      this.timeUniform = this.findUniform("Time");
      this.accentUniform = this.findUniform("Accent");
   }
}
