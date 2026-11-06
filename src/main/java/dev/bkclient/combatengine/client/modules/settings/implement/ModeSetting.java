package dev.bkclient.combatengine.client.modules.settings.implement;

import dev.bkclient.combatengine.AuraServices;
import dev.bkclient.combatengine.client.modules.settings.Setting;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import lombok.Generated;

public class ModeSetting extends Setting {
   private List<String> recoveredField1;
   private String recoveredField2;
   private int recoveredField3;
   private boolean recoveredField4;

   public ModeSetting(String name, String current, String... modes) {
      super(name);
      this.recoveredField1 = Arrays.asList(modes);
      this.recoveredField3 = this.recoveredField1.indexOf(current);
      if (this.recoveredField3 < 0) {
         this.recoveredField3 = 0;
      }

      this.recoveredField2 = this.recoveredField1.get(this.recoveredField3);
   }

   public void set(String selected) {
      int var2 = this.recoveredField1.indexOf(selected);
      if (var2 >= 0) {
         this.recoveredField2 = selected;
         this.recoveredField3 = var2;
      }
   }

   public boolean is(String mode) {
      return this.recoveredField2.equals(mode);
   }

   public String displayMode(String mode) {
      return AuraServices.INSTANCE.localizationStorage == null ? mode : AuraServices.INSTANCE.localizationStorage.translate(mode);
   }

   public String displayCurrent() {
      return this.displayMode(this.recoveredField2);
   }

   public ModeSetting visible(Supplier<Boolean> state) {
      this.visible = state;
      return this;
   }

   @Generated
   public List<String> getMods() {
      return this.recoveredField1;
   }

   @Generated
   public String getCurrent() {
      return this.recoveredField2;
   }

   @Generated
   public int getIndex() {
      return this.recoveredField3;
   }

   @Generated
   public boolean isExpanded() {
      return this.recoveredField4;
   }

   @Generated
   public void setMods(List<String> mods) {
      this.recoveredField1 = mods;
   }

   @Generated
   public void setCurrent(String current) {
      this.recoveredField2 = current;
   }

   @Generated
   public void setIndex(int index) {
      this.recoveredField3 = index;
   }

   @Generated
   public void setExpanded(boolean expanded) {
      this.recoveredField4 = expanded;
   }
}
