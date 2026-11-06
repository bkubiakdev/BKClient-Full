package dev.bkclient.combatengine.client.modules.impl.combat;

import dev.bkclient.combatengine.AuraServices;
import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventUpdate;
import dev.bkclient.combatengine.api.utils.combat.IdealHitUtils;
import dev.bkclient.combatengine.client.modules.Module;
import dev.bkclient.combatengine.client.modules.settings.implement.BooleanSetting;
import dev.bkclient.combatengine.client.modules.settings.implement.ListSetting;
import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class TriggerBot extends Module {
   public static TriggerBot INSTANCE = new TriggerBot();
   private final BooleanSetting recoveredField1 = new BooleanSetting("Только криты", true);
   private final ListSetting recoveredField2 = new ListSetting(
      "Цели",
      new BooleanSetting("Игроки", true),
      new BooleanSetting("Животные", true),
      new BooleanSetting("Мобы", true),
      new BooleanSetting("Невидимки", true),
      new BooleanSetting("Голые игроки", true),
      new BooleanSetting("Друзья", false)
   );
   private LivingEntity recoveredField3;

   public TriggerBot() {
      super("TriggerBot", "Автоматически атакует при наведении на цель", Module.ModuleCategory.COMBAT);
      this.addSettings(this.recoveredField1, this.recoveredField2);
   }

   @Override
   public void onEnable() {
      this.recoveredField3 = null;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.recoveredField3 = null;
      super.onDisable();
   }

   @EventLink
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (mc.targetedEntity instanceof LivingEntity var2 && this.recoveredMethod2(var2)) {
            this.recoveredField3 = var2;
            if (this.recoveredMethod1(var2)) {
               mc.interactionManager.attackEntity(mc.player, var2);
               mc.player.swingHand(Hand.MAIN_HAND);
            }
         } else {
            this.recoveredField3 = null;
         }
      } else {
         this.recoveredField3 = null;
      }
   }

   private boolean recoveredMethod1(LivingEntity entity) {
      if (mc.player == null) {
         return false;
      } else {
         return mc.player.getAttackCooldownProgress(0.5F) <= 0.93F ? false : !this.recoveredField1.isState() || IdealHitUtils.canCritical(entity);
      }
   }

   private boolean recoveredMethod2(LivingEntity entity) {
      if (entity == null || entity == mc.player || !entity.isAlive() || entity.getHealth() <= 0.0F) {
         return false;
      }

      if (entity instanceof ArmorStandEntity) {
         return false;
      }

      if (entity.distanceTo(mc.player) > 3.0F) {
         return false;
      }

      if (entity.isInvisible() && !this.recoveredField2.is("Невидимки")) {
         return false;
      }

      if (entity instanceof PlayerEntity var2) {
         if (AuraServices.INSTANCE.friendStorage.isFriend(var2.getName().getString()) && !this.recoveredField2.is("Друзья")) {
            return false;
         } else {
            return this.recoveredMethod3(var2) ? this.recoveredField2.is("Голые игроки") : this.recoveredField2.is("Игроки");
         }
      } else if (entity instanceof PassiveEntity || entity instanceof CodEntity) {
         return this.recoveredField2.is("Животные");
      } else {
         return entity instanceof HostileEntity ? this.recoveredField2.is("Мобы") : false;
      }
   }

   private boolean recoveredMethod3(PlayerEntity player) {
      return player.getInventory().armor.stream().allMatch(stack -> stack.isEmpty() || stack.isOf(Items.ELYTRA));
   }

   @Generated
   public LivingEntity getTarget() {
      return this.recoveredField3;
   }
}
