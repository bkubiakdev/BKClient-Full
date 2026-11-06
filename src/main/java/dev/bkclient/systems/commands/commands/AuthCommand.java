package dev.bkclient.systems.commands.commands;

import java.util.Map;
import java.util.Map.Entry;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.CommandContext;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.other.AutoAuth;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;

public class AuthCommand {
   public Command command() {
      return CommandBuilder.begin("auth", b -> b.aliases("autoAuth", "пароли", "passwords").desc("commands.auth.description").handler(this::handle)).build();
   }

   private void handle(CommandContext ctx) {
      Map<String, String> map = BKClientInit.getInstance().getModuleManager().getModule(AutoAuth.class).listPassword();
      int counter = 1;
      if (map.isEmpty()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.auth.empty")));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.auth.passwords")));

         for (Entry<String, String> entry : map.entrySet()) {
            String nickname = entry.getKey();
            String password = entry.getValue();
            MessageUtility.info(Text.of(counter++ + ") Nickname: " + nickname + " | Password: " + password));
         }
      }
   }
}
