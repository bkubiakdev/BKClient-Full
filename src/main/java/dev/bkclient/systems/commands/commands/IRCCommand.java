package dev.bkclient.systems.commands.commands;

import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;

public class IRCCommand {
   public Command command() {
      return CommandBuilder.begin("irc", b -> b.desc("Communication between users").handler(ctx -> {})).build();
   }
}
