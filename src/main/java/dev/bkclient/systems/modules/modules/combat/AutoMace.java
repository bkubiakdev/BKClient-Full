package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.rotations.RotationMath;
import java.util.Comparator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

/**
 * Smashes with the mace while you fall onto someone: when you are falling far enough and an enemy is in reach, the mace
 * is taken into the hand early (so its attack cooldown is already charged), the hit is turned silently at the enemy and
 * sent, and the previous item comes back after landing.
 */
@ModuleInfo(name = "Auto Mace", category = ModuleCategory.COMBAT, desc = "Smashes with the mace when you fall onto a target")
public final class AutoMace extends BaseModule {
   private final SliderSetting range = new SliderSetting(this, "Range").min(2.0F).max(3.5F).step(0.1F).currentValue(3.0F).suffix("blocks");
   private final SliderSetting minFall = new SliderSetting(this, "Min Fall Distance").min(0.0F).max(20.0F).step(0.5F).currentValue(3.0F).suffix("blocks");
   private final BooleanSetting onlyFalling = new BooleanSetting(this, "Only When Falling").enabled(true);
   private final BooleanSetting onlyPlayers = new BooleanSetting(this, "Only Players").enabled(true);
   private final BooleanSetting holdMace = new BooleanSetting(this, "Take Mace From Hotbar").enabled(true);

   private final CombatKit.Aimed aimed = new CombatKit.Aimed();
   private int previousSlot = -1;

   @Override
   public void onDisable() {
      this.aimed.clear();
      this.restoreSlot();
   }

   @Override
   public void tick() {
      if (!CombatKit.canAct()) return;
      var player = CombatKit.mc.player;
      if (player.isOnGround() || player.isTouchingWater() || player.isGliding()) {
         this.aimed.clear();
         this.restoreSlot();
         return;
      }
      if (this.onlyFalling.isEnabled() && player.getVelocity().y >= 0.0) return;
      if (player.fallDistance < this.minFall.getCurrentValue()) return;
      if (this.aimed.busy()) {
         this.aimed.tick();
         return;
      }
      LivingEntity target = this.findTarget(this.range.getCurrentValue());
      if (target == null) return;

      // The mace is taken before the hit so the swap does not reset its attack cooldown at the last moment.
      if (!CombatKit.holding(Items.MACE)) {
         int slot = CombatKit.hotbar(Items.MACE);
         if (!this.holdMace.isEnabled() || slot < 0) return;
         if (this.previousSlot < 0) this.previousSlot = player.getInventory().selectedSlot;
         CombatKit.selectSlot(slot);
         return;
      }
      if (player.getAttackCooldownProgress(0.0F) < 0.95F || player.isUsingItem()) return;
      double reach = this.range.getCurrentValue();
      this.aimed.request(RotationMath.getNearestPoint(target), () -> CombatKit.aimingAt(target, reach + 0.5), () -> {
         if (target.isAlive() && CombatKit.holding(Items.MACE)) CombatKit.attack(target);
      });
   }

   private LivingEntity findTarget(double reach) {
      var player = CombatKit.mc.player;
      return CombatKit.mc.world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(reach + 1.0), entity -> {
         if (entity == player || !entity.isAlive() || entity instanceof ArmorStandEntity || entity.isSpectator()) return false;
         if (this.onlyPlayers.isEnabled() && !(entity instanceof PlayerEntity)) return false;
         if (entity instanceof PlayerEntity other
            && BKClientInit.getInstance().getFriendManager().isFriend(other.getName().getString())) return false;
         return player.distanceTo(entity) <= reach && player.canSee(entity);
      }).stream().min(Comparator.comparingDouble(player::distanceTo)).orElse(null);
   }

   private void restoreSlot() {
      if (this.previousSlot >= 0 && CombatKit.ready()) CombatKit.selectSlot(this.previousSlot);
      this.previousSlot = -1;
   }
}
