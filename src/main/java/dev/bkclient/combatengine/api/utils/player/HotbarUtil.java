package dev.bkclient.combatengine.api.utils.player;

import dev.bkclient.combatengine.api.QClient;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;

public final class HotbarUtil implements QClient {
   private static int recoveredField1 = -1;

   public static int getItemCount(Item item) {
      if (mc.player == null) {
         return 0;
      }

      int var1 = 0;

      for (int var2 = 0; var2 < mc.player.getInventory().size(); var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (var3.isOf(item)) {
            var1 += var3.getCount();
         }
      }

      return var1;
   }

   public static SlotSearchResult getAxe() {
      return recoveredMethod1(itemStack -> itemStack.getItem() instanceof AxeItem, false);
   }

   public static SlotSearchResult getAxeHotBar() {
      return recoveredMethod1(itemStack -> itemStack.getItem() instanceof AxeItem, true);
   }

   public static SlotSearchResult getPickAxe() {
      return recoveredMethod1(itemStack -> itemStack.getItem() instanceof PickaxeItem, false);
   }

   public static SlotSearchResult getPickAxeHotbar() {
      return getPickAxeHotBar();
   }

   public static SlotSearchResult getPickAxeHotBar() {
      return recoveredMethod1(itemStack -> itemStack.getItem() instanceof PickaxeItem, true);
   }

   public static SlotSearchResult getSword() {
      return recoveredMethod1(itemStack -> itemStack.getItem() instanceof SwordItem, false);
   }

   public static SlotSearchResult getSwordHotBar() {
      return recoveredMethod1(itemStack -> itemStack.getItem() instanceof SwordItem, true);
   }

   public static SlotSearchResult getSkull() {
      return findInHotBar(
         stack -> stack.isOf(Items.SKELETON_SKULL)
            || stack.isOf(Items.WITHER_SKELETON_SKULL)
            || stack.isOf(Items.CREEPER_HEAD)
            || stack.isOf(Items.PLAYER_HEAD)
            || stack.isOf(Items.ZOMBIE_HEAD)
      );
   }

   public static int getElytra() {
      if (mc.player == null) {
         return -1;
      }

      for (ItemStack var1 : mc.player.getInventory().armor) {
         if (var1.isOf(Items.ELYTRA) && var1.getDamage() < var1.getMaxDamage() - 1) {
            return -2;
         }
      }

      for (int var2 = 0; var2 < 36; var2++) {
         ItemStack var3 = mc.player.getInventory().getStack(var2);
         if (var3.isOf(Items.ELYTRA) && var3.getDamage() < var3.getMaxDamage() - 1) {
            return var2 < 9 ? var2 + 36 : var2;
         }
      }

      return -1;
   }

   public static SlotSearchResult findInHotBar(HotbarUtil.Searcher searcher) {
      if (mc.player != null) {
         if (searcher.isValid(mc.player.getOffHandStack())) {
            return SlotSearchResult.inOffhand(mc.player.getOffHandStack());
         }

         for (int var1 = 0; var1 < 9; var1++) {
            ItemStack var2 = mc.player.getInventory().getStack(var1);
            if (searcher.isValid(var2)) {
               return new SlotSearchResult(var1, true, var2);
            }
         }
      }

      return SlotSearchResult.notFound();
   }

   public static SlotSearchResult findItemInHotBar(List<Item> items) {
      return findInHotBar(stack -> items.contains(stack.getItem()));
   }

   public static SlotSearchResult findItemInHotBar(Item... items) {
      return findItemInHotBar(Arrays.asList(items));
   }

   public static SlotSearchResult findInInventory(HotbarUtil.Searcher searcher) {
      if (mc.player != null) {
         for (int var1 = 35; var1 >= 0; var1--) {
            ItemStack var2 = mc.player.getInventory().getStack(var1);
            if (searcher.isValid(var2)) {
               return new SlotSearchResult(var1, true, var2);
            }
         }
      }

      return SlotSearchResult.notFound();
   }

   public static SlotSearchResult findItemInInventory(List<Item> items) {
      return findInInventory(stack -> items.contains(stack.getItem()));
   }

   public static SlotSearchResult findItemInInventory(Item... items) {
      return findItemInInventory(Arrays.asList(items));
   }

   public static SlotSearchResult findBlockInHotBar(@NotNull List<Block> blocks) {
      return findItemInHotBar(blocks.stream().map(Block::asItem).toList());
   }

   public static SlotSearchResult findBlockInHotBar(Block... blocks) {
      return findItemInHotBar(Arrays.stream(blocks).map(Block::asItem).toList());
   }

   public static SlotSearchResult findBlockInInventory(@NotNull List<Block> blocks) {
      return findItemInInventory(blocks.stream().map(Block::asItem).toList());
   }

   public static SlotSearchResult findBlockInInventory(Block... blocks) {
      return findItemInInventory(Arrays.stream(blocks).map(Block::asItem).toList());
   }

