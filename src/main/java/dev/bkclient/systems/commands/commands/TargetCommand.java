package dev.bkclient.systems.commands.commands;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.CommandContext;
import dev.bkclient.systems.commands.ValidationResult;
import dev.bkclient.systems.target.TargetManager;

public class TargetCommand {
   public Command command() {
      return CommandBuilder.begin(
            "target",
            b -> b.aliases("targets")
               .desc("Manage the target list")
               .param("action", p -> p.literal("add", "remove", "del", "delete", "clear", "list"))
               .param("id", p -> p.optional().validator(ValidationResult::ok))
               .handler(this::handle)
         )
         .build();
   }

   private void handle(CommandContext ctx) {
      String action = (String)ctx.arguments().get(0);
      String id = (String)ctx.arguments().get(1);
      TargetManager tm = BKClientInit.getInstance().getTargetManager();
      String var5 = action.toLowerCase();
      switch (var5) {
         case "add":
            tm.addTarget(id);
            break;
         case "remove":
         case "del":
         case "delete":
            tm.removeTarget(id);
            break;
         case "clear":
            tm.clearTarget();
            break;
         case "list":
            tm.listTarget();
      }
   }
}
