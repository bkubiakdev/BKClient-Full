package dev.bkclient.systems.modules.modules.bkmods.automarket;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.utility.game.XSK;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarketHistoryManager;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.utility.network.DiscordWebhook;
import dev.bkclient.utility.game.ScreenshotHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Main automation state machine \u2014 orchestrates scan \u2192 buy \u2192 confirm \u2192 restart
 * flow.
 * Ported from bk-rynek's SearchAutomationController.
 * Uses raw packet clicks for maximum speed.
 */
public final class AutoMarketController {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("\\p{M}");
    private static final java.util.regex.Pattern BK_RE_1 = java.util.regex.Pattern.compile("\u00A7[0-9a-fk-or]");


    private static final Pattern PAGE_PATTERN = Pattern.compile(XSK.PAGE_REGEX);
    private static final long MODE_FIRST_SCAN_WINDOW_MS = 220L;

    public volatile boolean debugMode = false;
    private long lastDebugMs = 0;

    public void setScannerDebug(boolean enabled) {
        if (scanner != null) scanner.debugMatch = enabled;
    }

    public void setLoreRegex(String regex) {
        if (scanner != null) scanner.setLoreRegex(regex);
    }

    public void setLoreSellerRegex(String regex) {
        if (scanner != null) scanner.setLoreSellerRegex(regex);
    }


    public static class SnipeEntry {
        public final String name;
        public final double price;
        public final int count;
        public final long timestamp;

