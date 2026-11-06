package dev.bkclient.larp.mixin;

import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.bkclient.larp.feature.BackpackScreen;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.DivineAxe;
import dev.bkclient.larp.feature.FakeConsumable;
import dev.bkclient.larp.feature.GuardianRod;
import dev.bkclient.larp.feature.HydroCage;
import dev.bkclient.larp.feature.IllusionistWand;
import dev.bkclient.larp.feature.PouchScreen;

@Mixin(value={ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
    @Inject(method={"interactItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$cancelItemUse(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = player.getStackInHand(hand);
        if (FakeConsumable.blocksUse(stack) || GuardianRod.blocksUse(stack)) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }
        if (Disguise.hasAbility(stack, Disguise.Ability.DROP_POUCH)) {
            PouchScreen.open();
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        if (Disguise.hasAbility(stack, Disguise.Ability.DRACULA_BACKPACK)) {
            BackpackScreen.open(stack);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        if (Disguise.hasAbility(stack, Disguise.Ability.HYDRO_CAGE) && HydroCage.tryShoot(stack)) {
            cir.setReturnValue(ActionResult.CONSUME);
            return;
        }
        if (Disguise.hasAbility(stack, Disguise.Ability.ILLUSIONIST_WAND) && IllusionistWand.onRightClick(stack)) {
            cir.setReturnValue(ActionResult.CONSUME);
            return;
        }
        if (Disguise.hasAbility(stack, Disguise.Ability.DIVINE_AXE) && DivineAxe.onRightClick(stack)) {
            cir.setReturnValue(ActionResult.CONSUME);
            return;
        }
        if (Disguise.isDisguised(stack)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method={"interactEntity"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$cancelEntityUse(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (Disguise.isDisguised(player.getStackInHand(hand))) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method={"interactEntityAtLocation"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$cancelEntityUseAt(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (Disguise.isDisguised(player.getStackInHand(hand))) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
