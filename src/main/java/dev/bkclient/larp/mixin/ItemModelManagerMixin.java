package dev.bkclient.larp.mixin;

import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.RubiksCube;

@Mixin(value={ItemModelManager.class})
public class ItemModelManagerMixin {
    @Unique
    private boolean easyeventowka$swapping;

    @ModifyVariable(method={"update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V"}, at=@At(value="HEAD"), argsOnly=true)
    private ItemStack easyeventowka$disguiseUpdate(ItemStack stack) {
        return Disguise.model(stack);
    }

    @ModifyVariable(method={"update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V"}, at=@At(value="HEAD"), argsOnly=true)
    private ItemStack easyeventowka$disguiseUpdateBool(ItemStack stack) {
        return Disguise.model(stack);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Inject(method={"updateForLivingEntity(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/entity/LivingEntity;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$scrambledItem(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, boolean bl, LivingEntity entity, CallbackInfo ci) {
        if (this.easyeventowka$swapping || entity == null) {
            return;
        }
        ItemStack swapped = RubiksCube.shownItem((Entity)entity, stack);
        if (swapped == stack) {
            return;
        }
        this.easyeventowka$swapping = true;
        try {
            ((ItemModelManager)(Object)this).updateForLivingEntity(state, swapped, mode, bl, entity);
        }
        finally {
            this.easyeventowka$swapping = false;
        }
        ci.cancel();
    }

    @ModifyVariable(method={"updateForLivingEntity(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/entity/LivingEntity;)V"}, at=@At(value="HEAD"), argsOnly=true)
    private ItemStack easyeventowka$disguiseLiving(ItemStack stack) {
        return Disguise.model(stack);
    }

    @ModifyVariable(method={"updateForNonLivingEntity(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/entity/Entity;)V"}, at=@At(value="HEAD"), argsOnly=true)
    private ItemStack easyeventowka$disguiseNonLiving(ItemStack stack) {
        return Disguise.model(stack);
    }
}
