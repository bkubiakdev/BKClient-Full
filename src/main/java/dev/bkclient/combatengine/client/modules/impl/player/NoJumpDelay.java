package dev.bkclient.combatengine.client.modules.impl.player;

import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventUpdate;
import dev.bkclient.combatengine.client.modules.Module;
import dev.bkclient.combatengine.mixin.ILivingEntity;

public class NoJumpDelay extends Module {
   public static NoJumpDelay INSTANCE = new NoJumpDelay();

   public NoJumpDelay() {
      super("NoJumpDelay", "Убирает задержку на прыжок", Module.ModuleCategory.PLAYER);
   }

   @EventLink
   public void onEvent(EventUpdate event) {
      if (mc.player != null && mc.world != null) {
         ((ILivingEntity)mc.player).setJumpingCooldown(0);
      }
   }
}
