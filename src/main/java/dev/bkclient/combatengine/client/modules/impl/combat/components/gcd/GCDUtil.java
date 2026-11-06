package dev.bkclient.combatengine.client.modules.impl.combat.components.gcd;

import dev.bkclient.combatengine.api.QClient;

public class GCDUtil implements QClient {
   public static float getFixedRotation(float rot) {
      return getDeltaMouse(rot) * getGCDValue();
   }

   public static float getGCDValue() {
      return (float)(getGCD() * 0.15);
   }

   public static float getGCD() {
      double var0 = 0.5;
      if (mc != null && mc.options != null) {
         var0 = mc.options.getMouseSensitivity().getValue();
      }

      double var2 = var0 * 0.6F + 0.2F;
      return (float)(var2 * var2 * var2 * 8.0);
   }

   public static float getDeltaMouse(float delta) {
      return Math.round(delta / getGCDValue());
   }
}
