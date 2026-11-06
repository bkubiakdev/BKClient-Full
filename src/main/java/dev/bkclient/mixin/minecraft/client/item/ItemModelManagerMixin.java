package dev.bkclient.mixin.minecraft.client.item;

import dev.bkclient.systems.modules.modules.visuals.CustomSwords;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies the selected Custom Swords model to a copy of the rendered stack.
 * The shared update overload covers held items, dropped items, item frames and GUI
 * without changing the inventory stack.
 */
@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {

    @ModifyVariable(
        method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private ItemStack bkClient$replaceSwordModel(
        ItemStack stack,
        ItemRenderState renderState,
        ItemStack unusedStack,
        ModelTransformationMode mode,
        World world,
        LivingEntity user,
        int seed
    ) {
        if (dev.bkclient.larp.feature.Disguise.isDisguised(stack) || dev.bkclient.larp.feature.Disguise.isVisualStack(stack)) return stack;
        CustomSwords module = CustomSwords.getInstance();
        if (module == null || !module.isEnabled() || !bkClient$isSword(stack)) {
            return stack;
        }

        if (module.isSelfOnly()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || user != mc.player) {
                return stack;
            }
        }

        ItemStack rendered = stack.copy();
        rendered.set(
            DataComponentTypes.CUSTOM_MODEL_DATA,
            new CustomModelDataComponent(List.of(), List.of(), List.of(module.getSelectedWeapon()), List.of())
        );
        return rendered;
    }

    private static boolean bkClient$isSword(ItemStack stack) {
        return stack.isOf(Items.WOODEN_SWORD)
            || stack.isOf(Items.STONE_SWORD)
            || stack.isOf(Items.IRON_SWORD)
            || stack.isOf(Items.GOLDEN_SWORD)
            || stack.isOf(Items.DIAMOND_SWORD)
            || stack.isOf(Items.NETHERITE_SWORD);
    }
}
