package dev.bkclient.systems.commands.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.CommandContext;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;

public class HelpCommand {
   public Command command() {
      return CommandBuilder.begin("help", b -> b.aliases("помощь", "команды", "commands", "helpme").desc("commands.help.description").handler(this::handle))
         .build();
   }

   private void handle(CommandContext ctx) {
      List<Command> list = new ArrayList<>(BKClientInit.getInstance().getCommandManager().commands());
      list.sort(Comparator.comparing(c -> (String)c.names().getFirst(), String.CASE_INSENSITIVE_ORDER));
      List<String> infos = new ArrayList<>();
      int counter = 1;

      for (Command command : list) {
         infos.add(
            String.format(
               "%d) %s%s - %s",
               counter++,
               BKClientInit.getInstance().getCommandManager().getPrefix(),
               command.names().getFirst(),
               Localizator.translate(command.description())
            )
         );
      }

      MessageUtility.info(Text.of("Available commands:\n" + String.join("\n", infos)));
   }
}
