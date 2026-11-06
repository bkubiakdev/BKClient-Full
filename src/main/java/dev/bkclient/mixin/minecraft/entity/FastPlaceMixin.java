package dev.bkclient.mixin.minecraft.entity;

import dev.bkclient.BKClientInit;
import dev.bkclient.mixin.minecraft.client.IMinecraftClient;
import dev.bkclient.systems.modules.modules.other.FastPlace;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class FastPlaceMixin {
    @Shadow
    private int blockBreakingCooldown;
    @Unique
    private long bkClient$lastPlaceMs;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bkClient$fastPlaceTick(CallbackInfo ci) {
        FastPlace module = BKClientInit.getInstance().getModuleManager().getModule(FastPlace.class);
        if (!module.isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        ItemStack held = client.player.getMainHandStack();
        if (canFastPlace(held)) blockBreakingCooldown = 0;
        if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS) {
            bkClient$lastPlaceMs = 0L;
            return;
        }
        if (!canFastPlace(held)) return;
        long now = System.currentTimeMillis();
        long interval = 1000L / Math.max(1, (int) module.cps.getCurrentValue());
        if (now - bkClient$lastPlaceMs < interval) return;
        bkClient$lastPlaceMs = now;
        ((IMinecraftClient) client).setUseCooldown(0);
        ((IMinecraftClient) client).idoItemUse();
    }

    @Unique
    private boolean canFastPlace(ItemStack stack) {
        return !stack.isEmpty()
            && stack.getItem() instanceof BlockItem blockItem
            && !(blockItem.getBlock() instanceof TrapdoorBlock);
    }
}
