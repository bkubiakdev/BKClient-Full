package dev.bkclient.systems.commands.commands;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;

public final class RpcCommand {
   public Command command() {
      return CommandBuilder.begin("rpc").handler(context -> {
         boolean enabled = BKClientInit.getInstance().getDiscordManager().toggle();
         MessageUtility.info(Text.of("Discord RPC: " + (enabled ? "enabled" : "disabled")));
      }).build();
   }
}
