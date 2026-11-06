package dev.bkclient.systems.modules.modules.visuals;

import lombok.Generated;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;

@ModuleInfo(name = "Anti Invisible", category = ModuleCategory.VISUALS)
public class AntiInvisible extends BaseModule {
   private final SliderSetting opacity = new SliderSetting(this, "modules.settings.anti_invisible.opacity")
      .min(10.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(50.0F)
      .suffix(number -> "%");

   public boolean shouldModifyOpacity(EntityRenderState renderState) {
      Entity entity = ((EntityRenderStateAddition)renderState).bkclient$getEntity();
      return entity.isInvisible();
   }

   @Generated
   public SliderSetting getOpacity() {
      return this.opacity;
   }
}
