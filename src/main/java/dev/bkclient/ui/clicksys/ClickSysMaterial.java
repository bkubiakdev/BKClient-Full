package dev.bkclient.ui.clicksys;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.ui.clicksys.port.module.client.ThemeModule;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.DrawUtility;
import org.joml.Matrix4f;

/** Uses BKClient's glass renderer only inside the current clicksys render pass. */
public final class ClickSysMaterial {
   private static UIContext context;
   private static final java.util.Deque<UIContext> contexts = new java.util.ArrayDeque<>();
   private static long cursorTime;
   private static float cursorX, cursorY;
   public static float cursorX() { return cursorX; }
   public static float cursorY() { return cursorY; }
   private ClickSysMaterial() { }
   public static boolean enabled() {
      var theme = ThemeModule.getThemeModule();
      return context != null && theme != null && theme.isEnabled() && theme.liquidPanels.isFlag();
   }
   public static void begin(UIContext ctx) {
      contexts.push(ctx); context = ctx;
      if (contexts.size() > 1) return;
      var mc = net.minecraft.client.MinecraftClient.getInstance();
      var theme = ThemeModule.getThemeModule();
      long now = System.nanoTime();
      float x = (float)(mc.mouse.getX() * mc.getFramebuffer().textureWidth / Math.max(1,mc.getWindow().getWidth()));
      float y = (float)((1-mc.mouse.getY()/Math.max(1,mc.getWindow().getHeight())) * mc.getFramebuffer().textureHeight);
      float dt = cursorTime == 0 ? 1 : Math.min(1,(now-cursorTime)/1_000_000_000f);
      float t = theme != null && theme.effect("Cursor inertia") ? 1-(float)Math.exp(-dt*12) : 1;
      cursorX += (x-cursorX)*t; cursorY += (y-cursorY)*t; cursorTime=now;
      if (theme != null && theme.check4()) {
         ctx.draw();
         dev.bkclient.ui.clicksys.port.render.MipmapCapture.update2();
         dev.bkclient.ui.clicksys.port.render.MipmapCapture.getInt();
      }
   }
   public static void end() {
      if(!contexts.isEmpty()) contexts.pop();
      context=contexts.peek();
   }
   public static UIContext context() { return context; }
   /** Shared focus treatment for native fields and the embedded market editor. */
   public static void inputField(Matrix4f matrix, float x, float y, float width, float height, float focus, float alpha) {
      int fill = dev.bkclient.ui.clicksys.port.api.Theme.background();
      int border = dev.bkclient.ui.clicksys.port.data.AnimatedInt.getIntByIntFloatInt(
         dev.bkclient.ui.clicksys.port.api.Theme.primary(), focus,
         panelBorder(fill, dev.bkclient.ui.clicksys.port.api.Theme.border()));
      ShapeShader.onFloatFloatFloatMatrix4fFloatIntFloatFloatIntFloat(
         1, width, alpha, matrix, y, fill, 8, x, border, height);
   }
   public static void logo(Matrix4f matrix, float x, float y, float size, float alpha, int tint) {
      if (context == null) return;
      context.draw(); context.getMatrices().push();
      try {
         context.getMatrices().peek().getPositionMatrix().set(matrix);
         DrawUtility.drawTextureMinified(context.getMatrices(), net.minecraft.util.Identifier.of("bkclient", "textures/gui/bk-logo.png"), x,y,size,size, ColorRGBA.fromInt(tint).mulAlpha(alpha));
      } finally { context.getMatrices().pop(); }
   }
   public static void friendAvatar(Matrix4f matrix, String name, float x, float y, float size, float alpha) {
      if (context == null) return;
      var skin = dev.bkclient.utility.render.PlayerHeadSkins.get(name);
      context.draw(); context.getMatrices().push();
      try {
         context.getMatrices().peek().getPositionMatrix().set(matrix);
         var tint = ColorRGBA.WHITE.mulAlpha(alpha);
         DrawUtility.drawCrispPlayerHead(context.getMatrices(), skin, x,y,size,BorderRadius.all(10),tint);
      } finally { context.getMatrices().pop(); }
   }
   public static boolean avatar(Matrix4f matrix, float x, float y, float alpha) {
      if (context == null) return false;
      var mc = net.minecraft.client.MinecraftClient.getInstance();
      var streamer = dev.bkclient.systems.modules.modules.other.StreamerMode.getInstance();
      var manager = dev.bkclient.BKClientInit.getInstance().getModuleManager();
      var protect = manager == null ? null : manager.getModule(dev.bkclient.systems.modules.modules.other.NameProtect.class);
      boolean hidden = streamer != null && streamer.isEnabled()
          || protect != null && protect.isEnabled() && protect.hideSkin.isEnabled();
      var skin = hidden ? dev.bkclient.BKClientInit.id("textures/skin/anon_skin.png")
          : mc.player != null ? mc.player.getSkinTextures().texture()
          : dev.bkclient.utility.render.PlayerHeadSkins.get(mc.getGameProfile().getName());
      context.draw(); context.getMatrices().push();
      try {
         context.getMatrices().peek().getPositionMatrix().set(matrix);
         DrawUtility.drawCrispPlayerHead(context.getMatrices(),skin,x,y,32,BorderRadius.all(16),ColorRGBA.WHITE.mulAlpha(alpha));
      } finally { context.getMatrices().pop(); }
      return true;
   }
   /** Thin monochrome track and outlined thumb matching the reference ClickGUI. */
   public static void slider(Matrix4f matrix, float x, float y, float width, float progress, float alpha) {
      if (width <= 0) return;
      float h = 6, kw = Math.min(width, 12), kh = 8;
      float fill = width * Math.clamp(progress, 0, 1);
      int rail = dev.bkclient.ui.clicksys.port.api.Theme.elevated();
      int foreground = dev.bkclient.ui.clicksys.port.api.Theme.foreground();
      ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(3, x, rail, matrix, h, width, alpha, y);
      if (fill > 0) ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(Math.min(3, fill/2), x, foreground, matrix, h, fill, alpha * .7F, y);
      float tip = x + Math.clamp(fill, kw/2, width-kw/2);
      ShapeShader.onFloatFloatFloatMatrix4fFloatIntFloatFloatIntFloat(1, kw, alpha, matrix, y+h/2-kh/2,
         dev.bkclient.ui.clicksys.port.api.Theme.background(), 4, tip-kw/2, foreground, kh);
   }
   public static void rangeSlider(Matrix4f matrix, float x, float y, float width, float low, float high, float alpha) {
      if (width <= 0) return;
      float h = 6, kw = Math.min(width, 12), kh = 8;
      float start = width * Math.clamp(low, 0, 1), end = width * Math.clamp(high, 0, 1);
      int rail = dev.bkclient.ui.clicksys.port.api.Theme.elevated();
      int accent = dev.bkclient.ui.clicksys.port.api.Theme.foreground();
      ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(h/2, x, rail, matrix, h, width, alpha, y);
      if (end > start) ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(Math.min(h/2, (end-start)/2), x+start, accent, matrix, h, end-start, alpha * .7F, y);
      for (float position : new float[]{start,end}) {
         float tip = x + Math.clamp(position, kw/2, width-kw/2);
         ShapeShader.onFloatFloatFloatMatrix4fFloatIntFloatFloatIntFloat(1, kw, alpha, matrix, y+h/2-kh/2,
            dev.bkclient.ui.clicksys.port.api.Theme.background(), 4, tip-kw/2, accent, kh);
      }
   }
   public static int layer(int color, float opacity) {
      return enabled() ? ShapeShader.getIntByIntFloat(color, opacity) : color;
   }
   /** Dark windows need only a quiet edge; keep light surfaces unchanged. */
   public static int panelBorder(int fill, int border) {
      ColorRGBA surface = ColorRGBA.fromInt(fill);
      float luminance = surface.getRed() * .2126F + surface.getGreen() * .7152F + surface.getBlue() * .0722F;
      if (luminance >= 128) return border;
      ColorRGBA edge = ColorRGBA.fromInt(border);
      return surface.mix(edge, .25F).withAlpha(edge.getAlpha()).getRGB();
   }
   public static void panel(Matrix4f matrix, float x, float y, float w, float h, float radius, int fill, int border, float alpha) {
      border = panelBorder(fill, border);
      var theme = ThemeModule.getThemeModule();
      if (context != null && enabled()) {
         dev.bkclient.ui.clicksys.port.render.TextShader.update2();
         context.draw();
         boolean blend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
         boolean cull = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
         context.getMatrices().push();
         try {
            context.getMatrices().peek().getPositionMatrix().set(matrix);
            DrawUtility.drawLiquidGlass(context.getMatrices(), x,y,w,h, BorderRadius.all(radius), ColorRGBA.WHITE,
               alpha, 26, ColorRGBA.WHITE.withAlpha(90), 1, true, .12f,
               theme.liquidRefraction.getValueAsFloat(), 2, false);
         } finally {
            context.getMatrices().pop();
            // The port can hold a ShapeShader batch across this native draw.
            if (blend) com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            else com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            if (cull) com.mojang.blaze3d.systems.RenderSystem.enableCull();
            else com.mojang.blaze3d.systems.RenderSystem.disableCull();
         }
         fill = ShapeShader.getIntByIntFloat(fill, theme.liquidOpacity.getValueAsFloat());
      }
      ColorRGBA surface = ColorRGBA.fromInt(fill);
      boolean dark = surface.getRed() * .2126F + surface.getGreen() * .7152F + surface.getBlue() * .0722F < 128;
      ShapeShader.onFloatFloatFloatMatrix4fFloatIntFloatFloatIntFloat(dark ? .5F : 1, w, alpha, matrix, y, fill, radius, x, border, h);
   }
}
