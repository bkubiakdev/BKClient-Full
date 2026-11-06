package dev.bkclient.systems.modules.impl;

import java.util.ArrayList;
import java.util.List;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.systems.localization.Language;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.sounds.ClientSounds;

public abstract class BaseModule implements Module {
   private final ModuleInfo info = this.getClass().getAnnotation(ModuleInfo.class);
   private int key;
   private ModuleCategory category;
   private boolean enabled;
   private boolean hidden;
   private boolean silentToggle;
   private String name;
   private List<Setting> settings = new ArrayList<>();
   private final Animation keybindsAnimation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   public BaseModule() {
      this.name = this.info.name();
      this.category = this.info.category();
      this.key = this.info.key();
   }

   @Override
   public void toggle() {
      this.setEnabled(!this.enabled, false);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void tick() {
   }

   public List<Setting> getDisplaySettings() { return this.settings; }

   protected boolean shouldAutoSaveOnToggle() { return true; }

   @Override
   public void disable() {
      this.setEnabled(false, false);
   }

   @Override
   public void enable() {
      this.setEnabled(true, false);
   }

   @Override
   public void setEnabled(boolean newState, boolean silent) {
      if (newState && BKClientInit.getInstance().isPanic()) return;
      if (this.enabled != newState) {
         this.enabled = newState;
         this.silentToggle = silent;
         if (!(this instanceof MenuModule) && BKClientInit.getInstance().getModuleManager().getModule(Sounds.class).isEnabled() && !silent) {
            if (net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof dev.bkclient.ui.clicksys.ClickSysScreen) {
               dev.bkclient.ui.clicksys.ClickSysSounds.play(this.enabled
                  ? dev.bkclient.ui.clicksys.port.enums.SoundEvent.MODULE_ENABLE
                  : dev.bkclient.ui.clicksys.port.enums.SoundEvent.MODULE_DISABLE);
            } else {
               ClientSounds.MODULE.play(BKClientInit.getInstance().getModuleManager().getModule(Sounds.class).getVolume().getCurrentValue(), this.enabled ? 1.1F : 1.0F);
            }
         }

         if (this.enabled) {
            BKClientInit.getInstance().getEventManager().subscribe(this);
            if (!silent) {
               BKClientInit.getInstance()
                  .getNotificationManager()
                  .addNotification(
                     NotificationType.SUCCESS,
                     this.name.replace(" ", "")
                        + " "
                        + Localizator.translate("enabled")
                        + (Localizator.getCurrentLanguage() == Language.RU_RU ? TextUtility.makeGender(this.name) : "")
                  );
            }

            this.onEnable();
         } else {
            BKClientInit.getInstance().getEventManager().unsubscribe(this);
            if (!silent) {
               BKClientInit.getInstance()
                  .getNotificationManager()
                  .addNotification(
                     NotificationType.ERROR,
                     this.name.replace(" ", "")
                        + " "
                        + Localizator.translate("disabled")
                        + (Localizator.getCurrentLanguage() == Language.RU_RU ? TextUtility.makeGender(this.name) : "")
                  );
            }

            this.onDisable();
         }
      }
   }

   /**
    * True while the enable/disable currently in progress was triggered silently
    * (e.g. config/autosave restore) rather than by an explicit user action.
    * Valid only from within {@link #onEnable()} / {@link #onDisable()}.
    */
   protected boolean isSilentToggle() {
      return this.silentToggle;
   }

   public String getSettingName(String key) {
      return "modules.settings." + this.getName().toLowerCase().replace(" ", "_") + "." + key;
   }

   @Generated
   @Override
   public ModuleInfo getInfo() {
      return this.info;
   }

   @Generated
   @Override
   public int getKey() {
      return this.key;
   }

   @Generated
   @Override
   public ModuleCategory getCategory() {
      return this.category;
   }

   @Generated
   @Override
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   @Override
   public boolean isHidden() {
      return this.hidden;
   }

   @Generated
   @Override
   public String getName() {
      return this.name;
   }

   @Generated
   @Override
   public List<Setting> getSettings() {
      return this.settings;
   }

   @Generated
   @Override
   public Animation getKeybindsAnimation() {
      return this.keybindsAnimation;
   }

   @Generated
   @Override
   public void setKey(int key) {
      this.key = key;
   }

   @Generated
   public void setCategory(ModuleCategory category) {
      this.category = category;
   }

   @Generated
   public void setEnabled(boolean enabled) {
      if (enabled && BKClientInit.getInstance().isPanic()) return;
      this.enabled = enabled;
   }

   @Generated
   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   @Generated
   public void setName(String name) {
      this.name = name;
   }

   @Generated
   public void setSettings(List<Setting> settings) {
      this.settings = settings;
   }
}
