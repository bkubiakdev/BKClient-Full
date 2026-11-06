package dev.bkclient.systems.modules.modules.player;

 import dev.bkclient.BKClientInit;
 import dev.bkclient.systems.event.EventListener;
 import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
 import dev.bkclient.systems.event.impl.window.KeyPressEvent;
 import dev.bkclient.systems.localization.Localizator;
 import dev.bkclient.systems.modules.api.ModuleCategory;
 import dev.bkclient.systems.modules.api.ModuleInfo;
 import dev.bkclient.systems.modules.impl.BaseModule;
 import dev.bkclient.systems.notifications.NotificationType;
 import dev.bkclient.systems.setting.settings.BindSetting;
 import dev.bkclient.systems.setting.settings.BooleanSetting;
 import dev.bkclient.systems.setting.settings.SliderSetting;
 import dev.bkclient.utility.time.Timer;
 import net.minecraft.entity.player.PlayerInventory;
 import net.minecraft.item.ItemStack;
 import net.minecraft.screen.ScreenHandler;
 import net.minecraft.screen.slot.Slot;
 import net.minecraft.screen.slot.SlotActionType;

 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;

 @ModuleInfo(name = "Inventory Set", category = ModuleCategory.PLAYER, desc = "Saves and restores inventory and hotbar layouts")
 public class InventorySet extends BaseModule {
     private static final int HOTBAR_SIZE = 9;
     private static final int HOTBAR_HISTORY_SIZE = 7;
     private static final int HOTBAR_DIFF_THRESHOLD = 3;
     private static final int HOTBAR_REACTION_TICKS = 100;
     private static final int HOTBAR_COOLDOWN_TICKS = 25;

     private final BindSetting swapKey = new BindSetting(this, "modules.settings.inventory_set.swap_key");
     private final BindSetting saveKey = new BindSetting(this, "modules.settings.inventory_set.save_key");
     private final BooleanSetting autoDetectHotbar = new BooleanSetting(this,
             "modules.settings.inventory_set.auto_detect_hotbar");
     private final SliderSetting delay = new SliderSetting(this, "modules.settings.inventory_set.delay")
             .min(0.0f).max(100.0f).currentValue(25.0f).step(5.0f);

     private final Map<Integer, ItemStack> savedLayout = new HashMap<>();
     private final LinkedList<List<ItemStack>> hotbarHistory = new LinkedList<>();
     private final List<Integer> hotbarRestoreSlots = new ArrayList<>();
     private final Timer actionTimer = new Timer();
     private final Timer hotbarActionTimer = new Timer();
     private List<ItemStack> detectedHotbar = null;
     private boolean active = false;
     private boolean restoringHotbar = false;
     private int hotbarReactionTicks = 0;
     private int hotbarCooldownTicks = 0;

     @SuppressWarnings("unused")
     private final EventListener<KeyPressEvent> onKeyPress = event -> {
         if (event.getAction() != 1) {
             return;
         }
         if (mc.player == null || mc.interactionManager == null) return;

         if (this.saveKey.isKey(event.getKey())) {
             saveLayout();
             BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS,
                     "Inventory Set", "Layout saved");
         }

         if (this.swapKey.isKey(event.getKey())) {
             if (restoringHotbar) {
                 stopHotbarRestore("Hotbar restore stopped");
                 return;
             }
             if (startDetectedHotbarRestore()) {
                 return;
             }
             if (active) {
                 active = false;
                 BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.INFO,
                         "Inventory Set", "Restore stopped");
             } else {
                 if (savedLayout.isEmpty()) {
                     BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.ERROR,
                             "Inventory Set", "No saved layout");
                     return;
                 }
                 active = true;
                 actionTimer.reset();
                 BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS,
                         "Inventory Set", "Restoring layout...");
             }
         }
     };

     @SuppressWarnings("unused")
     private final EventListener<ClientPlayerTickEvent> onTick = event -> {
         tickAutoDetectHotbar();
         tickHotbarRestore();

         if (!active || mc.player == null || mc.interactionManager == null) {
             return;
         }

         long delayVal = (long) delay.getCurrentValue();
         ScreenHandler handler = mc.player.currentScreenHandler;
         if (handler == null) {
             active = false;
             return;
         }

         while (actionTimer.finished(delayVal)) {
             if (!restoreTick(handler)) {
                 break;
             }
             actionTimer.reset();
             if (delayVal > 0) {
                 break;
             }
         }
     };

     @Override
     public void onDisable() {
         active = false;
         resetHotbarDetection();
         super.onDisable();
     }

     private void saveLayout() {
         savedLayout.clear();
         PlayerInventory inventory = mc.player.getInventory();
         for (int i = 0; i < inventory.main.size(); i++) {
             ItemStack stack = inventory.main.get(i);
             if (!stack.isEmpty()) {
                 savedLayout.put(i, stack.copy());
             }
         }
         // Off-hand intentionally excluded — bot never touches slot 40
     }

     private boolean restoreTick(ScreenHandler handler) {
         ItemStack cursorStack = handler.getCursorStack();

         if (cursorStack.isEmpty()) {
             for (Map.Entry<Integer, ItemStack> entry : savedLayout.entrySet()) {
                 int targetInvIdx = entry.getKey();
                 ItemStack targetStack = entry.getValue();

                 int guiSlotId = invIdxToGuiSlotId(targetInvIdx, handler);
                 if (guiSlotId == -1) {
                     continue;
                 }

                 ItemStack currentStack = handler.getSlot(guiSlotId).getStack();

                 if (!isSameItem(currentStack, targetStack)) {
                     int sourceGuiSlotId = findItemInInventory(handler, targetStack, targetInvIdx);
                     if (sourceGuiSlotId != -1) {
                         clickSlot(sourceGuiSlotId);
                         return true;
                     }
                 }
             }

             active = false;
             BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS,
                     "Inventory Set", "Layout restored");
             return false;
         } else {
             int targetInvIdx = -1;
             for (Map.Entry<Integer, ItemStack> entry : savedLayout.entrySet()) {

                 if (isSameItem(cursorStack, entry.getValue())) {
                     int guiId = invIdxToGuiSlotId(entry.getKey(), handler);
                     if (guiId != -1 && !isSameItem(handler.getSlot(guiId).getStack(), entry.getValue())) {
                         targetInvIdx = entry.getKey();
                         break;
                     }
                 }
             }

             if (targetInvIdx != -1) {
                 int guiSlotId = invIdxToGuiSlotId(targetInvIdx, handler);
                 clickSlot(guiSlotId);
                 return true;
             } else {
                 // Try free slot first
                 int freeSlot = findFreeUselessSlot(handler);
                 if (freeSlot != -1) {
                     clickSlot(freeSlot);
                     return true;
                 }
                 // Eq full — find a slot that has the "degraded" version of cursor item
                 // e.g. cursor=water_bucket, slot has bucket → swap there
                 int swapSlot = findSwapSlot(handler, cursorStack);
                 if (swapSlot != -1) {
                     clickSlot(swapSlot);
                     return true;
                 }
                 active = false;
                 return false;
             }
         }
     }

     private void tickAutoDetectHotbar() {
         if (!autoDetectHotbar.isEnabled()) {
             resetHotbarDetection();
             return;
         }
         if (mc.player == null) {
             resetHotbarDetection();
             return;
         }
         if (hotbarCooldownTicks > 0) {
             hotbarCooldownTicks--;
             hotbarHistory.clear();
         }
         if (hotbarReactionTicks > 0) {
             hotbarReactionTicks--;
         }
         if (restoringHotbar || active || hotbarCooldownTicks > 0 || mc.currentScreen != null) {
             return;
         }

         List<ItemStack> currentHotbar = getHotbarSnapshot();
         hotbarHistory.addFirst(currentHotbar);
         if (hotbarHistory.size() > HOTBAR_HISTORY_SIZE) {
             hotbarHistory.removeLast();
         }
         if (hotbarHistory.size() != HOTBAR_HISTORY_SIZE) {
             return;
         }

         List<ItemStack> oldHotbar = hotbarHistory.getLast();
         int diff = 0;
         for (int i = 0; i < HOTBAR_SIZE; i++) {
             if (!isSameItem(currentHotbar.get(i), oldHotbar.get(i))) {
                 diff++;
             }
         }
         if (diff >= HOTBAR_DIFF_THRESHOLD && hotbarReactionTicks <= 0) {
             detectedHotbar = copyStacks(oldHotbar);
             hotbarReactionTicks = HOTBAR_REACTION_TICKS;
             BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.INFO,
                     "Inventory Set", Localizator.translate("modules.notifications.inventory_set.hotbar_shuffle_detected"));
             startDetectedHotbarRestore();
         }
     }

     private void tickHotbarRestore() {
         if (!restoringHotbar || mc.player == null || mc.interactionManager == null) {
             return;
         }

         long delayVal = (long) delay.getCurrentValue();
         while (hotbarActionTimer.finished(delayVal)) {
             if (hotbarRestoreSlots.isEmpty()) {
                 finishHotbarRestore();
                 return;
             }
             restoreHotbarSlot(hotbarRestoreSlots.remove(0));
             hotbarActionTimer.reset();
             if (delayVal > 0) {
                 break;
             }
         }
     }

     private boolean startDetectedHotbarRestore() {
         if (!autoDetectHotbar.isEnabled() || detectedHotbar == null || hotbarReactionTicks <= 0 || mc.player == null
                 || mc.interactionManager == null) {
             return false;
         }

         active = false;
         restoringHotbar = true;
         hotbarRestoreSlots.clear();
         for (int i = 0; i < HOTBAR_SIZE; i++) {
             if (!detectedHotbar.get(i).isEmpty()) {
                 hotbarRestoreSlots.add(i);
             }
         }
         if (mc.currentScreen != null) {
             mc.setScreen(null);
         }
         stopAllMovement();
         hotbarActionTimer.reset();
         BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS,
                 "Inventory Set", "Restoring hotbar...");
         return true;
     }

     private void restoreHotbarSlot(int targetHotbarSlot) {
         if (detectedHotbar == null || targetHotbarSlot < 0 || targetHotbarSlot >= HOTBAR_SIZE) {
             return;
         }

         ItemStack goal = detectedHotbar.get(targetHotbarSlot);
         if (goal.isEmpty() || isSameItem(mc.player.getInventory().main.get(targetHotbarSlot), goal)) {
             return;
         }

         ScreenHandler handler = mc.player.currentScreenHandler;
         for (int invIdx = 0; invIdx < mc.player.getInventory().main.size(); invIdx++) {
             if (invIdx == targetHotbarSlot) {
                 continue;
             }
             if (!isSameItem(mc.player.getInventory().main.get(invIdx), goal)) {
                 continue;
             }

             int sourceGuiSlotId = invIdxToGuiSlotId(invIdx, handler);
             if (sourceGuiSlotId != -1) {
                 mc.interactionManager.clickSlot(handler.syncId, sourceGuiSlotId, targetHotbarSlot,
                         SlotActionType.SWAP, mc.player);
             }
             return;
         }
     }

     private void finishHotbarRestore() {
         restoringHotbar = false;
         detectedHotbar = null;
         hotbarReactionTicks = 0;
         hotbarCooldownTicks = HOTBAR_COOLDOWN_TICKS;
         hotbarHistory.clear();
         BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS,
                 "Inventory Set", "Hotbar restored");
     }

     private void stopHotbarRestore(String message) {
         restoringHotbar = false;
         hotbarRestoreSlots.clear();
         BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.INFO,
                 "Inventory Set", message);
     }

     private void resetHotbarDetection() {
         restoringHotbar = false;
         detectedHotbar = null;
         hotbarReactionTicks = 0;
         hotbarCooldownTicks = 0;
         hotbarHistory.clear();
         hotbarRestoreSlots.clear();
     }

     private List<ItemStack> getHotbarSnapshot() {
         List<ItemStack> snapshot = new ArrayList<>();
         for (int i = 0; i < HOTBAR_SIZE; i++) {
             snapshot.add(mc.player.getInventory().main.get(i).copy());
         }
         return snapshot;
     }

     private List<ItemStack> copyStacks(List<ItemStack> source) {
         List<ItemStack> copy = new ArrayList<>();
         for (ItemStack stack : source) {
             copy.add(stack.copy());
         }
         return copy;
     }

     private void stopAllMovement() {
         if (mc.options == null) {
             return;
         }
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
     }

     private int findItemInInventory(ScreenHandler handler, ItemStack target, int targetInvIdx) {
         for (int i = 0; i < handler.slots.size(); i++) {
             Slot slot = handler.getSlot(i);
             if (!(slot.inventory instanceof PlayerInventory) || !slot.hasStack()) continue;
             int invIdx = guiSlotToInvIdx(i, handler);
             // Don't pull from off-hand unless the target slot IS off-hand
             if (invIdx == 40 && targetInvIdx != 40) continue;
             if (isSameItem(slot.getStack(), target)) {
                 if (invIdx != -1 && invIdx != targetInvIdx) {
                     ItemStack shouldBeHere = savedLayout.get(invIdx);
                     if (shouldBeHere != null && isSameItem(slot.getStack(), shouldBeHere)) {
                         continue; // don't steal from already-correct slot
                     }
                 }
                 return i;
             }
         }
         return -1;
     }

     // Finds a slot to swap into when eq is full:
     // Priority 1 — slot with same item type as cursor but NOT in savedLayout (e.g. bucket when cursor=water_bucket)
     // Priority 2 — any slot not in savedLayout (not off-hand)
     private int findSwapSlot(ScreenHandler handler, ItemStack cursor) {
         int fallback = -1;
         for (int i = 0; i < handler.slots.size(); i++) {
             Slot slot = handler.getSlot(i);
             if (!(slot.inventory instanceof PlayerInventory) || !slot.hasStack()) continue;
             int invIdx = guiSlotToInvIdx(i, handler);
             if (invIdx == 40) continue; // never off-hand
             if (invIdx >= 0 && invIdx <= 8) continue; // never hotbar
             // Skip slots that are already correct per savedLayout
             if (invIdx != -1 && savedLayout.containsKey(invIdx) && isSameItem(slot.getStack(), savedLayout.get(invIdx))) continue;
             // Priority 1: same base item (e.g. bucket vs water_bucket share Item type base)
             if (slot.getStack().getItem() == cursor.getItem()) {
                 return i;
             }
             if (fallback == -1) fallback = i;
         }
         return fallback;
     }

     private int findFreeUselessSlot(ScreenHandler handler) {
         for (int i = 0; i < handler.slots.size(); i++) {
             Slot slot = handler.getSlot(i);
             if (slot.inventory instanceof PlayerInventory) {
                 int invIdx = guiSlotToInvIdx(i, handler);
                 if (invIdx == 40) continue; // never use off-hand as temp slot
                 if (!slot.hasStack() && (invIdx == -1 || !savedLayout.containsKey(invIdx))) {
                     return i;
                 }
             }
         }
         return -1;
     }

     private void clickSlot(int id) {
         if (id != -1) {
             mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, 0, SlotActionType.PICKUP,
                     mc.player);
         }
     }

     private boolean isSameItem(ItemStack s1, ItemStack s2) {
         if (s1.isEmpty() || s2.isEmpty()) {
             return s1.isEmpty() == s2.isEmpty();
         }
         if (s1.isDamageable() && s2.isDamageable()) {
             ItemStack copy1 = s1.copy();
             ItemStack copy2 = s2.copy();
             copy1.setDamage(0);
             copy2.setDamage(0);
             return ItemStack.areItemsAndComponentsEqual(copy1, copy2);
         }
         return ItemStack.areItemsAndComponentsEqual(s1, s2);
     }

     private int invIdxToGuiSlotId(int invIdx, ScreenHandler handler) {
         if (handler instanceof net.minecraft.screen.PlayerScreenHandler) {
             if (invIdx >= 0 && invIdx <= 8) {
                 return 36 + invIdx;
             }
             if (invIdx >= 9 && invIdx <= 35) {
                 return invIdx;
             }
             if (invIdx == 40) {
                 return 45;
             }
         }
         int totalSlots = handler.slots.size();
         if (totalSlots >= 36) {
             int playerInvStart = totalSlots - 36;
             if (invIdx >= 0 && invIdx <= 8) {
                 return playerInvStart + 27 + invIdx;
             }
             if (invIdx >= 9 && invIdx <= 35) {
                 return playerInvStart + (invIdx - 9);
             }
         }
         return -1;
     }

     private int guiSlotToInvIdx(int guiSlotId, ScreenHandler handler) {
         if (guiSlotId < 0 || guiSlotId >= handler.slots.size()) {
             return -1;
         }
         Slot slot = handler.getSlot(guiSlotId);
         if (slot.inventory instanceof PlayerInventory) {
             int index = slot.getIndex();
             if (index >= 0 && index <= 35) {
                 return index;
             }
             if (index == 40) {
                 return 40;
             }
         }
         return -1;
     }
 }
