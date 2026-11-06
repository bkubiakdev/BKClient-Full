package dev.bkclient.framework.objects.gradient.impl;

import dev.bkclient.framework.objects.gradient.Gradient;
import dev.bkclient.utility.colors.ColorRGBA;

public class HorizontalGradient extends Gradient {
   public HorizontalGradient(ColorRGBA startColor, ColorRGBA endColor) {
      super(startColor, startColor, endColor, endColor);
   }

   public HorizontalGradient rotate() {
      return new HorizontalGradient(this.bottomRightColor, this.topLeftColor);
   }
}
