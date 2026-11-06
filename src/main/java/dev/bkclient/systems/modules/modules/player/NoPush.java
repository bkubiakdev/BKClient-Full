package dev.bkclient.systems.modules.modules.player;

import lombok.Generated;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SelectSetting;

@ModuleInfo(name = "No Push", category = ModuleCategory.PLAYER, desc = "Prevents external push forces")
public class NoPush extends BaseModule {
   private final SelectSetting removePushFrom = new SelectSetting(this, "modules.settings.no_push.remove_from");
   private final SelectSetting.Value entities = new SelectSetting.Value(this.removePushFrom, "modules.settings.no_push.remove_from.entities", "modules.settings.no_push.remove_from.entities.description").select();
   private final SelectSetting.Value fluids = new SelectSetting.Value(this.removePushFrom, "modules.settings.no_push.remove_from.fluids", "modules.settings.no_push.remove_from.fluids.description");
   private final SelectSetting.Value blocks = new SelectSetting.Value(this.removePushFrom, "modules.settings.no_push.remove_from.blocks", "modules.settings.no_push.remove_from.blocks.description").select();

   @Generated
   public SelectSetting getRemovePushFrom() {
      return this.removePushFrom;
   }

   @Generated
   public SelectSetting.Value getEntities() {
      return this.entities;
   }

   @Generated
   public SelectSetting.Value getFluids() {
      return this.fluids;
   }

   @Generated
   public SelectSetting.Value getBlocks() {
      return this.blocks;
   }
}
