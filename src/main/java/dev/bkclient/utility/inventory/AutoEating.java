package dev.bkclient.utility.inventory;

import java.lang.ref.WeakReference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

/** One meal owned by one module. Does not retain a disconnected player/world. */
public final class AutoEating {
    private WeakReference<ClientPlayerEntity> player = new WeakReference<>(null);
    private WeakReference<Object> world = new WeakReference<>(null);
    private boolean active, restorePending;
    private Hand hand;
    private int previousSlot, foodSlot, initialHunger, initialCount, ticks, cooldown;
    private int lastAge = Integer.MIN_VALUE;

    public boolean isActive() { return active; }

    public void validate(MinecraftClient mc, boolean enabled) {
        if (restorePending && mc.player != null) {
            if (mc.player == player.get() && mc.player.getInventory().selectedSlot == foodSlot)
                mc.player.getInventory().selectedSlot = previousSlot;
            restorePending = false;
        }
        if (!enabled || mc.player == null || mc.world == null || mc.interactionManager == null
                || mc.currentScreen != null || (mc.player != null && (!mc.player.isAlive()
                    || mc.player.isSpectator() || mc.player.getAbilities().creativeMode)) || (active && (player.get() != mc.player || world.get() != mc.world))) {
            stop(mc);
        }
        // Runs before vanilla handles held mouse buttons. Between meals vanilla must
        // not reinterpret our held use key as a block interaction (e.g. open a chest).
        if (active && !mc.player.isUsingItem()) mc.options.useKey.setPressed(false);
    }

    public boolean tick(MinecraftClient mc, boolean enabled, int threshold) {
        validate(mc, enabled);
        if (!enabled || mc.player == null || mc.world == null || mc.interactionManager == null
                || mc.currentScreen != null || !mc.player.isAlive() || mc.player.isSpectator()
                || mc.player.getAbilities().creativeMode) return false;
        // MineHelper may also call standalone Auto Eat in the same player tick.
        if (player.get() == mc.player && world.get() == mc.world && lastAge == mc.player.age) return active;
        player = new WeakReference<>(mc.player);
        world = new WeakReference<>(mc.world);
        lastAge = mc.player.age;
        if (cooldown > 0) { cooldown--; return false; }
        int hunger = mc.player.getHungerManager().getFoodLevel();
        if (active) {
            ticks++;
            if (mc.player.getInventory().selectedSlot != foodSlot
                    || hunger > initialHunger || mc.player.getStackInHand(hand).getCount() < initialCount
                    || ticks >= 200 || !isFood(mc.player.getStackInHand(hand))) {
                stop(mc);
                cooldown = 5;
                return false;
            }
        } else {
            if (hunger >= 20 || hunger > threshold || mc.player.isUsingItem()
                    || mc.player.currentScreenHandler != mc.player.playerScreenHandler
                    || !mc.player.currentScreenHandler.getCursorStack().isEmpty()) return false;
            previousSlot = mc.player.getInventory().selectedSlot;
            hand = Hand.OFF_HAND;
            if (!isFood(mc.player.getOffHandStack())) {
                hand = Hand.MAIN_HAND;
                int source = -1;
                for (int i = 0; i < 36; i++) {
                    if (isFood(mc.player.getInventory().getStack(i))) { source = i; break; }
                }
                if (source < 0) return false;
                if (source >= 9) {
                    int destination = (previousSlot + 1) % 9;
                    for (int i = 0; i < 9; i++) {
                        if (i != previousSlot && mc.player.getInventory().getStack(i).isEmpty()) { destination = i; break; }
                    }
                    // SWAP leaves the displaced stack in the food's old slot, never on the cursor.
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, source,
                            destination, SlotActionType.SWAP, mc.player);
                    source = destination;
                }
                if (!isFood(mc.player.getInventory().getStack(source))) return false;
                mc.player.getInventory().selectedSlot = source;
            }
            foodSlot = mc.player.getInventory().selectedSlot;
            initialHunger = hunger;
            initialCount = mc.player.getStackInHand(hand).getCount();
            ticks = 0;
            active = true;
        }
        mc.options.attackKey.setPressed(false);
        mc.interactionManager.cancelBlockBreaking();
        mc.options.useKey.setPressed(true);
        // Direct item interaction avoids opening the chest/workbench under the crosshair.
        if (!mc.player.isUsingItem() && ticks % 5 == 0) mc.interactionManager.interactItem(mc.player, hand);
        return true;
    }

    public void stop(MinecraftClient mc) {
        if (!active) return; // Never release a different module's or the user's input.
        active = false;
        restorePending = mc.player == null && player.get() != null;
        mc.options.useKey.setPressed(false);
        if (mc.player != null && mc.player == player.get()) {
            if (mc.world != null && mc.interactionManager != null && mc.player.isUsingItem()) {
                mc.interactionManager.stopUsingItem(mc.player);
            }
            // Respect a manual slot change; restore only the slot we selected ourselves.
            if (mc.player.getInventory().selectedSlot == foodSlot) {
                mc.player.getInventory().selectedSlot = previousSlot;
                if (mc.world != null && mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
                }
            }
        }
        ticks = 0;
    }

    private static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.contains(DataComponentTypes.FOOD)
                && stack.contains(DataComponentTypes.CONSUMABLE);
    }
}
