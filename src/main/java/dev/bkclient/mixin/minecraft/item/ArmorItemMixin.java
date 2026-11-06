package dev.bkclient.mixin.minecraft.item;

import dev.bkclient.utility.mixins.ArmorItemAddition;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorItem.class)
public abstract class ArmorItemMixin implements ArmorItemAddition {
   @Unique
   private EquipmentType bkclient$type;
   @Unique
   private ArmorMaterial bkclient$material;

   @Inject(method = "<init>(Lnet/minecraft/item/equipment/ArmorMaterial;Lnet/minecraft/item/equipment/EquipmentType;Lnet/minecraft/item/Item$Settings;)V", at = @At("TAIL"))
   public void saveArgs(ArmorMaterial material, EquipmentType type, Settings settings, CallbackInfo ci) {
      this.bkclient$type = type;
      this.bkclient$material = material;
   }

   @Override
   public ArmorMaterial bkclient$getMaterial() {
      return this.bkclient$material;
   }

   @Override
   public EquipmentType bkclient$getType() {
      return this.bkclient$type;
   }
}
