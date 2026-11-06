package dev.bkclient.systems.modules.modules.bkmods;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.network.SendPacketEvent;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.event.impl.window.MouseEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.bkmods.enchant.EnchantRules;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.inventory.ItemStackExtractor;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(name = "Auto Zmianka", category = ModuleCategory.PLAYER)
public final class AutoZmianka extends BaseModule {
    private static final String KEY = "modules.settings.auto_zmianka.";
    private static final long RESPONSE_TIMEOUT_MS = 5000;
    private final BindSetting snipeKey = new BindSetting(this, KEY + "snipe_key");
    private final ModeSetting mode = new ModeSetting(this, KEY + "mode");
    private final ModeSetting.Value minimum = new ModeSetting.Value(mode, KEY + "minimum").select();
    private final ModeSetting.Value range = new ModeSetting.Value(mode, KEY + "range");
    private final ModeSetting.Value exact = new ModeSetting.Value(mode, KEY + "exact");
    private final SliderSetting value = new SliderSetting(this, KEY + "value", () -> range.isSelected())
            .min(0).max(80).step(1).currentValue(50).suffix("%");
    private final SliderSetting from = new SliderSetting(this, KEY + "from", () -> !range.isSelected())
            .min(0).max(80).step(1).currentValue(40).suffix("%");
    private final SliderSetting to = new SliderSetting(this, KEY + "to", () -> !range.isSelected())
            .min(0).max(80).step(1).currentValue(50).suffix("%");
    private final BooleanSetting advanced = new BooleanSetting(this, KEY + "advanced");
    private final SliderSetting delay = new SliderSetting(this, KEY + "delay", () -> !advanced.isEnabled())
            .min(500).max(3000).step(50).currentValue(750).suffix(" ms");
    private final ModeSetting source = new ModeSetting(this, KEY + "source", () -> !advanced.isEnabled());
    private final ModeSetting.Value automatic = new ModeSetting.Value(source, KEY + "automatic").select();
    private final ModeSetting.Value guiSlot = new ModeSetting.Value(source, KEY + "gui_slot");
    private final ModeSetting.Value mainHand = new ModeSetting.Value(source, KEY + "main_hand");
    private final SliderSetting slotNumber = new SliderSetting(this, KEY + "slot", () -> !advanced.isEnabled() || !guiSlot.isSelected())
            .min(1).max(54).step(1).currentValue(14);
    private final BooleanSetting showRolls = new BooleanSetting(this, KEY + "show_rolls", () -> !advanced.isEnabled()).enable();

    private GenericContainerScreenHandler session;
    private Slot target;
    private Item targetItem;
    private long openedAt;
    private long nextClickAt;
    private long clickedAt;
    private long resultAt;
    private int rolls;
    private int costBefore;
    private double before;
    private EnchantRules.Reading result;
    private volatile Watch watch;
    private volatile boolean ownClick;
    private volatile boolean manualClick;
    private volatile boolean manualClose;
    private boolean reopened;
    private boolean running;
    private long resumeNotBefore;

    /** Network threads only enqueue immutable snapshots; all decisions happen on the client tick. */
    private record Watch(int syncId, int slot, int inventoryIndex, ConcurrentLinkedQueue<ItemStack> updates) {}

