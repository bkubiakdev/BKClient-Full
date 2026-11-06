package dev.bkclient.combatengine;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.combatengine.api.utils.tps.TPSCalc;

/** Host services; combat implementation lives in the unmodified source package. */
public final class AuraServices {
   public static final AuraServices INSTANCE = new AuraServices();
   public final Friends friendStorage = new Friends();
   public final Localization localizationStorage = new Localization();
   public final TPSCalc tpsCalc = new TPSCalc();

   public static final class Friends {
      public boolean isFriend(String name) {
         var client = BKClientInit.getInstance();
         return client != null && client.getFriendManager() != null && client.getFriendManager().isFriend(name);
      }
   }
   public static final class Localization {
      public String translate(String text) { return Localizator.translate(text); }
   }
}
