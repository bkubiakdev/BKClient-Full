package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Error Killer", category = ModuleCategory.COMBAT, desc = "modules.descriptions.error_killer")
public final class ErrorKiller extends BaseModule {
   private enum Mode {
      COBWEB,
      OBSIDIAN
   }

   private final SliderSetting cobwebDelay = new SliderSetting(this, "modules.settings.error_killer.cobweb_delay")
      .min(1.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(1.0F)
      .suffix(" ms");
   private final SliderSetting obsidianDelay = new SliderSetting(this, "modules.settings.error_killer.obsidian_delay")
      .min(1.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(1.0F)
      .suffix(" ms");

   private int step = -1;
   private long lastStepTime;
   private boolean lastRightClickPressed;
   private boolean moveFixSprint;
   private int originalSlot = -1;
   private BlockHitResult target;
   private Mode mode = Mode.COBWEB;
   private boolean useOriginalSlotForPlace;

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
         this.lastRightClickPressed = false;
         this.clearState();
         return;
      }
      if (mc.currentScreen != null) {
         this.lastRightClickPressed = false;
         return;
      }
      if (this.isHeadInCobweb()) {
         this.lastRightClickPressed = false;
         this.reset(true);
         return;
      }
      if (this.step == -1) {
         this.detectTrigger();
         return;
      }

      this.applyMoveFix();
      this.runMacro();
   }

   private void detectTrigger() {
      boolean rightClick = GLFW.glfwGetMouseButton(
         mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT
      ) == GLFW.GLFW_PRESS;

      ItemStack handStack = mc.player.getMainHandStack();
      HitResult hit = mc.crosshairTarget;
      if (rightClick
         && !this.lastRightClickPressed
         && this.isValidTriggerItem(handStack)
         && hit instanceof BlockHitResult blockHit
         && this.isTargetingTrigger(blockHit)) {
         this.originalSlot = mc.player.getInventory().selectedSlot;
         this.target = this.normalizeTriggerTarget(blockHit);
         this.mode = handStack.isOf(Items.COBWEB) ? Mode.COBWEB : Mode.OBSIDIAN;
         this.useOriginalSlotForPlace = this.mode == Mode.OBSIDIAN;
         this.moveFixSprint = mc.player.isSprinting() || mc.options.sprintKey.isPressed();
         this.step = 0;
      }

      this.lastRightClickPressed = rightClick;
   }

   private boolean isValidTriggerItem(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (stack.isOf(Items.COBWEB)) {
         return true;
      }
      Item item = stack.getItem();
      if (!(item instanceof BlockItem blockItem)) {
         return false;
      }
      BlockState state = blockItem.getBlock().getDefaultState();
      return state.isFullCube((BlockView)mc.world, BlockPos.ORIGIN);
   }

   private boolean isTargetingTrigger(BlockHitResult hit) {
      BlockPos pos = hit.getBlockPos();
      if (this.isTriggerBlock(mc.world.getBlockState(pos).getBlock())) {
         return true;
      }
      return this.isTriggerBlock(mc.world.getBlockState(pos.offset(hit.getSide())).getBlock());
   }

   private boolean isTriggerBlock(Block block) {
      return block instanceof LeverBlock || block instanceof ButtonBlock;
   }

   private BlockHitResult normalizeTriggerTarget(BlockHitResult hit) {
      BlockPos pos = hit.getBlockPos();
      if (this.isTriggerBlock(mc.world.getBlockState(pos).getBlock())) {
         BlockPos supportPos = pos.offset(hit.getSide().getOpposite());
         if (!mc.world.getBlockState(supportPos).isAir()) {
            return new BlockHitResult(hit.getPos(), hit.getSide(), supportPos, hit.isInsideBlock());
         }
      }
      return hit;
   }

   private boolean isHeadInCobweb() {
      BlockPos headPos = mc.player.getBlockPos().up();
      return mc.world.getBlockState(headPos).getBlock() instanceof CobwebBlock;
   }

