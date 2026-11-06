package dev.bkclient.systems.modules.modules.bkmods.automarket;

import dev.bkclient.systems.modules.modules.bkmods.AutoMarketHistoryManager;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.game.ClientConstants;
import dev.bkclient.utility.inventory.ItemStackExtractor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import dev.bkclient.utility.network.DiscordWebhook;

import java.util.List;

/**
 * Auto-Sell controller \u2014 scans inventory for items matching sell-enabled
 * PriceEntries
 * and lists them on the market using the sell command.
 * Ported from bk-rynek's scanInventoryAndSell() logic.
 */
public final class AutoSellController {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("\\p{M}");
    private static final java.util.regex.Pattern BK_RE_1 = java.util.regex.Pattern.compile("&(?=[0-9A-FK-ORXa-fk-orx])");

    private boolean pauseActive = false;
    private long lastPauseMs = 0;
    private long lastInventoryScanMs = 0;
    private boolean marketLimitReached = false;
    private AutoMarketController controller;
    private List<PriceEntry> lastKnownEntries = new java.util.ArrayList<>();

    // Pending sell action
    private String pendingSellCommand = null;
    private long pendingSellDelay = 150;
    private long lastActionMs = 0;
    private int pendingSlotId = -1;
    private String pendingExpectedName = null;

    // Minestar split state machine \u2014 runs WITH inventory GUI open so clicks are server-legit
    private enum SplitState { IDLE, OPENING, SPLITTING, VERIFYING, CLOSING }
    private SplitState splitState = SplitState.IDLE;
    private long splitStateMs = 0;
    private int splitSourceIndex = -1; // current inventory index of the item (0-35)
    private int splitHotbarSlot = -1;  // hotbar slot it will be held in for the sell (0-8)
    private int splitKeepCount = 0;
    private String splitItemName = null;
    private double splitSellPrice = 0;
    private boolean splitNeedsRepair = false;

    // Listing tracking (for history)
    private String lastListingItemName = "";
    private double lastListingPrice = 0;
    private int lastListingAmount = 0;

    // Stats
    private int sessionListed = 0;
    private int sessionSold = 0;
    private double sessionProfit = 0;

    // Config
    private static final String DEFAULT_SELL_COMMAND = "wystaw {quantity} {price}";
    private String sellCommandTemplate = DEFAULT_SELL_COMMAND;
    private boolean minestarSellMode = false;
    private boolean minestarConfirmationMode = false;
    private int minestarMarketLimit;
    private boolean autoRepairEnabled = false;
    private String repairCommand = "repair";
    private String webhookUrl = "";
    private long sellIntervalMs = 1000;
    private boolean autoSellEnabled = false;
    private double sellTaxPercent = 2.0;
    private AutoMarketScanner scanner = new AutoMarketScanner();

    private static final long PAUSE_INTERVAL_MS = 10_000;
    private static final long MINESTAR_SPLIT_OPEN_DELAY_MS = 350;

    public AutoSellController() {
    }


    public void setSellCommandTemplate(String template) {
        this.sellCommandTemplate = template;
    }

    private String effectiveSellCommandTemplate() {
        String template = sellCommandTemplate == null ? "" : sellCommandTemplate.trim();
        if (template.isEmpty()) {
            return DEFAULT_SELL_COMMAND;
        }
        return template;
    }

    public void setMinestarSellMode(boolean enabled) {
        this.minestarSellMode = enabled;
    }

    public void setMinestarConfirmationMode(boolean enabled) {
        this.minestarConfirmationMode = enabled;
    }

    public void setAutoSellEnabled(boolean enabled) {
        this.autoSellEnabled = enabled;
    }

    public boolean isAutoSellEnabled() {
        return autoSellEnabled;
    }

    public void setAutoRepairEnabled(boolean enabled) {
        this.autoRepairEnabled = enabled;
    }

    public void setRepairCommand(String cmd) {
        this.repairCommand = cmd;
    }

    public void setWebhookUrl(String v) {
        this.webhookUrl = v;
    }

    public void setSellIntervalMs(long ms) {
        this.sellIntervalMs = ms;
    }

    public void setSellTaxPercent(double percent) {
        this.sellTaxPercent = Math.max(0.0, Math.min(100.0, percent));
    }

