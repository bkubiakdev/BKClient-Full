package dev.bkclient.combatengine.client.modules.settings;

import dev.bkclient.combatengine.AuraServices;
import dev.bkclient.combatengine.api.QClient;
import java.awt.Color;
import java.util.function.Supplier;
import lombok.Generated;

public abstract class Setting implements QClient {
   private final String recoveredField1;
   public Supplier<Boolean> visible = () -> true;
   public Color color = Color.WHITE;

   public Setting(String name) {
      this.recoveredField1 = name;
   }

   public Boolean visible() {
      return this.visible.get();
   }

   public String displayName() {
      return AuraServices.INSTANCE.localizationStorage == null ? this.recoveredField1 : AuraServices.INSTANCE.localizationStorage.translate(this.recoveredField1);
   }

   @Generated
   public String name() {
      return this.recoveredField1;
   }

   @Generated
   public Color color() {
      return this.color;
   }
}
