package dev.bkclient.combatengine.api.utils.notification;

import dev.bkclient.BKClientInit;

/** Toggle presentation is handled by the owning native BKClient module. */
public final class NotificationManager {
   public static void push(String name, String icon, boolean enabled) {
      BKClientInit.LOGGER.info("[Aura] {} {}", name, enabled ? "enabled" : "disabled");
   }
}
