package dev.bkclient.combatengine.client.modules.settings.implement;

import dev.bkclient.combatengine.client.modules.settings.Setting;
import java.util.function.Supplier;
import lombok.Generated;

public class BooleanSetting extends Setting {
   private boolean recoveredField1;
   private int recoveredField2 = -1;

   public BooleanSetting(String name, boolean state) {
      super(name);
      this.recoveredField1 = state;
   }

   public static BooleanSetting of(String name, boolean state) {
      return new BooleanSetting(name, state);
   }

   public BooleanSetting visible(Supplier<Boolean> state) {
      this.visible = state;
      return this;
   }

   public boolean getValue() {
      return this.recoveredField1;
   }

   @Generated
   public boolean isState() {
      return this.recoveredField1;
   }

   @Generated
   public int getKey() {
      return this.recoveredField2;
   }

   @Generated
   public void setState(boolean state) {
      this.recoveredField1 = state;
   }

   @Generated
   public void setKey(int key) {
      this.recoveredField2 = key;
   }
}
