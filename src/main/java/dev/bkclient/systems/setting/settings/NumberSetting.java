package dev.bkclient.systems.setting.settings;

import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.function.BooleanSupplier;

public class NumberSetting extends AbstractSetting {
   private double value;
   private final boolean integer;

   public NumberSetting(SettingsContainer container, String name, double defaultValue, boolean integer,
                        BooleanSupplier hideCondition) {
      super(container, name, hideCondition);
      this.value = defaultValue;
      this.integer = integer;
   }

   public NumberSetting(SettingsContainer container, String name, double defaultValue, boolean integer) {
      this(container, name, defaultValue, integer, () -> false);
   }

   public double getValue() {
      return this.value;
   }

   public int getIntValue() {
      return (int)this.value;
   }

   public void setValue(double value) {
      this.value = value;
   }

   public boolean isInteger() {
      return this.integer;
   }

   @Override
   public JsonElement save() {
      return new JsonPrimitive(this.value);
   }

   @Override
   public void load(JsonElement element) {
      if (element != null && element.isJsonPrimitive()) {
         this.value = element.getAsDouble();
      }
   }
}
