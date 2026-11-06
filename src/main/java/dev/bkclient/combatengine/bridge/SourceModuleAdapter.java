package dev.bkclient.combatengine.bridge;

import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.combatengine.client.modules.Module;
import dev.bkclient.combatengine.client.modules.settings.Setting;
import java.util.ArrayList;
import java.util.List;

/** UI/lifecycle adapter. It never implements targeting, rotations or attacks. */
public abstract class SourceModuleAdapter extends BaseModule {
   protected final Module source;
   private final List<Runnable> settingsSync = new ArrayList<>();

   protected SourceModuleAdapter(Module source) {
      this.source = source;
      source.setName(getName());
      for (Setting setting : source.getSettings()) mount(setting);
      AuraRuntime.attach(this);
   }

   private void mount(Setting setting) {
      String name = setting.name();
      if (setting instanceof dev.bkclient.combatengine.client.modules.settings.implement.BooleanSetting value) {
         var nativeSetting = new BooleanSetting(this, name, () -> !value.visible()).enabled(value.isState());
         settingsSync.add(() -> value.setState(nativeSetting.isEnabled()));
      } else if (setting instanceof dev.bkclient.combatengine.client.modules.settings.implement.FloatSetting value) {
         var nativeSetting = new SliderSetting(this, name, () -> !value.visible())
            .min(value.getMin()).max(value.getMax()).step(value.getIncrement()).currentValue(value.get());
         settingsSync.add(() -> value.setValue(nativeSetting.getCurrentValue()));
      } else if (setting instanceof dev.bkclient.combatengine.client.modules.settings.implement.ModeSetting value) {
         var nativeSetting = new ModeSetting(this, name, () -> !value.visible());
         for (int i = 0; i < value.getMods().size(); i++) {
            var mode = new ModeSetting.Value(nativeSetting, value.getMods().get(i));
            if (i == value.getIndex()) mode.select();
         }
         settingsSync.add(() -> value.set(nativeSetting.getValue().getName()));
      } else if (setting instanceof dev.bkclient.combatengine.client.modules.settings.implement.ListSetting value) {
         var nativeSetting = new SelectSetting(this, name, () -> !value.visible());
         if (source instanceof dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura && name.equals("Checks")) {
            nativeSetting.exclusive("Only critical hits", "Smart criticals");
         }
         for (var option : value.getSettings()) {
            var nativeOption = new SelectSetting.Value(nativeSetting, option.name(), "", () -> !option.visible());
            if (option.isState()) nativeOption.select();
            settingsSync.add(() -> option.setState(nativeOption.isSelected()));
         }
      } else throw new IllegalArgumentException("Unsupported AuraServices setting: " + setting.getClass());
   }

   public final void syncSourceSettings() { settingsSync.forEach(Runnable::run); }
   public final Module sourceModule() { return source; }

   @Override public void onEnable() {
      syncSourceSettings();
      source.setEnabled(true);
      super.onEnable();
   }
   @Override public void onDisable() {
      source.setEnabled(false);
      super.onDisable();
   }
}
