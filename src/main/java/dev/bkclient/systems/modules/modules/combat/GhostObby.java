package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Holding a totem and the use button on a block: puts obsidian on the face you look at without switching your hand
 * (the obsidian slot is selected only for the placement), so you can wall or ground yourself while keeping the totem out.
 */
@ModuleInfo(name = "Ghost Obby", category = ModuleCategory.COMBAT, desc = "Automatically places obsidian when looking at a block while holding a totem")
public final class GhostObby extends BaseModule {
   private final SliderSetting delay = new SliderSetting(this, "Delay").min(0.0F).max(1000.0F).step(10.0F).currentValue(200.0F).suffix("ms");

   private long nextAt;

   @Override
   public void tick() {
      if (!CombatKit.canAct() || System.currentTimeMillis() < this.nextAt) return;
      if (!CombatKit.holding(Items.TOTEM_OF_UNDYING) || !CombatKit.mc.options.useKey.isPressed()) return;
      if (!(CombatKit.mc.crosshairTarget instanceof BlockHitResult hit)) return;
      var state = CombatKit.mc.world.getBlockState(hit.getBlockPos());
      if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK)) return;
      BlockPos target = hit.getBlockPos().offset(hit.getSide());
      if (!CombatKit.mc.world.getBlockState(target).isReplaceable()
         || !CombatKit.mc.world.getOtherEntities(null, new Box(target), e -> !e.isSpectator()).isEmpty()) return;
      int slot = CombatKit.hotbar(Items.OBSIDIAN);
      if (slot < 0) return;
      int previous = CombatKit.mc.player.getInventory().selectedSlot;
      CombatKit.selectSlot(slot);
      try {
         if (CombatKit.mc.interactionManager.interactBlock(CombatKit.mc.player, Hand.MAIN_HAND, hit).isAccepted()) {
            CombatKit.mc.player.swingHand(Hand.MAIN_HAND);
         }
      } finally {
         CombatKit.selectSlot(previous);
      }
      this.nextAt = System.currentTimeMillis() + (long)this.delay.getCurrentValue();
   }
}
