package dev.bkclient.systems.modules.modules.player;

import lombok.Generated;
import dev.bkclient.mixin.minecraft.client.IMinecraftClient;
import dev.bkclient.mixin.accessors.LivingEntityAccessor;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;

@ModuleInfo(name = "No Delay", category = ModuleCategory.PLAYER, desc = "modules.descriptions.no_delay")
public class NoDelay extends BaseModule {
   private final BooleanSetting jump = new BooleanSetting(this, "modules.settings.no_delay.jump", "modules.settings.no_delay.jump.description").enable();
   private final BooleanSetting rightClick = new BooleanSetting(
      this, "modules.settings.no_delay.right_click", "modules.settings.no_delay.right_click.description"
   );

   @Override
   public void tick() {
      if (this.jump.isEnabled() && mc.player != null) {
         ((LivingEntityAccessor) mc.player).setJumpingCooldown(0);
      }
      if (this.rightClick.isEnabled()) {
         ((IMinecraftClient)mc).setUseCooldown(0);
      }

      super.tick();
   }

   @Generated
   public BooleanSetting getJump() {
      return this.jump;
   }

   @Generated
   public BooleanSetting getRightClick() {
      return this.rightClick;
   }
}
