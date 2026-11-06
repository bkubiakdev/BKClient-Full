package dev.bkclient.combatengine.api.utils.player;

import dev.bkclient.combatengine.api.QClient;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record SlotSearchResult(int slot, boolean found, ItemStack stack) implements QClient {
   private static final SlotSearchResult recoveredField4 = new SlotSearchResult(-1, false, ItemStack.EMPTY);

   public SlotSearchResult(int slot, boolean found, ItemStack stack) {
      this.slot = slot;
      this.found = found;
      this.stack = stack;
   }

   public static SlotSearchResult notFound() {
      return recoveredField4;
   }

   @NotNull
   public static SlotSearchResult inOffhand(ItemStack stack) {
      return new SlotSearchResult(999, true, stack);
   }

   public boolean isHolding() {
      return mc.player == null ? false : this.isOffhand() || mc.player.getInventory().selectedSlot == this.slot;
   }

   public boolean isOffhand() {
      return this.slot == 999;
   }

   public boolean isInHotBar() {
      return this.slot >= 0 && this.slot < 9;
   }

   public void switchTo() {
      if (this.found && this.isInHotBar()) {
         HotbarUtil.switchTo(this.slot);
      }
   }

   public void switchToSilent() {
      if (this.found && this.isInHotBar()) {
         HotbarUtil.switchToSilent(this.slot);
      }
   }

   public int slot() {
      return this.slot;
   }

   public boolean found() {
      return this.found;
   }

   public ItemStack stack() {
      return this.stack;
   }
}
