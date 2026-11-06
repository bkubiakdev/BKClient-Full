package dev.bkclient.utility.integration;

import dev.bkclient.BKClientInit;
import dev.bkclient.mixin.accessors.ClientPlayerInteractionManagerAccessor;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.modules.player.GuiMove;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.inventory.InventoryUtility;
import dev.bkclient.utility.inventory.ItemSlot;
import dev.bkclient.utility.inventory.group.SlotGroup;
import dev.bkclient.utility.inventory.group.SlotGroups;
import dev.bkclient.utility.inventory.slots.HotbarSlot;
import dev.bkclient.utility.inventory.slots.InventorySlot;
import dev.bkclient.utility.time.Timer;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class SwapIntegration implements IMinecraft {
   private Item itemToUse = null;
   private HotbarSlot originalSlot = null;
   private boolean isProcessingItem = false;
   private ItemSlot targetSlot = null;
   private final Timer itemUseTimer = new Timer();
   private SwapIntegration.ItemUseState currentState = SwapIntegration.ItemUseState.IDLE;
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (this.isProcessingItem) {
         this.processItemUse();
      }
   };

   public SwapIntegration() {
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   private void processItemUse() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && mc.player.getItemCooldownManager() != null) {
         if (!(this.targetSlot instanceof HotbarSlot)) {
            BKClientInit.getInstance().getModuleManager().getModule(GuiMove.class).setStay(true);
         }

         switch (this.currentState) {
            case USING_ITEM:
               if (this.targetSlot instanceof HotbarSlot) {
                  ((ClientPlayerInteractionManagerAccessor)mc.interactionManager)
                     .invokeSendSequencedPacket(
                        mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch())
                     );
                  this.currentState = SwapIntegration.ItemUseState.RETURNING_SLOT;
               } else if (BKClientInit.getInstance().getModuleManager().getModule(GuiMove.class).canSend()) {
                  ((ClientPlayerInteractionManagerAccessor)mc.interactionManager)
                     .invokeSendSequencedPacket(
                        mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch())
                     );
                  this.currentState = SwapIntegration.ItemUseState.RETURNING_SLOT;
               }
               break;
            case RETURNING_SLOT:
               if (this.targetSlot instanceof HotbarSlot) {
                  InventoryUtility.selectHotbarSlot(this.originalSlot);
                  this.resetUseState();
               } else if (BKClientInit.getInstance().getModuleManager().getModule(GuiMove.class).canSend()) {
                  HotbarSlot currentSlot = InventoryUtility.getCurrentHotbarSlot();
                  InventoryUtility.hotbarSwap(this.targetSlot.getIdForServer(), this.originalSlot.getSlotId());
                  this.resetUseState();
               }
               break;
            default:
               this.isProcessingItem = false;
               this.currentState = SwapIntegration.ItemUseState.IDLE;
         }
      } else {
         this.isProcessingItem = false;
         this.currentState = SwapIntegration.ItemUseState.IDLE;
      }
   }

   public void useItem(Item itemType) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && mc.currentScreen == null && !this.isProcessingItem) {
         SlotGroup<ItemSlot> group = SlotGroups.hotbar().and(SlotGroups.inventory());
         ItemSlot itemSlot = group.findItem(itemType);
         if (itemSlot == null) {
            BKClientInit.getInstance()
               .getNotificationManager()
               .addNotificationOther(
                  NotificationType.ERROR, "Item not found", "You need " + itemType.getName().getString() + " in your inventory"
               );
         } else if (!mc.player.getItemCooldownManager().isCoolingDown(itemSlot.itemStack())) {
            this.itemToUse = itemType;
            this.originalSlot = InventoryUtility.getCurrentHotbarSlot();
            this.targetSlot = itemSlot;
            this.isProcessingItem = true;
            this.currentState = SwapIntegration.ItemUseState.USING_ITEM;
            this.itemUseTimer.reset();
            if (itemSlot instanceof HotbarSlot itemHotbarSlot) {
               if (InventoryUtility.getCurrentHotbarSlot().item() != itemType) {
                  InventoryUtility.selectHotbarSlot(itemHotbarSlot);
               }
            } else if (itemSlot instanceof InventorySlot itemInventorySlot) {
               HotbarSlot currentSlot = InventoryUtility.getCurrentHotbarSlot();
               InventoryUtility.hotbarSwap(itemInventorySlot.getIdForServer(), currentSlot.getSlotId());
            }
         }
      }
   }

   private void resetUseState() {
      this.isProcessingItem = false;
      this.currentState = SwapIntegration.ItemUseState.IDLE;
      this.itemToUse = null;
      this.originalSlot = null;
      this.targetSlot = null;
   }

   private enum ItemUseState {
      IDLE,
      USING_ITEM,
      RETURNING_SLOT;
   }
}
