package dev.bkclient.combatengine;

import dev.bkclient.combatengine.v3.AuraV3Math;

/** Regression checks for the Java port of aura-working's pure engine. */
public final class AuraV3MathTest {
   private static void check(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }

   public static void main(String[] args) {
      check(AuraV3Math.wrap(181.0F) == -179.0F, "yaw wrapping");
      check(AuraV3Math.clampPitch(120.0F) == 90.0F, "pitch clamping");

      AuraV3Math.HitBox box = new AuraV3Math.HitBox(-.3, 0, -.3, .3, 1.8, .3);
      AuraV3Math.AimPoint point = AuraV3Math.resolveAimPoint(box, 0, 1.6, -3, 3, 120, 9);
      check(point.y() >= box.minY() && point.y() <= box.maxY(), "aim point stays in hitbox");

      AuraV3Math.MotionTrack track = new AuraV3Math.MotionTrack();
      track.observe(0, 0, 0, 0, .05);
      track.observe(1, 0, 0, 50, .05);
      check(track.valid() && track.speed() > 0, "motion velocity is observed");
      double[] predicted = track.predict(2, .35);
      check(predicted[0] > 1, "motion is led forward");

      AuraV3Math.SmoothTracker smoother = new AuraV3Math.SmoothTracker();
      AuraV3Math.Rotation current = new AuraV3Math.Rotation(0, 0);
      AuraV3Math.Rotation target = new AuraV3Math.Rotation(90, 20);
      AuraV3Math.Rotation next = smoother.step(current, target, .05F, .15F);
      check(next.yaw() > 0 && next.yaw() < 90 && next.pitch() > 0, "critically damped response");

      AuraV3Math.Backoff backoff = new AuraV3Math.Backoff();
      backoff.reject();
      check(backoff.blocked(), "rejected swing backs off");
      backoff.accept();
      check(!backoff.blocked(), "confirmed swing releases backoff");
      System.out.println("AuraV3 math: wrapping, hitbox aim, prediction, smoothing and backoff passed");
   }
}
