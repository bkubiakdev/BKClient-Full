package dev.bkclient.systems.discord;

import dev.bkclient.BKClientPreLaunch;
import dev.bkclient.utility.interfaces.IMinecraft;


public class DiscordManager implements IMinecraft {
   public void connect() { }

   public static boolean toggle() {
      BKClientPreLaunch.enabled = !BKClientPreLaunch.enabled;
      if (!BKClientPreLaunch.enabled) {
         BKClientPreLaunch.executor.execute(BKClientPreLaunch::clearPresence);
      } else if (BKClientPreLaunch.connected) {
         BKClientPreLaunch.executor.execute(BKClientPreLaunch::sendPresence);
      } else {
         BKClientPreLaunch.executor.execute(BKClientPreLaunch::tryConnect);
      }
      return BKClientPreLaunch.enabled;
   }

   public void shutdown() { BKClientPreLaunch.shutdown(); }
}
