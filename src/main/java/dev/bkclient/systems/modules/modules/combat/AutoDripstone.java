package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.GameTickEvent;
import dev.bkclient.mixin.accessors.ClientPlayerInteractionManagerAccessor;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;

/** Side-support placement from bkdripstone, with immediate clicks and held-use tick retries. */
@ModuleInfo(name = "Auto Dripstone", category = ModuleCategory.COMBAT, desc = "modules.descriptions.auto_dripstone")
public final class AutoDripstone extends BaseModule {
    private final SliderSetting placeDelay = new SliderSetting(this, "modules.settings.auto_dripstone.place_delay")
        .min(0).max(300).step(25).currentValue(0).suffix(" ms");
    private boolean internalInteraction;
    private ClientPlayerEntity swapPlayer;
    private ClientWorld swapWorld;
    private int swapBackSlot = -1;
    private int dripstoneSlot = -1;
    private long swapTick;
    private long inputTick;
    private long lastAttemptTick = Long.MIN_VALUE;
    private long lastAttemptNanos;

    private final EventListener<GameTickEvent> onTick = new EventListener<>() {
        @Override public int getPriority() { return 1000; }
        @Override public void onEvent(GameTickEvent event) { beginInputTick(); }
    };
    private void beginInputTick() {
        inputTick++;
        // Before vanilla input/movement: never change slots or place at player-tick RETURN.
        if (swapPlayer != null && (mc.player != swapPlayer || mc.world != swapWorld || inputTick != swapTick)) restoreSlot();
        if (isEnabled() && mc.player != null && mc.world != null && mc.currentScreen == null
                && mc.options.useKey.isPressed() && mc.crosshairTarget instanceof BlockHitResult hit) {
            ActionResult result = onUseBlock(mc.player, Hand.MAIN_HAND, hit);
            if (result != null && result.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    /** Null leaves vanilla interaction untouched. */
    public ActionResult onUseBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!isEnabled() || internalInteraction || player != mc.player || hand != Hand.MAIN_HAND
                || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null
                || player.isSpectator()) return null;
        // A held-use attempt already consumed this input cycle, even if prediction filled the target.
        // Check BEFORE target replaceability so vanilla cannot also click/open the trapdoor.
        if (lastAttemptTick == inputTick) return ActionResult.FAIL;
        var pos = hit.getBlockPos();
        var state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof TrapdoorBlock) || state.get(TrapdoorBlock.OPEN)) return null;
        var target = pos.down();
        if (!mc.world.getBlockState(target).isReplaceable()) return null;
        var facing = state.get(TrapdoorBlock.FACING);
        var support = target.offset(facing.getOpposite());
        if (mc.world.getBlockState(support).isReplaceable()) return null;
        var inventory = player.getInventory();
        int slot = player.getMainHandStack().isOf(Items.POINTED_DRIPSTONE) ? inventory.selectedSlot : -1;
        for (int i = 0; slot == -1 && i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.POINTED_DRIPSTONE)) slot = i;
        }
        if (slot == -1) return null;
        long now = System.nanoTime();
        if ((lastAttemptNanos != 0
                && now - lastAttemptNanos < (long)(placeDelay.getCurrentValue() * 1_000_000L))) return ActionResult.FAIL;
        var point = new Vec3d(support.getX() + .5 + facing.getOffsetX() * .5,
            support.getY() + .5, support.getZ() + .5 + facing.getOffsetZ() * .5);
        if (player.getEyePos().squaredDistanceTo(point) > Math.pow(player.getBlockInteractionRange(), 2)) return null;
        lastAttemptTick = inputTick;
        lastAttemptNanos = now;
        if (swapPlayer != null && (swapPlayer != player || swapWorld != mc.world)) restoreSlot();
        internalInteraction = true;
        try {
            if (inventory.selectedSlot != slot) {
                restoreSlot();
                swapBackSlot = inventory.selectedSlot;
                dripstoneSlot = slot;
                swapPlayer = player;
                swapWorld = mc.world;
                swapTick = inputTick;
                selectSlot(player, slot);
            }
            var result = mc.interactionManager.interactBlock(player, hand, new BlockHitResult(point, facing, support, false));
            return result.isAccepted() ? ActionResult.SUCCESS : ActionResult.FAIL;
        } catch (RuntimeException error) {
            restoreSlot();
            throw error;
        } finally { internalInteraction = false; }
    }

    private void restoreSlot() {
        var player = swapPlayer;
        int previous = swapBackSlot;
        int selected = dripstoneSlot;
        var world = swapWorld;
        swapPlayer = null; swapWorld = null; swapBackSlot = -1; dripstoneSlot = -1;
        if (player != null && player == mc.player && world == mc.world && previous >= 0
                && player.getInventory().selectedSlot == selected && mc.getNetworkHandler() != null) selectSlot(player, previous);
    }
    private void selectSlot(ClientPlayerEntity player, int slot) {
        player.getInventory().setSelectedSlot(slot);
        player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        // interactBlock/tick normally synchronize this cache; keep the explicit packet from being duplicated.
        ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).aura$setLastSelectedSlot(slot);
    }
    @Override public void onEnable() { restoreSlot(); lastAttemptTick = Long.MIN_VALUE; lastAttemptNanos = 0; super.onEnable(); }
    @Override public void onDisable() { restoreSlot(); lastAttemptTick = Long.MIN_VALUE; lastAttemptNanos = 0; super.onDisable(); }
}
