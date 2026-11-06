package dev.bkclient.systems.modules.modules.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.inventory.EnchantmentUtility;
import dev.bkclient.utility.mixins.ArmorItemAddition;
import dev.bkclient.utility.time.Timer;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "Auto Armor", category = ModuleCategory.COMBAT, desc = "modules.descriptions.auto_armor")
public class AutoArmor extends BaseModule {
   private final Timer timer = new Timer();
   private SliderSetting delay;
   private final EventListener<ClientPlayerTickEvent> onClientPlayerTickEvent = event -> {
      PlayerInventory inventory = mc.player.getInventory();
      int[] bestArmorSlots = new int[4];
      int[] bestArmorValues = new int[4];
      this.evaluateCurrentArmorValues(inventory, bestArmorSlots, bestArmorValues);
      List<Integer> types = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
      Collections.shuffle(types);

      for (int i : types) {
         int bestSlot = bestArmorSlots[i];
         if (bestSlot != -1) {
            ItemStack oldArmor = inventory.getArmorStack(i);
            if (oldArmor.isEmpty() || inventory.getEmptySlot() != -1) {
               this.transferArmorItem(inventory, bestSlot, i);
               break;
            }
         }
      }
   };

   private void initialize() {
      this.delay = new SliderSetting(this, "delay").min(50.0F).max(1000.0F).step(1.0F).currentValue(250.0F).suffix(" ms");
   }

   public AutoArmor() {
      this.initialize();
   }

   private void evaluateCurrentArmorValues(PlayerInventory inventory, int[] bestArmorSlots, int[] bestArmorValues) {
      for (int type = 0; type < 4; type++) {
         bestArmorSlots[type] = -1;
         ItemStack stack = inventory.getArmorStack(type);
         if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem item) {
            bestArmorValues[type] = this.getArmorValue(item, stack);
         }
      }

      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = inventory.getStack(slot);
         if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem item) {
            EquipmentSlot equipmentSlot = ((ArmorItemAddition)item).bkclient$getType().getEquipmentSlot();
            int armorType;
            switch (equipmentSlot) {
               case HEAD:
                  armorType = 3;
                  break;
               case CHEST:
                  armorType = 2;
                  break;
               case LEGS:
                  armorType = 1;
                  break;
               case FEET:
                  armorType = 0;
                  break;
               default:
                  continue;
            }

            int armorValue = this.getArmorValue(item, stack);
            if (armorValue > bestArmorValues[armorType]) {
               bestArmorSlots[armorType] = slot;
               bestArmorValues[armorType] = armorValue;
            }
         }
      }
   }

   private void transferArmorItem(PlayerInventory inventory, int bestSlot, int armorType) {
      if (bestSlot < 9) {
         bestSlot += 36;
      }

      if (this.timer.finished((long)this.delay.getCurrentValue())) {
         ItemStack oldArmor = inventory.getArmorStack(armorType);
         if (!oldArmor.isEmpty()) {
            mc.interactionManager.clickSlot(0, 8 - armorType, 0, SlotActionType.QUICK_MOVE, mc.player);
         }

         mc.interactionManager.clickSlot(0, bestSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
         this.timer.reset();
      }
   }

   private int getArmorValue(ArmorItem item, ItemStack stack) {
      ArmorMaterial material = ((ArmorItemAddition)item).bkclient$getMaterial();
      EquipmentType equipmentType = ((ArmorItemAddition)item).bkclient$getType();
      int armorPoints = material.defense().getOrDefault(equipmentType, 0);
      int armorToughness = (int)material.toughness();
      int protectionLevel = EnchantmentUtility.getEnchantmentLevel(stack, Enchantments.PROTECTION);
      return armorPoints * 5 + protectionLevel * 3 + armorToughness;
   }
}