    private final EventListener<ReceivePacketEvent> onPacket = event -> {
        Watch current = watch;
        if (current == null || event.isCancelled()) return;
        ItemStack stack = null;
        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            if (packet.getSyncId() == current.syncId() && packet.getSlot() == current.slot()) {
                stack = packet.getStack();
            } else if (current.inventoryIndex() >= 0 && packet.getSyncId() == 0
                    && packet.getSlot() == playerScreenSlot(current.inventoryIndex())) {
                stack = packet.getStack();
            }
        } else if (event.getPacket() instanceof InventoryS2CPacket packet) {
            int index = packet.getSyncId() == current.syncId() ? current.slot()
                    : packet.getSyncId() == 0 && current.inventoryIndex() >= 0
                    ? playerScreenSlot(current.inventoryIndex()) : -1;
            if (index >= 0 && index < packet.getContents().size()) stack = packet.getContents().get(index);
        } else if (event.getPacket() instanceof SetPlayerInventoryS2CPacket packet
                && current.inventoryIndex() >= 0 && packet.slot() == current.inventoryIndex()) {
            stack = packet.contents();
        }
        if (stack != null) current.updates().offer(stack.copy());
    };
    private final EventListener<SendPacketEvent> onClick = event -> {
        if (!ownClick && session != null && event.getPacket() instanceof ClickSlotC2SPacket) manualClick = true;
        // Server CloseScreenS2CPacket uses closeScreen(), which sends no client close packet.
        if (session != null && event.getPacket() instanceof CloseHandledScreenC2SPacket) manualClose = true;
    };
    private final EventListener<WorldChangeEvent> onWorld = event -> reset();
    private final EventListener<KeyPressEvent> onKey = event -> {
        if (!event.isCancelled() && isSnipeInput(event.getKey())) {
            if (event.getAction() == 1) toggleRunning();
            // Consume repeats/releases too: the bind must not drop or swap the hovered sword.
            event.cancel();
        }
    };
    private final EventListener<MouseEvent> onMouse = event -> {
        if (!event.isCancelled() && isSnipeInput(event.getButton())) {
            if (event.getAction() == 1) toggleRunning();
            event.cancel();
        }
    };

    private boolean isSnipeInput(int key) {
        return isEnabled() && snipeKey.getKey() != -1 && snipeKey.isKey(key)
                && mc.player != null && ((running && mc.currentScreen == null)
                || (mc.currentScreen instanceof GenericContainerScreen screen
                && EnchantRules.normalize(screen.getTitle().getString()).equals("zaczarowanie przedmiotu")));
    }

    private void toggleRunning() {
        if (running) {
            stop("paused");
        } else if (System.nanoTime() / 1_000_000 < resumeNotBefore) {
            message("settling");
        } else {
            reset();
            running = true;
            message("started");
        }
    }

    @Override
    public void onEnable() {
        reset();
        // An enabled module is only armed. Every run requires an explicit Snipe key press.
        if (!isSilentToggle()) message(snipeKey.getKey() == -1 ? "set_key" : "ready");
    }

    @Override
    public void onDisable() { reset(); }

    private void reset() {
        if (watch != null) resumeNotBefore = Math.max(resumeNotBefore, clickedAt + RESPONSE_TIMEOUT_MS);
        running = false;
        watch = null;
        session = null;
        target = null;
        targetItem = null;
        result = null;
        manualClick = false;
        manualClose = false;
        reopened = false;
        ownClick = false;
        rolls = 0;
    }

    @Override
    public void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }
        if (!running) return;
        long now = System.nanoTime() / 1_000_000;
        if (manualClick) { stop("manual"); return; }
        GenericContainerScreenHandler handler = mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container
                ? container : null;
        boolean matchingMenu = handler != null && mc.currentScreen instanceof GenericContainerScreen screen
                && EnchantRules.normalize(screen.getTitle().getString()).equals("zaczarowanie przedmiotu")
                && (session == null || session.getInventory().size() == handler.getInventory().size());
        EnchantRules.MenuAction action = EnchantRules.menuAction(session != null, session == handler, matchingMenu,
                mc.currentScreen == null, watch != null, manualClose, now - clickedAt, RESPONSE_TIMEOUT_MS);
        if (action == EnchantRules.MenuAction.WAIT) return;
        if (action == EnchantRules.MenuAction.STOP) { stop("closed"); return; }
        if (session == null) {
            session = handler;
            openedAt = now;
            nextClickAt = now + 750;
        } else if (action == EnchantRules.MenuAction.REATTACH) {
            // Keep the pending roll, timer and original sword slot across the server's menu refresh.
            int slotId = target.id;
            int inventoryIndex = watch.inventoryIndex();
            if (slotId >= handler.slots.size()) { stop("no_lore"); return; }
            Slot refreshedTarget = handler.getSlot(slotId);
            if ((refreshedTarget.inventory == mc.player.getInventory()) != (inventoryIndex >= 0)
                    || (inventoryIndex >= 0 && refreshedTarget.getIndex() != inventoryIndex)) {
                stop("no_lore");
                return;
            }
            session = handler;
            target = refreshedTarget;
            watch = new Watch(handler.syncId, slotId, inventoryIndex, new ConcurrentLinkedQueue<>());
            result = null;
            reopened = true;
        }
        if (range.isSelected() && from.getCurrentValue() > to.getCurrentValue()) {
            stop("bad_range");
            return;
        }
        if (target == null) {
            if (now < nextClickAt) return;
            int selected = findTarget(handler);
            if (selected < 0) {
                if (now - openedAt >= RESPONSE_TIMEOUT_MS) stop(selected == -2 ? "ambiguous" : "no_lore");
                return;
            }
            target = handler.getSlot(selected);
            targetItem = target.getStack().getItem();
            report(read(target.getStack()));
        }
        if (watch != null) {
            ItemStack updated;
            while ((updated = watch.updates().poll()) != null) {
                if (acceptResult(updated, now)) return;
            }
            // Initial contents for the new syncId may arrive before tick() can reattach the watcher.
            // The new handler's sword is authoritative too; wait while its contents are still empty.
            if (reopened && acceptResult(target.getStack(), now)) return;
            // Require a server update of THIS item, plus a changed bonus or spent currency.
            // This also permits consecutive rolls with the same percentage.
            if (result != null && now - resultAt >= 300 && now - clickedAt >= delay.getCurrentValue()
                    && EnchantRules.completedRoll(before, result.percent(), costBefore, countCurrency())) {
                EnchantRules.Reading live = read(target.getStack());
                if (live != null && live.percent() == result.percent() && target.getStack().isOf(targetItem)) {
                    if (showRolls.isEnabled()) report(result);
                    watch = null;
                    reopened = false;
                    result = null;
                    nextClickAt = now + 100;
                }
            }
            if (watch != null && now - clickedAt >= RESPONSE_TIMEOUT_MS) stop("timeout");
            return;
        }
        if (!target.getStack().isOf(targetItem)) { stop("no_lore"); return; }
        EnchantRules.Reading current = read(target.getStack());
        if (current == null) { stop("no_lore"); return; }
        if (matches(current.percent())) { finish(current); return; }
        if (now < nextClickAt) return;
        if (!handler.getCursorStack().isEmpty()) { stop("cursor"); return; }
        int button = findButton(handler);
        if (button < 0) { stop("no_button"); return; }
        int currency = countCurrency();
        if (currency == 0) { stop("no_currency"); return; }
        before = current.percent();
        costBefore = currency;
        result = null;
        reopened = false;
        clickedAt = now;
        watch = new Watch(handler.syncId, target.id,
                target.inventory == mc.player.getInventory() ? target.getIndex() : -1, new ConcurrentLinkedQueue<>());
        if (rolls == 0) message("clicking", button + 1);
        rolls++;
        ownClick = true;
        try {
            mc.interactionManager.clickSlot(handler.syncId, button, 0, SlotActionType.PICKUP, mc.player);
        } finally {
            ownClick = false;
        }
    }

    private boolean acceptResult(ItemStack updated, long now) {
        EnchantRules.Reading reading = updated.isOf(targetItem) ? read(updated) : null;
        if (reading != null && matches(reading.percent())) {
            finish(reading);
            return true;
        }
        if (reading == null || result == null || reading.percent() != result.percent()) resultAt = now;
        result = reading;
        return false;
    }

    private int findTarget(GenericContainerScreenHandler handler) {
        if (guiSlot.isSelected()) {
            int id = (int) slotNumber.getCurrentValue() - 1;
            return id < handler.getInventory().size() && !isButton(handler.getSlot(id).getStack())
                    && read(handler.getSlot(id).getStack()) != null ? id : -1;
        }
        if (automatic.isSelected()) {
            // The user's server shows the enchanted sword in row 2, column 5 (slot 14).
            if (handler.getInventory().size() > 13 && !isButton(handler.getSlot(13).getStack())
                    && read(handler.getSlot(13).getStack()) != null) return 13;
            int found = -1;
            for (int i = 0; i < handler.getInventory().size(); i++) {
                ItemStack stack = handler.getSlot(i).getStack();
                if (!isButton(stack) && read(stack) != null) {
                    if (found >= 0) return -2;
                    found = i;
                }
            }
            if (found >= 0) return found;
        }
        // Never select an arbitrary sword from the rest of the player's inventory.
        for (Slot slot : handler.slots) {
            if (slot.inventory == mc.player.getInventory() && slot.getIndex() == mc.player.getInventory().selectedSlot
                    && read(slot.getStack()) != null) return slot.id;
        }
        return -1;
    }

    private int findButton(GenericContainerScreenHandler handler) {
        int found = -1;
        for (int i = 0; i < handler.getInventory().size(); i++) {
            if (isButton(handler.getSlot(i).getStack())) {
                if (found >= 0) return -1;
                found = i;
            }
        }
        return found;
    }

    private static boolean isButton(ItemStack stack) {
        return !stack.isEmpty() && EnchantRules.normalize(stack.getName().getString()).equals("zaczaruj przedmiot");
    }

    private int countCurrency() {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && EnchantRules.normalize(stack.getName().getString()).equals("zaczarowanie przedmiotu")) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int playerScreenSlot(int index) { return index < 9 ? 36 + index : index == 40 ? 45 : index; }

    private static EnchantRules.Reading read(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        // Same raw component and text extraction as AutoMarketScanner; no tooltip/OCR guessing.
        return lore == null ? null : EnchantRules.read(lore.lines().stream().map(ItemStackExtractor::toLegacy).toList());
    }

    private boolean matches(double percent) {
        return EnchantRules.matches(percent, range.isSelected() ? EnchantRules.Mode.RANGE
                : exact.isSelected() ? EnchantRules.Mode.EXACT : EnchantRules.Mode.MINIMUM,
                range.isSelected() ? from.getCurrentValue() : value.getCurrentValue(), to.getCurrentValue());
    }

    private void report(EnchantRules.Reading reading) {
        if (reading != null) message("reading", formatPercent(reading.percent()), target.id + 1);
    }

    private void finish(EnchantRules.Reading reading) {
        String percent = formatPercent(reading.percent());
        message("success", percent, rolls);
        BKClientInit.getInstance().getNotificationManager().addThemedNotification(NotificationType.SUCCESS,
                Localizator.translate("auto_zmianka.notification.title", percent),
                Localizator.translate("auto_zmianka.notification.description", rolls), 5000L);
        watch = null;
        reset();
    }

    private void stop(String reason) {
        message(reason);
        reset();
    }

    private void message(String key, Object... args) {
        Text message = Text.literal("Auto Zmianka: " + Localizator.translate("auto_zmianka." + key, args));
        switch (key) {
            case "bad_range", "ambiguous", "no_lore", "timeout", "cursor", "no_button", "no_currency" ->
                    MessageUtility.warn(message);
            default -> MessageUtility.info(message);
        }
    }

    private static String formatPercent(double percent) {
        return BigDecimal.valueOf(percent).stripTrailingZeros().toPlainString();
    }
}
