package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.target.TargetSettings;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

@ModuleInfo(name = "Hitboxes", category = ModuleCategory.COMBAT, desc = "modules.descriptions.hitboxes")
public class Hitboxes extends BaseModule {
   private final SliderSetting scale = new SliderSetting(this, "size").min(0.0F).max(1.0F).step(0.1F).currentValue(0.3F);
   private final SelectSetting targets = new SelectSetting(this, "targets");
   private final SelectSetting.Value players = new SelectSetting.Value(this.targets, "players").select();
   private final SelectSetting.Value animals = new SelectSetting.Value(this.targets, "animals").select();
   private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets, "mobs").select();
   private final SelectSetting.Value invisibles = new SelectSetting.Value(this.targets, "invisibles").select();
   private final SelectSetting.Value nakedPlayers = new SelectSetting.Value(this.targets, "nakedPlayers").select();
   private final SelectSetting.Value friends = new SelectSetting.Value(this.targets, "friends");

   /** TargetSettings (never mutated after build) for a selection mask; rebuilt only when a selection changes. */
   private record CachedSettings(int mask, TargetSettings settings) {}
   private volatile CachedSettings cached;

   private TargetSettings settings() {
      int mask = (this.players.isSelected() ? 1 : 0)
         | (this.animals.isSelected() ? 2 : 0)
         | (this.mobs.isSelected() ? 4 : 0)
         | (this.invisibles.isSelected() ? 8 : 0)
         | (this.nakedPlayers.isSelected() ? 16 : 0)
         | (this.friends.isSelected() ? 32 : 0);
      CachedSettings current = this.cached;
      if (current != null && current.mask() == mask) {
         return current.settings();
      }
      TargetSettings settings = new TargetSettings.Builder()
         .targetPlayers((mask & 1) != 0)
         .targetAnimals((mask & 2) != 0)
         .targetMobs((mask & 4) != 0)
         .targetInvisibles((mask & 8) != 0)
         .targetNakedPlayers((mask & 16) != 0)
         .targetFriends((mask & 32) != 0)
         .build();
      this.cached = new CachedSettings(mask, settings);
      return settings;
   }

   public boolean shouldModifyHitbox(LivingEntity entity) {
      if (entity instanceof ClientPlayerEntity) {
         return false;
      } else if (entity.isDead()) {
         return false;
      } else {
         return BKClientInit.getInstance().isPanic() ? false : this.settings().isEntityValid(entity);
      }
   }

   @Generated
   public SliderSetting getScale() {
      return this.scale;
   }
}
