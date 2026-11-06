package dev.bkclient.ui.menu.csgui.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.config.ConfigManager;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.modules.modules.bkmods.config.AutoMarketConfigManager;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.csgui.content.config.ConfigItem;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.CloudConfigUtility;
import dev.bkclient.utility.game.ClientConstants;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.render.ScissorUtility;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ConfigContent {

    private static final int CLOUD_KEY_LENGTH = 12;

    private static final float PANEL_RADIUS = 8.0f;
    private static final float BORDER_THICKNESS = 0.05f;
    private static final float PANEL_GAP = 8.0f;
    private static final float PANEL_W = 194.0f;
    private static final float PANEL_H = 193.0f;

    private static final float TITLE_FONT_SIZE = 8.0f;
    private static final float TITLE_GAP = 8.0f;
    private static ColorRGBA TITLE_ACTIVE() { return dev.bkclient.ui.menu.csgui.CsTheme.Color.accent(); }
    private static final ColorRGBA TITLE_INACTIVE = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.75f);
    private static final ColorRGBA COUNT_COLOR = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);
    private static final ColorRGBA LINE_COLOR = new ColorRGBA(217.0f, 217.0f, 217.0f, 255.0f * 0.15f);

    private static final ColorRGBA PANEL_FILL = new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.6f);
    private static final ColorRGBA PANEL_BORDER = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.08f);
    private static final ColorRGBA PANEL_GLASS = new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.5f);

    private static final float BAR_H = 26.0f;
    private static final float BAR_RADIUS = 6.0f;
    private static final float BTN_SIZE = 26.0f;
    private static final float BAR_GAP = 4.0f;
    private static final float BAR_FONT_SIZE = 7.0f;
    private static final float BTN_ICON_SIZE = 8.5f;
    private static final ColorRGBA BAR_FILL = new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.6f);
    private static final ColorRGBA BAR_BORDER = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.05f);
    private static final ColorRGBA BAR_HINT = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);
    private static final ColorRGBA BTN_ICON_ADD = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);
    private static final ColorRGBA BTN_ICON_DL = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.75f);

    private static final float SW_W = 13.0f;
    private static final float SW_H = 7.0f;
    private static final float SW_KNOB = 5.0f;
    private static final float SW_ICON = 6.7f;
    private static final float SW_MARKET_ICON = 6.25f;
    private static final float SW_FONT = 5.5f;
    private static final char ICON_GLOBAL = '9';
    private static final char ICON_MARKET = '!';
    private static ColorRGBA SW_ACCENT() { return dev.bkclient.ui.menu.csgui.CsTheme.Color.accent(); }
    private static final ColorRGBA DIM = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);

    private boolean globalSide = true;
    private float swX, swY;

    // Rename w toku: nazwa wpisana do pola, Enter wykona rename zamiast create
    private String globalRenameFrom;
    private String marketRenameFrom;

    private final List<ConfigItem> globalConfigs = new ArrayList<>();
    private final List<ConfigItem> marketConfigs = new ArrayList<>();
    private final TextField globalNameField = new TextField(Fonts.SEMIBOLD.getFont(BAR_FONT_SIZE));
    private final TextField marketNameField = new TextField(Fonts.SEMIBOLD.getFont(BAR_FONT_SIZE));
    private final ScrollHandler globalScroll = new ScrollHandler();
    private final ScrollHandler marketScroll = new ScrollHandler();

    private String globalSignature = "";
    private String marketSignature = "";
    private volatile boolean globalDirty = true;
    private volatile boolean marketDirty = true;
    private float globalListX, globalListY, globalListW, globalListH;
    private float marketListX, marketListY, marketListW, marketListH;
    private float globalFieldX, globalFieldY, globalFieldW, globalFieldH;
    private float marketFieldX, marketFieldY, marketFieldW, marketFieldH;
    private float globalAddX, globalAddY, globalImportX, globalImportY;
    private float marketAddX, marketAddY, marketImportX, marketImportY;

    public ConfigContent() {
        this.globalNameField.onInit();
        this.marketNameField.onInit();
    }

    public void render(UIContext context, float x, float y, float w, float h, float alpha) {
        this.syncGlobalConfigs();
        this.syncMarketConfigs();

        Font titleFont = Fonts.SEMIBOLD.getFont(TITLE_FONT_SIZE);
        float headerH = titleFont.height() + TITLE_GAP;

        float panelW = Math.min(PANEL_W, (w - PANEL_GAP * 3.0f) / 2.0f);
        float panelH = Math.min(PANEL_H, h - PANEL_GAP * 2.0f - headerH);
        float totalW = panelW * 2.0f + PANEL_GAP;
        float startX = x + (w - totalW) / 2.0f;
        float headerY = y + (h - panelH - headerH) / 2.0f;
        float py = headerY + headerH;

        String[] titles = { "Global Configs", "Market Configs" };
        int[] counts = { globalConfigs.size(), marketConfigs.size() };
        float barAreaH = BAR_H + 6.0f;

        for (int i = 0; i < 2; ++i) {
            float px = startX + i * (panelW + PANEL_GAP);
            boolean active = (i == 0) == this.globalSide;
            ColorRGBA titleColor = i == 0 ? TITLE_ACTIVE() : TITLE_INACTIVE;
            String title = titles[i];
            String count = String.valueOf(counts[i]);
            float titleW = titleFont.width(title);
            float countW = titleFont.width(count);
            float titleCenterY = headerY + titleFont.height() / 2.0f;

            context.drawText(titleFont, title, px + 2.0f, headerY, titleColor.withAlpha(titleColor.getAlpha() * alpha));
            context.drawRightText(titleFont, count, px + panelW - 2.0f, headerY,
                    COUNT_COLOR.withAlpha(COUNT_COLOR.getAlpha() * alpha));

            float lineX1 = px + 2.0f + titleW + 6.0f;
            float lineX2 = px + panelW - 2.0f - countW - 6.0f;
            if (lineX2 > lineX1) {
                context.drawRect(lineX1, titleCenterY - 0.1f, lineX2 - lineX1, 1.0f,
                        LINE_COLOR.withAlpha(LINE_COLOR.getAlpha() * alpha));
            }

            this.panel(context, px, py, panelW, panelH, alpha);

            List<ConfigItem> items = i == 0 ? globalConfigs : marketConfigs;
            float itemX = px + 6.0f;
            float itemW = panelW - 12.0f;
            float itemStartY = py + 6.0f;
            float itemGap = 4.0f;
            float itemsBottom = py + panelH - barAreaH;
            float listH = Math.max(0.0f, itemsBottom - itemStartY);
            this.storeListBounds(i == 0, itemX, itemStartY, itemW, listH);
            ScrollHandler scroll = i == 0 ? this.globalScroll : this.marketScroll;
            float scrollY = (float) scroll.getValue();
            ScissorUtility.push(context.getMatrices(), itemX - 1.0f, itemStartY, itemW + 2.0f, listH);
            float panelBottom = py + panelH;
            float itemStride = ConfigItem.CARD_HEIGHT + itemGap;
            int firstVisible = Math.max(0, (int) Math.floor((scrollY - ConfigItem.CARD_HEIGHT) / itemStride));
            int lastVisible = Math.min(items.size(), (int) Math.ceil((scrollY + listH) / itemStride) + 1);
            for (int j = firstVisible; j < lastVisible; ++j) {
                ConfigItem item = items.get(j);
                item.setPanelBottom(panelBottom);
                float itemY = itemStartY + j * itemStride - scrollY;
                item.render(context, itemX, itemY, itemW, alpha);
            }
            ScissorUtility.pop();
            float contentH = Math.max(0.0f, items.size() * itemStride - itemGap);
            scroll.setMax(Math.min(0.0, listH - contentH));
            scroll.update();

            float dim = active ? 1.0f : 0.5f;
            float barY = py + panelH - 6.0f - BAR_H;
            float barX = px + 6.0f;
            float fieldW = panelW - 12.0f - (BTN_SIZE + BAR_GAP) * 2.0f;

            this.renderInputBar(context, i == 0, barX, barY, fieldW, alpha * dim);

            float btn1X = barX + fieldW + BAR_GAP;
            this.bar(context, btn1X, barY, BTN_SIZE, BAR_H, alpha * dim);
            context.drawIcon(Fonts.ICONS, '3', btn1X + (BTN_SIZE - BTN_ICON_SIZE) / 2.0f,
                    barY + (BAR_H - BTN_ICON_SIZE) / 2.0f, BTN_ICON_SIZE,
                    BTN_ICON_ADD.withAlpha(255.0f * 0.25f * alpha * dim));

            float btn2X = btn1X + BTN_SIZE + BAR_GAP;
            this.bar(context, btn2X, barY, BTN_SIZE, BAR_H, alpha * dim);
            context.drawIcon(Fonts.ICONS, 'S', btn2X + (BTN_SIZE - BTN_ICON_SIZE) / 2.0f,
                    barY + (BAR_H - BTN_ICON_SIZE) / 2.0f, BTN_ICON_SIZE,
                    BTN_ICON_DL.withAlpha(255.0f * 0.75f * alpha * dim));

            this.storeBarBounds(i == 0, barX, barY, fieldW, btn1X, btn2X);
        }

        this.renderSwitch(context, startX + totalW / 2.0f, py + panelH + 12.0f, alpha);

        for (ConfigItem item : globalConfigs) {
            if (item.shouldRenderPopup()) {
                item.renderPopup(context, alpha);
            }
        }
        for (ConfigItem item : marketConfigs) {
            if (item.shouldRenderPopup()) {
                item.renderPopup(context, alpha);
            }
        }
    }

    private void syncGlobalConfigs() {
        ConfigManager manager = BKClientInit.getInstance().getConfigManager();
        if (manager == null) {
            return;
        }
        if (!this.globalDirty) {
            return;
        }
        this.globalDirty = false;
        manager.refresh();
        StringBuilder signature = new StringBuilder();
        ConfigFile current = manager.getCurrent();
        String active = current != null ? current.getFileName() : "autosave";
        // Sygnatura bez lastModified — autosave ciagle zmienia czas pliku i lista
        // przebudowywalaby sie non-stop (miganie). Liczy sie tylko zbior nazw + aktywny.
        for (ConfigFile config : manager.getConfigFiles()) {
            signature.append(config.getFileName()).append(':')
                    .append(config.getFileName().equalsIgnoreCase(active)).append(';');
        }
        if (signature.toString().equals(this.globalSignature)) {
            return;
        }
        this.globalSignature = signature.toString();
        String openPopup = this.openPopupName(this.globalConfigs);
        this.globalConfigs.clear();
        for (ConfigFile config : manager.getConfigFiles()) {
            boolean enabled = config.getFileName().equalsIgnoreCase(active);
            ConfigItem item = new ConfigItem(config.getDisplayName(), config.getFile().lastModified(), enabled,
                    () -> this.loadGlobalConfig(config),
                    () -> this.saveGlobalConfig(config),
                    () -> this.shareGlobalConfig(config, false),
                    () -> this.shareGlobalConfig(config, true),
                    () -> this.deleteGlobalConfig(config),
                    () -> this.beginRenameGlobal(config),
                    () -> this.loadGlobalConfig(config));
            if (openPopup != null && openPopup.equals(item.getName())) {
                item.setPopupOpen(true);
            }
            this.globalConfigs.add(item);
        }
    }

    private String openPopupName(List<ConfigItem> items) {
        for (ConfigItem item : items) {
            if (item.isPopupOpen()) {
                return item.getName();
            }
        }
        return null;
    }

    private void invalidateGlobalConfigs() {
        this.globalDirty = true;
    }

    private void syncMarketConfigs() {
        AutoMarket module = this.autoMarket();
        if (module == null || module.getConfigManager() == null) {
            return;
        }
        if (!this.marketDirty) {
            return;
        }
        this.marketDirty = false;
        module.getConfigManager().refresh();
        String active = module.getActiveProfile();
        StringBuilder signature = new StringBuilder();
        for (AutoMarketConfigManager.ConfigInfo config : module.getConfigManager().getConfigs()) {
            signature.append(config.filename).append(':')
                    .append(config.date).append(':')
                    .append(config.filename.equalsIgnoreCase(active)).append(';');
        }
        if (signature.toString().equals(this.marketSignature)) {
            return;
        }
        this.marketSignature = signature.toString();
        String openPopup = this.openPopupName(this.marketConfigs);
        this.marketConfigs.clear();
        for (AutoMarketConfigManager.ConfigInfo config : module.getConfigManager().getConfigs()) {
            boolean enabled = active != null && config.filename.equalsIgnoreCase(active);
            ConfigItem item = new ConfigItem(config.name, config.date, enabled,
                    () -> this.loadMarketConfig(config),
                    () -> this.saveMarketConfig(config),
                    () -> this.shareMarketConfig(config, false),
                    () -> this.shareMarketConfig(config, true),
                    () -> this.deleteMarketConfig(config),
                    () -> this.beginRenameMarket(config),
                    () -> this.loadMarketConfig(config));
            if (openPopup != null && openPopup.equals(item.getName())) {
                item.setPopupOpen(true);
            }
            this.marketConfigs.add(item);
        }
    }

    private void invalidateMarketConfigs() {
        this.marketDirty = true;
    }

    private void invalidateMarketConfigsLater(long delayMs) {
        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(
                () -> net.minecraft.client.MinecraftClient.getInstance().execute(this::invalidateMarketConfigs));
    }

    private void loadGlobalConfig(ConfigFile config) {
        ConfigManager manager = BKClientInit.getInstance().getConfigManager();
        if (manager == null || config == null) {
            return;
        }
        ConfigFile prev = manager.getAutoSaveConfig();
        if (prev != null && !prev.getFileName().equalsIgnoreCase(config.getFileName()) && !prev.save()) {
            this.notifyError("Config", "Nie zapisano aktualnego configu. Przelaczanie anulowane.");
            return;
        }
        config.load();
        this.invalidateGlobalConfigs();
    }

    private void saveGlobalConfig(ConfigFile config) {
        if (config == null) {
            return;
        }
        if (!config.save()) {
            this.notifyError("Config", "Nie udalo sie zapisac configu.");
            return;
        }
        this.notify("Config", "Config zapisany.");
        this.invalidateGlobalConfigs();
    }

    private void shareGlobalConfig(ConfigFile config, boolean privateMode) {
        if (config == null) {
            return;
        }
        try {
            String content = Files.readString(config.getFile().toPath());
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            json.addProperty("meta_name", ConfigManager.sanitizeConfigName(config.getFileName()));
            String uploadContent = FileManager.GSON.toJson(json);
            this.notify("Config", "Udostepnianie...");
            CloudConfigUtility.uploadConfig(config.getFileName(), uploadContent).thenAccept(result -> {
                if (result != null) {
                    String code = ClientConstants.CLOUD_PREFIX + result.key;
                    if (privateMode && result.deletionKey != null) {
                        code += result.deletionKey;
                    }
                    net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(code);
                    this.notify("Config", privateMode ? "Prywatny kod skopiowany." : "Kod skopiowany.");
                } else {
                    this.notifyError("Config", "Nie udalo sie udostepnic.");
                }
            });
        } catch (Exception e) {
            this.notifyError("Config", "Nie udalo sie odczytac configu.");
        }
    }

    private void deleteGlobalConfig(ConfigFile config) {
        if (config == null || "autosave".equalsIgnoreCase(config.getFileName())) {
            return;
        }
        ConfigManager manager = BKClientInit.getInstance().getConfigManager();
        if (manager.getCurrent() != null
                && manager.getCurrent().getFileName().equalsIgnoreCase(config.getFileName())) {
            ConfigFile autosave = manager.getConfig("autosave", true);
            if (autosave == null || !autosave.load()) {
                this.notifyError("Config", "Nie udalo sie przelaczyc na autosave; config nie zostal usuniety.");
                return;
            }
        }
        config.delete();
        manager.refresh();
        this.invalidateGlobalConfigs();
    }

    private void beginRenameGlobal(ConfigFile config) {
        if (config == null || "autosave".equalsIgnoreCase(config.getFileName())) {
            return;
        }
        this.globalSide = true;
        this.globalRenameFrom = config.getFileName();
        this.globalNameField.setText(config.getDisplayName());
        this.globalNameField.setFocused(true);
    }

    private void renameGlobalConfig(String oldName, String newName) {
        ConfigManager manager = BKClientInit.getInstance().getConfigManager();
        if (manager == null || oldName == null) {
            return;
        }
        String clean = ConfigManager.sanitizeConfigName(newName);
        if (clean.isEmpty() || clean.equalsIgnoreCase(oldName)) {
            return;
        }
        ConfigFile config = manager.getConfig(oldName);
        if (config == null) {
            return;
        }
        java.io.File oldFile = config.getFile();
        // Zachowaj oryginalne rozszerzenie pliku (.bk) — inaczej skaner go nie znajdzie
        String oldFileName = oldFile.getName();
        int dot = oldFileName.lastIndexOf('.');
        String ext = dot >= 0 ? oldFileName.substring(dot) : "";
        java.io.File newFile = new java.io.File(oldFile.getParentFile(), clean + ext);
        if (newFile.exists()) {
            this.notifyError("Config", "Nazwa juz istnieje.");
            return;
        }
        try {
            manager.renameConfig(config, clean);
            this.notify("Config", "Zmieniono nazwe.");
        } catch (java.io.IOException error) {
            BKClientInit.LOGGER.error("Failed to rename config {}", oldName, error);
            this.notifyError("Config", "Nie udalo sie zmienic nazwy.");
        }
        manager.refresh();
        this.invalidateGlobalConfigs();
    }

    private void beginRenameMarket(AutoMarketConfigManager.ConfigInfo config) {
        if (config == null) {
            return;
        }
        this.globalSide = false;
        this.marketRenameFrom = config.filename;
        this.marketNameField.setText(config.name);
        this.marketNameField.setFocused(true);
    }

    private void renameMarketConfig(String oldName, String newName) {
        AutoMarket module = this.autoMarket();
        if (module == null || oldName == null) {
            return;
        }
        if (module.getConfigManager().rename(oldName, newName)) {
            this.notify("Auto-Market", "Zmieniono nazwe.");
        } else {
            this.notifyError("Auto-Market", "Nie udalo sie zmienic nazwy.");
        }
        module.getConfigManager().refresh();
        this.invalidateMarketConfigs();
    }

    private void loadMarketConfig(AutoMarketConfigManager.ConfigInfo config) {
        AutoMarket module = this.autoMarket();
        if (module == null || config == null) {
            return;
        }
        String currentProfile = module.getActiveProfile();
        if (currentProfile != null && !currentProfile.equalsIgnoreCase(config.filename)) {
            module.getConfigManager().save(currentProfile);
        }
        module.setLoading(true);
        try {
            module.configName.setText(config.filename);
            module.setActiveProfile(config.filename);
            module.getConfigManager().load(config.filename);
        } finally {
            module.setLoading(false);
        }
        this.invalidateMarketConfigs();
    }

    private void saveMarketConfig(AutoMarketConfigManager.ConfigInfo config) {
        AutoMarket module = this.autoMarket();
        if (module == null || config == null) {
            return;
        }
        module.configName.setText(config.filename);
        module.setActiveProfile(config.filename);
        module.getConfigManager().save(config.filename);
        this.notify("Auto-Market", "Config zapisany.");
        this.invalidateMarketConfigs();
    }

    private void shareMarketConfig(AutoMarketConfigManager.ConfigInfo config, boolean privateMode) {
        AutoMarket module = this.autoMarket();
        if (module != null && config != null) {
            module.getConfigManager().exportToCloud(config.filename, privateMode);
            this.invalidateMarketConfigs();
        }
    }

    private void deleteMarketConfig(AutoMarketConfigManager.ConfigInfo config) {
        AutoMarket module = this.autoMarket();
        if (module != null && config != null) {
            module.getConfigManager().delete(config.filename);
            this.invalidateMarketConfigs();
        }
    }

    private void renderInputBar(UIContext context, boolean global, float x, float y, float w, float alpha) {
        TextField field = global ? this.globalNameField : this.marketNameField;
        this.bar(context, x, y, w, BAR_H, alpha);
        field.setX(x + 8.0f);
        field.setY(y + 1.0f);
        field.setWidth(w - 16.0f);
        field.setHeight(BAR_H - 2.0f);
        if (field.getText().isEmpty() && !field.isFocused()) {
            context.drawText(Fonts.SEMIBOLD.getFont(BAR_FONT_SIZE), "Cfg name...", x + 8.0f,
                    y + (BAR_H - Fonts.SEMIBOLD.getFont(BAR_FONT_SIZE).height()) / 2.0f,
                    BAR_HINT.withAlpha(255.0f * 0.25f * alpha));
        }
        field.render(context);
    }

    private void storeBarBounds(boolean global, float fieldX, float fieldY, float fieldW, float addX, float importX) {
        if (global) {
            this.globalFieldX = fieldX;
            this.globalFieldY = fieldY;
            this.globalFieldW = fieldW;
            this.globalFieldH = BAR_H;
            this.globalAddX = addX;
            this.globalAddY = fieldY;
            this.globalImportX = importX;
            this.globalImportY = fieldY;
        } else {
            this.marketFieldX = fieldX;
            this.marketFieldY = fieldY;
            this.marketFieldW = fieldW;
            this.marketFieldH = BAR_H;
            this.marketAddX = addX;
            this.marketAddY = fieldY;
            this.marketImportX = importX;
            this.marketImportY = fieldY;
        }
    }

    private void storeListBounds(boolean global, float x, float y, float w, float h) {
        if (global) {
            this.globalListX = x;
            this.globalListY = y;
            this.globalListW = w;
            this.globalListH = h;
        } else {
            this.marketListX = x;
            this.marketListY = y;
            this.marketListW = w;
            this.marketListH = h;
        }
    }

    private void panel(UIContext context, float px, float py, float pw, float ph, float alpha) {
        BorderRadius r = BorderRadius.all(PANEL_RADIUS);
        if (dev.bkclient.systems.modules.modules.visuals.Interface.blurGui())
            context.drawBlurredRect(px, py, pw, ph, 12.0f, 2.0f, r, PANEL_GLASS.withAlpha(PANEL_GLASS.getAlpha() * alpha));
        context.drawRoundedRect(px, py, pw, ph, r, PANEL_FILL.withAlpha(PANEL_FILL.getAlpha() * alpha));
        context.drawRoundedBorder(px, py, pw, ph, BORDER_THICKNESS, r, PANEL_BORDER.withAlpha(PANEL_BORDER.getAlpha() * alpha));
    }

    private void bar(UIContext context, float px, float py, float pw, float ph, float alpha) {
        BorderRadius r = BorderRadius.all(BAR_RADIUS);
        context.drawRoundedRect(px, py, pw, ph, r, BAR_FILL.withAlpha(BAR_FILL.getAlpha() * alpha));
        context.drawRoundedBorder(px, py, pw, ph, 0.05f, r, BAR_BORDER.withAlpha(BAR_BORDER.getAlpha() * alpha));
    }

    private void renderSwitch(UIContext context, float centerX, float topY, float alpha) {
        Font font = Fonts.SEMIBOLD.getFont(SW_FONT + 2);
        float gap = 5.0f;
        String left = "Global";
        String right = "Market";
        float leftW = SW_ICON + gap + font.width(left);
        float rightW = SW_MARKET_ICON + gap + font.width(right);
        float total = leftW + 8.0f + SW_W + 8.0f + rightW;
        float cx = centerX - total / 2.0f;
        float cy = topY + SW_H / 2.0f - 5;

        ColorRGBA leftColor = this.globalSide ? SW_ACCENT() : DIM;
        ColorRGBA rightColor = this.globalSide ? DIM : SW_ACCENT();

        context.drawIcon(Fonts.ICONS, ICON_GLOBAL, cx - 2, cy - SW_ICON / 2.0f - 2, SW_ICON + 4,
                leftColor.withAlpha(leftColor.getAlpha() * alpha));
        context.drawText(font, left, cx + SW_ICON + gap, cy - font.height() / 2.0f,
                (this.globalSide ? ColorRGBA.WHITE : DIM).withAlpha((this.globalSide ? 255.0f : 255.0f * 0.25f) * alpha));

        this.swX = cx + leftW + 8.0f;
        this.swY = cy - SW_H / 2.0f;
        if (this.globalSide) {
            if (dev.bkclient.systems.modules.modules.visuals.Interface.shadow())
                context.drawShadow(this.swX, this.swY, SW_W, SW_H, 7.5f, BorderRadius.all(SW_H / 2.0f),
                    SW_ACCENT().withAlpha(255.0f * 0.5f * alpha));
            context.drawRoundedRect(this.swX, this.swY, SW_W, SW_H, BorderRadius.all(SW_H / 3.0f),
                    SW_ACCENT().withAlpha(SW_ACCENT().getAlpha() * alpha));
        } else {
            context.drawRoundedRect(this.swX, this.swY, SW_W, SW_H, BorderRadius.all(SW_H / 3.0f),
                    new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.15f).withAlpha(255.0f * 0.15f * alpha));
        }
        float knobPad = 1.0f;
        float knobX = this.swX + knobPad + (this.globalSide ? 0.0f : (SW_W - SW_KNOB - knobPad * 2.0f));
        ColorRGBA knobColor = this.globalSide ? ColorRGBA.WHITE : new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);
        context.drawRoundedRect(knobX, this.swY + (SW_H - SW_KNOB) / 2.0f, SW_KNOB, SW_KNOB,
                BorderRadius.all(SW_KNOB / 2.0f), knobColor.withAlpha(knobColor.getAlpha() * alpha));

        float mX = this.swX + SW_W + 8.0f;
        context.drawIcon(Fonts.ICONS, ICON_MARKET, mX - 2, cy - SW_MARKET_ICON / 2.0f - 2.5f, SW_MARKET_ICON + 4,
                rightColor.withAlpha(rightColor.getAlpha() * alpha));
        context.drawText(font, right, mX + SW_MARKET_ICON + gap, cy - font.height() / 2.0f,
                (this.globalSide ? DIM : ColorRGBA.WHITE).withAlpha((this.globalSide ? 255.0f * 0.25f : 255.0f) * alpha));
    }

    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT && button != MouseButton.RIGHT) {
            return false;
        }
        boolean left = button == MouseButton.LEFT;

        ConfigItem clickedItem = this.handleVisibleItemClick(this.globalConfigs, this.globalScroll, this.globalListH,
                mouseX, mouseY, button);
        if (clickedItem != null) {
            this.closeOtherPopups(clickedItem);
            return true;
        }
        clickedItem = this.handleVisibleItemClick(this.marketConfigs, this.marketScroll, this.marketListH,
                mouseX, mouseY, button);
        if (clickedItem != null) {
            this.closeOtherPopups(clickedItem);
            return true;
        }

        if (left) {
            this.globalNameField.onMouseClicked(mouseX, mouseY, button);
            this.marketNameField.onMouseClicked(mouseX, mouseY, button);
        }

        if (left && mouseX >= this.swX && mouseX <= this.swX + SW_W && mouseY >= this.swY && mouseY <= this.swY + SW_H) {
            this.globalSide = !this.globalSide;
            return true;
        }
        if (left && this.globalSide && this.hit(mouseX, mouseY, this.globalAddX, this.globalAddY, BTN_SIZE, BTN_SIZE)) {
            this.createGlobalConfig();
            return true;
        }
        if (left && this.globalSide && this.hit(mouseX, mouseY, this.globalImportX, this.globalImportY, BTN_SIZE, BTN_SIZE)) {
            this.importGlobalConfig();
            return true;
        }
        if (left && !this.globalSide && this.hit(mouseX, mouseY, this.marketAddX, this.marketAddY, BTN_SIZE, BTN_SIZE)) {
            this.createMarketConfig();
            return true;
        }
        if (left && !this.globalSide && this.hit(mouseX, mouseY, this.marketImportX, this.marketImportY, BTN_SIZE, BTN_SIZE)) {
            this.importMarketConfig();
            return true;
        }
        if (left && (this.hit(mouseX, mouseY, this.globalFieldX, this.globalFieldY, this.globalFieldW, this.globalFieldH)
                || this.hit(mouseX, mouseY, this.marketFieldX, this.marketFieldY, this.marketFieldW, this.marketFieldH))) {
            return true;
        }
        return false;
    }

    // Tylko jeden popup naraz — zamknij popupy na wszystkich kartach poza wskazana.
    private void closeOtherPopups(ConfigItem keep) {
        for (ConfigItem item : this.globalConfigs) {
            if (item != keep) {
                item.closePopup();
            }
        }
        for (ConfigItem item : this.marketConfigs) {
            if (item != keep) {
                item.closePopup();
            }
        }
    }

    private ConfigItem handleVisibleItemClick(List<ConfigItem> items, ScrollHandler scroll, float listH,
            double mouseX, double mouseY, MouseButton button) {
        float itemGap = 4.0f;
        float itemStride = ConfigItem.CARD_HEIGHT + itemGap;
        float scrollY = (float) scroll.getValue();
        int firstVisible = Math.max(0, (int) Math.floor((scrollY - ConfigItem.CARD_HEIGHT) / itemStride));
        int lastVisible = Math.min(items.size(), (int) Math.ceil((scrollY + listH) / itemStride) + 1);

        for (int i = firstVisible; i < lastVisible; ++i) {
            ConfigItem item = items.get(i);
            if (item.handleClick(mouseX, mouseY, button)) {
                return item;
            }
        }
        return null;
    }

    public void handleRelease() {
        this.globalNameField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.marketNameField.onMouseReleased(0, 0, MouseButton.LEFT);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.anyFieldFocused()) {
            return false;
        }
        if (keyCode == 256) {
            this.globalNameField.setFocused(false);
            this.marketNameField.setFocused(false);
            return true;
        }
        if (this.globalNameField.isFocused()) {
            this.globalNameField.onKeyPressed(keyCode, scanCode, modifiers);
            if (keyCode == 257) {
                this.createGlobalConfig();
            }
            return true;
        }
        if (this.marketNameField.isFocused()) {
            this.marketNameField.onKeyPressed(keyCode, scanCode, modifiers);
            if (keyCode == 257) {
                this.createMarketConfig();
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean handled = false;
        handled |= this.globalNameField.charTyped(chr, modifiers);
        handled |= this.marketNameField.charTyped(chr, modifiers);
        return handled;
    }

    public boolean onScroll(double mouseX, double mouseY, double verticalAmount) {
        if (this.hit(mouseX, mouseY, this.globalListX, this.globalListY, this.globalListW, this.globalListH)) {
            this.closeAllPopups();
            this.globalScroll.scroll(verticalAmount);
            return true;
        }
        if (this.hit(mouseX, mouseY, this.marketListX, this.marketListY, this.marketListW, this.marketListH)) {
            this.closeAllPopups();
            this.marketScroll.scroll(verticalAmount);
            return true;
        }
        return false;
    }

    private void closeAllPopups() {
        for (ConfigItem item : this.globalConfigs) {
            item.closePopup();
        }
        for (ConfigItem item : this.marketConfigs) {
            item.closePopup();
        }
    }

    private void createGlobalConfig() {
        String name = this.globalNameField.getText().trim();
        if (name.isEmpty()) {
            this.globalRenameFrom = null;
            return;
        }
        if (this.globalRenameFrom != null) {
            this.renameGlobalConfig(this.globalRenameFrom, name);
            this.globalRenameFrom = null;
            this.globalNameField.clear();
            return;
        }
        ConfigManager manager = BKClientInit.getInstance().getConfigManager();
        if (manager == null) {
            return;
        }
        if (!manager.createConfig(name)) {
            this.notifyError("Config", "Nie udalo sie zapisac configu.");
            return;
        }
        this.globalNameField.clear();
        this.invalidateGlobalConfigs();
    }

    private void importGlobalConfig() {
        String input=this.globalNameField.getText().trim();
        if(input.isEmpty())return;
        dev.bkclient.ui.clicksys.port.data.ConfigSync.getInstance().onConsumerString2(result->{
            if(result instanceof dev.bkclient.ui.clicksys.port.data.ConfigSync.Imported){this.globalNameField.clear();this.invalidateGlobalConfigs();}
        },input);
    }
    private void createMarketConfig() {
        AutoMarket module = this.autoMarket();
        String name = this.marketNameField.getText().trim();
        if (module == null || name.isEmpty()) {
            this.marketRenameFrom = null;
            return;
        }
        if (this.marketRenameFrom != null) {
            this.renameMarketConfig(this.marketRenameFrom, name);
            this.marketRenameFrom = null;
            this.marketNameField.clear();
            return;
        }
        String finalName = AutoMarketConfigManager.sanitizeProfileName(name);
        module.configName.setText(finalName);
        module.setActiveProfile(finalName);
        module.getConfigManager().save(finalName);
        module.getConfigManager().refresh();
        this.marketNameField.clear();
        this.invalidateMarketConfigs();
    }

    private void importMarketConfig() {
        AutoMarket module = this.autoMarket();
        String input = this.marketNameField.getText().trim();
        if (module == null || input.isEmpty()) {
            return;
        }
        module.getConfigManager().importFromCloud(input);
        this.marketNameField.clear();
        this.invalidateMarketConfigs();
        this.invalidateMarketConfigsLater(1500L);
        this.invalidateMarketConfigsLater(4000L);
    }

    private boolean anyFieldFocused() {
        return this.globalNameField.isFocused() || this.marketNameField.isFocused();
    }

    private boolean hit(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private AutoMarket autoMarket() {
        try {
            return BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void notify(String title, String message) {
        BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS, title, message);
    }

    private void notifyError(String title, String message) {
        BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.ERROR, title, message);
    }
}
