package dev.bkclient.ui.components;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

/** MusicPlayer's thin accent rail and small white thumb, shared by settings sliders. */
public final class MusicSlider {
   public static final float RAIL_HEIGHT = 1.5F, THUMB_WIDTH = 5.5F, THUMB_HEIGHT = 3.5F;
   private MusicSlider() {}
   public static void draw(UIContext c, float x, float y, float width, float start, float end,
         boolean range, boolean thumb, float alpha) {
      if (width <= 0) return;
      float a = width * Math.clamp(start, 0, 1), b = width * Math.clamp(end, 0, 1);
      float centerY = y + 1;
      drawRail(c, x, centerY, width, start, end, Colors.getAccent(), alpha);
      if (thumb) drawThumb(c, x, centerY, width, b, alpha);
      if (range && thumb) drawThumb(c, x, centerY, width, a, alpha);
   }
   /** Shared rail without a draggable thumb; y is the vertical center. */
   public static void drawRail(dev.bkclient.framework.base.CustomDrawContext c, float x, float y,
         float width, float start, float end, ColorRGBA fill, float alpha) {
      if (width <= 0) return;
      float a = width * Math.clamp(start, 0, 1), b = width * Math.clamp(end, 0, 1);
      c.drawSquircle(x, y - RAIL_HEIGHT / 2, width, RAIL_HEIGHT, 2, BorderRadius.all(RAIL_HEIGHT / 2),
         Colors.getTextColor().withAlpha(27 * alpha));
      if (b > a) c.drawSquircle(x + a, y - RAIL_HEIGHT / 2, b - a, RAIL_HEIGHT, 2,
         BorderRadius.all(Math.min(RAIL_HEIGHT, b-a) / 2), fill.mulAlpha(alpha));
   }

   private static void drawThumb(UIContext c, float x, float cy, float width, float offset, float alpha) {
      float w = Math.min(THUMB_WIDTH, width), h = THUMB_HEIGHT;
      float tip = x + Math.clamp(offset, w / 2, width - w / 2);
      c.drawShadow(tip-w/2, cy-h/2, w, h, 5, BorderRadius.all(1.5F), ColorRGBA.BLACK.withAlpha(64 * alpha));
      c.drawSquircle(tip-w/2, cy-h/2, w, h, 7, BorderRadius.all(Math.min(1.5F,w/2)), ColorRGBA.WHITE.mulAlpha(alpha));
   }
}
