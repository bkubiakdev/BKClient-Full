package dev.bkclient.utility.inventory.group.impl;

import java.util.List;
import dev.bkclient.utility.inventory.group.SlotGroup;
import dev.bkclient.utility.inventory.slots.OffhandSlot;

public class OffhandSlotGroup extends SlotGroup<OffhandSlot> {
   public OffhandSlotGroup() {
      super(List.of(new OffhandSlot()));
   }
}
