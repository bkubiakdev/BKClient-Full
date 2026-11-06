package dev.bkclient.combatengine.api.utils.animation;

import net.minecraft.util.math.MathHelper;

public class AnimationUtils {
   private float recoveredField1;
   private float recoveredField2;
   private float recoveredField3;
   private float recoveredField4;
   private Easing recoveredField5;
   private long recoveredField6;
   private double recoveredField7;
   private boolean recoveredField8;

   public AnimationUtils(float initialValue, float speed, Easing easing) {
      this.recoveredField1 = initialValue;
      this.recoveredField2 = initialValue;
      this.recoveredField3 = initialValue;
      this.recoveredField4 = speed;
      this.recoveredField5 = easing != null ? easing : Easings.LINEAR;
      this.recoveredField6 = 0L;
      this.recoveredField7 = 0.0;
      this.recoveredField8 = false;
   }

   public AnimationUtils(float initialValue, float speed) {
      this(initialValue, speed, Easings.LINEAR);
   }

   public void update(float target) {
      if (this.recoveredField2 != target || !this.recoveredField8) {
         this.recoveredField2 = target;
         this.recoveredField3 = this.recoveredField1;
         this.recoveredField6 = System.nanoTime();
         this.recoveredField7 = 1.0 / this.recoveredField4 * 2.0;
         this.recoveredField8 = true;
      }

      if (this.isDone()) {
         this.recoveredField1 = this.recoveredField2;
         this.recoveredField8 = false;
      } else {
         double var2 = this.recoveredMethod1();
         float var4 = (float)this.recoveredField5.ease(var2);
         this.recoveredField1 = MathHelper.lerp(var4, this.recoveredField3, this.recoveredField2);
      }
   }

   private double recoveredMethod1() {
      if (!this.recoveredField8) {
         return 1.0;
      }

      long var1 = System.nanoTime();
      double var3 = (var1 - this.recoveredField6) / 1.0E9;
      return MathHelper.clamp(var3 / this.recoveredField7, 0.0, 1.0);
   }

   public float getValue() {
      return this.recoveredField1;
   }

   public void setValue(float value) {
      this.recoveredField1 = value;
      this.recoveredField2 = value;
      this.recoveredField3 = value;
      this.recoveredField8 = false;
   }

   public float getTarget() {
      return this.recoveredField2;
   }

   public void setSpeed(float speed) {
      this.recoveredField4 = speed;
      this.recoveredField7 = 1.0 / speed;
   }

   public void setEasing(Easing easing) {
      this.recoveredField5 = easing != null ? easing : Easings.LINEAR;
   }

   public boolean isDone() {
      return this.recoveredMethod1() >= 1.0;
   }

   public boolean isAlive() {
      return !this.isDone();
   }
}