    public void setController(AutoMarketController controller) {
        this.controller = controller;
    }

    public void setScanner(AutoMarketScanner scanner) {
        this.scanner = scanner;
    }


    public boolean isPauseActive() {
        return pauseActive;
    }

    public boolean isMarketLimitReached() {
        return marketLimitReached;
    }

    public String getPendingSellCommand() {
        return pendingSellCommand;
    }

    public int getSessionListed() {
        if (minestarConfirmationMode) {
            var history = AutoMarketHistoryManager.getInstance();
            return history == null ? 0 : history.getListedCount();
        }
        return sessionListed;
    }

    public int getSessionSold() {
        if (minestarConfirmationMode) {
            var history = AutoMarketHistoryManager.getInstance();
            return history == null ? 0 : history.getSoldCount();
        }
        return sessionSold;
    }

    public double getSessionProfit() {
        if (minestarConfirmationMode) {
            var history = AutoMarketHistoryManager.getInstance();
            return history == null ? 0 : history.getTotalProfit();
        }
        return sessionProfit;
    }


    /**
     * Called every tick from AutoMarketController.
     * Returns true if auto-sell consumed this tick (caller should skip other
     * logic).
     */
    public boolean onTick(List<PriceEntry> entries) {
        this.lastKnownEntries = entries;
        long now = System.currentTimeMillis();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return false;

        // 0. Minestar split state machine (opens inventory GUI, splits, verifies, then queues sell)
        if (splitState != SplitState.IDLE) {
            tickSplitStateMachine(client, now);
            return true;
        }

        // 1. Execute pending sell command after delay
        if (pendingSellCommand != null) {
            if (now - lastActionMs > pendingSellDelay) {
                // PROTECTION: Do NOT send command if a GUI is open
                if (client.currentScreen != null) {
                    return true;
                }

                // PROTECTION: Verify the item is still in the expected slot
                if (pendingSlotId >= 0 && pendingExpectedName != null) {
                    ItemStack currentStack = client.player.getInventory().getStack(pendingSlotId);
                    String currentName = currentStack.isEmpty() ? "" : currentStack.getName().getString();
                    if (!currentName.equals(pendingExpectedName)) {
                        // Item changed \u2014 user scrolled, cancel and rescan
                        pendingSellCommand = null;
                        pendingSlotId = -1;
                        pendingExpectedName = null;
                        lastInventoryScanMs = 0;
                        return true;
                    }
                    // Force re-select the correct hotbar slot (user may have scrolled)
                    if (pendingSlotId < 9) {
                        client.player.getInventory().selectedSlot = pendingSlotId;
                    }
                }

                if (client.player.networkHandler != null) {
                    String cmd = pendingSellCommand;
                    if (cmd.startsWith("/"))
                        cmd = cmd.substring(1);
                    client.player.networkHandler.sendChatCommand(cmd);
                }
                pendingSellCommand = null;
                pendingSlotId = -1;
                pendingExpectedName = null;
                pendingSellDelay = 500;
                lastActionMs = now;
            }
            return true;
        }

        // 2. Handle active pause (market closed for selling)
        if (pauseActive) {
            if (marketLimitReached) {
                pauseActive = false;
                sendFeedback("&cOsi\u0105gni\u0119to limit wystawionych produkt\u00F3w! Wznawiam kupowanie...");
                if (controller != null) {
                    controller.restartScan(now, 0, true);
                }
                return false;
            }

            long pauseElapsed = now - lastPauseMs;

            // Wait for screen to close
            if (client.currentScreen != null) {
                if (pauseElapsed > 500 && pauseElapsed % 500 < 50) {
                    // Force close retry every 500ms
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.closeHandledScreen();
                            client.setScreen(null);
                        }
                    });
                }
                if (pauseElapsed > 3000) {
                    pauseActive = false;
                    if (controller != null) {
                        controller.restartScan(now);
                    }
                }
                return true;
            }

            // Screen is closed \u2014 perform selling
            if (now >= lastInventoryScanMs && pauseElapsed > sellIntervalMs) {
                scanInventoryAndSell(entries);
            }

            // Exit condition: done selling or timeout
            boolean hasMore = hasItemsToSell(entries);
            if ((!hasMore && pendingSellCommand == null && pauseElapsed > sellIntervalMs + 500)
                    || pauseElapsed > 30000) {
                pauseActive = false;
                if (controller != null) {
                    controller.restartScan(now);
                }
            }
            return true;
        }

        // 3. Background inventory scan (every X ms while no GUI open)
        if (client.currentScreen == null) {
            if (now >= lastInventoryScanMs + sellIntervalMs) {
                // Only scan if not market-limited
                if (!marketLimitReached) {
                    scanInventoryAndSell(entries);
                }
            }
        }

        return false;
    }


    /**
     * Request a sell pause \u2014 close market to list items.
     * Called by AutoMarketController after purchase or periodic scan.
     */
    public boolean shouldTriggerPause(long marketContinuousOpenTime, long lastActionMs) {
        long now = System.currentTimeMillis();
        return !pauseActive && !marketLimitReached
                && (now - marketContinuousOpenTime > 5000)
                && now - lastPauseMs > PAUSE_INTERVAL_MS;
    }

    public void startPause() {
        pauseActive = true;
        lastPauseMs = System.currentTimeMillis();
    }


    private void scanInventoryAndSell(List<PriceEntry> entries) {
        if (marketLimitReached)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        var inv = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty())
                continue;

            // Match against sell-enabled entries
            for (PriceEntry entry : entries) {
                if (!entry.isSellEnabled() || entry.getSellPrice() <= 0)
                    continue;
                if (!scanner.matchesForSell(stack, entry))
                    continue;

                // Quantity check
                // Require both the minimum sell price and the configured quantity.
                int sellQuantity = Math.max(1, entry.getMinSellCount());
                if (stack.getCount() < sellQuantity)
                    continue;

                // Auto-repair before sell if needed
                boolean needsRepair = autoRepairEnabled && stack.isDamageable()
                        && stack.getDamage() > 0;

                // Track for history (shared by both paths)
                lastListingItemName = stack.getName().getString();
                lastListingPrice = entry.getSellPrice();
                lastListingAmount = sellQuantity;

                // Minestar: /ah wystaw sells the WHOLE held stack.
                // If we hold MORE than sellQuantity, hand off to the GUI-open split
                // state machine (legit, server-acked clicks). It also handles moving
                // the item to the hotbar INSIDE the GUI to avoid ghost desync.
                if (minestarSellMode && stack.getCount() > sellQuantity) {
                    splitState = SplitState.OPENING;
                    splitStateMs = System.currentTimeMillis();
                    splitSourceIndex = i;          // where the item currently is (0-35)
                    splitHotbarSlot = i < 9 ? i : 0; // where it will be held for the sell
                    splitKeepCount = sellQuantity;
                    splitItemName = stack.getName().getString();
                    splitSellPrice = entry.getSellPrice();
                    splitNeedsRepair = needsRepair;
                    return; // state machine takes over on subsequent ticks
                }

                // Non-split paths still need the item in the hotbar \u2014 move it now
                int hotbarSlot = i < 9 ? i : 0;
                if (i < 9) {
                    client.player.getInventory().selectedSlot = i;
                } else {
                    client.player.getInventory().selectedSlot = 0;
                    if (client.interactionManager != null) {
                        int syncId = client.player.playerScreenHandler.syncId;
                        client.interactionManager.clickSlot(syncId, i, 0,
                                SlotActionType.SWAP, client.player);
                    }
                }

                // Normal path: exact quantity already held, or non-minestar {quantity} template
                String template = effectiveSellCommandTemplate();
                String cmd = template
                        .replace("{quantity}", String.valueOf(sellQuantity))
                        .replace("{price}", String.valueOf((int) entry.getSellPrice()));
                if (cmd.startsWith("/"))
                    cmd = cmd.substring(1);

                if (needsRepair) {
                    String repCmd = repairCommand;
                    if (repCmd.startsWith("/"))
                        repCmd = repCmd.substring(1);
                    client.player.networkHandler.sendChatCommand(repCmd);
                    pendingSellCommand = cmd;
                    pendingSellDelay = sellIntervalMs + 300;
                } else {
                    pendingSellCommand = cmd;
                    pendingSellDelay = sellIntervalMs;
                }

                lastActionMs = System.currentTimeMillis();
                lastInventoryScanMs = lastActionMs + sellIntervalMs;
                pendingSlotId = i < 9 ? i : 0;
                pendingExpectedName = stack.getName().getString();

                return; // One item per scan cycle
            }
        }

        // Debug feedback if we were in pause but found nothing
    }

    /**
     * Minestar split state machine. Opens the player inventory GUI so the split
     * clicks are processed by the server (no ghost items / desync), splits the
     * held stack down to exactly splitKeepCount, verifies it, closes the GUI,
     * then queues the /ah wystaw command. Runs one step per tick.
     */
    private void tickSplitStateMachine(MinecraftClient client, long now) {
        if (client.player == null) { resetSplit(); return; }

        // Global timeout \u2014 if anything stalls > 4s, abort cleanly
        if (now - splitStateMs > 4000 && splitState != SplitState.OPENING) {
            sendFeedback("&cSplit timeout \u2014 anuluj\u0119 sprzeda\u017C tej partii.");
            if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
                client.player.closeHandledScreen();
            }
            resetSplit();
            lastInventoryScanMs = now + sellIntervalMs;
            return;
        }

        switch (splitState) {
            case OPENING -> {
                // Open the survival inventory GUI client-side
                if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) {
                    client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player));
                    splitStateMs = now;
                } else {
                    if (now - splitStateMs < MINESTAR_SPLIT_OPEN_DELAY_MS) {
                        return;
                    }
                    splitState = SplitState.SPLITTING;
                    splitStateMs = now;
                }
            }
            case SPLITTING -> {
                if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) {
                    // GUI closed unexpectedly \u2014 retry open
                    splitState = SplitState.OPENING;
                    splitStateMs = now;
                    return;
                }
                if (!performSplitClicks(client)) {
                    sendFeedback("&cBrak wolnego slotu na split \u2014 pomijam.");
                    client.player.closeHandledScreen();
                    resetSplit();
                    lastInventoryScanMs = now + sellIntervalMs;
                    return;
                }
                splitState = SplitState.VERIFYING;
                splitStateMs = now;
            }
            case VERIFYING -> {
                ItemStack held = client.player.getInventory().getStack(splitHotbarSlot);
                if (!held.isEmpty() && held.getName().getString().equals(splitItemName)
                        && held.getCount() == splitKeepCount) {
                    // Split confirmed by server-synced client state \u2192 close & sell
                    splitState = SplitState.CLOSING;
                    splitStateMs = now;
                }
                // else keep waiting until count settles (timeout guard above handles stalls)
            }
            case CLOSING -> {
                if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
                    client.player.closeHandledScreen();
                    return; // wait one tick for screen to clear
                }
                // GUI closed \u2014 select the slot and queue the sell command
                if (splitHotbarSlot < 9) {
                    client.player.getInventory().selectedSlot = splitHotbarSlot;
                }

                String cmd = effectiveSellCommandTemplate()
                        .replace("{quantity}", String.valueOf(splitKeepCount))
                        .replace("{price}", String.valueOf((int) splitSellPrice));
                if (cmd.startsWith("/")) cmd = cmd.substring(1);

                if (splitNeedsRepair) {
                    String repCmd = repairCommand.startsWith("/") ? repairCommand.substring(1) : repairCommand;
                    client.player.networkHandler.sendChatCommand(repCmd);
                    pendingSellDelay = sellIntervalMs + 300;
                } else {
                    pendingSellDelay = sellIntervalMs;
                }
                pendingSellCommand = cmd;
                pendingSlotId = splitHotbarSlot;
                pendingExpectedName = splitItemName;
                lastActionMs = now;
                lastInventoryScanMs = now + sellIntervalMs;
                resetSplit();
            }
            default -> resetSplit();
        }
    }

    /**
     * Performs the actual split clicks (must be called with InventoryScreen open).
     * Pick up whole stack \u2192 right-click keepCount times to deposit keepCount back
     * into the source slot \u2192 drop the cursor remainder into an empty slot.
     * @return false if there is no empty slot to hold the remainder.
     */
    private boolean performSplitClicks(MinecraftClient client) {
        var inv = client.player.getInventory();
        int syncId = client.player.currentScreenHandler.syncId;

        // If the item lives in the main inventory, swap it into the target hotbar slot first (inside GUI = legit)
        if (splitSourceIndex >= 9 && splitSourceIndex != splitHotbarSlot) {
            int srcScreen = hotbarToScreenSlot(splitSourceIndex);
            // SWAP button = hotbar slot index (0-8) to swap with
            client.interactionManager.clickSlot(syncId, srcScreen, splitHotbarSlot,
                    SlotActionType.SWAP, client.player);
        }

        ItemStack source = inv.getStack(splitHotbarSlot);
        if (source.isEmpty() || source.getCount() <= splitKeepCount)
            return true; // already correct

        int emptyInvIndex = -1;
        for (int s = 0; s < 36; s++) {
            if (s == splitHotbarSlot) continue;
            if (inv.getStack(s).isEmpty()) { emptyInvIndex = s; break; }
        }
        if (emptyInvIndex == -1)
            return false;

        int sourceSlot = hotbarToScreenSlot(splitHotbarSlot);
        int emptySlot = hotbarToScreenSlot(emptyInvIndex);

        client.interactionManager.clickSlot(syncId, sourceSlot, 0, SlotActionType.PICKUP, client.player);
        for (int k = 0; k < splitKeepCount; k++) {
            client.interactionManager.clickSlot(syncId, sourceSlot, 1, SlotActionType.PICKUP, client.player);
        }
        client.interactionManager.clickSlot(syncId, emptySlot, 0, SlotActionType.PICKUP, client.player);
        return true;
    }

    private void resetSplit() {
        splitState = SplitState.IDLE;
        splitStateMs = 0;
        splitSourceIndex = -1;
        splitHotbarSlot = -1;
        splitKeepCount = 0;
        splitItemName = null;
        splitSellPrice = 0;
        splitNeedsRepair = false;
    }

    /** Maps an Inventory index (0-8 hotbar, 9-35 main) to a PlayerScreenHandler slot id. */
    private int hotbarToScreenSlot(int invIndex) {
        // PlayerScreenHandler: 9-35 = main inventory (same index), 36-44 = hotbar (0-8)
        return invIndex < 9 ? 36 + invIndex : invIndex;
    }

    /**
     * Check if there are items in inventory matching sell entries.
     */
    public boolean hasItemsToSell(List<PriceEntry> entries) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return false;

        var inv = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty())
                continue;

            for (PriceEntry entry : entries) {
                if (!entry.isSellEnabled() || entry.getSellPrice() <= 0)
                    continue;
                if (!scanner.matchesForSell(stack, entry))
                    continue;

                int sellQuantity = Math.max(1, entry.getMinSellCount());
                if (stack.getCount() >= sellQuantity)
                    return true;
            }
        }
        return false;
    }


    /** True when text matches any known "market listing limit reached" phrasing. */
    private boolean isMarketLimitText(String cleanTextLower) {
        String normalized = normalizeSignalText(cleanTextLower);
        if (normalized.contains("limit itemow")
                || normalized.contains("limit produktow")
                || normalized.contains("limit wystawionych")
                || normalized.contains("osiagnales limit")
                || (normalized.contains("osiagn") && normalized.contains("limit"))) {
            return true;
        }
        return cleanTextLower.contains("limit produkt\u00F3w")
                || cleanTextLower.contains("limit wystawionych")
                || cleanTextLower.contains("osi\u0105gn\u0105\u0142e\u015B limit")
                || cleanTextLower.contains("osiagnales limit")
                || cleanTextLower.contains("limit mo\u017Cesz zwi\u0119kszy\u0107")
                || cleanTextLower.contains("limit mozesz zwiekszyc")
                || (cleanTextLower.contains("osiagn") && cleanTextLower.contains("limit"));
    }

    /**
     * Mark the market listing limit as reached. Idempotent: the flag stays set until a
     * sell confirmation clears it, so duplicate detections (e.g. chat + title) are harmless.
     */
    private String normalizeSignalText(String text) {
        String clean = ItemStackExtractor.toReadablePlain(text == null ? "" : text).toLowerCase(java.util.Locale.ROOT);
        clean = BK_RE_0.matcher(java.text.Normalizer.normalize(clean, java.text.Normalizer.Form.NFD)).replaceAll("");
        clean = replacePolishLetters(clean);
        return clean
                .replace('\u0142', 'l')
                .replace('\u0141', 'L')
                .replace('\u0105', 'a')
                .replace('\u0119', 'e')
                .replace('\u00F3', 'o')
                .replace('\u0144', 'n')
                .replace('\u017C', 'z')
                .replace('\u017A', 'z')
                .replace('\u0107', 'c')
                .replace('\u015B', 's');
    }

    private String replacePolishLetters(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('\u0142', 'l')
                .replace('\u0141', 'L')
                .replace('\u0105', 'a')
                .replace('\u0104', 'A')
                .replace('\u0107', 'c')
                .replace('\u0106', 'C')
                .replace('\u0119', 'e')
                .replace('\u0118', 'E')
                .replace('\u0144', 'n')
                .replace('\u0143', 'N')
                .replace('\u00f3', 'o')
                .replace('\u00d3', 'O')
                .replace('\u015b', 's')
                .replace('\u015a', 'S')
                .replace('\u017a', 'z')
                .replace('\u0179', 'Z')
                .replace('\u017c', 'z')
                .replace('\u017b', 'Z');
    }

    private void triggerMarketLimit() {
        marketLimitReached = true;
        pendingSellCommand = null;
        if (pauseActive) {
            sendFeedback("&cOsi\u0105gni\u0119to limit wystawionych produkt\u00F3w! Wznawiam kupowanie...");
            if (controller != null) {
                controller.restartScan(System.currentTimeMillis(), 0, true);
            }
        }
    }

    /**
     * Minestar shows the market-limit notice as a screen title (not chat), so we also scan
     * titles \u2014 but ONLY in minestar sell mode, to avoid reacting to unrelated titles on other
     * servers. Titles never carry sell confirmations, so we check the limit only.
     */
    public void onTitleMessage(String cleanTextLower) {
        if (minestarConfirmationMode) return; // Routed centrally; only LIMIT is accepted from titles.
        String normalized = normalizeSignalText(cleanTextLower);
        if (isRapyTitleSale(normalized)) {
            onChatMessage(normalized);
            return;
        }
        if (isListingConfirmation(cleanTextLower)) {
            onChatMessage(cleanTextLower);
            return;
        }
        if (!minestarSellMode || marketLimitReached) {
            return;
        }
        if (isMarketLimitText(cleanTextLower)) {
            triggerMarketLimit();
        }
    }

    private boolean isRapyTitleSale(String normalizedText) {
        return normalizedText != null
                && normalizedText.contains("przedmiot zostal kupiony")
                && normalizedText.contains("za");
    }

    private boolean isListingConfirmation(String text) {
        String normalized = normalizeSignalText(text);
        return normalized.contains("wystawiono produkt")
                || normalized.contains("wystawiono przedmiot")
                || normalized.contains("successfully listed")
                || (normalized.contains("pomyslnie wystawiles")
                        && (normalized.contains("item") || normalized.contains("przedmiot") || normalized.contains("produkt")));
    }

    /**
     * Process chat messages for sell confirmations and market limits.
     */
    public void onChatMessage(String cleanTextLower) {
        if (minestarConfirmationMode) return; // Never fall back to legacy keyword detection on Minestar.
        // Ignore mod's own messages to avoid feedback loop (e.g. "Item already sold!" resetting the limit)
        if (cleanTextLower.contains("[bk") || cleanTextLower.contains("bk rynek") || cleanTextLower.contains("bk sprzeda\u017C")) {
            return;
        }

        // Market limit detection
        if (isMarketLimitText(cleanTextLower)) {
            triggerMarketLimit();
            return;
        }

        // Listing confirmation
        if (isListingConfirmation(cleanTextLower)) {
            if (!hasPendingListingContext()) {
                return;
            }
            sessionListed++;
            String normalizedListing = normalizeSignalText(cleanTextLower);
            if (normalizedListing.contains("pomyslnie wystawiles")) {
                AutoMarketHistoryManager historyManager = AutoMarketHistoryManager.getInstance();
                if (historyManager != null) {
                    historyManager.incrementListed();
                }
            }

            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                String serverAddr = MinecraftClient.getInstance().getCurrentServerEntry() != null
                        ? MinecraftClient.getInstance().getCurrentServerEntry().address
                        : "local";

                DiscordWebhook.sendListingNotification(
                        webhookUrl, lastListingItemName, String.valueOf((int) lastListingPrice),
                        serverAddr,
                        lastListingAmount);
            }
            clearPendingListingContext();
            return;
        }

        // Sale detection (sold item)
        // 1:1 Parity: Must NOT contain error keywords to be a real sale
        boolean isError = cleanTextLower.contains(ClientConstants.KW_PRAWDOPODOBNIE) || cleanTextLower.contains(ClientConstants.KW_USUN)
                || cleanTextLower.contains(ClientConstants.KW_JUZ_SPRZEDAN + "e") || cleanTextLower.contains(ClientConstants.KW_ALREADY_SOLD);

        // Minestar sprzeda\u017C: "Twoj przedmiot na rynku zostal kupiony przez X za Y"
        boolean isMinestarSale = (cleanTextLower.contains("tw\u00F3j przedmiot") || cleanTextLower.contains("twoj przedmiot"))
                && (cleanTextLower.contains("zosta\u0142 kupiony") || cleanTextLower.contains("zostal kupiony"));

        boolean rapyTitleSale = isRapyTitleSale(normalizeSignalText(cleanTextLower));
        if (!isError && (rapyTitleSale
                || cleanTextLower.contains(ClientConstants.KW_SPRZEDANY_BASE) || cleanTextLower.contains(ClientConstants.KW_BOUGHT_BASE)
                || cleanTextLower.contains("sold") || cleanTextLower.contains("dodane")
                || cleanTextLower.contains(ClientConstants.KW_ZAROBEK) || cleanTextLower.contains(ClientConstants.KW_SPRZEDALES)
                || cleanTextLower.contains(ClientConstants.KW_SPRZEDANO) || isMinestarSale)) {
            sessionSold++;

            String profit = extractPrice(cleanTextLower);
            double profitValue = 0.0;
            if (!"???".equals(profit)) {
                profitValue = AutoMarketScanner.parsePriceWithSuffix(profit);
                if (profitValue > 0) {
                    sessionProfit += profitValue;
                }
            }
            if (rapyTitleSale) {
                AutoMarketHistoryManager historyManager = AutoMarketHistoryManager.getInstance();
                if (historyManager != null) {
                    historyManager.incrementSold();
                    historyManager.decrementListed();
                    if (profitValue > 0) {
                        historyManager.addProfit(profitValue);
                    }
                }
            }
            String msg = Localizator.translate("modules.notifications.auto_market.sold", profit);
            sendFeedback("\u00A7a" + msg);

            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                String serverAddr = MinecraftClient.getInstance().getCurrentServerEntry() != null
                        ? MinecraftClient.getInstance().getCurrentServerEntry().address
                        : "local";

                String itemName = findItemNameByPrice(profit);

                DiscordWebhook.sendSaleNotification(
                        webhookUrl, profit,
                        serverAddr, itemName,
                        "---");
            }

            if (marketLimitReached) {
                marketLimitReached = false;
            }
        }
    }

    /** Called once, after the history manager has accounted for an authoritative message. */
    public void onMinestarEvent(MinestarMessages.Event event) {
        if (!minestarConfirmationMode || event == null) return;
        switch (event.kind()) {
            case PURCHASE -> { }
            case LIMIT -> {
                minestarMarketLimit = event.marketLimit();
                boolean first = !marketLimitReached;
                boolean wasPaused = pauseActive;
                marketLimitReached = true;
                pauseActive = false;
                pendingSellCommand = null;
                pendingSlotId = -1;
                pendingExpectedName = null;
                resetSplit();
                clearPendingListingContext();
                if (first) {
                    sendFeedback("\u00a7cLimit rynku: " + minestarMarketLimit + ". Wznawiam kupowanie.");
                    if (wasPaused && controller != null) {
                        controller.restartScan(System.currentTimeMillis(), 0, true);
                    }
                }
            }
            case LISTING -> {
                // Counted by HistoryManager even for manual listings. Metadata is used only
                // if it belongs to this outstanding auto-listing, never to multiply the price.
                if (hasPendingListingContext() && lastListingAmount == event.quantity()
                        && (int) lastListingPrice == event.amount()
                        && webhookUrl != null && !webhookUrl.isEmpty()) {
                    DiscordWebhook.sendListingNotification(webhookUrl, lastListingItemName,
                            event.displayPrice(), minestarServerAddress(), event.quantity());
                }
                clearPendingListingContext();
            }
            case SALE -> {
                marketLimitReached = false;
                String proceeds = AutoMarketHistoryManager.formatPrice(event.proceeds(sellTaxPercent));
                sendFeedback("\u00a7a" + Localizator.translate("modules.notifications.auto_market.sold", proceeds));
                if (webhookUrl != null && !webhookUrl.isEmpty()) {
                    String itemName = null;
                    for (PriceEntry entry : lastKnownEntries) {
                        if (entry.isSellEnabled() && (int) entry.getSellPrice() == event.amount()) {
                            String storedName = entry.getName();
                            itemName = ItemStackExtractor.toReadablePlain(storedName == null ? ""
                                    : BK_RE_1.matcher(storedName).replaceAll("\u00a7"));
                            break;
                        }
                    }
                    DiscordWebhook.sendSaleNotification(webhookUrl, proceeds,
                            minestarServerAddress(), itemName, "---");
                }
            }
        }
    }

    private String minestarServerAddress() {
        var server = MinecraftClient.getInstance().getCurrentServerEntry();
        return server == null ? "local" : server.address;
    }

    private boolean hasPendingListingContext() {
        return lastListingAmount > 0
                && lastListingPrice > 0
                && lastListingItemName != null
                && !lastListingItemName.isBlank();
    }

    private void clearPendingListingContext() {
        lastListingItemName = "";
        lastListingPrice = 0;
        lastListingAmount = 0;
    }


    public void reset() {
        pauseActive = false;
        lastPauseMs = 0;
        lastInventoryScanMs = 0;
        marketLimitReached = false;
        pendingSellCommand = null;
        pendingSellDelay = 500;
        lastActionMs = 0;
        resetSplit();
        // NOTE: lastListingItemName/Price/Amount intentionally NOT reset here \u2014
        // server confirmation message arrives AFTER reset(), clearing too early loses webhook data
    }

    public void resetSessionStats() {
        sessionListed = 0;
        sessionSold = 0;
        sessionProfit = 0;
    }

    private String findItemNameByPrice(String profitStr) {
        try {
            double profitVal = dev.bkclient.systems.modules.modules.bkmods.automarket.AutoMarketScanner
                    .parsePriceWithSuffix(profitStr);
            double taxFactor = 1.0 - (sellTaxPercent / 100.0);
            for (PriceEntry entry : lastKnownEntries) {
                if (entry.isSellEnabled() && Math.round(entry.getSellPrice() * taxFactor) == Math.round(profitVal)) {
                    return entry.getName();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractPrice(String msg) {
        try {
            // Priority 1: Specifically look for the profit after keywords
            java.util.regex.Pattern priorityPattern = java.util.regex.Pattern.compile(
                    "(?:dodane|profit|zarobek|otrzymasz|za|sold for)\\s*([\\d\\s.,]+[kKmMldLD]?)\\s*\\$");
            java.util.regex.Matcher priorityMatcher = priorityPattern.matcher(msg);
            if (priorityMatcher.find()) {
                return priorityMatcher.group(1).trim();
            }

            java.util.regex.Pattern minestarPattern = java.util.regex.Pattern.compile(
                    "(?:za)\\s*([0-9][0-9\\s.,]*[kKmMldLD]?)");
            java.util.regex.Matcher minestarMatcher = minestarPattern.matcher(msg);
            if (minestarMatcher.find()) {
                return minestarMatcher.group(1).trim();
            }

            // Priority 3: Standard number followed by $ (fallback)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([\\d\\s.,]+[kKmMldLD]?)\\s*\\$");
            java.util.regex.Matcher matcher = pattern.matcher(msg);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception ignored) {
        }
        return "???";
    }

    private void sendFeedback(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String prefix = "\u00A78[\u00A72B\u00A7aK \u00A7fClient\u00A78] ";
            client.player.sendMessage(net.minecraft.text.Text.literal(prefix + msg), false);
        }
    }
}


