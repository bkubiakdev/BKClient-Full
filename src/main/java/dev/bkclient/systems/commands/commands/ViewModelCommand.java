package dev.bkclient.systems.commands.commands;

import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.CommandContext;
import dev.bkclient.systems.modules.modules.visuals.ViewModel;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;

public class ViewModelCommand {
   public Command command() {
      return CommandBuilder.begin("vm", b -> b.aliases("viewmodel").desc("commands.viewmodel.description")
         .param("action", p -> p.literal("reset"))
         .handler(this::handle))
         .build();
   }

   private void handle(CommandContext ctx) {
      ViewModel viewModel = ViewModel.getInstance();
      if (viewModel == null) {
         MessageUtility.error(Text.of("ViewModel module is not available"));
         return;
      }
      viewModel.resetLayout();
      MessageUtility.info(Text.of("ViewModel layout has been reset"));
   }
}
