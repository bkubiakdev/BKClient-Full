package dev.bkclient.combatengine.client.modules.settings.implement;

import dev.bkclient.combatengine.client.modules.settings.Setting;
import java.util.function.Supplier;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;

public class FloatSetting extends Setting {
   private float recoveredField1;
   private final float recoveredField2;
   private final float recoveredField3;
   private final float recoveredField4;
   private boolean recoveredField5;

   public FloatSetting(String name, float value, float min, float max, float increment) {
      super(name);
      this.recoveredField1 = value;
      this.recoveredField2 = min;
      this.recoveredField3 = max;
      this.recoveredField4 = increment;
   }

   public Number getValue() {
      return MathHelper.clamp(this.recoveredField1, this.getMin(), this.getMax());
   }

   public void setValue(float value) {
      this.recoveredField1 = MathHelper.clamp(value, this.getMin(), this.getMax());
   }

   public float get() {
      return this.getValue().floatValue();
   }

   public FloatSetting visible(Supplier<Boolean> state) {
      this.visible = state;
      return this;
   }

   @Generated
   public float getMin() {
      return this.recoveredField2;
   }

   @Generated
   public float getMax() {
      return this.recoveredField3;
   }

   @Generated
   public float getIncrement() {
      return this.recoveredField4;
   }

   @Generated
   public boolean isActive() {
      return this.recoveredField5;
   }

   @Generated
   public void setActive(boolean active) {
      this.recoveredField5 = active;
   }
}