        public SnipeEntry(String name, double price, int count) {
            this.name = name;
            this.price = price;
            this.count = count;
            this.timestamp = System.currentTimeMillis();
        }
    }


    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "automarket-scheduler");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> SCAN_FUTURE = null;

    private volatile boolean active = false;
    private volatile boolean buyingItem = false;
    private volatile boolean purchasePending = false;
    private volatile boolean isRestarting = false;
    private volatile boolean isInsideConfirmScreen = false;
    private volatile long refreshBlockedUntil = 0;
    private volatile boolean isAnarchia = false;
    private volatile boolean aggressiveMode = false;
    private volatile long purchaseSucceededUntil = 0;

    // AtomicBoolean lock: prevents two scanner iterations from both entering the
    // buy path simultaneously
    private final AtomicBoolean buyLock = new AtomicBoolean(false);
    private final AtomicLong purchaseGeneration = new AtomicLong();

    private long buyingStartTime = 0;

    private String startBuyTitle = "";
    // volatile: read from background scanner thread, written from render/main
    // thread
    private volatile long lastActionMs;
    private volatile long lastClickMs;
    private String lastPageSignature = "";
    private boolean waitingForConfirmMsg = false;
    private int confirmClickCount = 0;

    private String lastMatchedItemName = "";
    private String lastMatchedPrice = "";
    private double lastMatchedPriceDouble = 0;
    private int lastMatchedSlotId = -1;
    private int lastMatchedAmount = 0;
    private String lastMatchedLore = "";
    private String lastMatchedEnchants = "";
    private String webhookUrl = "";
    private volatile boolean webhookScreenshot = false;
    private String customMarketTitle = "";
    private volatile boolean ignoreMarketTitle = false;
    private volatile boolean minestarMode = false;
    private volatile boolean rapyMode = false;
    private volatile int minestarNextSlot = 53;
    private volatile int minestarPrevSlot = 52;
    private boolean minestarOnNextPage = true;

    private long marketFirstSeenTime = 0;
    private long marketContinuousOpenTime = 0;
    private boolean marketDetectedLogged = false;
    private long lastSyncId = -1;
    private int lastSyncIdWhenBought = -1;
    private int cachedMarketItemCount = 0;
    private int cachedConfirmSlotFound = -1;
    private long cachedSlotAnalysisSyncId = -2;
    private long screenFirstSeenTime = 0;
    private boolean sortingConfirmed = false;
    public long sortingPauseUntil = 0;
    private boolean waitingForMarketOpen = false;

    private int openAttemptsLeft = 0;
    private long lastOpenAttemptMs = 0;
    private long stuckInConfirmStartTime = 0;
    private long lastFrozenCheckMs = 0;
    private String idleMarketSignature = "";
    private long idleMarketSinceMs = 0;
    private long lastIdleMarketResetMs = 0;
    private static final long IDLE_MARKET_RESET_MS = 10_000L;
    private long restartStartTime = 0;
    private int extraRestartDelay = 0;
    private volatile long initialScanPauseUntil = 0;
    private volatile long modeFirstScanWindowUntil = 0;

    // Caches
    private final Map<Integer, Long> purchasedCache = new ConcurrentHashMap<>();
    private int consecutivePurchaseFailures = 0;
    private final Map<String, Long> tooExpensiveCache = new ConcurrentHashMap<>();
    // ConcurrentHashMap: written from scanner thread, read/written from render
    // thread
    private final Map<Integer, Long> invalidSlots = new ConcurrentHashMap<>();
    private final Set<Integer> currentMatches = ConcurrentHashMap.newKeySet();

    private net.minecraft.item.ItemStack lastMatchedItemStack = net.minecraft.item.ItemStack.EMPTY;

    public Set<Integer> getCurrentMatches() {
        return currentMatches;
    }

    // Session Stats (Delegated to AutoMarketHistoryManager)
    private String lastKnownItem = "None";
    private String lastKnownPrice = "---";
    private final List<SnipeEntry> snipeHistory = new ArrayList<>();

    // Threading flags
    private volatile boolean isThreadedBuyActive = false;
    private volatile boolean isThreadedInitialClickActive = false;

    private String confirmItemName = "";

    public List<PriceEntry> getEntries() {
        return entries;
    }

    public java.util.Map<String, List<PriceEntry>> getEntryMap() {
        return entryMap;
    }

    // Config references (set by AutoMarket module)
    private volatile String marketCommand = "/rynek";
    private volatile int sortingSlot = 53;
    private volatile int nextPageSlot = 51;
    private volatile int refreshSlot = 0;
    private volatile int confirmSlot = 11;

    private final AutoMarket module;
    private final AutoSellController autoSellController;
    private final AutoMarketScanner scanner;
    private volatile List<PriceEntry> entries = new java.util.ArrayList<>();
    private volatile java.util.Map<String, List<PriceEntry>> entryMap = new java.util.HashMap<>();

    public AutoMarketController(AutoMarket module, AutoSellController autoSellController) {
        this.module = module;
        this.autoSellController = autoSellController;
        this.scanner = new AutoMarketScanner();
        this.autoSellController.setController(this);
    }

    private volatile int refreshDelayMs = 25;
    private volatile int confirmDelayMs = 50;
    private volatile int restartDelayMs = 800;
    private volatile int confirmAttempts = 5;
    private volatile String sortingKeyword = "najnowsz";
    private volatile long priceVersion = 0;

    public void notifyRegistryChanged() {
        if (module != null && !module.isLoading()
                && dev.bkclient.systems.setting.GuiSaveBatch.defer(this, this::notifyRegistryChanged)) return;
        this.priceVersion++;
        if (module == null || module.isLoading())
            return;

        try {
            if (BKClientInit.getInstance().getConfigManager() != null &&
                    BKClientInit.getInstance().getConfigManager().getAutoSaveConfig() != null) {
                BKClientInit.getInstance().getConfigManager().getAutoSaveConfig().save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (module.configManager != null) {
            String name = module.getActiveProfile();
            if (name == null || name.isEmpty())
                name = "default";
            module.configManager.save(name);
        }
    }

    public long getPriceVersion() {
        return priceVersion;
    }

    public AutoMarketScanner getScanner() {
        return scanner;
    }


    public void setEntries(List<PriceEntry> entries) {
        this.entries = entries;
        java.util.Map<String, List<PriceEntry>> map = new java.util.HashMap<>();
        if (entries != null) {
            for (PriceEntry e : entries) {
                String mat = e.getMaterial().toLowerCase();
                if (mat.startsWith(XSK.MINECRAFT_PFX))
                    mat = mat.substring(XSK.MINECRAFT_PFX.length());
                map.computeIfAbsent(mat, k -> new java.util.ArrayList<>()).add(e);
            }
        }
        this.entryMap = map;
    }

    public void setAggressiveMode(boolean v) {
        this.aggressiveMode = v && !isSortingDisabledForMode();
    }

    public void setMarketCommand(String v) {
        this.marketCommand = v;
    }

    public void setSortingKeyword(String v) {
        if (rapyMode)
            return;
        this.sortingKeyword = v;
    }

    public void setIsAnarchia(boolean v) {
        this.isAnarchia = v;
        if (v && !isSortingDisabledForMode()) {
            this.aggressiveMode = true;
            this.sortingKeyword = XSK.SORT_KW_NOWSZE;
        }
    }

    public void setSortingSlot(int v) {
        if (rapyMode)
            return;
        this.sortingSlot = v;
    }

    public void setNextPageSlot(int v) {
        this.nextPageSlot = v;
    }

    public void setConfirmDelayMs(int v) {
        this.confirmDelayMs = v;
    }

    public void setConfirmAttempts(int v) {
        this.confirmAttempts = v;
    }

    public void setRefreshDelayMs(int v) {
        this.refreshDelayMs = v;
    }

    public void setRestartDelayMs(int v) {
        this.restartDelayMs = v;
    }


    public void setCustomMarketTitle(String v) {
        this.customMarketTitle = v;
    }

    public void setIgnoreMarketTitle(boolean v) {
        this.ignoreMarketTitle = (this.minestarMode || this.rapyMode) && v;
    }

    public void setMinestarMode(boolean v) {
        if (this.minestarMode != v) {
            this.minestarOnNextPage = true;
            this.lastPageSignature = "";
        }
        this.minestarMode = v;
        this.ignoreMarketTitle = this.minestarMode || this.rapyMode;
        if (v) {
            this.sortingConfirmed = true;
            this.sortingPauseUntil = 0;
        }
    }

    public void setRapyMode(boolean v) {
        if (this.rapyMode != v) {
            this.lastPageSignature = "";
        }
        this.rapyMode = v;
        this.ignoreMarketTitle = this.minestarMode || this.rapyMode;
        if (v) {
            this.aggressiveMode = false;
            this.sortingConfirmed = true;
            this.sortingPauseUntil = 0;
        }
    }

    private boolean isSortingDisabledForMode() {
        return minestarMode || rapyMode;
    }

    public void setMinestarSlots(int nextSlot, int prevSlot) {
        this.minestarNextSlot = nextSlot;
        this.minestarPrevSlot = prevSlot;
    }

    public void setConfirmSlot(int v) {
        this.confirmSlot = v;
    }

    public void setRefreshSlot(int v) {
        this.refreshSlot = v;
    }

    public void setWebhookUrl(String v) {
        this.webhookUrl = v;
    }

    public void setWebhookScreenshot(boolean v) {
        this.webhookScreenshot = v;
    }

    public void setConfirmItemName(String v) {
        this.confirmItemName = v;
    }


    public boolean isActive() {
        return active;
    }

    public boolean isBuyingItem() {
        return buyingItem;
    }

    public boolean isInsideConfirmScreen() {
        return isInsideConfirmScreen;
    }

    public int getSessionBought() {
        return AutoMarketHistoryManager.getInstance().getBoughtCount();
    }

    public double getSessionProfit() {
        return AutoMarketHistoryManager.getInstance().getTotalProfit();
    }

    public long getSessionStartTime() {
        return AutoMarketHistoryManager.getInstance().getStartTime();
    }

    public String getLastKnownItem() {
        return lastKnownItem;
    }

    public String getLastKnownPrice() {
        return lastKnownPrice;
    }

    public List<SnipeEntry> getSnipeHistory() {
        return snipeHistory;
    }

    public String getCurrentStatus() {
        return active ? (buyingItem ? "\u00A7eBUYING" : "\u00A7aSCANNING") : "\u00A77IDLE";
    }


    public void start() {
        reset();
        if (autoSellController != null) autoSellController.reset();
        if (isSortingDisabledForMode()) {
            sortingConfirmed = true;
            sortingPauseUntil = 0;
            initialScanPauseUntil = 0;
        }
        active = true;
        sendFeedback("\u00A7aSkaner uruchomiony! Tryb: " + (aggressiveMode ? XSK.MODE_AGGRESSIVE : XSK.MODE_NORMAL));
    }

    public void cancel() {
        if (active)
            active = false;
        stopThreadedScanner();
        reset();
        if (autoSellController != null) autoSellController.reset();
    }


    /**
     * Called from HandledScreenMixin.onRender() every frame.
     */
    public void onScreenRender(Text title, ScreenHandler handler, List<Slot> slots) {
        if (!active || mc.player == null || mc.currentScreen == null)
            return;

        if (slots == null || handler == null)
            return;

        long now = System.currentTimeMillis();
        String currentTitle = title.getString().toLowerCase();
        int syncId = handler.syncId;

        // frame) ---
        // (applied in the syncId-change block below)

        boolean titleIsMarket = currentTitle.contains(XSK.TITLE_RYNEK) || currentTitle.contains(XSK.TITLE_AUKCJ)
                || currentTitle.contains(XSK.TITLE_AUCTION) || currentTitle.contains(XSK.TITLE_ITEMY)
                || currentTitle.contains("aukcje") || currentTitle.contains("aukcja")
                || (!customMarketTitle.isEmpty() && currentTitle.contains(customMarketTitle.toLowerCase()));

        int marketItemCount;
        int confirmSlotFound;
        if (syncId == cachedSlotAnalysisSyncId) {
            marketItemCount = cachedMarketItemCount;
            confirmSlotFound = cachedConfirmSlotFound;
        } else {
            marketItemCount = 0;
            confirmSlotFound = -1;
        for (int i = 0; i < Math.min(slots.size(), 54); i++) {
            boolean needSeller = marketItemCount < 5;
            boolean needConfirm = confirmSlotFound == -1;
            if (!needSeller && !needConfirm)
                break;

            Slot s = slots.get(i);
            if (s == null || !s.hasStack())
                continue;

            long invalidUntil = invalidSlots.getOrDefault(i, 0L);
            if (now < invalidUntil)
                continue;

            ItemStack stack = s.getStack();
            String name = stack.getName().getString().toLowerCase();

            List<Text> tooltip;
            try {
                tooltip = stack.getTooltip(net.minecraft.item.Item.TooltipContext.DEFAULT, null,
                        net.minecraft.item.tooltip.TooltipType.BASIC);
            } catch (Exception ignored) {
                continue;
            }

            boolean hasSellerLore = false;
            boolean hasPriceLore = false;
            if (needSeller || needConfirm) {
                for (Text t : tooltip) {
                    String line = t.getString().toLowerCase();
                    if (line.contains("wystawi") || line.contains("seller")) {
                        hasSellerLore = true;
                    }
                    if (line.contains("cena") || line.contains("koszt") || line.contains("$")
                            || line.contains("waluta") || line.contains("monet") || line.contains("kredyt")
                            || line.contains("price")) {
                        hasPriceLore = true;
                    }
                    if (hasSellerLore && hasPriceLore)
                        break;
                }
            }

            if (needSeller && hasSellerLore) {
                marketItemCount++;
            }

            if (needConfirm && !hasSellerLore) {
                boolean isConfirm = (confirmItemName != null && !confirmItemName.isEmpty())
                        ? name.contains(confirmItemName.toLowerCase())
                        : (name.contains(XSK.KW_POTWIERDZ1) || name.contains(XSK.KW_POTWIERDZ2)
                                || name.contains(XSK.KW_CONFIRM) || name.contains(XSK.KW_ZAKUP)
                                || name.contains(XSK.KW_POTWIERDZANIE) || name.contains(XSK.KW_NA_PEWNO)
                                || name.contains(XSK.KW_SURE)
                                || (currentTitle.contains("zakup") && name.contains("produkt")));
                // Like SAC: cancel button also proves we are in a confirm screen even if
                // the confirm button has an unexpected name. Don't click cancel,
                // but use it as a detection signal (sentinel -2).
                boolean isCancelOnly = !isConfirm && (name.contains(XSK.KW_ANULUJ) || name.contains(XSK.KW_CANCEL));
                if (isConfirm) {
                    confirmSlotFound = i;
                } else if (isCancelOnly && confirmSlotFound == -1) {
                    confirmSlotFound = -2; // detected via cancel button; confirmSlot falls back to config
                }
            }
        }
            cachedMarketItemCount = marketItemCount;
            cachedConfirmSlotFound = confirmSlotFound;
            cachedSlotAnalysisSyncId = syncId;
        } // end else (syncId changed)

        // confirmSlotFound >= 0: exact slot found; -2: screen detected via cancel
        // button only
        final boolean confirmKeywords = (confirmSlotFound >= 0 || confirmSlotFound == -2)
                || currentTitle.contains(XSK.KW_NA_PEWNO) || currentTitle.contains(XSK.KW_SURE)
                || currentTitle.contains(XSK.KW_POTWIERDZ1) || currentTitle.contains(XSK.KW_POTWIERDZ2)
                || currentTitle.contains(XSK.KW_CONFIRM)
                || (currentTitle.contains("zakup") && marketItemCount <= 1);

        boolean looksLikeMarket = ((minestarMode || rapyMode) && ignoreMarketTitle) || titleIsMarket || marketItemCount >= 2;


        if (lastSyncId != syncId) {
            screenFirstSeenTime = now;
            lastSyncId = syncId;
            stuckInConfirmStartTime = 0;
            // Like SAC: reset thread-active flags so the next syncId starts clean.
            // The actual threads exit via their own loop conditions (buyingItem, syncId
            // check).
            isThreadedBuyActive = false;
            isThreadedInitialClickActive = false;

            // Anarchia auto-config on screen change (not every frame)
            String address = (mc.getCurrentServerEntry() != null)
                    ? mc.getCurrentServerEntry().address.toLowerCase()
                    : "";
            boolean isAn = address.contains(XSK.ANARCHIA_GG) || address.contains(XSK.ANARCHIA_PL);
            if (isAn && !isSortingDisabledForMode()) {
                this.setSortingSlot(53);
                this.setNextPageSlot(51);
                this.setSortingKeyword(XSK.SORT_KW_NOWSZE);
                this.setAggressiveMode(true);
                this.isAnarchia = true;
            }

            // Start scanner only when a new market screen appears
            if (active && looksLikeMarket && !confirmKeywords) {
                if (isSortingDisabledForMode()) {
                    modeFirstScanWindowUntil = now + MODE_FIRST_SCAN_WINDOW_MS;
                }
                startThreadedScanner(handler);
            } else {
                stopThreadedScanner();
            }
        }

        // 1. FAST EXIT: If we are already doing a purchase, handle confirmation
        if (buyingItem || purchasePending) {
            String cleanTitle = currentTitle.toLowerCase();
            boolean isSuccess = cleanTitle.contains(XSK.KW_ZAKUPION) || cleanTitle.contains(XSK.KW_SUKCES)
                    || cleanTitle.contains(XSK.KW_POMYSLN) || cleanTitle.contains(XSK.KW_BOUGHT)
                    || cleanTitle.contains(XSK.KW_GRATULACJ);

            boolean isFail = cleanTitle.contains(XSK.KW_BRAK3) || cleanTitle.contains(XSK.KW_BRAK2)
                    || cleanTitle.contains(XSK.KW_JUZ_SPRZEDAN) || cleanTitle.contains(XSK.KW_USUNIETO)
                    || cleanTitle.contains(XSK.KW_PRAWDOPODOBNIE) || cleanTitle.contains(XSK.KW_ERROR)
                    || cleanTitle.contains(XSK.KW_BLAD_PL);

            handleConfirmScreen(handler, syncId, currentTitle, now, confirmKeywords, isSuccess, isFail);
            return;
        }

        if (looksLikeMarket && !marketDetectedLogged) {
            sendFeedback(Localizator.translate("modules.notifications.auto_market.market_detected"));
            marketDetectedLogged = true;
        }

        // Inventory safety
        if (active && !buyingItem) {
            int emptySlots = 0;
            var inv = mc.player.getInventory();
            for (int i = 0; i < 36; i++) {
                if (inv.getStack(i).isEmpty())
                    emptySlots++;
            }
            if (emptySlots < 1) {
                String msg = Localizator.translate("modules.notifications.auto_market.inventory_full");
                sendFeedback("\u00A7c" + msg);
                BKClientInit.getInstance().getNotificationManager().addNotification(NotificationType.ERROR, msg);
                active = false;
                if (webhookUrl != null && !webhookUrl.isEmpty()) {
                    String serverAddr = mc.getCurrentServerEntry() != null
                            ? mc.getCurrentServerEntry().address
                            : "local";
                    DiscordWebhook.sendFullInventoryNotification(webhookUrl, serverAddr);
                }
                return;
            }
        }

        boolean isMarket = (marketItemCount > 1) || (looksLikeMarket && !confirmKeywords);
        if (marketContinuousOpenTime > 0 && looksLikeMarket && !confirmKeywords) {
            isMarket = true;
        }

        isInsideConfirmScreen = confirmKeywords && !isMarket;

        if (isMarket && !isRestarting && !purchasePending) {
            if (minestarMode || rapyMode) {
                sortingConfirmed = true;
                sortingPauseUntil = 0;
            }

            // Safety: if we're back in market with buyingItem=true, the purchase
            // succeeded or failed without a chat message \u2014 reset after 1s like bk-rynek
            if (buyingItem && now - buyingStartTime > 1000) {
                buyingItem = false;
                buyLock.set(false);
                isInsideConfirmScreen = false;
            }

            if (marketFirstSeenTime == 0)
                marketFirstSeenTime = now;
            if (marketContinuousOpenTime == 0)
                marketContinuousOpenTime = now;

            // If the same market page stays open without any slot/page change,
            // the server or GUI can be stuck. Close it and let the normal restart
            // path reopen the configured market command.
            String currentMarketSignature = buildPageSignature(slots);
            if (!currentMarketSignature.equals(idleMarketSignature)) {
                idleMarketSignature = currentMarketSignature;
                idleMarketSinceMs = now;
            } else if (!buyingItem && !purchasePending
                    && idleMarketSinceMs > 0
                    && now - idleMarketSinceMs >= IDLE_MARKET_RESET_MS
                    && now - lastIdleMarketResetMs >= IDLE_MARKET_RESET_MS) {
                lastIdleMarketResetMs = now;
                sendFeedback("§c[Watchdog] /ah bez zmian przez 10 s — hard reset rynku");
                restartScan(now, 500, true);
                return;
            }

            // Scanner is already running (started on syncId change) \u2014 no restart needed
            // here

            if (aggressiveMode && !isSortingDisabledForMode()) {
                if (handleSortingOnly(handler, slots, syncId, now, sortingKeyword)) {
                    if (!buyingItem && !purchasePending)
                        currentMatches.clear();
                    return;
                }
            } else {
                if (!isSortingDisabledForMode() && now < initialScanPauseUntil) {
                    currentMatches.clear();
                    return;
                }
                handlePageNavigation(handler, slots, title, now);
            }

            // Auto-sell pause check
            if (autoSellController != null && !autoSellController.isPauseActive() && entries != null) {
                if (autoSellController.shouldTriggerPause(marketContinuousOpenTime, lastActionMs)
                        && autoSellController.hasItemsToSell(entries)) {
                    autoSellController.startPause();
                    sendFeedback("\u00A7e[Auto-Sell] Zamykanie rynku...");
                    closeScreen();
                    return;
                }
            }
        } else {
            marketFirstSeenTime = 0;
            stopThreadedScanner();
        }

        if (!active || isRestarting)
            return;

        // Confirm branch fallback
        if (confirmKeywords && (buyingItem || purchasePending)) {
            String cleanTitle = currentTitle.toLowerCase();
            boolean isSuccess = cleanTitle.contains(XSK.KW_ZAKUPION) || cleanTitle.contains(XSK.KW_SUKCES);
            boolean isFail = cleanTitle.contains(XSK.KW_ERROR) || cleanTitle.contains(XSK.KW_BLAD_PL);
            handleConfirmScreen(handler, syncId, currentTitle, now, confirmKeywords, isSuccess, isFail);
        }
    }


    private void handleConfirmScreen(ScreenHandler handler, int syncId,
            String currentTitle, long now, boolean confirmKeywords, boolean isSuccess, boolean isFail) {
        if (now < purchaseSucceededUntil)
            return;

        // Mark confirm screen so scanner thread doesn't scan it
        if (confirmKeywords)
            isInsideConfirmScreen = true;

        if (!buyingItem && !purchasePending) {
            // Stuck: confirm screen visible but no active purchase
            if (stuckInConfirmStartTime == 0) {
                stuckInConfirmStartTime = now;
            } else if (now - stuckInConfirmStartTime > 2000) {
                stuckInConfirmStartTime = 0;
                isInsideConfirmScreen = false;
                sendFeedback("\u00A7cZaci\u0119cie na ekranie potwierdzenia \u2014 restart!");
                restartScan(now, 500);
            }
            return;
        }
        stuckInConfirmStartTime = 0;

        // Hard timeout 6s \u2014 serwer nie odpowiedzia\u0142 wcale
        if (buyingItem && now - buyingStartTime > 6000) {
            buyingItem = false;
            buyLock.set(false);
            isInsideConfirmScreen = false;
            isThreadedBuyActive = false;
            sendFeedback("\u00A7cTimeout zakupu \u2014 restart!");
            restartScan(now, 1000);
            return;
        }

        int confirmDelay = (confirmDelayMs > 0) ? confirmDelayMs : 10;

        int targetSlot = findConfirmSlotID(handler.slots, currentTitle);
        if (targetSlot == -1)
            targetSlot = confirmSlot;

        boolean windowDelayPassed = (now - screenFirstSeenTime >= confirmDelay);
        boolean confirmSlotDetected = findConfirmSlotID(handler.slots, currentTitle) != -1;
        boolean confirmVisible = confirmKeywords || confirmSlotDetected;

        if (confirmVisible && windowDelayPassed) {
            if (aggressiveMode || isAnarchia) {
                if (!isThreadedBuyActive) {
                    startThreadedBuy(syncId, targetSlot, confirmDelay);
                }
            } else if (now - lastActionMs > confirmDelay) {
                if (confirmClickCount < confirmAttempts) {
                    clickSlot(handler, targetSlot, 0);
                    confirmClickCount++;
                    lastActionMs = now;
                    sendFeedback("\u00A7ePotwierdzanie: slot " + targetSlot + " (pr\u00F3ba " + confirmClickCount + "/" + confirmAttempts + ")");
                } else {
                    buyingItem = false;
                    buyLock.set(false);
                    sendFeedback("\u00A7cMax attempts reached.");
                    restartScan(now, 1000);
                }
            }
        }
    }


    private boolean handleSortingOnly(ScreenHandler handler, List<Slot> slots, int syncId, long now,
            String keyword) {
        if (isSortingDisabledForMode()) {
            sortingConfirmed = true;
            sortingPauseUntil = 0;
            return false;
        }

        if (buyingItem || purchasePending) {
            return !isInsideConfirmScreen;
        }

        if (sortingSlot < 0 || sortingSlot >= handler.slots.size()) {
            sortingConfirmed = true;
            return false;
        }
        Slot sortSlot = handler.getSlot(sortingSlot);
        if (sortSlot == null || !sortSlot.hasStack()) {
            sortingConfirmed = true;
            return false;
        }

        if (sortingConfirmed && now < sortingPauseUntil) {
            return false;
        }

        boolean isSorted = isNajnowszeSelected(sortSlot.getStack(), keyword);

        if (now < sortingPauseUntil && sortingConfirmed) {
            isSorted = true;
        }

        if (!isSorted) {
            sortingConfirmed = false;
            if (now - lastClickMs > 50) {
                final int fSyncId = syncId;
                mc.execute(() -> {
                    if (active && mc.player != null
                            && mc.player.currentScreenHandler != null
                            && mc.player.currentScreenHandler.syncId == fSyncId) {
                        int button = isAnarchia ? 1 : 0;
                        sendClickPacket(fSyncId, sortingSlot, button);
                    }
                });
                lastClickMs = now;
                lastActionMs = now;
                sortingPauseUntil = now + (aggressiveMode ? Math.max(550, Math.min(900, refreshDelayMs + 200)) : 1500);
            }
            return true;
        } else {
            if (!sortingConfirmed) {
                sortingConfirmed = true;
                BKClientInit.getInstance().getNotificationManager().addNotificationOther(
                        dev.bkclient.systems.notifications.NotificationType.SUCCESS,
                        XSK.LABEL_AUTO_MARKET, "\u00A7aWykryto sortowanie: " + XSK.KW_NAJNOWSZE);
                // Scanner may need to be started now that sorting is confirmed
                if (mc.player != null && mc.player.currentScreenHandler != null) {
                    startThreadedScanner(mc.player.currentScreenHandler);
                }
            }
            return false;
        }
    }


    private void handlePageNavigation(ScreenHandler handler, List<Slot> slots,
            Text title, long now) {
        if (isSortingDisabledForMode() && now < modeFirstScanWindowUntil)
            return;

        if (minestarMode) {
            handleMinestarNavigation(handler, slots, now);
            return;
        }
        if (rapyMode) {
            handleRapyRefresh(handler, now);
            return;
        }

        Slot nextSlot = findSlot(slots, nextPageSlot);
        String nextSlotName = (nextSlot != null) ? nextSlot.getStack().getName().getString() : "";
        int[] pages = extractPageNumbers(nextSlotName);
        if (pages == null)
            pages = extractPageNumbers(title.getString());

        if (pages != null && pages[0] >= pages[1] && pages[1] > 0) {
            restartScan(now);
            return;
        }

        String currentSignature = buildPageSignature(slots);
        if (nextSlot != null && !nextSlot.getStack().isEmpty()) {
            if (currentSignature.equals(lastPageSignature)) {
                if (now - lastClickMs > 800)
                    restartScan(now);
            } else {
                // Page changed \u2014 clear locks so new items in same slots are buyable
                purchasedCache.clear();
                invalidSlots.clear();

                clickSlot(handler, nextPageSlot, 0);
                lastClickMs = now;
                lastActionMs = now;
                lastPageSignature = currentSignature;
            }
        } else if (now - marketFirstSeenTime > 1500) {
            restartScan(now);
        }
    }

    private void handleMinestarNavigation(ScreenHandler handler, List<Slot> slots, long now) {
        if (!currentMatches.isEmpty())
            return;

        if (now - lastClickMs < refreshDelayMs)
            return;

        String currentSignature = buildPageSignature(slots);
        if (!currentSignature.equals(lastPageSignature)) {
            purchasedCache.clear();
            invalidSlots.clear();
            lastPageSignature = currentSignature;
        }

        int clickTarget = minestarOnNextPage ? minestarNextSlot : minestarPrevSlot;
        Slot targetSlot = findSlot(slots, clickTarget);
        if (targetSlot == null || targetSlot.getStack().isEmpty()) {
            minestarOnNextPage = !minestarOnNextPage;
            return;
        }

        clickSlot(handler, clickTarget, 0);
        lastClickMs = now;
        lastActionMs = now;
        minestarOnNextPage = !minestarOnNextPage;
    }

    private void handleRapyRefresh(ScreenHandler handler, long now) {
        if (now < refreshBlockedUntil)
            return;
        if (!currentMatches.isEmpty())
            return;
        if (now - lastClickMs < refreshDelayMs)
            return;
        if (refreshSlot < 0 || refreshSlot >= handler.slots.size())
            return;
        clickSlot(handler, refreshSlot, 1);
        lastClickMs = now;
        lastActionMs = now;
    }


    public void executePurchase(Slot slot, double price, PriceEntry priceEntry, int syncId) {
        if (!mc.isOnThread()) {
            mc.execute(() -> executePurchase(slot, price, priceEntry, syncId));
            return;
        }

        if (mc.player == null || mc.player.currentScreenHandler == null)
            return;

        if (slot.id > 53)
            return;

        // Capture purchase metadata before the first click can produce a server response.
        ItemStack stack = slot.getStack();
        lastMatchedItemName = resolveMatchedItemName(stack, priceEntry);
        lastMatchedAmount = stack.getCount();
        lastMatchedPriceDouble = price;
        lastMatchedPrice = formatPrice(price);
        captureMatchedDetails(stack);

        long now = System.currentTimeMillis();
        lastActionMs = now;
        confirmClickCount = 0;
        startBuyTitle = mc.currentScreen != null ? mc.currentScreen.getTitle().getString().toLowerCase() : "";
        lastMatchedSlotId = slot.id;
        lastMatchedItemStack = stack.copy();
        purchasePending = false;
        lastSyncIdWhenBought = mc.player.currentScreenHandler.syncId;

        currentMatches.clear();
        sendFeedback("\u00A7eMATCH FOUND: \u00A7f" + lastMatchedItemName + " \u00A77(\u00A7a" + lastMatchedPrice + "\u00A77)");
    }

    private String resolveMatchedItemName(ItemStack stack, PriceEntry priceEntry) {
        String entryName = readableEntryName(priceEntry);

        if (!entryName.isEmpty()) {
            return entryName;
        }
        String stackName = readableItemName(stack);
        if (!stackName.isEmpty()) {
            return stackName;
        }
        return "???";
    }

    private String readableItemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return dev.bkclient.utility.inventory.ItemStackExtractor.toReadablePlain(stack.getName());
    }

    private String readableEntryName(PriceEntry priceEntry) {
        if (priceEntry == null || priceEntry.getName() == null) {
            return "";
        }
        return dev.bkclient.utility.inventory.ItemStackExtractor.toReadablePlain(priceEntry.getName());
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


    public void onTitleMessage(String cleanTextLower) {
        if (minestarMode) return; // Minestar purchases are confirmed by server chat only.
        if (cleanTextLower == null || cleanTextLower.isEmpty()) {
            return;
        }
        if (!active || !(buyingItem || waitingForConfirmMsg || purchasePending)) {
            return;
        }
        String normalizedTitle = BK_RE_0.matcher(java.text.Normalizer.normalize(cleanTextLower, java.text.Normalizer.Form.NFD)).replaceAll("");
        normalizedTitle = replacePolishLetters(normalizedTitle);

        boolean looksLikePurchaseResult = cleanTextLower.contains(XSK.KW_KUPILES1)
                || cleanTextLower.contains(XSK.KW_KUPILES2)
                || cleanTextLower.contains(XSK.KW_POMYSLNIE1)
                || cleanTextLower.contains(XSK.KW_POMYSLNIE2)
                || cleanTextLower.contains(XSK.KW_ZAKUPIONO1)
                || cleanTextLower.contains(XSK.KW_ZAKUPIONO2)
                || cleanTextLower.contains(XSK.KW_BOUGHT)
                || cleanTextLower.contains(XSK.KW_SUKCES)
                || cleanTextLower.contains("kupi\u0142e\u015B przedmiot")
                || cleanTextLower.contains("kupiles przedmiot")
                || normalizedTitle.contains("kupiles przedmiot za");

        boolean looksLikePurchaseFailure = cleanTextLower.contains(XSK.KW_PRAWDOPODOBNIE)
                || cleanTextLower.contains(XSK.KW_NIEDOSTEPNY1)
                || cleanTextLower.contains(XSK.KW_NIEDOSTEPNY2)
                || cleanTextLower.contains(XSK.KW_SPRZEDANY1)
                || cleanTextLower.contains(XSK.KW_SPRZEDANY2)
                || cleanTextLower.contains(XSK.KW_SPRZEDANY3)
                || cleanTextLower.contains(XSK.KW_KUPIONY1)
                || cleanTextLower.contains(XSK.KW_KUPIONY2)
                || cleanTextLower.contains(XSK.KW_ALREADY_SOLD)
                || cleanTextLower.contains(XSK.KW_ALREADY_BOUGHT)
                || cleanTextLower.contains(XSK.KW_USUNIETO)
                || cleanTextLower.contains(XSK.KW_EXPIRED);

        boolean looksLikeNoMoney = cleanTextLower.contains(XSK.KW_BRAK1)
                || cleanTextLower.contains(XSK.KW_BRAK2)
                || cleanTextLower.contains(XSK.KW_BRAK3)
                || cleanTextLower.contains(XSK.KW_BRAK4)
                || cleanTextLower.contains(XSK.KW_BRAK5)
                || cleanTextLower.contains(XSK.KW_NO_MONEY)
                || cleanTextLower.contains(XSK.KW_INSUF);

        if (looksLikePurchaseResult || looksLikePurchaseFailure || looksLikeNoMoney) {
            onChatMessage(Text.literal("\u00A7a" + cleanTextLower), true);
        }
    }

    public void onChatMessage(Text message, boolean overlay) {
        if (minestarMode) return; // HistoryManager owns the Minestar packet route.
        processChatMessage(message, overlay, null, false);
    }

    public void onMinestarChatMessage(Text message, MinestarMessages.Event event) {
        if (!minestarMode) return;
        if (event != null && event.kind() != MinestarMessages.Kind.PURCHASE) return;
        processChatMessage(message, false, event, true);
    }

    private void processChatMessage(Text message, boolean overlay, MinestarMessages.Event minestarEvent,
                                    boolean minestarDispatch) {
        // Use Text API for formatting check \u2014 more reliable than raw \u00A7 scanning
        // (matches SAC approach)
        String rawText = dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(message);
        String cleanText = BK_RE_1.matcher(rawText).replaceAll("").trim();
        String cleanTextLower = cleanText.toLowerCase();
        long now = System.currentTimeMillis();

        // Ignore bot messages
        if (rawText.contains("[" + XSK.CHAT_PREFIX + "]") || rawText.contains(XSK.CHAT_PREFIX))
            return;

        // Delegate sell-related messages to AutoSellController
        if (!minestarDispatch && autoSellController != null) {
            autoSellController.onChatMessage(cleanTextLower);
        }

        if (!active)
            return;

        // Check formatting via both raw \u00A7 codes and Text siblings (like SAC)
        boolean hasFormatting = rawText.contains("\u00A7")
                || !message.getStyle().isEmpty()
                || message.getSiblings().stream().anyMatch(s -> !s.getStyle().isEmpty());
        boolean looksLikePlayerChat = cleanText.contains(" \u00BB ");
        if ((!hasFormatting && !minestarDispatch) || looksLikePlayerChat)
            return;

        // Success detection
        boolean hasSuccess = minestarDispatch ? minestarEvent != null : (cleanTextLower.contains(XSK.KW_KUPILES1)
                || cleanTextLower.contains(XSK.KW_KUPILES2)
                || cleanTextLower.contains(XSK.KW_POMYSLNIE1)
                || cleanTextLower.contains(XSK.KW_POMYSLNIE2)
                || cleanTextLower.contains(XSK.KW_ZAKUPIONO1)
                || cleanTextLower.contains(XSK.KW_SUKCES)
                || cleanTextLower.contains(XSK.KW_ZAKUPIONO2)
                || cleanTextLower.contains(XSK.KW_BOUGHT)
                || cleanTextLower.contains(XSK.KW_GRATULACJ));

        // Error detection
        boolean hasError = cleanTextLower.contains(XSK.KW_PRAWDOPODOBNIE)
                || cleanTextLower.contains(XSK.KW_NIEDOSTEPNY1)
                || cleanTextLower.contains(XSK.KW_NIEDOSTEPNY2)
                || cleanTextLower.contains(XSK.KW_SPRZEDANY1)
                || cleanTextLower.contains(XSK.KW_SPRZEDANY2)
                || cleanTextLower.contains(XSK.KW_SPRZEDANY3)
                || cleanTextLower.contains(XSK.KW_KUPIONY1)
                || cleanTextLower.contains(XSK.KW_KUPIONY2)
                || cleanTextLower.contains(XSK.KW_ALREADY_SOLD)
                || cleanTextLower.contains(XSK.KW_ALREADY_BOUGHT)
                || cleanTextLower.contains(XSK.KW_PRODUKT1)
                || cleanTextLower.contains(XSK.KW_PRODUKT2)
                || cleanTextLower.contains(XSK.KW_PRZEDMIOT1)
                || cleanTextLower.contains(XSK.KW_PRZEDMIOT2)
                || cleanTextLower.contains(XSK.KW_USUNIETO)
                || cleanTextLower.contains(XSK.KW_WYGASL1)
                || cleanTextLower.contains(XSK.KW_WYGASL2)
                || cleanTextLower.contains(XSK.KW_EXPIRED);

        // No money detection
        boolean hasNoMoney = cleanTextLower.contains(XSK.KW_BRAK1)
                || cleanTextLower.contains(XSK.KW_BRAK2)
                || cleanTextLower.contains(XSK.KW_BRAK3)
                || cleanTextLower.contains(XSK.KW_BRAK4)
                || cleanTextLower.contains(XSK.KW_BRAK5)
                || cleanTextLower.contains(XSK.KW_NO_MONEY)
                || cleanTextLower.contains(XSK.KW_INSUF);

        if (minestarDispatch && hasSuccess) {
            hasError = false;
            hasNoMoney = false;
        }

        // 1. Error Handling
        if (hasError && !hasNoMoney && (buyingItem || waitingForConfirmMsg || purchasePending)) {
            sortingPauseUntil = now + 2000;
            if (lastMatchedSlotId != -1) {
                invalidSlots.put(lastMatchedSlotId, now + 10000);
            }

            consecutivePurchaseFailures++;
            buyingItem = false;
            purchasePending = false;
            buyLock.set(false);
            waitingForConfirmMsg = false;
            isInsideConfirmScreen = false;

            String msgStr = Localizator.translate("modules.notifications.auto_market.already_sold");
            sendFeedback("\u00A7c" + msgStr + " (Fail " + consecutivePurchaseFailures + "/3)");

            if (consecutivePurchaseFailures >= 3) {
                sendFeedback("\u00A7cToo many failures! Hard restarting market...");
                consecutivePurchaseFailures = 0;
                restartScan(now, 500, true);
                return;
            }

            refreshBlockedUntil = now + 10;
            currentMatches.clear();
            lastActionMs = now - refreshDelayMs;
            return;
        }

        // 2. No Money Handling
        if (hasNoMoney && (buyingItem || waitingForConfirmMsg || purchasePending)) {
            String noMoneyMsg = Localizator.translate("modules.notifications.auto_market.no_money",
                    lastMatchedItemName != null ? lastMatchedItemName : "???");
            sendFeedback("\u00A7c" + noMoneyMsg);

            // Lock this item+price for 120s so we don't retry it (mirrors SAC
            // tooExpensiveCache)
            if (lastMatchedItemName != null && !lastMatchedItemName.isEmpty()) {
                markItemTooExpensive(lastMatchedItemStack.getName().getString(), lastMatchedPriceDouble);
                sendFeedback("\u00A7e[Lock 120s] " + lastMatchedItemName + " (" + (int) lastMatchedPriceDouble + "$)");
            }

            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                String serverAddr = mc.getCurrentServerEntry() != null
                        ? mc.getCurrentServerEntry().address
                        : "local";
                DiscordWebhook.sendErrorNotification(webhookUrl, noMoneyMsg, serverAddr,
                        lastMatchedItemName, lastMatchedPrice);
            }
            // Release only this purchase. Keep the current market and scanner running;
            // its next pass skips the locked offer and evaluates the remaining slots.
            purchaseGeneration.incrementAndGet();
            isThreadedBuyActive = false;
            isThreadedInitialClickActive = false;
            waitingForConfirmMsg = false;
            purchasePending = false;
            isInsideConfirmScreen = mc.player != null && mc.player.currentScreenHandler != null
                    && mc.player.currentScreenHandler.syncId != lastSyncIdWhenBought;
            buyingStartTime = 0;
            confirmClickCount = 0;
            sortingPauseUntil = 0;
            currentMatches.clear();
            lastActionMs = now;
            lastClickMs = now;
            refreshBlockedUntil = now + refreshDelayMs;
            idleMarketSinceMs = now;
            buyLock.set(false);
            buyingItem = false;
            return;
        }

        // 3. Success Handling
        if (hasSuccess && (buyingItem || waitingForConfirmMsg || purchasePending)) {
            if (minestarDispatch) {
                // Use the server's total transaction price, never quantity * displayed price.
                lastMatchedPriceDouble = minestarEvent.amount();
                lastMatchedPrice = minestarEvent.displayPrice();
            }
            if (overlay) {
                AutoMarketHistoryManager historyManager = AutoMarketHistoryManager.getInstance();
                if (historyManager != null) {
                    historyManager.incrementBought();
                    if (lastMatchedPriceDouble > 0) {
                        historyManager.addProfit(-lastMatchedPriceDouble);
                    }
                }
            }
            sortingPauseUntil = now + 2000;
            marketContinuousOpenTime = 0;
            consecutivePurchaseFailures = 0;
            if (lastMatchedSlotId != -1) {
                markItemPurchasedRecent(lastMatchedSlotId);
            }
            buyingItem = false;
            purchasePending = false;
            buyLock.set(false);
            waitingForConfirmMsg = false;
            isInsideConfirmScreen = false;
            purchaseSucceededUntil = now + 1000;
            lastActionMs = now - refreshDelayMs;
            refreshBlockedUntil = now + 10;
            currentMatches.clear();

            String boughtMsg = Local_translate_bought(lastMatchedItemName, lastMatchedPrice);
            sendFeedback("\u00A7a" + boughtMsg);
            BKClientInit.getInstance().getNotificationManager().addNotificationOther(
                    NotificationType.SUCCESS, XSK.LABEL_AUTO_MARKET, boughtMsg);

            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                String serverAddr = mc.getCurrentServerEntry() != null
                        ? mc.getCurrentServerEntry().address
                        : "local";
                final String _webhookUrl = webhookUrl;
                final String _itemName = lastMatchedItemName;
                final String _price = lastMatchedPrice;
                final String _server = serverAddr;
                final int _amount = lastMatchedAmount;
                final String _enchants = lastMatchedEnchants;
                final String _lore = lastMatchedLore;
                final boolean _screenshot = webhookScreenshot;
                SCHEDULER.schedule(() -> mc.execute(() -> {
                    byte[] shot = _screenshot ? ScreenshotHelper.takeScreenshotBytes() : null;
                    DiscordWebhook.sendPurchaseNotificationWithScreenshot(
                            _webhookUrl, _itemName, _price, _server, _amount, _enchants, _lore, shot);
                }), 1500, TimeUnit.MILLISECONDS);
            }

            snipeHistory.add(0, new SnipeEntry(lastMatchedItemName, lastMatchedPriceDouble, lastMatchedAmount));
            if (snipeHistory.size() > 5)
                snipeHistory.remove(5);

            AutoMarketHistoryManager.getInstance().addSnipeEntry(lastMatchedItemName, lastMatchedPriceDouble,
                    lastMatchedAmount, lastMatchedItemStack,
                    dev.bkclient.utility.game.ScoreboardUtility.getFormattedBalance());

            // Auto-Sell Trigger
            if (autoSellController != null && entries != null
                    && !autoSellController.isMarketLimitReached()
                    && autoSellController.hasItemsToSell(entries)) {
                autoSellController.startPause();
                closeScreen();
            }
        }
    }

    private String Local_translate_bought(String name, String price) {
        try {
            return Localizator.translate("modules.notifications.auto_market.bought", name, price);
        } catch (Exception e) {
            return "Zakupiono " + name + " za " + price;
        }
    }


    public void onTick() {
        if (!active)
            return;

        long now = System.currentTimeMillis();

        if (autoSellController != null && entries != null) {
            if (autoSellController.onTick(entries))
                return;
        }

        if (isRestarting) {
            long delay = restartDelayMs + extraRestartDelay;

            if (now - restartStartTime > delay && now > lastActionMs) {
                isRestarting = false;
                buyingItem = false;
                waitingForConfirmMsg = false;
                purchasePending = false;

                if (active) {
                    sendMarketCommand();
                    waitingForMarketOpen = true;
                    lastOpenAttemptMs = now;
                    openAttemptsLeft = 3;
                }
            }
            return;
        }

        if (waitingForMarketOpen && MinecraftClient.getInstance().currentScreen == null) {
            if (now - lastOpenAttemptMs > 2800) {
                if (openAttemptsLeft > 0) {
                    openAttemptsLeft--;
                    lastOpenAttemptMs = now;
                    sendMarketCommand();
                } else {
                    // Exhausted retries \u2014 hard restart instead of full stop
                    waitingForMarketOpen = false;
                    sendFeedback("\u00A7cNie uda\u0142o si\u0119 otworzy\u0107 rynku \u2014 hard restart!");
                    lastFrozenCheckMs = now;
                    restartScan(now, 2000, true);
                }
            }
        }

        // If market is open but nothing happened for 7s (no refresh, no buy, no page
        // flip) \u2014 force restart.
        if (!isRestarting && !waitingForMarketOpen && !buyingItem && !purchasePending
                && MinecraftClient.getInstance().currentScreen != null
                && lastActionMs > 0 && now - lastActionMs > 7000) {
            if (now - lastFrozenCheckMs > 7000) {
                lastFrozenCheckMs = now;
                sendFeedback("\u00A7c[Watchdog] Ekran zamro\u017Cony \u2014 force restart!");
                restartScan(now, 500, true);
            }
        }

    }


    public void restartScan(long now) {
        restartScan(now, 0, false);
    }

    private void restartScan(long now, int extraDelay) {
        restartScan(now, extraDelay, false);
    }

    public void restartScan(long now, int extraDelay, boolean forceHard) {
        if (isRestarting)
            return;
        isRestarting = true;
        purchaseGeneration.incrementAndGet();
        isThreadedBuyActive = false;
        isThreadedInitialClickActive = false;
        stopThreadedScanner();
        currentMatches.clear();
        restartStartTime = now;
        this.extraRestartDelay = extraDelay;

        buyingItem = false;
        purchasePending = false;
        buyLock.set(false);
        waitingForConfirmMsg = false;
        isInsideConfirmScreen = false;

        sortingConfirmed = minestarMode || rapyMode;
        sortingPauseUntil = (minestarMode || rapyMode) ? 0 : now + (isAnarchia ? 2000 : 500);

        lastActionMs = now + extraDelay;
        lastPageSignature = "";
        idleMarketSignature = "";
        idleMarketSinceMs = 0;
        refreshBlockedUntil = 0;
        modeFirstScanWindowUntil = 0;

        String msg = forceHard
                ? Localizator.translate("modules.notifications.auto_market.force_restart")
                : Localizator.translate("modules.notifications.auto_market.restart");

        sendFeedback("\u00A7e" + msg);

        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.currentScreen != null) {
                client.player.closeHandledScreen();
                client.setScreen(null);
            }
        });
    }


    public void startThreadedScanner(ScreenHandler handler) {
        stopThreadedScanner();
        int syncId = handler.syncId;

        SCAN_FUTURE = SCHEDULER.scheduleWithFixedDelay(() -> {
            try {
                if (!active || isRestarting || mc.player == null)
                    return;

                // Inventory safety
                int emptySlots = 0;
                var inv = mc.player.getInventory();
                for (int i = 0; i < 36; i++) {
                    if (inv.getStack(i).isEmpty())
                        emptySlots++;
                }
                if (emptySlots < 1) {
                    active = false;
                    return;
                }

                ScreenHandler currentHandler = mc.player.currentScreenHandler;
                if (currentHandler == null || currentHandler.syncId != syncId)
                    return;

                // Stop scanning while a purchase is in progress \u2014 never scan confirm screen
                // items.
                // This mirrors SAC: once buyingItem=true the scanner loop simply doesn't run.
                if (buyingItem || purchasePending)
                    return;

                long now = System.currentTimeMillis();
                if (minestarMode || rapyMode) {
                    sortingConfirmed = true;
                    sortingPauseUntil = 0;
                }
                // Minestar mode has no sorting step \u2014 scanner must run without waiting for sort confirmation
                if ((!sortingConfirmed && !minestarMode && !rapyMode) || (!minestarMode && !rapyMode && now < sortingPauseUntil) || isInsideConfirmScreen)
                    return;

                List<Slot> slots = null;
                if (currentHandler instanceof dev.bkclient.mixin.accessors.ScreenHandlerAccessor accessor) {
                    slots = accessor.getSlots();
                }
                if (slots == null)
                    return;

                invalidSlots.entrySet().removeIf(e -> now > e.getValue());
                currentMatches.clear();
                boolean matchFoundThisTick = false;
                for (int i = 0; i < Math.min(slots.size(), 45); i++) {
                    long invalidUntil = invalidSlots.getOrDefault(i, 0L);
                    if (now < invalidUntil)
                        continue;

                    Slot slot = slots.get(i);
                    if (slot == null || !slot.hasStack())
                        continue;

                    AutoMarketScanner.ScanResult result = scanner.evaluateSlot(slot, entryMap, false);
                    if (!result.matched)
                        continue;

                    if (isItemPurchasedRecent(slot.id))
                        continue;

                    // tooExpensiveCache check \u2014 skip items we know we can't afford
                    if (isItemTooExpensive(slot.getStack().getName().getString(), result.foundPrice))
                        continue;

                    matchFoundThisTick = true;
                    currentMatches.add(slot.id);

                    // Atomic lock: only ONE thread enters the buy path
                    if (!buyLock.compareAndSet(false, true))
                        continue;

                    // Re-verify after locking (double-check same slot still has the same item)
                    AutoMarketScanner.ScanResult verify = scanner.evaluateSlot(slot, entryMap, false);
                    if (!verify.matched || !active || isRestarting || purchasePending || buyingItem
                            || isItemTooExpensive(slot.getStack().getName().getString(), verify.foundPrice)) {
                        buyLock.set(false);
                        continue;
                    }

                    buyingItem = true;
                    final long generation = purchaseGeneration.incrementAndGet();
                    buyingStartTime = System.currentTimeMillis();
                    lastMatchedSlotId = slot.id;
                    markItemPurchasedRecent(slot.id);

                    final Slot matchedSlot = slot;
                    final double foundPrice = verify.foundPrice;
                    final PriceEntry matchedEntryObj = verify.entry;
                    final int finalSyncId = syncId;

                    // Save the exact attempted offer BEFORE sending any click: the server can
                    // reject it immediately, before a later feedback callback would run.
                    mc.execute(() -> {
                        if (generation != purchaseGeneration.get() || !active || isRestarting || !buyingItem)
                            return;
                        if (mc.player == null || mc.player.currentScreenHandler != currentHandler) {
                            buyingItem = false;
                            buyLock.set(false);
                            return;
                        }
                        AutoMarketScanner.ScanResult latest = scanner.evaluateSlot(matchedSlot, entryMap, false);
                        if (!latest.matched || latest.entry != matchedEntryObj
                                || Double.compare(latest.foundPrice, foundPrice) != 0
                                || isItemTooExpensive(matchedSlot.getStack().getName().getString(), latest.foundPrice)) {
                            currentMatches.clear();
                            buyingItem = false;
                            buyLock.set(false);
                            return;
                        }
                        executePurchase(matchedSlot, foundPrice, matchedEntryObj, finalSyncId);
                        sendClickPacket(finalSyncId, matchedSlot.id, 0);
                        startInitialClickFallback(finalSyncId, matchedSlot.id,
                                lastMatchedItemStack.getName().getString());
                        startThreadedInitialClick(finalSyncId, matchedSlot.id, 20,
                                lastMatchedItemStack.getName().getString());
                    });
                    break;
                }

                // Threaded refresh (aggressive mode only, no match this tick)
                if (!matchFoundThisTick && !buyingItem && !purchasePending && aggressiveMode && !rapyMode) {
                    if (now > refreshBlockedUntil && now - lastActionMs > refreshDelayMs) {
                        sendClickPacket(syncId, refreshSlot, 1);
                        lastActionMs = now;
                    }
                }
            } catch (Exception e) {
                BKClientInit.LOGGER.error("AutoMarket scanner iteration failed", e);
            }
        }, 0, 5, TimeUnit.MILLISECONDS);
    }

    public void stopThreadedScanner() {
        if (SCAN_FUTURE != null) {
            SCAN_FUTURE.cancel(false);
            SCAN_FUTURE = null;
        }
    }

    public void shutdown() {
        active = false;
        stopThreadedScanner();
        SCHEDULER.shutdownNow();
    }


    private void captureMatchedDetails(ItemStack stack) {
        try {
            StringBuilder loreBuilder = new StringBuilder();
            net.minecraft.component.type.LoreComponent lore = stack
                    .get(net.minecraft.component.DataComponentTypes.LORE);
            if (lore != null) {
                for (Text line : lore.lines()) {
                    String str = line.getString();
                    String cleanStr = str.toLowerCase();
                    if (cleanStr.contains("wystawi") || cleanStr.contains("cena") || cleanStr.contains("kliknij")
                            || cleanStr.contains("koszt") || cleanStr.contains("wyga\u015B") || cleanStr.contains("seller")
                            || cleanStr.contains("price") || cleanStr.contains("click") || cleanStr.contains("expired")
                            || cleanStr.contains("sprzedaj"))
                        continue;
                    if (loreBuilder.length() > 0)
                        loreBuilder.append("\n");
                    loreBuilder.append(str);
                }
            }
            lastMatchedLore = loreBuilder.toString();

            StringBuilder enchantsBuilder = new StringBuilder();
            var enchs = stack.get(net.minecraft.component.DataComponentTypes.ENCHANTMENTS);
            if (enchs != null) {
                for (var e : enchs.getEnchantmentEntries()) {
                    String name = e.getKey().value().description().getString();
                    int lvl = e.getIntValue();
                    if (enchantsBuilder.length() > 0)
                        enchantsBuilder.append(", ");
                    enchantsBuilder.append(name).append(" ").append(lvl);
                }
            }
            lastMatchedEnchants = enchantsBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void sendClickPacket(int syncId, int slotId, int button) {
        if (dev.bkclient.systems.modules.modules.player.AutoLeave.isStashing() || !active) return;
        if (mc.player == null || mc.player.networkHandler == null
                || mc.player.currentScreenHandler == null)
            return;

        int revision = 0;
        if (mc.player.currentScreenHandler instanceof dev.bkclient.mixin.accessors.ScreenHandlerAccessor accessor) {
            revision = accessor.getRevision();
        }

        mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket(
                syncId, revision, slotId, button,
                net.minecraft.screen.slot.SlotActionType.PICKUP,
                net.minecraft.item.ItemStack.EMPTY,
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<net.minecraft.item.ItemStack>()));
    }

    private void startThreadedInitialClick(int syncId, int slotId, int intervalMs, String expectedItemName) {
        final long generation = purchaseGeneration.get();
        final double expectedPrice = lastMatchedPriceDouble;
        isThreadedInitialClickActive = true;
        SCHEDULER.submit(() -> {
            try {
                while (generation == purchaseGeneration.get() && active && buyingItem && !isInsideConfirmScreen
                        && !isItemTooExpensive(expectedItemName, expectedPrice)) {
                    if (mc.player == null) break;
                    ScreenHandler handler = mc.player.currentScreenHandler;
                    if (handler == null || handler.syncId != syncId) break;

                    Slot slot = handler.getSlot(slotId);
                    if (slot == null || !slot.hasStack()
                            || !slot.getStack().getName().getString().equals(expectedItemName)) break;

                    sendClickPacket(syncId, slotId, 0);
                    Thread.sleep(intervalMs);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                if (generation == purchaseGeneration.get())
                    isThreadedInitialClickActive = false;
            }
        });
    }

    private void startInitialClickFallback(int syncId, int slotId, String expectedItemName) {
        final long generation = purchaseGeneration.get();
        final double expectedPrice = lastMatchedPriceDouble;
        SCHEDULER.schedule(() -> mc.execute(() -> {
            if (generation != purchaseGeneration.get() || isItemTooExpensive(expectedItemName, expectedPrice)
                    || !active || !buyingItem || isInsideConfirmScreen || mc.player == null
                    || mc.interactionManager == null)
                return;

            ScreenHandler handler = mc.player.currentScreenHandler;
            if (handler == null || handler.syncId != syncId)
                return;

            Slot slot;
            try {
                slot = handler.getSlot(slotId);
            } catch (Exception ignored) {
                return;
            }

            if (slot == null || !slot.hasStack())
                return;
            if (!slot.getStack().getName().getString().equals(expectedItemName))
                return;

            clickSlot(handler, slotId, 0);
        }), 120, TimeUnit.MILLISECONDS);
    }

    private void startThreadedBuy(int syncId, int fallbackSlotId, int delay) {
        final long generation = purchaseGeneration.get();
        isThreadedBuyActive = true;
        SCHEDULER.submit(() -> {
            try {
                long start = System.currentTimeMillis();
                while (generation == purchaseGeneration.get() && active && buyingItem && System.currentTimeMillis() - start < 4000L) {
                    if (mc.player != null && mc.player.currentScreenHandler != null
                            && mc.player.currentScreenHandler.syncId == syncId) {
                        sendClickPacket(syncId, fallbackSlotId, 0);
                    }
                    Thread.sleep(delay > 0 ? delay : 20L);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                if (generation == purchaseGeneration.get())
                    isThreadedBuyActive = false;
            }
        });
    }


    public void onScreenClosed() {
        stopThreadedScanner();
        marketContinuousOpenTime = 0;
        marketFirstSeenTime = 0;
        isInsideConfirmScreen = false;
    }


    private void clickSlot(ScreenHandler handler, int slotId, int button) {
        if (mc.player == null || mc.interactionManager == null)
            return;
        mc.interactionManager.clickSlot(handler.syncId, slotId, button,
                net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
    }

    private net.minecraft.screen.slot.Slot findSlot(List<net.minecraft.screen.slot.Slot> slots, int id) {
        for (net.minecraft.screen.slot.Slot s : slots) {
            if (s.id == id)
                return s;
        }
        return null;
    }

    public void forceMarketOpen() {
        if (!active)
            return;
        sortingConfirmed = false;
        String cmd = isAnarchia ? "ah" : marketCommand;
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand(cmd.replace("/", ""));
        }
    }

    private void sendMarketCommand() {
        if (mc.player == null || mc.player.networkHandler == null)
            return;
        String cmd = marketCommand;
        if (cmd.startsWith("/"))
            cmd = cmd.substring(1);
        mc.player.networkHandler.sendChatCommand(cmd);
    }

    private void closeScreen() {
        mc.execute(() -> {
            if (mc.player != null) {
                if (mc.player.networkHandler != null && mc.player.currentScreenHandler != null) {
                    mc.player.networkHandler.sendPacket(
                            new net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket(
                                    mc.player.currentScreenHandler.syncId));
                }
                mc.player.closeHandledScreen();
                if (mc.currentScreen != null)
                    mc.setScreen(null);
            }
        });
    }

    private boolean isItemPurchasedRecent(int slotId) {
        Long expiry = purchasedCache.get(slotId);
        if (expiry != null) {
            if (System.currentTimeMillis() < expiry)
                return true;
            else
                purchasedCache.remove(slotId);
        }
        return false;
    }

    private void markItemPurchasedRecent(int slotId) {
        purchasedCache.put(slotId, System.currentTimeMillis() + 5000);
    }

    private boolean isItemTooExpensive(String name, double price) {
        String key = name + ":" + (int) price;
        Long expiry = tooExpensiveCache.get(key);
        if (expiry != null) {
            if (System.currentTimeMillis() < expiry)
                return true;
            else
                tooExpensiveCache.remove(key);
        }
        return false;
    }

    private void markItemTooExpensive(String name, double price) {
        String key = name + ":" + (int) price;
        tooExpensiveCache.put(key, System.currentTimeMillis() + 120_000);
    }

    private boolean confirmKeywordsDetected(ScreenHandler handler) {
        if (handler == null)
            return false;
        // Access screen title only from main thread \u2014 safe here since called from
        // startThreadedInitialClick
        // which checks mc.currentScreen indirectly via handler sync
        String title = "";
        if (mc.isOnThread() && mc.currentScreen != null)
            title = mc.currentScreen.getTitle().getString().toLowerCase();
        return findConfirmSlotID(handler.slots, title) != -1;
    }

    private int findConfirmSlotID(List<net.minecraft.screen.slot.Slot> slots, String title) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < Math.min(slots.size(), 54); i++) {
            net.minecraft.screen.slot.Slot s = slots.get(i);
            if (s == null || !s.hasStack())
                continue;

            long invalidUntil = invalidSlots.getOrDefault(i, 0L);
            if (now < invalidUntil)
                continue;

            ItemStack stack = s.getStack();
            String name = stack.getName().getString().toLowerCase();

            List<Text> tooltip;
            try {
                tooltip = stack.getTooltip(net.minecraft.item.Item.TooltipContext.DEFAULT, null,
                        net.minecraft.item.tooltip.TooltipType.BASIC);
            } catch (Exception ignored) {
                continue;
            }

            boolean hasSellerLore = false;
            for (Text t : tooltip) {
                String line = t.getString().toLowerCase();
                if (line.contains("wystawi") || line.contains("seller")) {
                    hasSellerLore = true;
                    break;
                }
            }
            if (hasSellerLore)
                continue;

            boolean isConfirm = (confirmItemName != null && !confirmItemName.isEmpty())
                    ? name.contains(confirmItemName.toLowerCase())
                    : (name.contains(XSK.KW_POTWIERDZ1) || name.contains(XSK.KW_POTWIERDZ2)
                            || name.contains(XSK.KW_CONFIRM) || name.contains(XSK.KW_ZAKUP)
                            || name.contains(XSK.KW_POTWIERDZANIE) || name.contains(XSK.KW_NA_PEWNO)
                            || name.contains(XSK.KW_SURE)
                            || (title.contains("zakup") && name.contains("produkt")));

            if (isConfirm)
                return i;
        }
        return -1;
    }

    private boolean isNajnowszeSelected(ItemStack stack, String keyword) {
        if (stack == null || stack.isEmpty())
            return false;

        String kw = keyword.toLowerCase();

        var loreComponent = stack.get(net.minecraft.component.DataComponentTypes.LORE);
        if (loreComponent == null || loreComponent.lines() == null || loreComponent.lines().isEmpty())
            return false;

        for (Text line : loreComponent.lines()) {
            String text = line.getString().toLowerCase();
            if (!text.contains(kw) && !text.contains(XSK.SORT_KW_NOWSZE))
                continue;

            var style = line.getStyle();
            if (style != null && style.getColor() != null) {
                String colorStr = style.getColor().toString().toLowerCase();
                // Explicit color names and hex values only \u2014 no single-letter matches
                if (colorStr.contains("yellow") || colorStr.contains("gold")
                        || colorStr.contains("ffff55") || colorStr.contains("ffaa00")) {
                    return true;
                }
            }

            for (Text sibling : line.getSiblings()) {
                String sibText = sibling.getString().toLowerCase();
                var sibStyle = sibling.getStyle();
                if ((sibText.contains(kw) || sibText.contains(XSK.SORT_KW_NOWSZE)
                        || sibText.contains("\u27A4") || sibText.contains("\u00BB"))
                        && sibStyle != null && sibStyle.getColor() != null) {
                    String sibColor = sibStyle.getColor().toString().toLowerCase();
                    if (sibColor.contains("yellow") || sibColor.contains("gold")
                            || sibColor.contains("ffff55") || sibColor.contains("ffaa00")) {
                        return true;
                    }
                }
            }

            // Fallback: raw \u00A7e (yellow) or \u00A76 (gold) in legacy string
            String rawLine = dev.bkclient.utility.inventory.ItemStackExtractor.toLegacy(line).toLowerCase();
            if (rawLine.contains("\u00A7e") || rawLine.contains("\u00A76"))
                return true;
        }

        return false;
    }

    private String buildPageSignature(List<net.minecraft.screen.slot.Slot> slots) {
        if (slots == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(slots.size(), 45); i++) {
            net.minecraft.screen.slot.Slot s = slots.get(i);
            if (s != null && !s.getStack().isEmpty()) {
                sb.append(s.getStack().getName().getString());
            }
            sb.append("|");
        }
        return sb.toString();
    }

    private int[] extractPageNumbers(String text) {
        if (text == null)
            return null;
        java.util.regex.Matcher m = PAGE_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) };
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static String formatPrice(double price) {
        if (price >= 1_000_000)
            return String.format("%.1fM", price / 1_000_000);
        if (price >= 1_000)
            return String.format("%.1fk", price / 1_000);
        return String.valueOf((int) price);
    }

    public void sendFeedback(String msg) {
        if (mc.player != null) {
            String prefix = "\u00A78[\u00A72B\u00A7aK \u00A7fClient\u00A78] ";
            mc.player.sendMessage(Text.literal(prefix + msg), false);
        }
    }


    private void reset() {
        purchaseGeneration.incrementAndGet();
        active = false;
        buyingItem = false;
        purchasePending = false;
        buyLock.set(false);
        isRestarting = false;
        isInsideConfirmScreen = false;
        buyingStartTime = 0;
        startBuyTitle = "";
        lastActionMs = 0;
        lastClickMs = 0;
        lastPageSignature = "";
        idleMarketSignature = "";
        idleMarketSinceMs = 0;
        lastIdleMarketResetMs = 0;
        waitingForConfirmMsg = false;
        confirmClickCount = 0;
        lastMatchedItemName = "";
        lastMatchedPrice = "";
        lastMatchedPriceDouble = 0;
        lastMatchedSlotId = -1;
        lastMatchedAmount = 0;
        marketFirstSeenTime = 0;
        marketContinuousOpenTime = 0;
        marketDetectedLogged = false;
        lastSyncId = -1;
        lastSyncIdWhenBought = -1;
        screenFirstSeenTime = 0;
        sortingConfirmed = minestarMode || rapyMode;
        sortingPauseUntil = 0;
        waitingForMarketOpen = false;
        openAttemptsLeft = 0;
        lastOpenAttemptMs = 0;
        lastFrozenCheckMs = 0;
        initialScanPauseUntil = 0;
        modeFirstScanWindowUntil = 0;
        extraRestartDelay = 0;
        refreshBlockedUntil = 0;
        minestarOnNextPage = true;
        tooExpensiveCache.clear();
        currentMatches.clear();
        isThreadedBuyActive = false;
        isThreadedInitialClickActive = false;
        lastKnownItem = "None";
        lastKnownPrice = "---";
        consecutivePurchaseFailures = 0;
        stopThreadedScanner();
    }

    public void resetSessionStats() {
        AutoMarketHistoryManager.getInstance().resetStats();
        snipeHistory.clear();
        if (autoSellController != null)
            autoSellController.resetSessionStats();
    }
}
