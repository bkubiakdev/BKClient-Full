package dev.bkclient.systems.modules.modules.movement;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.event.impl.window.MouseEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.utility.inventory.EnchantmentUtility;
import dev.bkclient.utility.inventory.InventoryUtility;
import dev.bkclient.utility.inventory.ItemSlot;
import dev.bkclient.utility.inventory.group.SlotGroups;
import dev.bkclient.utility.mixins.ArmorItemAddition;
import net.minecraft.block.Block;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Water Speed", category = ModuleCategory.MOVEMENT)
public class WaterSpeed extends BaseModule {
   private final ModeSetting bypass = new ModeSetting(this, "modules.settings.water_speed.bypass");
   private final ModeSetting.Value vonTam = new ModeSetting.Value(this.bypass, "modules.settings.water_speed.vontam").select();
   private final BindSetting swapDepthStriderKey = new BindSetting(this, "modules.settings.water_speed.swap_depth_strider", () -> !this.bypass.is(this.vonTam));
   private final EventListener<KeyPressEvent> onKeyPress = event -> {
      if (event.getAction() == 1 && mc.currentScreen == null && this.swapDepthStriderKey.isKey(event.getKey())) {
         this.swapDepthStriderBoots();
      }
   };
   private final EventListener<MouseEvent> onMouseClick = event -> {
      if (event.getAction() == 1 && mc.currentScreen == null && this.swapDepthStriderKey.isKey(event.getButton())) {
         this.swapDepthStriderBoots();
      }
   };

   @Override
   public void tick() {
      if (this.bypass.is(this.vonTam) && mc.player != null && mc.world != null && mc.player.isTouchingWater()) {
         Vec3d velocity = mc.player.getVelocity();
         boolean bubbleColumn = this.getBlock(0.0, 1.0, 0.0) instanceof BubbleColumnBlock || this.getBlock(0.0, 2.0, 0.0) instanceof BubbleColumnBlock;
         if (bubbleColumn) {
            boolean hasDepthStrider = EnchantmentUtility.getEnchantmentLevel(mc.player.getInventory().getArmorStack(0), Enchantments.DEPTH_STRIDER) > 0;
            boolean hasDolphinsGrace = mc.player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE);
            double multiplier = hasDepthStrider
               ? (hasDolphinsGrace ? 1.021 : (mc.player.age % 7 == 0 ? 1.12 : 1.11))
               : (mc.player.age % 8 == 0 ? 1.074 : 1.065);
            mc.player.setVelocity(velocity.x * multiplier, velocity.y, velocity.z * multiplier);
         } else {
            double multiplier = mc.player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE) ? 1.01 : 1.05;
            mc.player.setVelocity(velocity.x * multiplier, velocity.y * 0.8, velocity.z * multiplier);
         }

         super.tick();
      } else {
         super.tick();
      }
   }

   private Block getBlock(double x, double y, double z) {
      return mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(x, y, z))).getBlock();
   }

   private void swapDepthStriderBoots() {
      if (mc.player != null) {
         ItemStack currentBoots = mc.player.getInventory().getArmorStack(0);
         boolean shouldFindDepthStrider = !this.hasDepthStrider(currentBoots);
         ItemSlot bootsSlot = SlotGroups.inventory()
            .and(SlotGroups.hotbar())
            .findItem(stack -> this.isBoots(stack) && this.hasDepthStrider(stack) == shouldFindDepthStrider);
         if (bootsSlot != null) {
            bootsSlot.swapTo(InventoryUtility.getBootsSlot());
         }
      }
   }

   private boolean isBoots(ItemStack stack) {
      return stack.getItem() instanceof ArmorItem armorItem && ((ArmorItemAddition)armorItem).bkclient$getType() == EquipmentType.BOOTS;
   }

   private boolean hasDepthStrider(ItemStack stack) {
      return EnchantmentUtility.getEnchantmentLevel(stack, Enchantments.DEPTH_STRIDER) > 0;
   }
}
