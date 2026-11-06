package dev.bkclient.systems.modules.modules.bkmods;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import dev.bkclient.mixin.accessors.HandledScreenAccessor;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.player.InputEvent;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.window.ContainerClickEvent;
import dev.bkclient.systems.event.impl.window.ContainerReleaseEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.bkmods.automarket.AutoMarketController;
import dev.bkclient.systems.modules.modules.bkmods.automarket.AutoSellController;
import dev.bkclient.systems.modules.modules.bkmods.automarket.LobbyController;
import dev.bkclient.systems.modules.modules.bkmods.automarket.ReconnectController;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ItemListSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import dev.bkclient.ui.menu.rynek.QuickAddScreen;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.utility.game.ClientConstants;
import dev.bkclient.utility.inventory.ItemStackExtractor;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import dev.bkclient.systems.modules.modules.bkmods.config.AutoMarketConfigManager;
import dev.bkclient.systems.config.WebhookConfig;

@ModuleInfo(name = "Auto-Market", category = ModuleCategory.OTHER)
public class AutoMarket
        extends BaseModule {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("\u00A7[0-9a-fk-or]");

    private final AutoSellController autoSellController = new AutoSellController();
    public final AutoMarketController autoMarketController = new AutoMarketController(this, autoSellController);
    private final LobbyController lobbyController = new LobbyController();
    private final ReconnectController reconnectController = new ReconnectController();
    public final AutoMarketConfigManager configManager = new AutoMarketConfigManager(this);

    public final StringSetting configName = new StringSetting(this, "modules.settings.auto_market.config_name",
            (java.util.function.BooleanSupplier) () -> true) {
        @Override
        public void setText(String text) {
            text = AutoMarketConfigManager.sanitizeProfileName(text);
            String old = getText();
            if (text != null && !text.equals(old) && !loadingProfile) {
                // Zapisujemy stary profil przed wczytaniem nowego!
                if (configManager != null && configManager.profileExists(old)) {
                    configManager.save(old);
                }
                super.setText(text);
                activeProfile = text;
                loadingProfile = true;
                try {
                    configManager.load(text);
                } finally {
                    loadingProfile = false;
                }
            } else {
                super.setText(text);
                activeProfile = text != null ? text : "default";
            }
        }
    }.text("default");

    private boolean loading = true;
    private boolean loadingProfile = false;
    private long lastSyncMs = 0;

    public boolean isLoading() {

        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public void setLoadingProfile(boolean loadingProfile) {
        this.loadingProfile = loadingProfile;
    }

    public boolean isLoadingProfile() {
        return loadingProfile;
    }

    private final java.util.Random random = new java.util.Random();
    private long nextJumpTime = 0;
    private int jumpTicksRemaining = 0;
    private boolean wasScreenOpen = false;
    private long jumpStartTime = 0;
    private long lastJumpCloseAttempt = 0;
    private long reopenTime = 0;
    private int lastRegistrySize = -1;
    private int lastPriceVersion = -1;
    private long lastAutoSaveTime = 0;
    private boolean profileDirty = false;
    private long lastProfileChangeTime = 0;
    private String title = "";
    public String activeProfile = "default";

    public String getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(String profile) {
        this.activeProfile = profile;
    }

    public void markProfileDirty() {
        if (!loading && !loadingProfile) {
            profileDirty = true;
            lastProfileChangeTime = System.currentTimeMillis();
        }
    }

    public final ItemListSetting itemRegistry = new ItemListSetting(this, "modules.settings.auto_market.item_registry")
            .onChanged(() -> {
                if (autoMarketController != null) {
                    autoMarketController.notifyRegistryChanged();
                }
                markProfileDirty();
            });

    public void addEntry(PriceEntry entry) {
        if (!itemRegistry.getValue().contains(entry)) {
            itemRegistry.getValue().add(entry);
            itemRegistry.notifyChanged();
        }
    }

    public final BooleanSetting autoRepair = new BooleanSetting(this, "modules.settings.auto_market.auto_repair") {
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.enabled(true);
    public final ModeSetting unitModeGlobal = new ModeSetting(this, "modules.settings.auto_market.unit_mode_global") {
        @Override
        public void setValue(ModeSetting.Value value) {
            super.setValue(value);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
            if (!loading && !loadingProfile) configManager.save(activeProfile);
        }
    };
    public final ModeSetting.Value unitModeVal = new ModeSetting.Value(this.unitModeGlobal, "Unit").select();
    public final ModeSetting.Value sumModeVal = new ModeSetting.Value(this.unitModeGlobal, "Sum");

    public static PriceEntry editingEntry;

    public final ColorSetting cheapColor = new ColorSetting(this, "modules.settings.auto_market.cheap_color") {
        @Override
        public void setColor(dev.bkclient.utility.colors.ColorRGBA color) {
            super.setColor(color);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.color(new dev.bkclient.utility.colors.ColorRGBA(128, 255, 0, 204)).alpha(true);
    public final ColorSetting expensiveColor = new ColorSetting(this, "modules.settings.auto_market.expensive_color") {
        @Override
        public void setColor(dev.bkclient.utility.colors.ColorRGBA color) {
            super.setColor(color);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.color(new dev.bkclient.utility.colors.ColorRGBA(255, 0, 0, 200)).alpha(true);

    public final SliderSetting refreshRate = new SliderSetting(this, "modules.settings.auto_market.refresh_rate") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.min(10.0f).max(1000.0f).step(1.0f).currentValue(77.0f).suffix("ms");

    public final SliderSetting confirmDelay = new SliderSetting(this, "modules.settings.auto_market.confirm_delay") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.min(0.0f).max(1000.0f).step(1.0f).currentValue(173.0f).suffix("ms").maxMultiplier(5);

    public final SliderSetting restartDelay = new SliderSetting(this, "modules.settings.auto_market.restart_delay") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.min(0.0f).max(5000.0f).step(10.0f).currentValue(1500.0f).suffix("ms").maxMultiplier(5);


    public final SliderSetting sellInterval = new SliderSetting(this, "modules.settings.auto_market.sell_interval") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.min(0.1f).max(5.0f).step(0.1f).currentValue(1.0f).suffix("s");

    public final SliderSetting sellTax = new SliderSetting(this, "modules.settings.auto_market.sell_tax") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoSellController.setSellTaxPercent(getCurrentValue());
            markProfileDirty();
        }
    }.min(0.0f).max(25.0f).step(0.1f).currentValue(2.0f).suffix("%");

    public final StringSetting sellCommand = new StringSetting(this, "modules.settings.auto_market.sell_command") {
        @Override
        public void setText(String text) {
            super.setText(text);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.text("/wystaw {quantity} {price}");

    public final BooleanSetting showHistory = new BooleanSetting(this, "modules.settings.auto_market.show_history") {
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.enabled(true);

    public final SliderSetting reconnectDelay = new SliderSetting(this,
            "modules.settings.auto_market.reconnect_delay") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.min(0.0f).max(30.0f).step(1.0f).currentValue(5.0f).suffix("s");

    public final StringSetting discordWebhookUrl = new StringSetting(this, "modules.settings.auto_market.webhook_url",
            "") {
        @Override
        public void setText(String text) {
            super.setText(text);
            if (text != null && !text.isEmpty() && !isLoading()) {
                WebhookConfig.save(text);
            }
            if (autoMarketController != null) {
                autoMarketController.notifyRegistryChanged();
            }
            markProfileDirty();
        }
    }.text("");
    public final BooleanSetting autoJump = new BooleanSetting(this, "modules.settings.auto_market.auto_jump") {
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (enabled) {
                nextJumpTime = 0;
            } else {
                resetAntiAfkState();
            }
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.enabled(false);
    public final BooleanSetting autoReconnect = new BooleanSetting(this,
            "modules.settings.auto_market.auto_reconnect") {
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.enabled(true);
    public final ModeSetting highlightMode = new ModeSetting(this, "modules.settings.auto_market.highlight_mode") {
        @Override
        public void setValue(ModeSetting.Value value) {
            super.setValue(value);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    };
    public final ModeSetting.Value highlightNone = new ModeSetting.Value(this.highlightMode, "None");
    public final ModeSetting.Value highlightBorder = new ModeSetting.Value(this.highlightMode, "Border").select();
    public final ModeSetting.Value highlightFill = new ModeSetting.Value(this.highlightMode, "Fill");

    public final BooleanSetting aggressiveMode = new BooleanSetting(this,
            "modules.settings.auto_market.aggressive_mode") {
        @Override
        public void toggle() {
            setEnabled(!isEnabled());
            configManager.save(activeProfile);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (enabled) {
                refreshSlotSetting.setCurrentValue(0.0f);
                autoMarketController.setRefreshSlot(0);
                rapyMode.setEnabled(false);
                minestarMode.setEnabled(false);
            }
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    };

    public final BooleanSetting debugScreenInfo = new BooleanSetting(this,
            "modules.settings.auto_market.debug_screen_info") {
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            autoMarketController.debugMode = enabled;
            autoMarketController.setScannerDebug(enabled);
            markProfileDirty();
        }
    };

    public final StringSetting marketCommand = new StringSetting(this, "modules.settings.auto_market.market_command") {
        @Override
        public void setText(String text) {
            super.setText(text);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.text("/ah");

    public final StringSetting customMarketTitle = new StringSetting(this,
            "modules.settings.auto_market.custom_title") {
        @Override
        public void setText(String text) {
            super.setText(text);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.text("");

    public final BooleanSetting ignoreMarketTitle = new BooleanSetting(this,
            "modules.settings.auto_market.ignore_market_title") {
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            autoMarketController.setIgnoreMarketTitle(enabled);
            markProfileDirty();
        }
    };

    private static final String MINESTAR_LORE_REGEX = "(?i).*(?:Cena|Koszt|Price).*?(?:\\$\\s*)?([\\d\\s.,]+(?:mld|[kmbt])?)\\s*\\$?.*";

    public final BooleanSetting rapyMode = new BooleanSetting(this,
            "modules.settings.auto_market.rapy_mode") {
        @Override
        public void toggle() {
            setEnabled(!isEnabled());
            configManager.save(activeProfile);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            autoMarketController.setRapyMode(enabled);
            if (enabled) {
                confirmSlotSetting.setCurrentValue(11.0f);
                refreshSlotSetting.setCurrentValue(1.0f);
                autoMarketController.setConfirmSlot(11);
                autoMarketController.setRefreshSlot(1);
                aggressiveMode.setEnabled(false);
                minestarMode.setEnabled(false);
                ignoreMarketTitle.setEnabled(true);
            }
            markProfileDirty();
        }
    };

    public final BooleanSetting minestarMode = new BooleanSetting(this,
            "modules.settings.auto_market.minestar_mode") {
        @Override
        public void toggle() {
            setEnabled(!isEnabled());
            configManager.save(activeProfile);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            autoMarketController.setMinestarMode(enabled);
            autoSellController.setMinestarConfirmationMode(enabled);
            if (enabled) {
                rapyMode.setEnabled(false);
                aggressiveMode.setEnabled(false);
                ignoreMarketTitle.setEnabled(true);
                loreRegex.setText(MINESTAR_LORE_REGEX);
            } else if (!rapyMode.isEnabled()) {
                ignoreMarketTitle.setEnabled(false);
            }
            markProfileDirty();
        }
    };

    public final SliderSetting minestarNextSlot = new SliderSetting(this,
            "modules.settings.auto_market.minestar_next_slot", () -> !minestarMode.isEnabled()) {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            markProfileDirty();
        }
    }.min(0.0f).max(53.0f).step(1.0f).currentValue(53.0f);

    public final SliderSetting minestarPrevSlot = new SliderSetting(this,
            "modules.settings.auto_market.minestar_prev_slot", () -> !minestarMode.isEnabled()) {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            markProfileDirty();
        }
    }.min(0.0f).max(53.0f).step(1.0f).currentValue(52.0f);

    public final SliderSetting confirmSlotSetting = new SliderSetting(this,
            "modules.settings.auto_market.confirm_slot") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.min(0.0f).max(53.0f).step(1.0f).currentValue(10.0f);

    public final SliderSetting sortingSlotSetting = new SliderSetting(this,
            "modules.settings.auto_market.sorting_slot") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.min(0.0f).max(53.0f).step(1.0f).currentValue(53.0f);

    public final SliderSetting refreshSlotSetting = new SliderSetting(this,
            "modules.settings.auto_market.refresh_slot") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.min(0.0f).max(53.0f).step(1.0f).currentValue(51.0f);

    public final SliderSetting nextPageSlotSetting = new SliderSetting(this,
            "modules.settings.auto_market.next_page_slot") {
        @Override
        public void setCurrentValue(float val) {
            float previous = getCurrentValue();
            super.setCurrentValue(val);
            if (Float.compare(previous, getCurrentValue()) == 0) return;
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.min(0.0f).max(53.0f).step(1.0f).currentValue(53.0f);
    public final BooleanSetting autoSellEnabled = new BooleanSetting(this, "modules.settings.auto_market.auto_sell") {
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (autoSellController != null) {
                autoSellController.setAutoSellEnabled(enabled);
            }
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    };

    public final BooleanSetting webhookScreenshot = new BooleanSetting(this, "modules.settings.auto_market.webhook_screenshot") {
        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (autoMarketController != null)
                autoMarketController.setWebhookScreenshot(enabled);
            markProfileDirty();
        }
    };

    public final StringSetting loreRegex = new StringSetting(this, "modules.settings.auto_market.lore_regex") {
        @Override
        public void setText(String text) {
            super.setText(text);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.text("(?i).*(?:Koszt|Cena|Price).*?(?:\\$\\s*)?([\\d\\s.,]+(?:mld|[kmbt])?)\\s*\\$?.*");

    public final StringSetting loreSellerRegex = new StringSetting(this,
            "modules.settings.auto_market.lore_seller_regex") {
        @Override
        public void setText(String text) {
            super.setText(text);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.text("");
    public final StringSetting confirmItemName = new StringSetting(this,
            "modules.settings.auto_market.confirm_item_name") {
        @Override
        public void setText(String text) {
            super.setText(text);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.text("");

    public final StringSetting sortingKeyword = new StringSetting(this,
            "modules.settings.auto_market.sorting_keyword") {
        @Override
        public void setText(String text) {
            super.setText(text);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.text("najnowsz");

    public final ModeSetting accountType = new ModeSetting(this, "modules.settings.auto_market.account_type") {
        @Override
        public void setValue(ModeSetting.Value value) {
            super.setValue(value);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    };
    public final ModeSetting.Value premium = new ModeSetting.Value(this.accountType,
            "modules.settings.auto_market.account_type.premium");
    public final ModeSetting.Value nonPremium = new ModeSetting.Value(this.accountType,
            "modules.settings.auto_market.account_type.non_premium").select();

    public final StringSetting password = new StringSetting((SettingsContainer) this,
            "modules.settings.auto_market.login_password",
            premium::isSelected) {
        @Override
        public void setText(String text) {
            super.setText(text);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.text("123123");
    public final StringSetting loginCommand = new StringSetting((SettingsContainer) this,
            "modules.settings.auto_market.login_command",
            premium::isSelected) {
        @Override
        public void setText(String text) {
            super.setText(text);
            autoMarketController.notifyRegistryChanged();
            markProfileDirty();
        }
    }.text("/login {password}");

    public final BindSetting openBind = new BindSetting(this, "modules.settings.auto_market.open_bind") {
        @Override
        public void setKey(int key) {
            super.setKey(key);
            autoMarketController.notifyRegistryChanged();
        }
    };
    public final BindSetting quickAddBind = new BindSetting(this, "modules.settings.auto_market.quick_add_bind") {
        @Override
        public void setKey(int key) {
            super.setKey(key);
            autoMarketController.notifyRegistryChanged();
        }
    };
    public final BindSetting scanKey = new BindSetting(this, "modules.settings.auto_market.scan_key") {
        @Override
        public void setKey(int key) {
            super.setKey(key);
            autoMarketController.notifyRegistryChanged();
        }
    };

    private final ButtonSetting toggleMarket = new ButtonSetting(this, "modules.settings.auto_market.toggle_market")
            .action(() -> openCsGuiTab(4)); // AutoMarket


    private final ButtonSetting openConfig = new ButtonSetting(this, "modules.settings.auto_market.open_config")
            .action(() -> openCsGuiTab(5)); // Configs

    /** Otwiera CsGui na zadanej zakładce zamiast starego dropdown ekranu. */
    private static void openCsGuiTab(int category) {
        if (category == 4) {
            if (mc.currentScreen instanceof dev.bkclient.ui.clicksys.ClickSysScreen screen) {
                screen.openAutoMarket();
                return;
            }
            dev.bkclient.ui.menu.automarket.AutoMarketScreen.open();
            return;
        }
        if (category == 4 || category == 5) {
            dev.bkclient.ui.menu.MenuScreen current = BKClientInit.getInstance().getMenuScreen();
            var clickGui = current instanceof dev.bkclient.ui.menu.clickgui.ClickGuiV3Screen v3
                ? v3 : new dev.bkclient.ui.menu.clickgui.ClickGuiV3Screen();
            BKClientInit.getInstance().setMenuScreen(clickGui);
            if (mc.currentScreen != clickGui) mc.setScreen(clickGui);
            clickGui.setClosing(false);
            if (category == 4) clickGui.showAutoMarket(); else clickGui.showConfigs();
            return;
        }
        dev.bkclient.ui.menu.MenuScreen screen = BKClientInit.getInstance().getMenuScreen();
        if (screen != null) mc.setScreen(screen);
    }

    private final EventListener<ReceivePacketEvent> onPacket = event -> {
        if (dev.bkclient.systems.modules.modules.player.AutoLeave.isStashing()) return;
        Packet<?> p = event.getPacket();
        if (p instanceof GameMessageS2CPacket packet) {
            Text msg = packet.content();
            boolean overlay = packet.overlay();
            // Minestar is dispatched once by the history manager on the client thread.
            if (!minestarMode.isEnabled()) autoMarketController.onChatMessage(msg, overlay);
            lobbyController.onChatMessage(msg, overlay);
        } else if (p instanceof TitleS2CPacket titlePacket) {
            // Minestar shows the market-limit notice as a screen title; AutoSellController
            // only reacts in minestar sell mode, so this is a no-op on other servers.
            if (!minestarMode.isEnabled()) handleTitleText(titlePacket.text());
        } else if (p instanceof SubtitleS2CPacket subtitlePacket) {
            if (!minestarMode.isEnabled()) handleTitleText(subtitlePacket.text());
        }
    };

    public void onMinestarMessage(
            dev.bkclient.systems.modules.modules.bkmods.automarket.MinestarPacketRouter.Message message) {
        if (dev.bkclient.systems.modules.modules.player.AutoLeave.isStashing()) return;
        if (!minestarMode.isEnabled()) return;
        if (message.event() != null) autoSellController.onMinestarEvent(message.event());
        if (message.chat()) autoMarketController.onMinestarChatMessage(message.text(), message.event());
    }

    private void handleTitleText(Text text) {
        if (text == null) {
            return;
        }
        String clean = BK_RE_0.matcher(ItemStackExtractor.toLegacy(text)).replaceAll("").trim().toLowerCase();
        clean = ItemStackExtractor.toReadablePlain(text).trim().toLowerCase();
        if (!clean.isEmpty()) {
            autoSellController.onTitleMessage(clean);
            autoMarketController.onTitleMessage(clean);
        }
    }

    private final EventListener<KeyPressEvent> onKey = event -> {
        if (dev.bkclient.systems.modules.modules.player.AutoLeave.isStashing()) return;
        if (this.openBind.isKey(event.getKey()) && mc.currentScreen == null) {
            openCsGuiTab(4); // AutoMarket
        }
        if (event.getAction() == 1 && this.quickAddBind.isKey(event.getKey())
                && mc.currentScreen instanceof HandledScreen<?> handledScreen) {
            // Manually find the slot under the mouse cursor
            double mouseX = mc.mouse.getX() * (double) mc.getWindow().getScaledWidth()
                    / (double) mc.getWindow().getWidth();
            double mouseY = mc.mouse.getY() * (double) mc.getWindow().getScaledHeight()
                    / (double) mc.getWindow().getHeight();
            int guiX = ((HandledScreenAccessor) handledScreen).getX();
            int guiY = ((HandledScreenAccessor) handledScreen).getY();

            Slot foundSlot = null;
            for (Slot slot : handledScreen.getScreenHandler().slots) {
                int slotX = guiX + slot.x;
                int slotY = guiY + slot.y;
                if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                    foundSlot = slot;
                    break;
                }
            }

            if (foundSlot != null && foundSlot.hasStack()) {
                PriceEntry entry = ItemStackExtractor.extract(foundSlot.getStack());
                if (entry != null) {
                    mc.setScreen(new QuickAddScreen(handledScreen, entry, foundSlot.getStack()));
                }
            }
        }
        if (event.getAction() == 1 && this.scanKey.isKey(event.getKey())) {
            if (autoMarketController.isActive()) {
                autoMarketController.cancel();
                BKClientInit.getInstance().getNotificationManager()
                        .addNotification(dev.bkclient.systems.notifications.NotificationType.ERROR,
                                Localizator.translate("modules.notifications.auto_market.scanner_off"));
            } else {
                syncControllersConfig();
                autoMarketController.start();
                if (mc.currentScreen == null && mc.player != null && shouldOpenMarketFromScanKey()) {
                    autoMarketController.forceMarketOpen();
                }
                BKClientInit.getInstance().getNotificationManager()
                        .addNotification(dev.bkclient.systems.notifications.NotificationType.SUCCESS,
                                Localizator.translate("modules.notifications.auto_market.scanner_on"));
            }
        }
    };

    private boolean shouldOpenMarketFromScanKey() {
        return !rapyMode.isEnabled() && !minestarMode.isEnabled();
    }

    private final EventListener<HudRenderEvent> onHud = event -> {
        if (mc.currentScreen == null) {
            this.title = "";
            // Legacy Popup rendering removed
        }
    };
    private final EventListener<ContainerClickEvent> onClick = event -> {
    };
    private final EventListener<ContainerReleaseEvent> onRelease = event -> {
    };

    @Generated
    public EventListener<HudRenderEvent> getOnHud() {
        return this.onHud;
    }

    @Generated
    public EventListener<ContainerClickEvent> getOnClick() {
        return this.onClick;
    }

    @Generated
    public EventListener<ContainerReleaseEvent> getOnRelease() {
        return this.onRelease;
    }

    public AutoMarket() {
        // Open the AutoMarket configuration screen.
        this.getSettings().clear();
        // Next/Prev Page Slot are configured in AutoMarketContent, not here \u2014 keep them out of
        // this module's UI list (their values + minestar navigation logic stay active elsewhere).
        this.getSettings().addAll(List.of(
                configName,
                openBind, quickAddBind, scanKey,
                autoJump, autoRepair, discordWebhookUrl, webhookScreenshot,
                cheapColor, expensiveColor,
                aggressiveMode, rapyMode, minestarMode,
                toggleMarket, openConfig));

        // Wire lobby controller callback
        lobbyController.setOnFlowComplete(() -> {
            syncControllersConfig();
            autoMarketController.start();
            autoMarketController.forceMarketOpen();
        });

        WebhookConfig.load();
        this.discordWebhookUrl.setText(WebhookConfig.getUrl());

        // Startup: Load the profile specified in configName.
        // configName.getText() is read INSIDE mc.execute so it reflects the value
        // after autosave.bk has been loaded (which may have changed it from
        // "default").
    }

    /** Completes profile loading after the global autosave has populated this module's settings. */
    public void finishInitialization() {
        if (!this.loading) return;
        try {
            String profileToLoad = this.configName.getText();
            if (profileToLoad != null && !profileToLoad.isEmpty()) {
                this.activeProfile = profileToLoad;
                this.configManager.load(profileToLoad);
                this.lastRegistrySize = itemRegistry.getValue().size();
                this.lastPriceVersion = (int) autoMarketController.getPriceVersion();
            }
        } finally {
            this.loading = false;
            String webhookValue = discordWebhookUrl.getText();
            if (webhookValue != null && !webhookValue.isEmpty()) {
                WebhookConfig.save(webhookValue);
            }
        }
    }

    @Override
    public List<Setting> getDisplaySettings() {
        return this.getSettings().stream()
                .filter(s -> s != aggressiveMode && s != rapyMode && s != minestarMode)
                .toList();
    }

    /**
     * Sync current UI settings to all controllers.
     */
    public void syncControllersConfig() {
        autoMarketController.setEntries(itemRegistry.getValue());
        autoMarketController.setMarketCommand(marketCommand.getText());
        autoMarketController.setConfirmSlot((int) confirmSlotSetting.getCurrentValue());
        autoMarketController.setScannerDebug(debugScreenInfo.isEnabled());
        autoMarketController.setLoreRegex(loreRegex.getText());
        autoMarketController.setLoreSellerRegex(loreSellerRegex.getText());
        boolean rapy = rapyMode.isEnabled();
        boolean mineStar = minestarMode.isEnabled();
        autoMarketController.setRapyMode(rapy);
        autoMarketController.setMinestarMode(mineStar);
        autoMarketController.setIgnoreMarketTitle(mineStar || rapy);

        String address = (mc.getCurrentServerEntry() != null) ? mc.getCurrentServerEntry().address.toLowerCase() : "";
        boolean isAn = address.contains(ClientConstants.ANARCHIA_GG) || address.contains(ClientConstants.ANARCHIA_PL);

        if (isAn && !rapy && !mineStar) {
            autoMarketController.setIsAnarchia(true);
            autoMarketController.setSortingSlot(53); // Forced for Anarchia
            autoMarketController.setNextPageSlot(51); // Forced avoid conflict with sort
            autoMarketController.setSortingKeyword(ClientConstants.SORT_KW_NOWSZE);
            autoMarketController.setAggressiveMode(true);
        } else {
            autoMarketController.setIsAnarchia(false);
            autoMarketController.setSortingSlot((int) sortingSlotSetting.getCurrentValue());
            autoMarketController.setNextPageSlot((int) nextPageSlotSetting.getCurrentValue());
            autoMarketController.setSortingKeyword(sortingKeyword.getText());
            autoMarketController.setAggressiveMode(aggressiveMode.isEnabled());
        }

        autoMarketController.setRefreshSlot((int) refreshSlotSetting.getCurrentValue());

        autoMarketController.setConfirmDelayMs((int) confirmDelay.getCurrentValue());
        autoMarketController.setConfirmAttempts(confirmDelay.getMultiplier());
        autoMarketController.setRefreshDelayMs((int) refreshRate.getCurrentValue());
        autoMarketController.setRestartDelayMs((int) restartDelay.getCurrentValue());


        reconnectController.setDelaySeconds((int) reconnectDelay.getCurrentValue());
        autoSellController.setSellCommandTemplate(sellCommand.getText());
        autoSellController.setSellIntervalMs((long) (sellInterval.getCurrentValue() * 1000.0f));
        autoSellController.setSellTaxPercent(sellTax.getCurrentValue());
        autoSellController.setAutoSellEnabled(autoSellEnabled.isEnabled());
        autoSellController.setAutoRepairEnabled(autoRepair.isEnabled());

        // Webhooks
        autoMarketController.setWebhookUrl(discordWebhookUrl.getText());
        autoSellController.setWebhookUrl(discordWebhookUrl.getText());
        autoMarketController.setWebhookScreenshot(webhookScreenshot.isEnabled());

        autoMarketController.setCustomMarketTitle(customMarketTitle.getText());
        autoMarketController.setMinestarSlots((int) minestarNextSlot.getCurrentValue(),
                (int) minestarPrevSlot.getCurrentValue());
        autoSellController.setMinestarSellMode(mineStar || rapy);
        autoSellController.setMinestarConfirmationMode(mineStar);

        // Legacy 1:1 Configs (Internally handled if needed)
        autoMarketController.setConfirmItemName(confirmItemName.getText());

        // Sync sell controller scanner
        autoSellController.setScanner(autoMarketController.getScanner());
    }

    @Override
    public void tick() {
        if (dev.bkclient.systems.modules.modules.player.AutoLeave.isStashing()) return;
        // 1. Sync config at most every 500ms
        long now2 = System.currentTimeMillis();
        if (now2 - lastSyncMs >= 500) {
            syncControllersConfig();
            lastSyncMs = now2;
        }

        // 2.1 Anti-AFK Logic
        handleAntiAfk();

        // 2.2 Periodic Auto-Save and Registry/Price Monitoring
        long now = System.currentTimeMillis();
        int currentSize = itemRegistry.getValue().size();
        int currentPriceVersion = (int) autoMarketController.getPriceVersion();

        boolean sizeChanged = currentSize != lastRegistrySize;
        boolean versionChanged = currentPriceVersion != lastPriceVersion;
        boolean periodic = now - lastAutoSaveTime > 30000L;

        if (sizeChanged || versionChanged || periodic) {
            if ((sizeChanged || periodic) && lastRegistrySize != -1 && !loading) {
                autoMarketController.notifyRegistryChanged();
                currentPriceVersion = (int) autoMarketController.getPriceVersion();
            }
            lastRegistrySize = currentSize;
            lastPriceVersion = currentPriceVersion;
            lastAutoSaveTime = now;
        }

        // Save profile on change (debounced 500ms)
        if (profileDirty && now - lastProfileChangeTime > 500 && !loading && !loadingProfile
                && !dev.bkclient.systems.setting.GuiSaveBatch.isEditing()) {
            configManager.save(activeProfile);
            profileDirty = false;
        }

        // 3. Lobby controller tick
        lobbyController.onTick();

        // 4. Main automation controller tick
        autoMarketController.onTick();

        // 5. Handle reconnect \u2192 lobby flow
        // Start movement as soon as the reconnected player exists. Waiting only for
        // the action bar caused a race when its prompt arrived before this tick.
        if (reconnectController.isReconnecting() && mc.player != null && mc.currentScreen == null) {
            reconnectController.setReconnecting(false);
            lobbyController.onAutoReconnectComplete();
        }

        super.tick();
    }

    // Add the automatic jump to this tick's input without changing the physical
    // key binding. Resetting the timer must never release a key held by the user.
    private final EventListener<InputEvent> onJumpInput = event -> {
        if (dev.bkclient.systems.modules.modules.player.AutoLeave.isStashing()) return;
        if (isEnabled() && autoJump.isEnabled() && jumpTicksRemaining > 0
                && mc.player != null && mc.currentScreen == null) {
            event.setJump(true);
        }
    };

    private void handleAntiAfk() {
        if (!autoJump.isEnabled() || mc.player == null) {
            resetAntiAfkState();
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Oczekiwanie na ponowne otwarcie rynku po skoku
        if (reopenTime > 0) {
            if (now >= reopenTime) {
                reopenTime = 0;
                if (autoMarketController.isActive()) {
                    autoMarketController.forceMarketOpen();
                }
            }
            return;
        }

        // 2. Sekwencja skoku (30 tick\u00F3w = 1.5s, jak bk-rynek)
        if (jumpStartTime > 0) {
            if (now - lastJumpCloseAttempt >= 500) {
                closeScreenForJump();
                lastJumpCloseAttempt = now;
            }
            if (now >= jumpStartTime) {
                if (mc.currentScreen != null) {
                    jumpStartTime = now + 500;
                    return;
                }
                jumpStartTime = 0;
                lastJumpCloseAttempt = 0;
                jumpTicksRemaining = 30;
                scheduleNextJump();
            }
            return;
        }

        if (jumpTicksRemaining > 0) {
            jumpTicksRemaining--;
            if (jumpTicksRemaining == 0) {
                if (wasScreenOpen) {
                    reopenTime = now + 2000;
                    wasScreenOpen = false;
                }
            }
            return;
        }

        // 3. Ustaw czas pierwszego skoku
        if (nextJumpTime == 0) {
            scheduleNextJump();
            return;
        }

        // 4. Czas na skok
        if (now >= nextJumpTime) {
            if (mc.currentScreen != null) {
                wasScreenOpen = true;
                closeScreenForJump();
                lastJumpCloseAttempt = now;
                jumpStartTime = now + 3000;
            } else {
                wasScreenOpen = false;
                jumpTicksRemaining = 30;
                scheduleNextJump();
            }
        }
    }

    private void closeScreenForJump() {
        if (mc.player != null) {
            mc.player.closeHandledScreen();
        }
        if (mc.currentScreen != null) {
            mc.setScreen(null);
        }
    }

    public void prepareForAutoLeave() {
        autoMarketController.cancel();
        autoSellController.reset();
        lobbyController.stopAll();
        reconnectController.cancel();
        resetAntiAfkState();
    }

    private void resetAntiAfkState() {
        nextJumpTime = 0;
        jumpTicksRemaining = 0;
        wasScreenOpen = false;
        jumpStartTime = 0;
        lastJumpCloseAttempt = 0;
        reopenTime = 0;
    }

    private void scheduleNextJump() {
        long min = 120000;
        long max = 150000;
        nextJumpTime = System.currentTimeMillis() + min + (long) (random.nextDouble() * (max - min));
    }

    @Override
    protected boolean shouldAutoSaveOnToggle() {
        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        syncControllersConfig();
        var hud = BKClientInit.getInstance().getHud();
        if (hud != null) hud.getElements().stream()
            .filter(element -> element instanceof dev.bkclient.ui.hud.impl.AutoMarketHud)
            .forEach(element -> element.setShowing(true));
    }

    @Override
    public void onDisable() {
        autoMarketController.cancel();
        lobbyController.reset();
        reconnectController.reset();
        autoSellController.reset();
        resetAntiAfkState();

        // Force save current market profile immediately on disable
        if (!loading && !loadingProfile) {
            configManager.save(activeProfile);
            profileDirty = false;
        }

        super.onDisable();
    }

    public void shutdown() {
        autoMarketController.shutdown();
    }

    public AutoMarketConfigManager getConfigManager() {

        return configManager;
    }

    public AutoMarketController getAutoMarketController() {
        return autoMarketController;
    }

    public boolean isAutoJumping() {
        return jumpTicksRemaining > 0;
    }

    public LobbyController getLobbyController() {
        return lobbyController;
    }

    public ReconnectController getReconnectController() {
        return reconnectController;
    }

    public AutoSellController getAutoSellController() {
        return autoSellController;
    }

    @Generated
    public EventListener<ReceivePacketEvent> getOnPacket() {
        return this.onPacket;
    }

    @Generated
    public EventListener<KeyPressEvent> getOnKey() {
        return this.onKey;
    }

    /**
     * Used by ConfigManager to save EVERYTHING, even if hidden from ClickGUI.
     */
    public List<Setting> getAllPersistentSettings() {
        List<Setting> all = new ArrayList<>();
        all.add(openBind);
        all.add(quickAddBind);
        all.add(scanKey);
        all.add(refreshRate);
        all.add(confirmDelay);
        all.add(restartDelay);
        all.add(sellInterval);
        all.add(sellTax);
        all.add(sellCommand);
        all.add(showHistory);
        all.add(reconnectDelay);
        all.add(autoJump);
        all.add(autoReconnect);
        all.add(highlightMode);
        all.add(aggressiveMode);
        all.add(debugScreenInfo);
        all.add(marketCommand);
        all.add(customMarketTitle);
        all.add(ignoreMarketTitle);
        all.add(rapyMode);
        all.add(minestarMode);
        all.add(minestarNextSlot);
        all.add(minestarPrevSlot);
        all.add(confirmSlotSetting);
        all.add(sortingSlotSetting);
        all.add(refreshSlotSetting);
        all.add(nextPageSlotSetting);
        all.add(autoSellEnabled);
        all.add(loreRegex);
        all.add(loreSellerRegex);
        all.add(autoRepair);
        all.add(cheapColor);
        all.add(expensiveColor);
        all.add(accountType);
        all.add(password);
        all.add(loginCommand);
        all.add(configName);
        all.add(discordWebhookUrl);
        all.add(webhookScreenshot);
        all.add(unitModeGlobal);
        all.add(itemRegistry);
        return all;
    }

    /**
     * Settings that are exported/imported via Market Profiles
     * (auto_market_configs).
     * Excludes binds, account info, and visual preferences.
     */
    public List<Setting> getMarketSettings() {
        List<Setting> all = new ArrayList<>();
        // Binds
        all.add(openBind);
        all.add(quickAddBind);
        all.add(scanKey);
        // Bot logic & timing
        all.add(refreshRate);
        all.add(confirmDelay);
        all.add(restartDelay);
        all.add(aggressiveMode);
        all.add(debugScreenInfo);
        all.add(unitModeGlobal);
        all.add(autoJump);
        all.add(autoRepair);
        all.add(autoSellEnabled);
        all.add(sellInterval);
        all.add(sellTax);
        all.add(showHistory);
        all.add(discordWebhookUrl);
        all.add(webhookScreenshot);
        all.add(accountType);
        all.add(password);
        all.add(loginCommand);

        // Server titles/commands
        all.add(sellCommand);
        all.add(marketCommand);
        all.add(customMarketTitle);
        all.add(ignoreMarketTitle);
        all.add(rapyMode);
        all.add(minestarMode);
        all.add(minestarNextSlot);
        all.add(minestarPrevSlot);

        // Slots
        all.add(confirmSlotSetting);
        all.add(sortingSlotSetting);
        all.add(refreshSlotSetting);
        all.add(nextPageSlotSetting);

        // Regex / Scanning
        all.add(sortingKeyword);
        all.add(confirmItemName);
        all.add(loreRegex);
        all.add(loreSellerRegex);

        // Data
        all.add(itemRegistry);
        return all;
    }

}
