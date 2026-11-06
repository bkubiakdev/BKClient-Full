package dev.bkclient.utility.animation.types;

import lombok.NonNull;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;

public class ColorAnimation {
   private static final Easing DEFAULT_EASING = Easing.CUBIC_IN_OUT;
   private final long duration;
   private final Animation r;
   private final Animation g;
   private final Animation b;
   private final Animation a;
   private ColorRGBA cachedColor;
   private int cachedR = Integer.MIN_VALUE;
   private int cachedG = Integer.MIN_VALUE;
   private int cachedB = Integer.MIN_VALUE;
   private int cachedA = Integer.MIN_VALUE;

   public ColorAnimation(long duration, Easing easing) {
      this.duration = duration;
      this.r = new Animation(duration, easing);
      this.g = new Animation(duration, easing);
      this.b = new Animation(duration, easing);
      this.a = new Animation(duration, easing);
   }

   public ColorAnimation(long duration) {
      this(duration, DEFAULT_EASING);
   }

   public ColorAnimation(long duration, ColorRGBA initalColor, Easing easing) {
      this.duration = duration;
      this.r = new Animation(duration, initalColor.getRed(), easing);
      this.g = new Animation(duration, initalColor.getGreen(), easing);
      this.b = new Animation(duration, initalColor.getBlue(), easing);
      this.a = new Animation(duration, initalColor.getAlpha(), easing);
   }

   public ColorAnimation(long duration, ColorRGBA initalColor) {
      this(duration, initalColor, DEFAULT_EASING);
   }

   public void update(@NonNull ColorRGBA targetColor) {
      if (targetColor == null) {
         throw new NullPointerException("targetColor is marked non-null but is null");
      }

      this.r.update(targetColor.getRed());
      this.g.update(targetColor.getGreen());
      this.b.update(targetColor.getBlue());
      this.a.update(targetColor.getAlpha());
   }

   public ColorRGBA getColor() {
      int red = (int)this.r.getValue();
      int green = (int)this.g.getValue();
      int blue = (int)this.b.getValue();
      int alpha = (int)this.a.getValue();
      if (this.cachedColor == null || red != this.cachedR || green != this.cachedG || blue != this.cachedB || alpha != this.cachedA) {
         this.cachedR = red;
         this.cachedG = green;
         this.cachedB = blue;
         this.cachedA = alpha;
         this.cachedColor = new ColorRGBA(red, green, blue, alpha);
      }
      return this.cachedColor;
   }

   public void setEasing(Easing easing) {
      this.r.setEasing(easing);
      this.g.setEasing(easing);
      this.b.setEasing(easing);
      this.a.setEasing(easing);
   }

   public void setDuration(long duration) {
      this.r.setDuration(duration);
      this.g.setDuration(duration);
      this.b.setDuration(duration);
      this.a.setDuration(duration);
   }

   public void setColor(@NonNull ColorRGBA color) {
      if (color == null) {
         throw new NullPointerException("color is marked non-null but is null");
      }

      this.r.setValue(color.getRed());
      this.g.setValue(color.getGreen());
      this.b.setValue(color.getBlue());
      this.a.setValue(color.getAlpha());
      this.cachedColor = null;
   }
}
