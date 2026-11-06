package dev.bkclient.systems.commands.commands;

import java.util.List;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.ParameterBuilder;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;

public class ToggleCommand {
   public Command command() {
      List<String> moduleNames = BKClientInit.getInstance().getModuleManager().getModules().stream().map(module -> module.getName().replace(" ", "")).toList();
      return CommandBuilder.begin("toggle")
         .aliases("t")
         .desc("commands.toggle.description")
          .<Module>param("module", p -> p.validator(ParameterBuilder.MODULE).suggests(moduleNames))
         .handler(
            context -> {
               Module module = (Module)context.arguments().getFirst();
               module.toggle();
               MessageUtility.info(
                  Text.of(Localizator.translate("commands.toggle." + (module.isEnabled() ? "enabled" : "disabled"), module.getName()))
               );
            }
         )
         .build();
   }
}