   private void runMacro() {
      PlayerInventory inventory = mc.player.getInventory();
      int fluidBucket = -1;
      int emptyBucket = -1;
      int placeSlot = -1;
      Item placeItem = this.mode == Mode.COBWEB ? Items.COBWEB : Items.OBSIDIAN;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = inventory.getStack(slot);
         if (stack.isOf(Items.WATER_BUCKET) || stack.isOf(Items.LAVA_BUCKET)) {
            fluidBucket = slot;
         }
         if (stack.isOf(Items.BUCKET)) {
            emptyBucket = slot;
         }
         if (stack.isOf(placeItem)) {
            placeSlot = slot;
         }
      }

      if (this.useOriginalSlotForPlace && this.originalSlot >= 0 && this.originalSlot <= 8) {
         placeSlot = this.originalSlot;
      }

      int delayMs = Math.max(1, Math.round(
         this.mode == Mode.OBSIDIAN
            ? this.obsidianDelay.getCurrentValue()
            : this.cobwebDelay.getCurrentValue()
      ));
      BlockHitResult placementTarget = this.target;
      if (placementTarget == null && mc.crosshairTarget instanceof BlockHitResult currentHit) {
         placementTarget = this.normalizeTriggerTarget(currentHit);
      }

      if (delayMs <= 1) {
         if (fluidBucket != -1 && placeSlot != -1 && placementTarget != null) {
            this.useItemFromSlot(fluidBucket);
            int pickupSlot = fluidBucket != -1 ? fluidBucket : emptyBucket;
            if (pickupSlot != -1) {
               this.useItemFromSlot(pickupSlot);
            }
            this.placeBlockFromSlot(placeSlot, placementTarget);
         }
         this.reset(true);
         return;
      }

      long now = System.currentTimeMillis();
      switch (this.step) {
         case 0 -> {
            if (fluidBucket == -1) {
               this.reset(true);
               return;
            }
            this.useItemFromSlot(fluidBucket);
            this.lastStepTime = now;
            this.step = 1;
         }
         case 1 -> {
            if (now - this.lastStepTime >= delayMs) {
               this.step = 2;
            }
         }
         case 2 -> {
            int pickupSlot = fluidBucket != -1 ? fluidBucket : emptyBucket;
            if (pickupSlot != -1) {
               this.useItemFromSlot(pickupSlot);
            }
            this.step = 3;
         }
         case 3 -> {
            if (placeSlot != -1 && placementTarget != null) {
               this.placeBlockFromSlot(placeSlot, placementTarget);
            }
            this.reset(true);
         }
         default -> this.reset(true);
      }
   }

   private void useItemFromSlot(int slot) {
      this.selectSlot(slot);
      mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      this.applyMoveFix();
   }

   private void placeBlockFromSlot(int slot, BlockHitResult hit) {
      this.selectSlot(slot);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
      this.applyMoveFix();
   }

   private void selectSlot(int slot) {
      if (slot < 0 || slot > 8 || mc.player == null || mc.getNetworkHandler() == null) {
         return;
      }
      mc.player.getInventory().setSelectedSlot(slot);
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
   }

   private void applyMoveFix() {
      if (this.moveFixSprint
         && mc.player != null
         && !mc.options.sneakKey.isPressed()
         && mc.options.forwardKey.isPressed()) {
         mc.player.setSprinting(true);
      }
   }

   private void reset(boolean restoreSlot) {
      if (restoreSlot && this.originalSlot != -1) {
         this.selectSlot(this.originalSlot);
      }
      this.clearState();
   }

   private void clearState() {
      this.step = -1;
      this.target = null;
      this.originalSlot = -1;
      this.mode = Mode.COBWEB;
      this.useOriginalSlotForPlace = false;
      this.moveFixSprint = false;
   }

   @Override
   public void onEnable() {
      this.lastRightClickPressed = false;
      this.clearState();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.lastRightClickPressed = false;
      this.reset(true);
      super.onDisable();
   }
}