   public static void saveSlot() {
      if (mc.player != null) {
         recoveredField1 = mc.player.getInventory().selectedSlot;
      }
   }

   public static void returnSlot() {
      if (recoveredField1 != -1) {
         switchTo(recoveredField1);
      }

      recoveredField1 = -1;
   }

   public static void saveAndSwitchTo(int slot) {
      saveSlot();
      switchTo(slot);
   }

   public static void switchTo(int slot) {
      if (mc.player != null && mc.getNetworkHandler() != null && slot >= 0 && slot <= 8) {
         if (mc.player.getInventory().selectedSlot != slot) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
      }
   }

   public static void switchToSilent(int slot) {
      if (mc.player != null && mc.getNetworkHandler() != null && slot >= 0 && slot <= 8) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   public static SlotSearchResult getAntiWeaknessItem() {
      if (mc.player == null) {
         return SlotSearchResult.notFound();
      }

      Item var0 = mc.player.getMainHandStack().getItem();
      return !(var0 instanceof SwordItem) && !(var0 instanceof PickaxeItem) && !(var0 instanceof AxeItem) && !(var0 instanceof ShovelItem)
         ? findInHotBar(
            stack -> stack.getItem() instanceof SwordItem
               || stack.getItem() instanceof PickaxeItem
               || stack.getItem() instanceof AxeItem
               || stack.getItem() instanceof ShovelItem
         )
         : new SlotSearchResult(mc.player.getInventory().selectedSlot, true, mc.player.getMainHandStack());
   }

   public static float getHitDamage(@NotNull ItemStack weapon, PlayerEntity entity) {
      if (mc.player != null && mc.world != null) {
         float var2 = recoveredMethod2(weapon);
         if (mc.player.fallDistance > 0.0F) {
            var2 += var2 / 2.0F;
         }

         if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            int var3 = Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.STRENGTH)).getAmplifier() + 1;
            var2 += 3.0F * var3;
         }

         return DamageUtil.getDamageLeft(
            entity, var2, mc.world.getDamageSources().generic(), entity.getArmor(), (float)entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS)
         );
      } else {
         return 0.0F;
      }
   }

   public static SlotSearchResult findBedInHotBar() {
      return findInHotBar(stack -> stack.getItem() instanceof BedItem);
   }

   public static SlotSearchResult findBed() {
      return findInInventory(stack -> stack.getItem() instanceof BedItem);
   }

   public static Item getItem(String name) {
      if (name == null) {
         return Items.AIR;
      }

      String var1 = name.toLowerCase();

      for (Block var3 : Registries.BLOCK) {
         if (var3.getTranslationKey().replace("block.minecraft.", "").equals(var1)) {
            return Item.fromBlock(var3);
         }
      }

      for (Item var5 : Registries.ITEM) {
         if (var5.getTranslationKey().replace("item.minecraft.", "").equals(var1)) {
            return var5;
         }
      }

      return Items.DIRT;
   }

   public static int getBedsCount() {
      if (mc.player == null) {
         return 0;
      }

      int var0 = 0;

      for (int var1 = 0; var1 < mc.player.getInventory().size(); var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (var2.getItem() instanceof BedItem) {
            var0 += var2.getCount();
         }
      }

      return var0;
   }

   private static SlotSearchResult recoveredMethod1(HotbarUtil.Searcher searcher, boolean hotbarOnly) {
      if (mc.player == null) {
         return SlotSearchResult.notFound();
      }

      int var2 = -1;
      float var3 = 0.0F;
      int var4 = hotbarOnly ? 8 : 35;

      for (int var5 = 0; var5 <= var4; var5++) {
         ItemStack var6 = mc.player.getInventory().getStack(var5);
         if (searcher.isValid(var6)) {
            float var7 = recoveredMethod2(var6);
            if (var7 > var3) {
               var3 = var7;
               var2 = var5;
            }
         }
      }

      return var2 == -1 ? SlotSearchResult.notFound() : new SlotSearchResult(var2, true, mc.player.getInventory().getStack(var2));
   }

   private static float recoveredMethod2(ItemStack stack) {
      AttributeModifiersComponent var1 = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
      double var2 = 1.0;

      for (Entry var5 : var1.modifiers()) {
         if (var5.attribute().equals(EntityAttributes.ATTACK_DAMAGE)) {
            var2 += var5.modifier().value();
         }
      }

      return (float)var2;
   }

   public static boolean isHolding(Item item) {
      return mc.player != null && (mc.player.getMainHandStack().isOf(item) || mc.player.getOffHandStack().isOf(item));
   }

   public static Hand getHand(Item item) {
      if (mc.player == null) {
         return null;
      } else if (mc.player.getOffHandStack().isOf(item)) {
         return Hand.OFF_HAND;
      } else {
         return mc.player.getMainHandStack().isOf(item) ? Hand.MAIN_HAND : null;
      }
   }

   @Generated
   private HotbarUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public interface Searcher {
      boolean isValid(ItemStack var1);
   }
}
