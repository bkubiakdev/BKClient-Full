package dev.bkclient.utility.inventory.group;

import dev.bkclient.utility.inventory.group.impl.ArmorSlotsGroup;
import dev.bkclient.utility.inventory.group.impl.HotbarSlotsGroup;
import dev.bkclient.utility.inventory.group.impl.InventorySlotsGroup;
import dev.bkclient.utility.inventory.group.impl.OffhandSlotGroup;
import dev.bkclient.utility.inventory.slots.ArmorSlot;
import dev.bkclient.utility.inventory.slots.HotbarSlot;
import dev.bkclient.utility.inventory.slots.InventorySlot;
import dev.bkclient.utility.inventory.slots.OffhandSlot;

public class SlotGroups {
   private SlotGroups() {
   }

   public static SlotGroup<HotbarSlot> hotbar() {
      return new HotbarSlotsGroup();
   }

   public static SlotGroup<InventorySlot> inventory() {
      return new InventorySlotsGroup();
   }

   public static SlotGroup<ArmorSlot> armor() {
      return new ArmorSlotsGroup();
   }

   public static SlotGroup<OffhandSlot> offhand() {
      return new OffhandSlotGroup();
   }
}
