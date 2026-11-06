package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.inventory.InventoryUtility;
import dev.bkclient.utility.inventory.ItemSlot;
import dev.bkclient.utility.inventory.slots.HotbarSlot;
import dev.bkclient.utility.inventory.slots.OffhandSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@ModuleInfo(name = "Auto GApple", category = ModuleCategory.COMBAT, desc = "modules.descriptions.auto_gapple")
public class AutoGapple extends BaseModule {
   private boolean eating;
   private ItemSlot swappedSlot = null;
   private ItemStack savedOffhandItem = null;
   private final SliderSetting healthEat = new SliderSetting(this, "modules.settings.auto_gapple.health").step(1.0F).min(1.0F).max(20.0F).currentValue(15.0F);
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (mc.player != null && mc.interactionManager != null) {
         float totalHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         boolean shouldEat = totalHealth <= this.healthEat.getCurrentValue();
         OffhandSlot offhand = InventoryUtility.getOffHandSlot();
         ItemStack offhandStack = offhand.itemStack();
         if (!shouldEat || mc.player.getItemCooldownManager().isCoolingDown(Items.GOLDEN_APPLE.getDefaultStack())) {
            this.stopEating();
            if (this.savedOffhandItem != null && this.swappedSlot != null) {
               InventoryUtility.moveToOffHand(this.swappedSlot);
               this.savedOffhandItem = null;
               this.swappedSlot = null;
            }
         } else if (this.isGoldenApple(offhandStack)) {
            this.startEating();
         } else {
            ItemSlot gappleSlot = this.findGappleSlot();
            if (gappleSlot != null) {
               if (offhand.isEmpty()) {
                  InventoryUtility.moveToOffHand(gappleSlot);
                  this.startEating();
               } else if (this.savedOffhandItem == null) {
                  this.savedOffhandItem = offhand.itemStack().copy();
                  this.swappedSlot = gappleSlot;
                  InventoryUtility.moveToOffHand(gappleSlot);
                  this.startEating();
               }
            }
         }
      }
   };

   private void startEating() {
      this.eating = true;
      mc.options.useKey.setPressed(true);
   }

   private void stopEating() {
      if (this.eating) {
         mc.options.useKey.setPressed(false);
         this.eating = false;
      }
   }

   private boolean isGoldenApple(ItemStack itemStack) {
      Item item = itemStack.getItem();
      return item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE;
   }

   private ItemSlot findGappleSlot() {
      for (int i = 0; i < 9; i++) {
         HotbarSlot slot = InventoryUtility.getHotbarSlot(i);
         if (slot.contains(Items.GOLDEN_APPLE) || slot.contains(Items.ENCHANTED_GOLDEN_APPLE)) {
            return slot;
         }
      }

      return null;
   }

   @Override
   public void onDisable() {
      this.stopEating();
      if (this.savedOffhandItem != null && this.swappedSlot != null) {
         InventoryUtility.moveToOffHand(this.swappedSlot);
         this.savedOffhandItem = null;
         this.swappedSlot = null;
      }

      super.onDisable();
   }
}
