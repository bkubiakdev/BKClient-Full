package dev.bkclient.combatengine.client.modules;

import dev.bkclient.combatengine.AuraServices;
import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.events.EventInvoker;
import dev.bkclient.combatengine.api.utils.animation.AnimationUtils;
import dev.bkclient.combatengine.api.utils.animation.Easings;
import dev.bkclient.combatengine.api.utils.notification.NotificationManager;
import dev.bkclient.combatengine.client.modules.settings.Setting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Generated;

public abstract class Module implements QClient {
   private String recoveredField1;
   private String recoveredField2;
   private int recoveredField3;
   private Module.ModuleCategory recoveredField4;
   private boolean recoveredField5;
   private boolean recoveredField6;
   private final List<Setting> recoveredField7 = new ArrayList<>();
   private final AnimationUtils recoveredField8 = new AnimationUtils(60.0F, 11.0F, Easings.LINEAR);
   private final AnimationUtils recoveredField9 = new AnimationUtils(0.0F, 11.0F, Easings.LINEAR);

   public Module(String name, String description, Module.ModuleCategory category) {
      this.recoveredField1 = name;
      this.recoveredField2 = description;
      this.recoveredField4 = category;
      this.recoveredField3 = -1;
   }

   public Module(String name, Module.ModuleCategory category) {
      this.recoveredField1 = name;
      this.recoveredField2 = "NULLABLE";
      this.recoveredField4 = category;
      this.recoveredField3 = -1;
   }

   public void onEnable() {
      this.recoveredField6 = true;
      EventInvoker.register(this);
      this.recoveredField8.update(1.0F);
      NotificationManager.push(this.recoveredField1, this.recoveredField4.getIcons(), true);
   }

   public void onDisable() {
      this.recoveredField6 = false;
      EventInvoker.unregister(this);
      this.recoveredField8.update(0.0F);
      NotificationManager.push(this.recoveredField1, this.recoveredField4.getIcons(), false);
   }

   public void toggle() {
      this.recoveredField6 = !this.recoveredField6;
      if (this.recoveredField6) {
         this.onEnable();
      } else {
         this.onDisable();
      }
   }

   public void setEnabled(boolean state) {
      boolean var2 = this.recoveredField6;
      this.recoveredField6 = state;

      try {
         if (state) {
            this.onEnable();
         } else if (var2) {
            this.onDisable();
         }
      } catch (Exception var4) {
         this.recoveredField6 = false;
         this.onDisable();
      }
   }

   public void addSettings(Setting... settings) {
      if (settings != null && settings.length != 0) {
         Arrays.stream(settings).filter(Objects::nonNull).forEach(this.recoveredField7::add);
      }
   }

   public String getDisplayName() {
      return AuraServices.INSTANCE.localizationStorage == null ? this.recoveredField1 : AuraServices.INSTANCE.localizationStorage.translate(this.recoveredField1);
   }

   public String getDisplayDescription() {
      return AuraServices.INSTANCE.localizationStorage == null ? this.recoveredField2 : AuraServices.INSTANCE.localizationStorage.translate(this.recoveredField2);
   }

   @Generated
   public String getName() {
      return this.recoveredField1;
   }

   @Generated
   public String getDescription() {
      return this.recoveredField2;
   }

   @Generated
   public int getKey() {
      return this.recoveredField3;
   }

   @Generated
   public Module.ModuleCategory getCategory() {
      return this.recoveredField4;
   }

   @Generated
   public boolean isOpen() {
      return this.recoveredField5;
   }

   @Generated
   public boolean isEnable() {
      return this.recoveredField6;
   }

   @Generated
   public List<Setting> getSettings() {
      return this.recoveredField7;
   }

   @Generated
   public AnimationUtils getAnimka() {
      return this.recoveredField8;
   }

   @Generated
   public AnimationUtils getArrayAnimka() {
      return this.recoveredField9;
   }

   @Generated
   public void setName(String name) {
      this.recoveredField1 = name;
   }

   @Generated
   public void setDescription(String description) {
      this.recoveredField2 = description;
   }

   @Generated
   public void setKey(int key) {
      this.recoveredField3 = key;
   }

   @Generated
   public void setCategory(Module.ModuleCategory category) {
      this.recoveredField4 = category;
   }

   @Generated
   public void setOpen(boolean isOpen) {
      this.recoveredField5 = isOpen;
   }

   @Generated
   public void setEnable(boolean enable) {
      this.recoveredField6 = enable;
   }

   public enum ModuleCategory {
      COMBAT("Combat", "b"),
      MOVEMENT("Movement", "c"),
      RENDER("Render", "d"),
      MODELS("Models", "f"),
      MISC("Misc", "h"),
      PLAYER("Player", "e"),
      AUTOBUY("AutoBuy", "j");

      private final String recoveredField1;
      private final String recoveredField2;

      @Generated
      ModuleCategory(final String name, final String icons) {
         this.recoveredField1 = name;
         this.recoveredField2 = icons;
      }

      @Generated
      public String getName() {
         return this.recoveredField1;
      }

      @Generated
      public String getIcons() {
         return this.recoveredField2;
      }
   }
}
