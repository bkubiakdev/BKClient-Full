package dev.bkclient.systems.commands.commands;

import java.util.List;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.CommandContext;
import dev.bkclient.systems.commands.ValidationResult;
import dev.bkclient.systems.friends.FriendManager;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;

public class FriendCommand {
   public Command command() {
      return CommandBuilder.begin(
            "friend",
            b -> b.aliases("friends")
               .desc("commands.friends.description")
               .param("action", p -> p.literal("add", "remove", "del", "delete", "clear", "list"))
               .param("id", p -> p.optional().validator(ValidationResult::ok))
               .handler(this::handle)
         )
         .build();
   }

   private void handle(CommandContext ctx) {
      String action = (String)ctx.arguments().get(0);
      String id = (String)ctx.arguments().get(1);
      FriendManager fm = BKClientInit.getInstance().getFriendManager();
      String var5 = action.toLowerCase();
      switch (var5) {
         case "add":
            fm.add(id);
            break;
         case "remove":
         case "del":
         case "delete":
            fm.remove(id);
            break;
         case "clear":
            fm.clear();
            break;
         case "list":
            this.printList();
      }
   }

   private void printList() {
      List<String> friends = BKClientInit.getInstance().getFriendManager().listFriends();
      if (friends.isEmpty()) {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.empty")));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.list")));

         for (int i = 0; i < friends.size(); i++) {
            MessageUtility.info(Text.of("[" + (i + 1) + "] " + friends.get(i)));
         }
      }
   }
}
