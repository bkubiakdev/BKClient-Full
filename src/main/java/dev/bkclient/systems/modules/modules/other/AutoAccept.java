package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.utility.game.server.ServerUtility;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "Auto Accept", category = ModuleCategory.OTHER, desc = "Automatically accepts teleport requests")
public class AutoAccept extends BaseModule {
   private final ModeSetting acceptMode = new ModeSetting(this, "Accept");
   private final ModeSetting.Value acceptAll = new ModeSetting.Value(this.acceptMode, "Everyone");
   private final ModeSetting.Value friendsOnly = new ModeSetting.Value(this.acceptMode, "Friends only");
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet
         && mc.player != null
         && packet.content().getString().contains("телепортироваться")
         && !ServerUtility.hasCT
         && this.canAccept(packet.content().getString())) {
         mc.player.networkHandler.sendChatCommand("tpaccept");
      }
   };

   private boolean canAccept(String message) {
      if (this.acceptMode.is(this.acceptAll)) {
         return true;
      }

      if (this.acceptMode.is(this.friendsOnly)) {
         if (BKClientInit.getInstance().getFriendManager().isFriend(message.split(" ")[1])
            || BKClientInit.getInstance()
               .getFriendManager()
               .isFriend(message.replace("\u0a77 просит телепортироваться к Вам.\u0a77§l [ੲ§l✔\u0a77§l]\u0a77§l [\u0a7c§l✗\u0a77§l]", "").replace("੶", ""))
            || BKClientInit.getInstance().getFriendManager().isFriend(message.replace("➝ Nickname: ", ""))) {
            return true;
         }

         if (message.contains("телепортироваться")) {
            String[] parts = message.split(" ");
            return parts.length >= 2 && BKClientInit.getInstance().getFriendManager().isFriend(parts[2]);
         }
      }

      return false;
   }
}
