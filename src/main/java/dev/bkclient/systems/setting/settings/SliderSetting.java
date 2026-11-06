package dev.bkclient.systems.setting.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import lombok.Generated;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class SliderSetting extends AbstractSetting {
   protected float min;
   protected float max;
   protected float step;
   protected float currentValue;
   private SliderSetting.Suffix suffix = number -> "";
   private int multiplier = 1;
   private int maxMultiplier = 1;

   public SliderSetting(@NotNull SettingsContainer parent, String name, String description, @NotNull BooleanSupplier hideCondition) {
      super(parent, name, hideCondition);
   }

   public SliderSetting(@NotNull SettingsContainer parent, String name, @NotNull BooleanSupplier hideCondition) {
      super(parent, name, hideCondition);
   }

   public SliderSetting(@NotNull SettingsContainer parent, String name, String description) {
      super(parent, name);
   }

   public SliderSetting(@NotNull SettingsContainer parent, String id) {
      super(parent, id);
   }

   public SliderSetting min(float min) {
      this.min = min;
      return this;
   }

   public SliderSetting max(float max) {
      this.max = max;
      return this;
   }

   public SliderSetting step(float step) {
      this.step = step;
      this.setCurrentValue(this.currentValue);
      return this;
   }

   public SliderSetting suffix(SliderSetting.Suffix suffix) {
      this.suffix = suffix;
      return this;
   }

   public SliderSetting suffix(String suffix) {
      this.suffix = number -> suffix;
      return this;
   }

   public SliderSetting currentValue(float currentValue) {
      this.setCurrentValue(currentValue);
      return this;
   }

   /** The suffix function; together with the current value and language it determines {@link #getSuffix()}. */
   public SliderSetting.Suffix getSuffixFunction() {
      return this.suffix;
   }

   public String getSuffix() {
      return this.suffix.apply(this.getCurrentValue()).contains(" ")
         ? " " + Localizator.translate(this.suffix.apply(this.getCurrentValue()).replace(" ", ""))
         : Localizator.translate(this.suffix.apply(this.getCurrentValue()));
   }

   @Override
   public JsonElement save() {
      if (this.maxMultiplier <= 1) return new JsonPrimitive(this.currentValue);
      JsonObject object = new JsonObject();
      object.addProperty("value", this.currentValue);
      object.addProperty("multiplier", this.multiplier);
      return object;
   }

   @Override
   public void load(JsonElement element) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
         this.setCurrentValue(element.getAsFloat());
      } else if (element.isJsonObject()) {
         JsonObject object = element.getAsJsonObject();
         if (object.has("value")) this.setCurrentValue(object.get("value").getAsFloat());
         if (object.has("multiplier")) this.multiplier = MathHelper.clamp(object.get("multiplier").getAsInt(), 1, this.maxMultiplier);
      }
   }

   public void setCurrentValue(float currentValue) {
      if (!Float.isFinite(currentValue)) return;
      // Builders may set the default before step(). Rounding with step=0
      // turned every such default into zero/minimum through division by infinity.
      float rounded = Float.isFinite(this.step) && this.step > 0F
         ? (float)(Math.round(currentValue / (double)this.step) * (double)this.step)
         : currentValue;
      this.currentValue = MathHelper.clamp(rounded, this.min, this.max);
   }

   public void setCurrentValueAndSave(float currentValue) {
      this.setCurrentValue(currentValue);
      saveConfig();
   }

   @Generated
   public float getMin() {
      return this.min;
   }

   @Generated
   public float getMax() {
      return this.max;
   }

   @Generated
   public float getStep() {
      return this.step;
   }

   @Generated
   public float getCurrentValue() {
      return this.currentValue;
   }

   public int getMultiplier() { return this.multiplier; }

   public int getMaxMultiplier() { return this.maxMultiplier; }

   public void cycleMultiplier() {
      this.multiplier = this.multiplier >= this.maxMultiplier ? 1 : this.multiplier + 1;
   }

   public SliderSetting maxMultiplier(int maxMultiplier) {
      this.maxMultiplier = Math.max(1, maxMultiplier);
      this.multiplier = MathHelper.clamp(this.multiplier, 1, this.maxMultiplier);
      return this;
   }

   public interface Suffix {
      String apply(float var1);
   }
}
