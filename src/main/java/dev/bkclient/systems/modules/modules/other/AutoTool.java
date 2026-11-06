package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.BlockBreakEvent;
import dev.bkclient.systems.event.impl.game.StartBreakBlockEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;

@ModuleInfo(name = "Auto Tool", category = ModuleCategory.OTHER, desc = "Automatically selects the fastest hotbar tool")
public class AutoTool extends BaseModule {
    public final BooleanSetting switchBack = new BooleanSetting(this, "Switch Back").enable();
    private int previousSlot = -1;

    private final EventListener<StartBreakBlockEvent> onStartBreak = event -> {
        if (mc.player == null || mc.world == null) return;
        BlockState state = mc.world.getBlockState(event.getBlockPos());
        if (state.isAir()) return;
        int bestSlot = findBestTool(state);
        int currentSlot = mc.player.getInventory().selectedSlot;
        if (bestSlot >= 0 && bestSlot != currentSlot) {
            previousSlot = currentSlot;
            mc.player.getInventory().selectedSlot = bestSlot;
        }
    };

    private final EventListener<BlockBreakEvent> onBlockBreak = event -> restorePreviousSlot();

    @Override
    public void onDisable() {
        restorePreviousSlot();
        super.onDisable();
    }

    private int findBestTool(BlockState state) {
        if (mc.player == null) return -1;
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private void restorePreviousSlot() {
        if (switchBack.isEnabled() && previousSlot >= 0 && mc.player != null) {
            mc.player.getInventory().selectedSlot = previousSlot;
        }
        previousSlot = -1;
    }
}
