package dev.bkclient.ui.hud.impl;

import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.event.impl.render.PostHudRenderEvent;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.RingRenderer;
import dev.bkclient.utility.render.Utils;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public final class WaypointRenderer implements IMinecraft {
   private WaypointRenderer() {}
   public static void draw(PostHudRenderEvent event, Vec3d position, String name, ColorRGBA accent, long appearedAt) {
      Vec2f screen = Utils.worldToScreen(position);
      if (screen == null) return;
      float screenWidth = mc.getWindow().getScaledWidth();
      float screenHeight = mc.getWindow().getScaledHeight();
      if (screen.x < 0 || screen.x > screenWidth || screen.y < 0 || screen.y > screenHeight) return;

      var context = event.getContext();
      Vec3d playerPosition = Utils.getInterpolatedPos(mc.player, event.getTickDelta());
      double meters = playerPosition.distanceTo(position);
      // Shrink around the projected world position, keeping a readable lower bound.
      float scale = (float)(0.45 + 0.25 / (1 + meters / 40.0));
      screen = new Vec2f(screen.x / scale, screen.y / scale);
      screenWidth /= scale;
      screenHeight /= scale;
      context.getMatrices().push();
      try {
      context.getMatrices().scale(scale, scale, 1);
      var titleFont = Fonts.SEMIBOLD.getFont(8);
      var distanceFont = Fonts.MEDIUM.getFont(7);
      String distance = Math.round(meters) + " m";
      float alpha = Math.clamp((System.nanoTime() - appearedAt) / 280_000_000.0F, 0, 1);
      alpha = 1 - (1 - alpha) * (1 - alpha) * (1 - alpha);
      float width = Math.min(Math.min(160, screenWidth-10), Math.max(80, 49 + Math.max(titleFont.width(name), distanceFont.width(distance))));
      float x = Math.clamp(screen.x - width / 2, 5, Math.max(5, screenWidth - width - 5));
      float y = Math.clamp(screen.y - 54 + 5 * (1 - alpha), 5, Math.max(5, screenHeight - 43));

      var radius = BorderRadius.all(11);

      context.drawLine(new Vec2f(x + width / 2, y + 38), new Vec2f(screen.x, screen.y), accent.withAlpha(85 * alpha));
      // Use the existing glass shader, including when the regular HUD uses another style.
      DrawUtility.drawLiquidGlass(context.getMatrices(), x, y, width, 38, radius,
         ColorRGBA.WHITE.withAlpha(255 * alpha), alpha, 26, ColorRGBA.WHITE,
         1, true, 0, 0.035F, 2, false);
      context.drawRoundedRect(x, y, width, 38, radius, new ColorRGBA(16, 20, 27).withAlpha(145 * alpha));
      context.drawRoundedRect(x + 8, y + 8, 22, 22, BorderRadius.all(8), accent.withAlpha(28 * alpha));
      RingRenderer.progress(context.getMatrices(), x + 19, y + 19, 5, 0.8F, 1, 0,
         accent.withAlpha(0), accent.withAlpha(235 * alpha));
      context.drawRoundedRect(x + 17.5F, y + 17.5F, 3, 3, BorderRadius.all(1.5F), accent.withAlpha(255 * alpha));
      context.drawFadeoutText(titleFont, name, x + 38, y + 8, ColorRGBA.WHITE.withAlpha(245 * alpha), 0.8F, 1, width-47);
      context.drawText(distanceFont, distance, x + 38, y + 22, ColorRGBA.WHITE.withAlpha(165 * alpha));
      RingRenderer.progress(context.getMatrices(), screen.x, screen.y, 4, 0.75F, 1, 0,
         accent.withAlpha(0), accent.withAlpha(210 * alpha));
      context.drawRoundedRect(screen.x - 1.5F, screen.y - 1.5F, 3, 3, BorderRadius.all(1.5F), accent.withAlpha(255 * alpha));
      } finally { context.getMatrices().pop(); }
   }
}
