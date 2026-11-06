package dev.bkclient.systems.commands.commands;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.*;
import dev.bkclient.systems.modules.modules.visuals.Waypoints;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.text.Text;
import java.util.List;
import java.util.stream.Collectors;

public final class WaypointDeleteCommand {
   public Command command() {
      return CommandBuilder.begin("waypoint").aliases("way", "waypoints")
         .desc("Delete or clear saved waypoints in the current world and dimension")
         .param("action", p -> p.literal("delete", "del", "remove", "clear"))
         .param("name", p -> p.optional().vararg().validator(ValidationResult::ok))
         .handler(this::handle).build();
   }
   private void handle(CommandContext context) {
      String name = ((List<?>)context.arguments().get(1)).stream().map(Object::toString).collect(Collectors.joining(" ")).strip();
      boolean clear = "clear".equalsIgnoreCase(context.arguments().get(0).toString());
      if (clear && !name.isEmpty()) {
         MessageUtility.error(Text.literal("Usage: .waypoint clear")); return;
      }
      if (!clear && name.isEmpty()) {
         MessageUtility.error(Text.literal("Usage: .waypoint delete name")); return;
      }
      if (Waypoints.scope().isEmpty()) {
         MessageUtility.error(Text.literal("Join a world first.")); return;
      }
      try {
         var module = BKClientInit.getInstance().getModuleManager().getModule(Waypoints.class);
         if (clear) {
            int removed = module.store().clear(Waypoints.scope(), Waypoints.dimension());
            module.refresh();
            MessageUtility.info(Text.literal("Cleared " + removed + " waypoints in this world and dimension."));
            return;
         }
         int removed = module.store().removeByName(Waypoints.scope(), Waypoints.dimension(), name);
         if (removed == 0) MessageUtility.error(Text.literal("Waypoint not found here: " + name));
         else {
            module.refresh();
            MessageUtility.info(Text.literal("Deleted waypoint: " + name + (removed > 1 ? " (" + removed + " points)" : "")));
         }
      } catch (Exception failure) {
         MessageUtility.error(Text.literal("Could not save waypoint deletion. Try again."));
      }
   }
}
