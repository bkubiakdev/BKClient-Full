package dev.bkclient.combatengine.api.utils.player;

import dev.bkclient.combatengine.api.QClient;
import java.util.function.Predicate;
import lombok.Generated;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class SwapManager implements QClient {
   private static final int recoveredField1 = 40;

   public static int findInInventory(Predicate<ItemStack> predicate) {
      if (mc.player == null) {
         return -1;
      }

      for (int var1 = 0; var1 < 9; var1++) {
         if (predicate.test(mc.player.getInventory().getStack(var1))) {
            return var1;
         }
      }

      for (int var2 = 9; var2 < 36; var2++) {
         if (predicate.test(mc.player.getInventory().getStack(var2))) {
            return var2;
         }
      }

      return -1;
   }

   public static int findMatching(ItemStack target) {
      return target != null && !target.isEmpty() ? findInInventory(stack -> matches(stack, target)) : -1;
   }

   public static boolean ensureInOffhand(ItemStack target) {
      if (mc.player == null || mc.interactionManager == null) {
         return false;
      }

      if (target != null && !target.isEmpty()) {
         if (matches(mc.player.getOffHandStack(), target)) {
            return true;
         }

         int var1 = findMatching(target);
         return var1 != -1 && swapToOffhand(var1);
      } else {
         return false;
      }
   }

   public static boolean swapToOffhand(int inventoryIndex) {
      if (mc.player == null || mc.interactionManager == null || mc.player.networkHandler == null) {
         return false;
      } else {
         return inventoryIndex >= 0 && inventoryIndex <= 35 ? swapInventorySlotHidden(inventoryIndex, 40) : false;
      }
   }

   public static boolean openHiddenInventory() {
      if (mc.player == null || mc.interactionManager == null || mc.player.networkHandler == null) {
         return false;
      }

      if (mc.currentScreen instanceof SwapManager.HiddenInventoryScreen) {
         return true;
      }

      if (mc.currentScreen != null) {
         return false;
      }

      mc.setScreen(new SwapManager.HiddenInventoryScreen(mc.player));
      return true;
   }

   public static boolean closeHiddenInventory() {
      if (mc.player == null || mc.player.networkHandler == null) {
         return false;
      }

      if (!(mc.currentScreen instanceof SwapManager.HiddenInventoryScreen)) {
         return false;
      }

      mc.player.closeHandledScreen();
      return true;
   }

   public static boolean swapInventorySlotHidden(int inventoryIndex, int button) {
      if (mc.player == null || mc.interactionManager == null || mc.player.networkHandler == null) {
         return false;
      }

      if (inventoryIndex < 0 || inventoryIndex > 35) {
         return false;
      }

      if (!openHiddenInventory()) {
         return false;
      }

      try {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, toContainerSlot(inventoryIndex), button, SlotActionType.SWAP, mc.player);
         return true;
      } finally {
         closeHiddenInventory();
      }
   }

   public static int toContainerSlot(int inventoryIndex) {
      return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
   }

   public static boolean swapSelectedWithOffhand() {
      if (mc.player == null || mc.player.networkHandler == null) {
         return false;
      }

      if (mc.player.isSpectator()) {
         return false;
      }

      mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      return true;
   }

   public static boolean matches(ItemStack stack, ItemStack target) {
      if (stack == null || target == null || stack.isEmpty() || target.isEmpty()) {
         return false;
      } else if (!stack.isOf(target.getItem())) {
         return false;
      } else if (target.isOf(Items.TOTEM_OF_UNDYING)) {
         return !target.hasEnchantments() ? true : stack.hasEnchantments() && stack.getEnchantments().equals(target.getEnchantments());
      } else {
         return target.isOf(Items.PLAYER_HEAD) ? stack.getName().getString().equals(target.getName().getString()) : true;
      }
   }

   @Generated
   private SwapManager() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   private static final class HiddenInventoryScreen extends HandledScreen<PlayerScreenHandler> {
      private HiddenInventoryScreen(PlayerEntity player) {
         super(player.playerScreenHandler, player.getInventory(), Text.empty());
      }

      @Override
      protected void init() {
      }

      @Override
      protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
      }

      @Override
      public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      }
   }
}
