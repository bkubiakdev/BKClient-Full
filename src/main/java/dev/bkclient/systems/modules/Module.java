package dev.bkclient.systems.modules;

import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.Setting;
import java.util.List;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.interfaces.Toggleable;

public interface Module extends Toggleable, IMinecraft, IScaledResolution, SettingsContainer {
   default List<Setting> getDisplaySettings() {
      return this.getSettings();
   }

   void disable();

   void enable();

   void tick();

   ModuleInfo getInfo();

   String getName();

   default String getDescription() {
      String translationKey = "modules.descriptions.%s".formatted(this.getName().toLowerCase().replace(" ", "_"));
      return Localizator.translate(translationKey);
   }

   int getKey();

   ModuleCategory getCategory();

   boolean isEnabled();

   boolean isHidden();

   Animation getKeybindsAnimation();

   void setKey(int var1);

   void setEnabled(boolean var1, boolean var2);
}
