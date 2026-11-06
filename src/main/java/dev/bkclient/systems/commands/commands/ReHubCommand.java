package dev.bkclient.systems.commands.commands;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.CommandContext;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.game.server.ServerUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.time.Timer;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;

public class ReHubCommand implements IMinecraft {
   private boolean processing;
   private final Timer timer = new Timer();
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (this.processing
         && mc.world != null
         && mc.player != null
         && (ServerUtility.isFT() || ServerUtility.isFT())
         && mc.world.getDifficulty() == Difficulty.EASY
         && this.timer.finished(1000L)) {
         mc.player.networkHandler.sendChatCommand("an" + ServerUtility.ftAn);
         this.timer.reset();
         this.processing = false;
      }
   };

   public ReHubCommand() {
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   public Command command() {
      return CommandBuilder.begin("rct").aliases("reconnect").desc("commands.rehub.description").handler(this::handle).build();
   }

   private void handle(CommandContext ctx) {
      if (mc.player != null && mc.world != null) {
         if (ServerUtility.hasCT) {
            MessageUtility.error(Text.of(Localizator.translate("commands_rehub.ct")));
         } else {
            this.timer.reset();
            mc.player.networkHandler.sendChatCommand("hub");
            this.processing = true;
         }
      }
   }
}
