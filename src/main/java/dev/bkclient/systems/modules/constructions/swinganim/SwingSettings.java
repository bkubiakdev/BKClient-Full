package dev.bkclient.systems.modules.constructions.swinganim;

import java.util.ArrayList;
import java.util.List;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.SettingsContainer;

public class SwingSettings implements SettingsContainer {
   protected final List<Setting> settings = new ArrayList<>();

   @Override
   public List<Setting> getSettings() {
      return this.settings;
   }
}
