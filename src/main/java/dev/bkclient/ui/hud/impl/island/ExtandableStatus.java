package dev.bkclient.ui.hud.impl.island;

import dev.bkclient.systems.setting.settings.SelectSetting;

public class ExtandableStatus extends IslandStatus {
   public ExtandableStatus(SelectSetting setting, String name) {
      super(setting, name);
   }

   @Override
   public boolean canShow() {
      return false;
   }
}
