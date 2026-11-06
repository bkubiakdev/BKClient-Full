package dev.bkclient.systems.modules.modules.player;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.GameTickEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.inventory.AutoEating;

@ModuleInfo(name = "Auto Eat", category = ModuleCategory.PLAYER)
public class AutoEat extends BaseModule {
   private final AutoEating eating = new AutoEating();
   private final SliderSetting food = new SliderSetting(this, "modules.settings.auto_eat.food").step(1.0F).min(1.0F).max(20.0F).currentValue(15.0F);
   private final EventListener<GameTickEvent> onGameTick = event -> eating.validate(mc, isEnabled());
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> tickEating();

   public boolean tickEating() {
      if (isEnabled()) {
         MineHelper mine = BKClientInit.getInstance().getModuleManager().getModule(MineHelper.class);
         if (mine != null) mine.stopEating();
      }
      return eating.tick(mc, isEnabled(), (int) food.getCurrentValue());
   }

   @Override
   public void onDisable() {
      eating.stop(mc);
      super.onDisable();
   }

   public boolean shouldPauseMovement() { return eating.isActive(); }
}
