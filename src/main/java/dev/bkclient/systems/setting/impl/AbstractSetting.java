package dev.bkclient.systems.setting.impl;

import java.util.function.BooleanSupplier;
import lombok.Generated;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.SettingsContainer;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSetting implements Setting {
   private final String name;
   @NotNull
   private final BooleanSupplier hideCondition;

   public static volatile boolean suppressSave;

   public static void saveConfig() {
      if (suppressSave) return;
      if (dev.bkclient.systems.setting.GuiSaveBatch.defer(AbstractSetting.class, AbstractSetting::saveConfig)) return;
      try {
         var client = dev.bkclient.BKClientInit.getInstance();
         if (client == null || client.isPanic() || client.getConfigManager() == null) return;
         var config = client.getConfigManager().getAutoSaveConfig();
         if (config != null) config.save();
      } catch (Exception ignored) {
      }
   }

   public AbstractSetting(@NotNull SettingsContainer parent, String name, @NotNull BooleanSupplier hideCondition) {
      this.name = name;
      this.hideCondition = hideCondition;
      this.register(parent);
   }

   public AbstractSetting(@NotNull SettingsContainer parent, String name) {
      this(parent, name, () -> false);
   }

   @Override
   public void register(SettingsContainer parent) {
      parent.getSettings().add(this);
   }

   @Override
   public String getDescription() {
      return this.getName() + ".description";
   }

   @Generated
   @Override
   public String getName() {
      return this.name;
   }

   @NotNull
   @Generated
   @Override
   public BooleanSupplier getHideCondition() {
      return this.hideCondition;
   }
}
