package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.waypoints.WayPointsManager;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;

@ModuleInfo(name = "Death Cords", category = ModuleCategory.OTHER, desc = "Sends death coordinates to chat")
public class DeathCords extends BaseModule {
   private boolean death;
   private final BooleanSetting wayDeath = new BooleanSetting(this, "Create waypoint");
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (!(mc.currentScreen instanceof DeathScreen) || mc.player == null) {
         this.death = true;
      } else if (this.death) {
         int xCord = (int)mc.player.getX();
         int yCord = (int)mc.player.getY();
         int zCord = (int)mc.player.getZ();
         MessageUtility.info(Text.of("Death coordinates: " + xCord + " " + yCord + " " + zCord));
         if (this.wayDeath.isEnabled()) {
            WayPointsManager wayPointsManager = BKClientInit.getInstance().getWayPointsManager();
            if (wayPointsManager.contains("Death")) {
               wayPointsManager.del("Death");
            }

            wayPointsManager.add("Death", xCord, yCord, zCord);
         }

         this.death = false;
      }
   };
}
