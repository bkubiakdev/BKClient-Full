/*
 * AutoMarketContent - AutoMarket tab for CsGui.
 * The layout stays in the existing three-panel design, but the controls are bound
 * to the real Auto-Market registry/settings.
 */
package dev.bkclient.ui.menu.csgui.content;

import static dev.bkclient.systems.localization.Localizator.translate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.ui.clicksys.AutoMarketControls;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.inventory.ItemStackExtractor;
import dev.bkclient.utility.render.ScissorUtility;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class AutoMarketContent implements IMinecraft {

    private final boolean clickSysStyle;
    private final AutoMarketControls controls;

    public AutoMarketContent() { this(false); }
    public AutoMarketContent(boolean clickSysStyle) { this.clickSysStyle = clickSysStyle; this.controls = clickSysStyle ? new AutoMarketControls() : null; }

    private ColorRGBA accent() {
        return this.clickSysStyle && ClickGuiV2Theme.active() ? ClickGuiV2Theme.current().accent : CsTheme.Color.accent();
    }

    public void deactivate() {
        this.enchantSelectors.deactivate();
        this.handleRelease();
        this.unfocusAllTextFields();
        this.unfocusAllSliders();
    }

    private static final float PANEL_RADIUS = 8.0f;
    private static final float BORDER_THICKNESS = 0.05f;
    private static final float PANEL_GAP = 8.0f;
    private static final float PAD = 8.0f;

    private static final ColorRGBA PANEL_TINT = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.01f);
    private static final ColorRGBA PANEL_FILL = new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.6f);
    private static final ColorRGBA PANEL_BORDER = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.08f);
    private static final ColorRGBA PANEL_GLASS = new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.5f);

    private ColorRGBA WHITE_75 = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.75f);
    private ColorRGBA WHITE_50 = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.5f);
    private ColorRGBA WHITE_25 = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);
    private ColorRGBA SUB_FILL = new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.6f);
    private ColorRGBA SUB_BORDER = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.05f);
    private static final ColorRGBA GREEN = new ColorRGBA(76.0f, 217.0f, 100.0f, 255.0f);

    private static final String[] ALL_ENCHANTS = {
            "protection", "fire_protection", "feather_falling", "blast_protection", "projectile_protection",
            "respiration", "aqua_affinity", "thorns", "depth_strider", "frost_walker", "binding_curse",
            "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting",
            "sweeping", "efficiency", "silk_touch", "unbreaking", "fortune", "power", "punch",
            "flame", "infinity", "luck_of_the_sea", "lure", "loyalty", "impaling", "riptide",
            "channeling", "multishot", "piercing", "quick_charge", "mending", "vanishing_curse",
            "density", "breach", "wind_burst"
    };

    private final TextField marketSearch = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField enchantSearch = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField bannedSearch = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField nameField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField materialField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField loreField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField bannedLoreField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField maxBuyField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField minBuyField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField sellPriceField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField minSellField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField marketCommandField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField customTitleField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField sellCommandField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField loreRegexField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField sellerRegexField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField confirmItemField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField sortingKeywordField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField passwordField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final TextField loginCommandField = new TextField(Fonts.MEDIUM.getFont(6.5f));
    private final ScrollHandler marketScroll = new ScrollHandler();
    private final ScrollHandler enchantScroll = new ScrollHandler();
    private final ScrollHandler bannedScroll = new ScrollHandler();
    private final ScrollHandler editScroll = new ScrollHandler();
    private final ScrollHandler settingsScroll = new ScrollHandler();

    private final dev.bkclient.ui.menu.csgui.widget.CsSlider refreshSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Refresh Rate", 10, 1000, 77,
                    v -> Math.round(v) + "ms");
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider confirmSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Confirm Delay", 0, 1000, 173,
                    v -> Math.round(v) + "ms");
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider restartSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Restart Delay", 0, 5000, 1500,
                    v -> Math.round(v) + "ms");
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider sellIntervalSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Sell Interval", 0.1, 5, 1,
                    v -> String.format(Locale.ROOT, "%.1fs", v));
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider sellTaxSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Sell Tax", 0, 25, 2,
                    v -> String.format(Locale.ROOT, "%.1f%%", v));
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider reconnectDelaySlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Reconnect Delay", 0, 30, 5,
                    v -> Math.round(v) + "s");
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider confirmSlotSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Confirm Slot", 0, 53, 10,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider sortingSlotSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Sorting Slot", 0, 53, 53,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider refreshSlotSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Refresh Slot", 0, 53, 51,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider nextPageSlotSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Next Page Slot", 0, 53, 53,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider minestarNextSlotSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("MineStar Next", 0, 53, 53,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider minestarPrevSlotSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("MineStar Prev", 0, 53, 52,
                    v -> String.valueOf(Math.round(v)));

    // Min Days Left slider — tylko dla itemów z datą wygaśnięcia (zwierzątka/kostiumy). 0 = off.
    private final dev.bkclient.ui.menu.csgui.widget.CsSlider minDaysSlider =
            new dev.bkclient.ui.menu.csgui.widget.CsSlider("Min Days Left", 0, 60, 0,
                    v -> Math.round(v) <= 0 ? "off" : String.valueOf(Math.round(v)));
    private PriceEntry minDaysSliderEntry;

    private final List<EntryHit> entryHits = new ArrayList<>();
    private final List<ActionHit> actionHits = new ArrayList<>();
    private PriceEntry selectedEntry;
    private PriceEntry syncedEditorEntry;
    private String syncedEditorNameText = "";
    private String syncedEditorLoreText = "";
    private AutoMarket syncedSettingsModule;
    private boolean editMode;
    private final dev.bkclient.ui.clicksys.AutoMarketEnchantSelectors enchantSelectors = new dev.bkclient.ui.clicksys.AutoMarketEnchantSelectors();
    private float marketListX, marketListY, marketListW, marketListH;
    private float enchantListX, enchantListY, enchantListW, enchantListH;
    private float bannedListX, bannedListY, bannedListW, bannedListH;
    private float editPanelX, editPanelY, editPanelW, editPanelH;
    private float settingsPanelX, settingsPanelY, settingsPanelW, settingsPanelH;

    public void render(UIContext context, float x, float y, float w, float h, float alpha) {
        this.enchantSelectors.beginFrame();
        if (this.clickSysStyle && ClickGuiV2Theme.active()) {
            var theme = ClickGuiV2Theme.current();
            this.WHITE_75 = theme.text;
            this.WHITE_50 = theme.text;
            this.WHITE_25 = theme.text;
            this.SUB_FILL = theme.field;
            this.SUB_BORDER = theme.border;
        }
        this.releaseSlidersWhenMouseIsUp();
        AutoMarket module = this.module();
        this.entryHits.clear();
        this.actionHits.clear();
        this.ensureSelected(module);

        float panelW = (w - PANEL_GAP * 4.0f) / 3.0f;
        float panelH = h - PANEL_GAP * 2.0f;
        float py = y + PANEL_GAP;

        float p0 = x + PANEL_GAP;
        float p1 = p0 + panelW + PANEL_GAP;
        float p2 = p1 + panelW + PANEL_GAP;

        this.panel(context, p0, py, panelW, panelH, alpha);
        this.panel(context, p1, py, panelW, panelH, alpha);
        this.panel(context, p2, py, panelW, panelH, alpha);

        this.renderMarket(context, module, p0, py, panelW, panelH, alpha);
        this.renderAutoActions(context, module, p1, py, panelW, panelH, alpha);
        this.renderSettings(context, module, p2, py, panelW, panelH, alpha);
    }

    private void renderMarket(UIContext context, AutoMarket module, float x, float y, float w, float h, float alpha) {
        float cx = x + PAD;
        float cw = w - PAD * 2.0f;
        List<PriceEntry> entries = this.filteredEntries(module);
        float cy = this.header(context, x, y, w, '!', translate("Market") + " (" + entries.size() + ")", alpha, 11.0f, 9.0f);

        this.divider(context, cx, cy, cw, alpha);
        cy += 7.0f;

        this.searchField(context, this.marketSearch, cx, cy, cw, alpha);
        cy += 18.0f;

        this.marketListX = cx - 3.0f;
        this.marketListY = cy - 3.0f;
        this.marketListW = cw + 6.0f;
        this.marketListH = Math.max(42.0f, y + h - PAD - cy);
        this.renderMarketList(context, entries, cx, cy, cw, this.marketListH, alpha);
    }

    private void renderMarketList(UIContext context, List<PriceEntry> entries, float x, float y, float w, float h,
            float alpha) {
        float rowH = 14.0f;
        float detailsH = 92.0f;
        float scrollY = (float) this.marketScroll.getValue();
        ScissorUtility.push(context.getMatrices(), this.marketListX, this.marketListY, this.marketListW, h + 3.0f);
        if (entries.isEmpty()) {
            Font empty = Fonts.MEDIUM.getFont(6.5f);
            context.drawText(empty, translate("No items configured"), x + 2.0f, y + 1.0f,
                    WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
        }
        float contentH = 0.0f;
        for (PriceEntry entry : entries) {
            float rowY = y + contentH - scrollY;
            if (rowY + rowH >= y && rowY <= y + h) {
                this.marketRow(context, x, rowY, w, entry, alpha);
            }
            contentH += rowH;

            if (entry == this.selectedEntry) {
                float detailsY = y + contentH - scrollY;
                if (detailsY + detailsH >= y && detailsY <= y + h) {
                    this.marketDetails(context, entry, x, detailsY, w, detailsH, alpha);
                }
                contentH += detailsH + 5.0f;
            }
        }
        ScissorUtility.pop();
        this.marketScroll.setMax(Math.min(0.0, h - contentH));
        this.marketScroll.update();
    }

    private void marketDetails(UIContext context, PriceEntry entry, float x, float y, float w, float h, float alpha) {
        if (this.clickSysStyle) {
            context.draw();
            dev.bkclient.ui.clicksys.ClickSysMaterial.panel(AutoMarketControls.matrix(context),
                x * 2, y * 2, w * 2, h * 2, 12,
                dev.bkclient.ui.clicksys.port.api.Theme.background(),
                dev.bkclient.ui.clicksys.port.api.Theme.border(), alpha);
            float inset = 7, innerWidth = w - inset * 2;
            this.pricesBlock(context, entry, x + inset, y + 7, innerWidth, alpha);
            this.divider(context, x + inset, y + 39, innerWidth, alpha);
            this.infoBlock(context, x + inset, y + 46, innerWidth, "LIMIT", this.limitText(entry), alpha);
            this.divider(context, x + inset, y + 66, innerWidth, alpha);
            this.infoBlock(context, x + inset, y + 73, innerWidth, "ENCHANTMENTS", this.enchantText(entry), alpha);
            return;
        }
        BorderRadius cr = BorderRadius.all(5.0f);
        context.drawRoundedRect(x - 2.5f, y, w + 5.0f, h, cr,
                SUB_FILL.withAlpha(SUB_FILL.getAlpha() * alpha));

        float py = y + 6.0f;
        this.pricesBlock(context, entry, x + 2.0f, py, w, alpha);
        this.divider(context, x, py + 31.0f, w, alpha);
        this.infoBlock(context, x + 2.0f, py + 41.0f, w, "LIMIT", this.limitText(entry), alpha);
        this.divider(context, x, py + 63.0f, w, alpha);
        this.infoBlock(context, x + 2.0f, py + 73.0f, w, "ENCHANTMENTS", this.enchantText(entry), alpha);
    }

    private void marketRow(UIContext context, float x, float y, float w, PriceEntry entry, float alpha) {
        Font font = Fonts.SEMIBOLD.getFont(6.5f);
        float centerY = y + 5.0f;
        boolean selected = entry == this.selectedEntry;

        if (selected) {
            context.drawRoundedRect(x - 3.0f, y - 1.0f, w + 6.0f, 12.0f, BorderRadius.all(4.0f),
                    accent().withAlpha(255.0f * 0.08f * alpha));
        }

        ItemStack icon = this.iconStack(entry);
        if (icon != null && !icon.isEmpty()) {
            context.drawItem(icon, x - 1.5f, centerY - 7.0f, 0.75f);
        }

        String name = this.trim(this.displayName(entry), font, w - 57.0f);
        context.drawText(font, name, x + 13.0f, centerY - font.height() / 2.0f,
                WHITE_75.withAlpha(255.0f * (this.clickSysStyle ? 1.0f : 0.75f) * alpha));

        float tx = x + w - 38.0f;
        this.miniToggle(context, entry, tx, centerY, entry != null && entry.isEnabled(), alpha);
        if (this.clickSysStyle) {
            this.controls.actions(context, entry, x + w - 22, centerY, alpha);
        } else {
        context.drawIcon(Fonts.ICONS, 'L', x + w - 22.0f, centerY - 5.0f, 9.5f,
                WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
        context.drawIcon(Fonts.ICONS, '5', x + w - 10.0f, centerY - 5.7f, 10.5f,
                WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));

        }

        this.entryHits.add(new EntryHit(entry, x - 3.0f, y - 1.0f, w + 6.0f, 12.0f,
                tx - 3.0f, centerY - 6.0f, 18.0f, 12.0f,
                x + w - 24.0f, centerY - 7.0f, 12.0f, 12.0f,
                x + w - 11.0f, centerY - 7.0f, 12.0f, 12.0f));
    }

    private void pricesBlock(UIContext context, PriceEntry entry, float x, float y, float w, float alpha) {
        if (this.clickSysStyle) {
            Font heading = Fonts.SEMIBOLD.getFont(6.0f);
            context.drawText(heading, translate("PRICES"), x, y, WHITE_25.withAlpha(140 * alpha));
            float gap = 8, columnWidth = (w - gap) / 2;
            this.marketPrice(context, "Buy", entry != null ? entry.getMaxPrice() : 0,
                CategoryType.MARKET_BUY, x, y + 12, columnWidth, alpha);
            this.marketPrice(context, "Sell", entry != null ? entry.getSellPrice() : 0,
                CategoryType.MARKET_SELL, x + columnWidth + gap, y + 12, columnWidth, alpha);
            return;
        }
        Font small = Fonts.SEMIBOLD.getFont(6.5f);
        Font lbl = Fonts.MEDIUM.getFont(7.0f);
        Font amt = Fonts.SEMIBOLD.getFont(6.5f);
        context.drawText(small, translate("PRICES"), x, y, WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));

        float rowY = y + 11.0f;
        float iconSz = 10.0f;
        float iconY = rowY + 1.0f;
        context.drawIcon(Fonts.ICONS, '6', x - 1, iconY - 1.5f, iconSz + 3,
                ColorRGBA.GREEN.withAlpha(255.0f * alpha));

        float bx = x + iconSz + 4.0f;
        context.drawText(lbl, translate("Buy"), bx, rowY, WHITE_75.withAlpha(255.0f * (this.clickSysStyle ? 1.0f : 0.75f) * alpha));
        context.drawText(amt, this.price(entry != null ? entry.getMaxPrice() : 0.0), bx, rowY + 7.0f,
                GREEN.withAlpha(255.0f * alpha));

        float sx = x + w * 0.52f;
        context.drawIcon(Fonts.ICONS, '7', sx - 1.5f, iconY - 1.5f, iconSz + 4,
                accent().withAlpha(255.0f * alpha));

        float sbx = sx + iconSz + 4.0f;
        context.drawText(lbl, translate("Sold"), sbx, rowY, WHITE_75.withAlpha(255.0f * (this.clickSysStyle ? 1.0f : 0.75f) * alpha));
        context.drawText(amt, this.price(entry != null ? entry.getSellPrice() : 0.0), sbx, rowY + 7.0f,
                accent().withAlpha(255.0f * alpha));
    }

    private void marketPrice(UIContext context, String label, double value, CategoryType icon,
            float x, float y, float width, float alpha) {
        AutoMarketControls.icon(context, icon, x, y + 2, 11,
            dev.bkclient.ui.clicksys.port.api.Theme.foreground(), alpha * .8f);
        Font labelFont = Fonts.MEDIUM.getFont(6.5f);
        Font valueFont = Fonts.SEMIBOLD.getFont(6.5f);
        float textX = x + 15;
        context.drawText(labelFont, translate(label), textX, y, WHITE_50.withAlpha(170 * alpha));
        context.drawText(valueFont, this.trim(this.price(value), valueFont, Math.max(0, width - 15)),
            textX, y + 9, WHITE_75.withAlpha(255 * alpha));
    }

    private void infoBlock(UIContext context, float x, float y, float w, String title, String value, float alpha) {
        Font small = Fonts.SEMIBOLD.getFont(6.0f);
        Font val = Fonts.SEMIBOLD.getFont(6.5f);
        context.drawText(small, translate(title), x, y, WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
        context.drawText(val, this.trim(value, val, w - 4.0f), x, y + 8.0f,
                WHITE_50.withAlpha(255.0f * (this.clickSysStyle ? 0.7f : 0.5f) * alpha));
    }

    private void renderAutoActions(UIContext context, AutoMarket module, float x, float y, float w, float h,
            float alpha) {
        float cx = x + PAD;
        float cw = w - PAD * 2.0f;
        float cy = this.header(context, x, y, w, '8', this.editMode ? "Edit Item" : "Auto Actions",
                alpha, 11.0f, 9.0f);
        PriceEntry entry = this.selectedEntry;

        this.divider(context, cx, cy, cw, alpha);
        cy += 6.0f;

        if (entry == null) {
            Font empty = Fonts.MEDIUM.getFont(7.0f);
            context.drawText(empty, translate("Select an item"), cx + 2.0f, cy,
                    WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
            return;
        }

        if (this.editMode) {
            this.renderEditMode(context, module, entry, cx, cy, cw, y + h - PAD - cy, alpha);
            return;
        }

        this.toggleRow(context, cx, cy, cw, "Unit Mode", "(Price per item)", entry.isUnitMode(), alpha);
        this.addHit(cx, cy - 2.0f, cw, 12.0f, () -> {
            if (this.selectedEntry != null && module != null) {
                this.selectedEntry.setUnitMode(!this.selectedEntry.isUnitMode());
                module.itemRegistry.notifyChanged();
            }
        });
        cy += 12.0f;

        this.toggleRow(context, cx, cy, cw, "Buying Mode", null, entry.isBuyEnabled(), alpha);
        this.addHit(cx, cy - 2.0f, cw, 12.0f, () -> {
            if (this.selectedEntry != null && module != null) {
                this.selectedEntry.setBuyEnabled(!this.selectedEntry.isBuyEnabled());
                module.itemRegistry.notifyChanged();
            }
        });
        cy += 12.0f;

        this.toggleRow(context, cx, cy, cw, "Selling Mode", null, entry.isSellEnabled(), alpha);
        this.addHit(cx, cy - 2.0f, cw, 12.0f, () -> {
            if (this.selectedEntry != null && module != null) {
                this.selectedEntry.setSellEnabled(!this.selectedEntry.isSellEnabled());
                module.itemRegistry.notifyChanged();
            }
        });
        cy += 14.0f;

        this.divider(context, cx, cy, cw, alpha);
        cy += 6.0f;

        if (this.clickSysStyle) {
            this.enchantSelectors.render(context, entry, ALL_ENCHANTS, module.itemRegistry::notifyChanged,
                cx, cy, cw, cy, y + h - PAD - cy, alpha, true);
            return;
        }
        this.sectionLabel(context, cx, cy, cw, "Select Enchantments", entry.getEnchantments().size() + " / 40", alpha);
        cy += 10.0f;
        this.searchField(context, this.enchantSearch, cx, cy, cw, alpha);
        cy += 14.0f;
        this.enchantListX = cx - 2.5f;
        this.enchantListY = cy;
        this.enchantListW = cw + 5.0f;
        this.enchantListH = this.editMode ? 42.0f : 56.0f;
        this.enchantList(context, module, entry, this.enchantSearch.getText(), false, this.enchantScroll,
                this.enchantListX, this.enchantListY, this.enchantListW, this.enchantListH, alpha);
        cy += this.enchantListH + 7.0f;

        this.sectionLabel(context, cx, cy, cw, "Banned Enchants", entry.getBannedEnchantsList().size() + " / 40", alpha);
        cy += 10.0f;
        this.searchField(context, this.bannedSearch, cx, cy, cw, alpha);
        cy += 14.0f;
        this.bannedListX = cx - 2.5f;
        this.bannedListY = cy;
        this.bannedListW = cw + 5.0f;
        this.bannedListH = Math.max(0.0f, y + h - PAD - cy);
        if (this.bannedListH > 8.0f) {
            this.enchantList(context, module, entry, this.bannedSearch.getText(), true, this.bannedScroll,
                    this.bannedListX, this.bannedListY, this.bannedListW, this.bannedListH, alpha);
        }
    }

    private void renderEditMode(UIContext context, AutoMarket module, PriceEntry entry, float x, float y, float w,
            float h, float alpha) {
        this.editPanelX = x;
        this.editPanelY = y;
        this.editPanelW = w;
        this.editPanelH = h;
        this.syncEditorFields(entry);

        float scrollY = (float) this.editScroll.getValue();
        float contentY = y + 3.0f - scrollY;
        float cy = contentY;
        ScissorUtility.push(context.getMatrices(), x - 3.0f, y - 3.0f, w + 6.0f, h + 3.0f);

        cy = this.editorInfo(context, entry, x, cy, w, alpha);
        cy = this.editorField(context, this.nameField, "Item Name", x, cy, w, alpha);
        cy = this.editorField(context, this.materialField, "Material (ID)", x, cy, w, alpha);
        cy = this.editorField(context, this.loreField, "Required Lore (Regex/Text)", x, cy, w, alpha);
        cy = this.editorField(context, this.bannedLoreField, "Banned lore (buying)", x, cy, w, alpha);

        float colW = (w - 6.0f) / 2.0f;
        cy = this.editorFieldPair(context, this.maxBuyField, "Max Buy Price", this.minBuyField, "Min Buy Count",
                x, cy, colW, alpha);
        cy = this.editorFieldPair(context, this.sellPriceField, "Sell Price", this.minSellField, "Min Sell Count",
                x, cy, colW, alpha);

        // Min Days Left — TYLKO dla itemów z datą wygaśnięcia (zwierzątka/kostiumy na Minestar,
        // pet/kostium na Anarchii). Filtruje pety wygasające za szybko (entry.minDaysLeft).
        if (this.isExpiringItem(entry)) {
            this.minDaysSliderEntry = entry;
            if (!this.minDaysSlider.isDragging()) {
                this.minDaysSlider.setValue(entry.getMinDaysLeft() != null ? entry.getMinDaysLeft() : 0);
            }
            cy = this.minDaysSlider.render(context, x + 6.0f, cy + 2.0f, w - 12.0f, alpha) + 5.0f;
        }

        // Ignore CMD — TYLKO gdy item ma CustomModelData. Kostiumy/pety mają CMD różny per egzemplarz,
        // który blokowałby match — ten toggle pomija sprawdzanie CMD dla tego itemu.
        if (entry.getCustomModelData() != null || this.isExpiringItem(entry)) {
            this.toggleRow(context, x, cy, w, "Ignore CMD", null,
                    entry.isIgnoreCustomModelData(), alpha);
            this.addHit(x, cy - 2.0f, w, 12.0f, () -> {
                if (this.selectedEntry != null && module != null) {
                    this.selectedEntry.setIgnoreCustomModelData(!this.selectedEntry.isIgnoreCustomModelData());
                    module.itemRegistry.notifyChanged();
                }
            });
            cy += 14.0f;
        }

        this.toggleRow(context, x, cy, w, "Unit Mode", "(Price per item)", entry.isUnitMode(), alpha);
        this.addHit(x, cy - 2.0f, w, 12.0f, () -> {
            if (this.selectedEntry != null && module != null) {
                this.selectedEntry.setUnitMode(!this.selectedEntry.isUnitMode());
                module.itemRegistry.notifyChanged();
            }
        });
        cy += 12.0f;
        this.toggleRow(context, x, cy, w, "Buying Mode", null, entry.isBuyEnabled(), alpha);
        this.addHit(x, cy - 2.0f, w, 12.0f, () -> {
            if (this.selectedEntry != null && module != null) {
                this.selectedEntry.setBuyEnabled(!this.selectedEntry.isBuyEnabled());
                module.itemRegistry.notifyChanged();
            }
        });
        cy += 12.0f;
        this.toggleRow(context, x, cy, w, "Selling Mode", null, entry.isSellEnabled(), alpha);
        this.addHit(x, cy - 2.0f, w, 12.0f, () -> {
            if (this.selectedEntry != null && module != null) {
                this.selectedEntry.setSellEnabled(!this.selectedEntry.isSellEnabled());
                module.itemRegistry.notifyChanged();
            }
        });
        cy += 14.0f;

        this.divider(context, x, cy, w, alpha);
        cy += 6.0f;
        if (this.clickSysStyle) {
            cy += this.enchantSelectors.render(context, entry, ALL_ENCHANTS, module.itemRegistry::notifyChanged,
                x, cy, w, y, h, alpha, false) + 4;
        } else {
            this.sectionLabel(context, x, cy, w, "Select Enchantments", entry.getEnchantments().size() + " / 40", alpha);
            cy += 10.0f;
            this.searchField(context, this.enchantSearch, x, cy, w, alpha);
            cy += 14.0f;
            this.enchantListX = x - 2.5f;
            this.enchantListY = cy;
            this.enchantListW = w + 5.0f;
            this.enchantListH = 50.0f;
            this.enchantList(context, module, entry, this.enchantSearch.getText(), false, this.enchantScroll,
                    x - 2.5f, cy, w + 5.0f, this.enchantListH, alpha);
            cy += this.enchantListH + 7.0f;

            this.sectionLabel(context, x, cy, w, "Banned Enchants", entry.getBannedEnchantsList().size() + " / 40", alpha);
            cy += 10.0f;
            this.searchField(context, this.bannedSearch, x, cy, w, alpha);
            cy += 14.0f;
            this.bannedListX = x - 2.5f;
            this.bannedListY = cy;
            this.bannedListW = w + 5.0f;
            this.bannedListH = 50.0f;
            this.enchantList(context, module, entry, this.bannedSearch.getText(), true, this.bannedScroll,
                    x - 2.5f, cy, w + 5.0f, this.bannedListH, alpha);
            cy += this.bannedListH + 4.0f;

        }

        this.divider(context, x, cy, w, alpha);
        cy += 6.0f;

        ScissorUtility.pop();
        this.editScroll.setMax(Math.min(0.0, h - (cy - contentY)));
        this.editScroll.update();
        this.commitEditorFields(module, entry);
    }

    private float editorInfo(UIContext context, PriceEntry entry, float x, float y, float w, float alpha) {
        ItemStack icon = this.iconStack(entry);
        if (icon != null && !icon.isEmpty()) {
            context.drawItem(icon, x - 1.5f, y - 2.0f, 0.75f);
        }
        Font nameFont = Fonts.SEMIBOLD.getFont(6.5f);
        context.drawText(nameFont, this.trim(this.displayName(entry), nameFont, w - 16.0f), x + 13.0f, y + 1.0f,
                WHITE_75.withAlpha(255.0f * (this.clickSysStyle ? 1.0f : 0.75f) * alpha));
        y += 14.0f;

        Font label = Fonts.MEDIUM.getFont(6.0f);
        Font value = Fonts.SEMIBOLD.getFont(6.2f);
        context.drawText(label, translate("Material"), x, y, WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
        context.drawText(value, this.trim(entry.getMaterial() != null ? entry.getMaterial() : "air", value, w * 0.44f),
                x, y + 7.0f, WHITE_50.withAlpha(255.0f * (this.clickSysStyle ? 0.7f : 0.5f) * alpha));
        context.drawText(label, translate("Lore"), x + w * 0.5f, y, WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
        String lore = entry.getLore() == null || entry.getLore().isBlank()
                ? "No lore"
                : ItemStackExtractor.toReadablePlain(entry.getLore());
        context.drawText(value, this.trim(lore, value, w * 0.45f), x + w * 0.5f, y + 7.0f,
                WHITE_50.withAlpha(255.0f * (this.clickSysStyle ? 0.7f : 0.5f) * alpha));
        y += 19.0f;
        this.divider(context, x, y, w, alpha);
        return y + 5.0f;
    }

    private float editorField(UIContext context, TextField field, String label, float x, float y, float w, float alpha) {
        Font labelFont = Fonts.MEDIUM.getFont(6.0f);
        context.drawText(labelFont, translate(label), x + 1.0f, y, WHITE_50.withAlpha(255.0f * (this.clickSysStyle ? 0.7f : 0.5f) * alpha));
        y += 8.0f;
        this.textInput(context, field, x, y, w, alpha);
        return y + 15.0f;
    }

    private float editorFieldPair(UIContext context, TextField left, String leftLabel, TextField right,
            String rightLabel, float x, float y, float colW, float alpha) {
        Font labelFont = Fonts.MEDIUM.getFont(6.0f);
        context.drawText(labelFont, translate(leftLabel), x + 1.0f, y, WHITE_50.withAlpha(255.0f * (this.clickSysStyle ? 0.7f : 0.5f) * alpha));
        context.drawText(labelFont, translate(rightLabel), x + colW + 7.0f, y, WHITE_50.withAlpha(255.0f * (this.clickSysStyle ? 0.7f : 0.5f) * alpha));
        y += 8.0f;
        this.textInput(context, left, x, y, colW, alpha);
        this.textInput(context, right, x + colW + 6.0f, y, colW, alpha);
        return y + 15.0f;
    }

    private void textInput(UIContext context, TextField field, float x, float y, float w, float alpha) {
        float h = 12.0f;
        if (this.clickSysStyle) {
            this.controls.field(context, field, x - 2.5f, y, w + 4, h, alpha);
        } else {
        context.drawRoundedRect(x - 2.5f, y, w + 4.0f, h, BorderRadius.all(4.0f),
                SUB_FILL.withAlpha(SUB_FILL.getAlpha() * alpha));
        context.drawRoundedBorder(x - 2.5f, y, w + 4.0f, h, 0.05f, BorderRadius.all(4.0f),
                SUB_BORDER.withAlpha(SUB_BORDER.getAlpha() * alpha));
        }
        field.setAlpha(alpha);
        field.setTextColor(WHITE_75);
        field.setPreview(field == this.bannedLoreField ? "Otrucie; Spowolnienie" : "Enter text");
        field.set(x - 0.5f, y, w - 2.0f, h);
        field.render(context);
    }

    private void enchantList(UIContext context, AutoMarket module, PriceEntry entry, String query, boolean banned,
            ScrollHandler scroll, float x, float y, float w, float h, float alpha) {
        context.drawRoundedRect(x, y, w, h, BorderRadius.all(5.0f), SUB_FILL.withAlpha(SUB_FILL.getAlpha() * alpha));
        context.drawRoundedBorder(x, y, w, h, 0.05f, BorderRadius.all(5.0f),
                SUB_BORDER.withAlpha(SUB_BORDER.getAlpha() * alpha));

        List<String> shown = this.filteredEnchants(query);
        float rowH = 7.5f;
        float scrollY = (float) scroll.getValue();
        ScissorUtility.push(context.getMatrices(), x, y, w, h);
        Font ef = Fonts.MEDIUM.getFont(6.5f);
        float ey = y + 5.0f;
        for (int i = 0; i < shown.size(); ++i) {
            String enchant = shown.get(i);
            float rowY = ey + i * rowH - scrollY;
            if (rowY + rowH < y || rowY > y + h) {
                continue;
            }
            boolean selected = banned
                    ? entry.getBannedEnchantsList().contains(enchant)
                    : entry.getEnchantments().containsKey(enchant);
            ColorRGBA color = selected ? accent() : WHITE_50;
            context.drawText(ef, enchant, x + 5.5f, rowY - 1.5f,
                    color.withAlpha((selected ? 255.0f : 255.0f * 0.5f) * alpha));
            final String id = enchant;
            this.addHit(x, rowY - 3.0f, w, rowH, () -> {
                if (banned) {
                    this.toggleBannedEnchant(module, id);
                } else {
                    this.toggleRequiredEnchant(module, id);
                }
            });
        }
        ScissorUtility.pop();

        float contentH = shown.size() * rowH + 8.0f;
        scroll.setMax(Math.min(0.0, h - contentH));
        scroll.update();
        if (contentH > h) {
            float trackH = h - 6.0f;
            float sbW = 1.5f;
            float sbX = x + w - sbW - 4.0f;
            float thumbH = Math.max(12.0f, trackH * h / contentH);
            float maxScroll = Math.max(1.0f, contentH - h);
            float thumbY = y + 3.0f + (trackH - thumbH) * ((float) scroll.getValue() / maxScroll);
            context.drawRoundedRect(sbX, y + 3.0f, sbW, trackH, BorderRadius.all(sbW / 2.0f),
                    (this.clickSysStyle ? SUB_BORDER : new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.06f))
                            .withAlpha(255.0f * 0.06f * alpha));
            context.drawRoundedRect(sbX, thumbY, sbW, thumbH, BorderRadius.all(sbW / 2.0f),
                    WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
        }
    }

    private void renderSettings(UIContext context, AutoMarket module, float x, float y, float w, float h, float alpha) {
        float cx = x + PAD;
        float cw = w - PAD * 2.0f;
        float cy = this.header(context, x, y, w, '5', "Settings", alpha, 11.0f, 9.0f);
        this.settingsPanelX = x;
        this.settingsPanelY = y;
        this.settingsPanelW = w;
        this.settingsPanelH = h;

        this.divider(context, cx, cy, cw, alpha);
        float contentTop = cy + 6.0f;
        float contentBottom = y + h - PAD;
        float contentH = 0.0f;

        boolean showMineStarSlots = module != null && module.minestarMode.isEnabled();
        boolean showLoginFields = module != null && module.nonPremium.isSelected();
        this.syncSettingsFields(module);
        contentH += 12.0f; // anarchia/aggressive
        contentH += 12.0f; // rapy
        contentH += 12.0f; // minestar
        contentH += 12.0f; // auto-reconnect
        contentH += 12.0f; // auto jump
        contentH += 14.0f; // auto repair
        contentH += 1.0f + 8.0f + 9.0f + 22.0f + 8.0f; // divider + price mode
        contentH += 1.0f + 8.0f; // divider
        contentH += 19.0f * 10.0f; // main sliders
        if (showMineStarSlots) {
            contentH += 19.0f * 2.0f;
        }
        contentH += 1.0f + 8.0f; // divider before text fields
        contentH += 20.0f * 7.0f;
        if (showLoginFields) {
            contentH += 20.0f * 2.0f;
        }
        contentH += 28.0f;

        float viewH = Math.max(0.0f, contentBottom - contentTop);
        this.settingsScroll.setMax(Math.min(0.0, viewH - contentH));
        this.settingsScroll.update();
        float scrollY = (float) this.settingsScroll.getValue();
        cy = contentTop - scrollY;

        ScissorUtility.push(context.getMatrices(), x, contentTop - 2.0f, w, viewH + 2.0f);

        boolean aggressive = module != null && module.aggressiveMode.isEnabled();
        this.toggleRow(context, cx, cy, cw, "Anarchia", null, aggressive, alpha);
        this.addHit(cx, cy - 2.0f, cw, 12.0f, () -> {
            if (module != null) module.aggressiveMode.toggle();
        });
        cy += 12.0f;

        boolean rapy = module != null && module.rapyMode.isEnabled();
        this.toggleRow(context, cx, cy, cw, "Rapy", null, rapy, alpha);
        this.addHit(cx, cy - 2.0f, cw, 12.0f, () -> {
            if (module != null) module.rapyMode.toggle();
        });
        cy += 12.0f;

        boolean minestar = module != null && module.minestarMode.isEnabled();
        this.toggleRow(context, cx, cy, cw, "MineStar", null, minestar, alpha);
        this.addHit(cx, cy - 2.0f, cw, 12.0f, () -> {
            if (module != null) module.minestarMode.toggle();
        });
        cy += 12.0f;

        boolean autoReconnect = module != null && module.autoReconnect.isEnabled();
        this.toggleRow(context, cx, cy, cw, "Auto Reconnect", null, autoReconnect, alpha);
        this.addHit(cx, cy - 2.0f, cw, 12.0f, () -> {
            if (module != null) module.autoReconnect.toggle();
        });
        cy += 12.0f;

        boolean autoJump = module != null && module.autoJump.isEnabled();
        this.toggleRow(context, cx, cy, cw, "Auto Jump", null, autoJump, alpha);
        this.addHit(cx, cy - 2.0f, cw, 12.0f, () -> {
            if (module != null) module.autoJump.toggle();
        });
        cy += 12.0f;

        boolean autoRepair = module != null && module.autoRepair.isEnabled();
        this.toggleRow(context, cx, cy, cw, "Auto Repair", null, autoRepair, alpha);
        this.addHit(cx, cy - 2.0f, cw, 12.0f, () -> {
            if (module != null) module.autoRepair.toggle();
        });
        cy += 14.0f;

        this.divider(context, cx, cy, cw, alpha);
        cy += 8.0f;

        if (this.clickSysStyle) {
            boolean unit = module == null || module.unitModeVal.isSelected();
            float selectorY = this.controls.priceMode(context, cx, cy, cw, unit, alpha);
            this.addHit(cx, selectorY, cw / 2, 16, () -> {
                if (module != null) module.unitModeGlobal.setValue(module.unitModeVal);
            });
            this.addHit(cx + cw / 2, selectorY, cw / 2, 16, () -> {
                if (module != null) module.unitModeGlobal.setValue(module.sumModeVal);
            });
            cy += 39;
        } else {
        Font lbl = Fonts.SEMIBOLD.getFont(7.0f);
        context.drawText(lbl, translate("Price Mode"), cx + 2, cy, WHITE_50.withAlpha(255.0f * (this.clickSysStyle ? 0.7f : 0.5f) * alpha));
        cy += 9.0f;
        float pmH = 22.0f;
        context.drawRoundedRect(cx, cy, cw, pmH, BorderRadius.all(5.0f),
                (this.clickSysStyle ? SUB_FILL : new ColorRGBA(10.0f, 10.0f, 10.0f, 255.0f * 0.35f))
                        .withAlpha(255.0f * 0.25f * alpha));
        context.drawRoundedBorder(cx, cy, cw, pmH, 0.05f, BorderRadius.all(5.0f),
                SUB_BORDER.withAlpha(SUB_BORDER.getAlpha() * alpha));
        Font pmf = Fonts.SEMIBOLD.getFont(6.5f);
        boolean unit = module == null || module.unitModeVal.isSelected();
        context.drawText(pmf, translate("Unit"), cx + 5.0f, cy + 5.0f,
                (unit ? accent() : WHITE_50).withAlpha((unit ? 255.0f : 255.0f * 0.5f) * alpha));
        context.drawText(pmf, translate("Sum"), cx + 5.0f, cy + 13.0f,
                (!unit ? accent() : WHITE_50).withAlpha((!unit ? 255.0f : 255.0f * 0.5f) * alpha));
        float priceModeY = cy;
        this.addHit(cx, priceModeY, cw, pmH / 2.0f, () -> {
            if (module != null) module.unitModeGlobal.setValue(module.unitModeVal);
        });
        this.addHit(cx, priceModeY + pmH / 2.0f, cw, pmH / 2.0f, () -> {
            if (module != null) module.unitModeGlobal.setValue(module.sumModeVal);
        });
        cy += pmH + 8.0f;

        }

        this.divider(context, cx, cy, cw, alpha);
        cy += 8.0f;

        if (module != null) {
            this.syncSlider(this.refreshSlider, module.refreshRate);
            this.refreshSlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            this.syncSlider(this.confirmSlider, module.confirmDelay);
            this.confirmSlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            this.syncSlider(this.restartSlider, module.restartDelay);
            this.restartSlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            this.syncSlider(this.sellIntervalSlider, module.sellInterval);
            this.sellIntervalSlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            this.syncSlider(this.sellTaxSlider, module.sellTax);
            this.sellTaxSlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            this.syncSlider(this.reconnectDelaySlider, module.reconnectDelay);
            this.reconnectDelaySlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            this.syncSlider(this.confirmSlotSlider, module.confirmSlotSetting);
            this.confirmSlotSlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            this.syncSlider(this.sortingSlotSlider, module.sortingSlotSetting);
            this.sortingSlotSlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            this.syncSlider(this.refreshSlotSlider, module.refreshSlotSetting);
            this.refreshSlotSlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            this.syncSlider(this.nextPageSlotSlider, module.nextPageSlotSetting);
            this.nextPageSlotSlider.render(context, cx, cy, cw, alpha);

            cy += 19.0f;
            if (module.minestarMode.isEnabled()) {
                this.syncSlider(this.minestarNextSlotSlider, module.minestarNextSlot);
                this.minestarNextSlotSlider.render(context, cx, cy, cw, alpha);

                cy += 19.0f;
                this.syncSlider(this.minestarPrevSlotSlider, module.minestarPrevSlot);
                this.minestarPrevSlotSlider.render(context, cx, cy, cw, alpha);

                cy += 19.0f;
            }

            this.divider(context, cx, cy, cw, alpha);
            cy += 8.0f;

            cy = this.editorField(context, this.marketCommandField, "Market Command", cx, cy, cw, alpha);
            cy = this.editorField(context, this.customTitleField, "Custom Title", cx, cy, cw, alpha);
            cy = this.editorField(context, this.sellCommandField, "Sell Command", cx, cy, cw, alpha);
            cy = this.editorField(context, this.loreRegexField, "Lore Regex", cx, cy, cw, alpha);
            cy = this.editorField(context, this.sellerRegexField, "Seller Regex", cx, cy, cw, alpha);
            cy = this.editorField(context, this.confirmItemField, "Confirm Item", cx, cy, cw, alpha);
            cy = this.editorField(context, this.sortingKeywordField, "Sorting Keyword", cx, cy, cw, alpha);
            if (module.nonPremium.isSelected()) {
                cy = this.editorField(context, this.passwordField, "Login Password", cx, cy, cw, alpha);
                cy = this.editorField(context, this.loginCommandField, "Login Command", cx, cy, cw, alpha);
            }
            this.commitSettingsFields(module);
        }

        ScissorUtility.pop();
    }

    private void divider(UIContext context, float x, float y, float w, float alpha) {
        context.drawRect(x, y, w, 1.0f, (this.clickSysStyle ? SUB_BORDER : new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.06f))
                .withAlpha(255.0f * 0.06f * alpha));
    }

    private float header(UIContext context, float x, float y, float w, char icon, String title, float alpha,
            float iconSize, float fontSize) {
        float hy = y + PAD;
        Font font = Fonts.SEMIBOLD.getFont(fontSize);
        float lineH = Math.max(iconSize, font.height());
        if (this.clickSysStyle) {
            CategoryType type = icon == '!' ? CategoryType.RACK : icon == '5' ? CategoryType.SETTINGS : CategoryType.BOLT;
            AutoMarketControls.icon(context, type, x + PAD, hy + (lineH - iconSize) / 2, iconSize, accent().getRGB(), alpha);
        } else {
        context.drawIcon(Fonts.ICONS, icon, x + PAD, hy + (lineH - iconSize) / 2.0f, iconSize,
                accent().withAlpha(255.0f * alpha));
        }
        context.drawText(font, translate(title), x + PAD + iconSize + 3.0f, hy + (lineH - font.height()) / 2.0f,
                (this.clickSysStyle && ClickGuiV2Theme.active() ? ClickGuiV2Theme.current().text : ColorRGBA.WHITE).withAlpha(255.0f * alpha));
        return hy + lineH + 6.0f;
    }

    private void searchField(UIContext context, TextField field, float x, float y, float w, float alpha) {
        float fh = 12.0f;
        if (this.clickSysStyle) {
            this.controls.field(context, field, x - 2.5f, y, w + 4, fh, alpha);
        } else {
        context.drawRoundedRect(x - 2.5f, y, w + 4f, fh, BorderRadius.all(4.0f),
                SUB_FILL.withAlpha(SUB_FILL.getAlpha() * alpha));
        context.drawRoundedBorder(x - 2.5f, y, w + 4f, fh, 0.05f, BorderRadius.all(4.0f),
                SUB_BORDER.withAlpha(SUB_BORDER.getAlpha() * alpha));
        }
        field.setAlpha(alpha);
        field.setTextColor(WHITE_75);
        field.setPreview(translate("Search..."));
        field.set(x - 0.5f, y, w - 10.0f, fh);
        field.render(context);
        if (this.clickSysStyle) {
            AutoMarketControls.icon(context, CategoryType.SEARCH, x + w - 9, y + 2.5f, 7,
                WHITE_50.mulAlpha(.7f).getRGB(), alpha);
        } else {
        context.drawIcon(Fonts.ICONS, 'v', x + w - 9.0f, y + (fh - 5.5f) / 2.0f - 1.25f, 7.5f,
                WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
        }
    }

    private void toggleRow(UIContext context, float x, float y, float w, String name, String hint, boolean on,
            float alpha) {
        Font font = Fonts.SEMIBOLD.getFont(7.0f);
        float centerY = y + 4.0f;
        if (this.clickSysStyle) {
            AutoMarketControls.icon(context, CategoryType.SETTING_CHECKBOX, x, centerY - 3.5f, 7, WHITE_50.mulAlpha(.7f).getRGB(), alpha);
        }
        context.drawText(font, translate(name), x + (this.clickSysStyle ? 10 : 0), centerY - font.height() / 2.0f,
                WHITE_75.withAlpha(255.0f * (this.clickSysStyle ? 1.0f : 0.75f) * alpha));
        if (hint != null) {
            Font hintFont = Fonts.MEDIUM.getFont(6.5f);
            float hx = x + (this.clickSysStyle ? 10 : 0) + font.width(translate(name)) + 3.0f;
            context.drawText(hintFont, translate(hint), hx, centerY - hintFont.height() / 2.0f,
                    WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
        }
        this.miniToggle(context, name, x + w - 13.0f, centerY, on, alpha);
    }

    private String marginText(double value) {
        return String.format(Locale.US, "%.0f%%", value);
    }

    private void miniToggle(UIContext context, Object key, float x, float centerY, boolean on, float alpha) {
        if (this.clickSysStyle) {
            this.controls.checkbox(context, key, x, centerY, on, alpha);
            return;
        }
        float tw = 12.0f, th = 7.0f, knob = 4.5f;
        float y = centerY - th / 2.0f;
        ColorRGBA bg = on ? accent() : (this.clickSysStyle ? SUB_BORDER : new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.15f));
        context.drawRoundedRect(x, y, tw, th, BorderRadius.all(th / 3.0f), bg.withAlpha(bg.getAlpha() * alpha));
        float t = on ? 1.0f : 0.0f;
        float knobX = x + 1.5f + (tw - knob - 3.0f) * t;
        float knobY = y + (th - knob) / 2.0f;
        ColorRGBA knobColor = on ? ColorRGBA.WHITE : WHITE_25;
        context.drawRoundedRect(knobX, knobY, knob, knob, BorderRadius.all(knob / 2.0f),
                knobColor.withAlpha(knobColor.getAlpha() * alpha));
    }

    private void sectionLabel(UIContext context, float x, float y, float w, String name, String right, float alpha) {
        Font font = Fonts.SEMIBOLD.getFont(7.0f);
        context.drawText(font, translate(name), x, y, WHITE_50.withAlpha(255.0f * (this.clickSysStyle ? 0.7f : 0.5f) * alpha));
        context.drawRightText(font, right, x + w, y, WHITE_25.withAlpha(255.0f * (this.clickSysStyle ? 0.55f : 0.25f) * alpha));
    }

    private void panel(UIContext context, float px, float py, float pw, float ph, float alpha) {
        BorderRadius r = BorderRadius.all(PANEL_RADIUS);
        if (this.clickSysStyle && ClickGuiV2Theme.active()) {
            var theme = ClickGuiV2Theme.current();
            context.drawRoundedRect(px, py, pw, ph, r, theme.surface.mulAlpha(alpha));
            context.drawRoundedBorder(px, py, pw, ph, 0.5f, r, theme.border.mulAlpha(alpha));
            return;
        }
        if (dev.bkclient.systems.modules.modules.visuals.Interface.blurGui())
            context.drawBlurredRect(px, py, pw, ph, 12.0f, 2.0f, r,
                    PANEL_GLASS.withAlpha(PANEL_GLASS.getAlpha() * alpha));
        context.drawRoundedRect(px, py, pw, ph, r, PANEL_TINT.withAlpha(PANEL_TINT.getAlpha() * alpha));
        context.drawRoundedRect(px, py, pw, ph, r, PANEL_FILL.withAlpha(PANEL_FILL.getAlpha() * alpha));
        context.drawRoundedBorder(px, py, pw, ph, BORDER_THICKNESS, r,
                PANEL_BORDER.withAlpha(PANEL_BORDER.getAlpha() * alpha));
    }

    public boolean handleClick(double mouseX, double mouseY) {
        if (this.clickSysStyle && this.enchantSelectors.click(mouseX, mouseY)) {
            this.unfocusAllTextFields();
            this.unfocusAllSliders();
            return true;
        }
        this.unfocusAllTextFields();
        this.unfocusAllSliders();
        this.marketSearch.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.enchantSearch.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.bannedSearch.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.marketCommandField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.customTitleField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.sellCommandField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.loreRegexField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.sellerRegexField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.confirmItemField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.sortingKeywordField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.passwordField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        this.loginCommandField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        if (this.editMode) {
            this.nameField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            this.materialField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            this.loreField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            this.bannedLoreField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            this.maxBuyField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            this.minBuyField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            this.sellPriceField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            this.minSellField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        }
        if (this.anySearchFocused()) {
            return true;
        }

        if (this.handleSliderClick(mouseX, mouseY)) {
            return true;
        }

        for (EntryHit hit : new ArrayList<>(this.entryHits)) {
            if (hit.containsToggle(mouseX, mouseY)) {
                this.toggleEntry(hit.entry);
                this.playMarketClick();
                return true;
            }
            if (hit.containsRemove(mouseX, mouseY)) {
                this.removeEntry(hit.entry);
                this.playMarketClick();
                return true;
            }
            if (hit.containsEdit(mouseX, mouseY)) {
                this.selectedEntry = hit.entry;
                this.editMode = true;
                this.syncedEditorEntry = null;
                this.playMarketClick();
                return true;
            }
            if (hit.containsRow(mouseX, mouseY)) {
                this.playMarketClick();
                if (this.selectedEntry == hit.entry && !this.editMode) {
                    this.selectedEntry = null;
                } else {
                    this.selectedEntry = hit.entry;
                    this.editMode = false;
                }
                return true;
            }
        }

        for (ActionHit hit : new ArrayList<>(this.actionHits)) {
            if (hit.contains(mouseX, mouseY)) {
                hit.action.run();
                this.playMarketClick();
                return true;
            }
        }

        return false;
    }

    private void playMarketClick() {
        if (this.clickSysStyle) dev.bkclient.ui.clicksys.ClickSysSounds.play(
                dev.bkclient.ui.clicksys.port.enums.SoundEvent.CATEGORY_SWITCH);
    }

    public void handleRelease() {
        this.enchantSelectors.release();
        this.marketSearch.onMouseReleased(0, 0, MouseButton.LEFT);
        this.enchantSearch.onMouseReleased(0, 0, MouseButton.LEFT);
        this.bannedSearch.onMouseReleased(0, 0, MouseButton.LEFT);
        this.nameField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.materialField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.loreField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.bannedLoreField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.maxBuyField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.minBuyField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.sellPriceField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.minSellField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.marketCommandField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.customTitleField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.sellCommandField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.loreRegexField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.sellerRegexField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.confirmItemField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.sortingKeywordField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.passwordField.onMouseReleased(0, 0, MouseButton.LEFT);
        this.loginCommandField.onMouseReleased(0, 0, MouseButton.LEFT);
        AutoMarket module = this.module();
        this.releaseSlider(this.refreshSlider, module != null ? module.refreshRate : null);
        this.releaseSlider(this.confirmSlider, module != null ? module.confirmDelay : null);
        this.releaseSlider(this.restartSlider, module != null ? module.restartDelay : null);
        this.releaseSlider(this.sellIntervalSlider, module != null ? module.sellInterval : null);
        this.releaseSlider(this.sellTaxSlider, module != null ? module.sellTax : null);
        this.releaseSlider(this.reconnectDelaySlider, module != null ? module.reconnectDelay : null);
        this.releaseSlider(this.confirmSlotSlider, module != null ? module.confirmSlotSetting : null);
        this.releaseSlider(this.sortingSlotSlider, module != null ? module.sortingSlotSetting : null);
        this.releaseSlider(this.refreshSlotSlider, module != null ? module.refreshSlotSetting : null);
        this.releaseSlider(this.nextPageSlotSlider, module != null ? module.nextPageSlotSetting : null);
        this.releaseSlider(this.minestarNextSlotSlider, module != null ? module.minestarNextSlot : null);
        this.releaseSlider(this.minestarPrevSlotSlider, module != null ? module.minestarPrevSlot : null);
        // Per-item margin slidery: na puszczenie zapisz override TYLKO tego itemu (raz, nie co piksel).
        // Min Days slider: na puszczenie zatrzaśnij na pełnym dniu i notyfikuj registry (zapis configu).
        boolean minDaysWasDragging = this.minDaysSlider.isDragging();
        this.minDaysSlider.onRelease();
        if (minDaysWasDragging && this.minDaysSliderEntry != null) {
            int v = Math.round((float) this.minDaysSlider.getValue());
            this.minDaysSlider.setValue(v);
            this.minDaysSliderEntry.setMinDaysLeft(v <= 0 ? null : v);
            if (module != null) module.itemRegistry.notifyChanged();
        }

    }

    public boolean handleDrag(double mouseX, double mouseY) {
        if (this.clickSysStyle && this.enchantSelectors.drag(mouseX, mouseY)) return true;
        return this.handleSliderDrag(mouseX);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.clickSysStyle && this.enchantSelectors.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (this.handleSliderKey(keyCode)) {
            return true;
        }
        if (!this.anySearchFocused()) {
            return false;
        }
        if (keyCode == 256) {
            this.marketSearch.setFocused(false);
            this.enchantSearch.setFocused(false);
            this.bannedSearch.setFocused(false);
            this.unfocusEditorFields();
            this.unfocusSettingsFields();
            return true;
        }
        this.marketSearch.onKeyPressed(keyCode, scanCode, modifiers);
        this.enchantSearch.onKeyPressed(keyCode, scanCode, modifiers);
        this.bannedSearch.onKeyPressed(keyCode, scanCode, modifiers);
        this.nameField.onKeyPressed(keyCode, scanCode, modifiers);
        this.materialField.onKeyPressed(keyCode, scanCode, modifiers);
        this.loreField.onKeyPressed(keyCode, scanCode, modifiers);
        this.bannedLoreField.onKeyPressed(keyCode, scanCode, modifiers);
        this.maxBuyField.onKeyPressed(keyCode, scanCode, modifiers);
        this.minBuyField.onKeyPressed(keyCode, scanCode, modifiers);
        this.sellPriceField.onKeyPressed(keyCode, scanCode, modifiers);
        this.minSellField.onKeyPressed(keyCode, scanCode, modifiers);
        this.marketCommandField.onKeyPressed(keyCode, scanCode, modifiers);
        this.customTitleField.onKeyPressed(keyCode, scanCode, modifiers);
        this.sellCommandField.onKeyPressed(keyCode, scanCode, modifiers);
        this.loreRegexField.onKeyPressed(keyCode, scanCode, modifiers);
        this.sellerRegexField.onKeyPressed(keyCode, scanCode, modifiers);
        this.confirmItemField.onKeyPressed(keyCode, scanCode, modifiers);
        this.sortingKeywordField.onKeyPressed(keyCode, scanCode, modifiers);
        this.passwordField.onKeyPressed(keyCode, scanCode, modifiers);
        this.loginCommandField.onKeyPressed(keyCode, scanCode, modifiers);
        this.commitEditorFields(this.module(), this.selectedEntry);
        this.commitSettingsFields(this.module());
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.clickSysStyle && this.enchantSelectors.charTyped(chr, modifiers)) return true;
        boolean handled = false;
        handled |= this.marketSearch.charTyped(chr, modifiers);
        handled |= this.enchantSearch.charTyped(chr, modifiers);
        handled |= this.bannedSearch.charTyped(chr, modifiers);
        handled |= this.nameField.charTyped(chr, modifiers);
        handled |= this.materialField.charTyped(chr, modifiers);
        handled |= this.loreField.charTyped(chr, modifiers);
        handled |= this.bannedLoreField.charTyped(chr, modifiers);
        handled |= this.maxBuyField.charTyped(chr, modifiers);
        handled |= this.minBuyField.charTyped(chr, modifiers);
        handled |= this.sellPriceField.charTyped(chr, modifiers);
        handled |= this.minSellField.charTyped(chr, modifiers);
        handled |= this.marketCommandField.charTyped(chr, modifiers);
        handled |= this.customTitleField.charTyped(chr, modifiers);
        handled |= this.sellCommandField.charTyped(chr, modifiers);
        handled |= this.loreRegexField.charTyped(chr, modifiers);
        handled |= this.sellerRegexField.charTyped(chr, modifiers);
        handled |= this.confirmItemField.charTyped(chr, modifiers);
        handled |= this.sortingKeywordField.charTyped(chr, modifiers);
        handled |= this.passwordField.charTyped(chr, modifiers);
        handled |= this.loginCommandField.charTyped(chr, modifiers);
        if (handled) {
            this.commitEditorFields(this.module(), this.selectedEntry);
            this.commitSettingsFields(this.module());
        }
        return handled;
    }

    public boolean onScroll(double mouseX, double mouseY, double verticalAmount) {
        boolean handled = this.scrollContent(mouseX, mouseY, verticalAmount);
        if (handled && verticalAmount != 0 && this.clickSysStyle && !this.isAnySliderDragging())
            dev.bkclient.ui.clicksys.ClickSysSounds.scroll();
        return handled;
    }

    private boolean scrollContent(double mouseX, double mouseY, double verticalAmount) {
        if (this.clickSysStyle && this.enchantSelectors.scroll(mouseX, mouseY, verticalAmount, !this.editMode)) return true;
        if (this.isAnySliderDragging()) {
            return true;
        }
        if (this.editMode && this.hover(mouseX, mouseY, this.editPanelX - 3.0f, this.editPanelY,
                this.editPanelW + 6.0f, this.editPanelH)) {
            if (this.hover(mouseX, mouseY, this.enchantListX, this.enchantListY, this.enchantListW, this.enchantListH)) {
                this.enchantScroll.scroll(verticalAmount);
            } else if (this.hover(mouseX, mouseY, this.bannedListX, this.bannedListY, this.bannedListW, this.bannedListH)) {
                this.bannedScroll.scroll(verticalAmount);
            } else {
                this.editScroll.scroll(verticalAmount);
            }
            return true;
        }
        if (this.hover(mouseX, mouseY, this.enchantListX, this.enchantListY, this.enchantListW, this.enchantListH)) {
            this.enchantScroll.scroll(verticalAmount);
            return true;
        }
        if (this.hover(mouseX, mouseY, this.bannedListX, this.bannedListY, this.bannedListW, this.bannedListH)) {
            this.bannedScroll.scroll(verticalAmount);
            return true;
        }
        if (this.hover(mouseX, mouseY, this.marketListX, this.marketListY, this.marketListW, this.marketListH)) {
            this.marketScroll.scroll(verticalAmount);
            return true;
        }
        if (this.hover(mouseX, mouseY, this.settingsPanelX, this.settingsPanelY,
                this.settingsPanelW, this.settingsPanelH)) {
            this.settingsScroll.scroll(verticalAmount);
            return true;
        }
        return false;
    }

    private boolean anySearchFocused() {
        return this.marketSearch.isFocused() || this.enchantSearch.isFocused() || this.bannedSearch.isFocused()
                || (this.editMode && (this.nameField.isFocused() || this.materialField.isFocused() || this.loreField.isFocused() || this.bannedLoreField.isFocused()
                || this.maxBuyField.isFocused() || this.minBuyField.isFocused()
                || this.sellPriceField.isFocused() || this.minSellField.isFocused()))
                || this.marketCommandField.isFocused() || this.customTitleField.isFocused()
                || this.sellCommandField.isFocused() || this.loreRegexField.isFocused()
                || this.sellerRegexField.isFocused() || this.confirmItemField.isFocused()
                || this.sortingKeywordField.isFocused() || this.passwordField.isFocused()
                || this.loginCommandField.isFocused();
    }

    private AutoMarket module() {
        try {
            return BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void ensureSelected(AutoMarket module) {
        if (module == null) {
            this.selectedEntry = null;
            return;
        }
        List<PriceEntry> entries = module.itemRegistry.getValue();
        if (this.selectedEntry == null || !entries.contains(this.selectedEntry)) {
            this.selectedEntry = null;
            this.editMode = false;
        }
    }

    private void syncEditorFields(PriceEntry entry) {
        if (entry == null || this.syncedEditorEntry == entry) {
            return;
        }
        this.syncedEditorEntry = entry;
        this.editScroll.reset();
        this.enchantScroll.reset();
        this.bannedScroll.reset();
        this.syncedEditorNameText = entry.getName() != null ? ItemStackExtractor.toEditableLegacy(entry.getName()) : "";
        this.nameField.setText(this.syncedEditorNameText);
        this.materialField.setText(entry.getMaterial() != null ? entry.getMaterial() : "");
        this.syncedEditorLoreText = entry.getLore() != null ? ItemStackExtractor.toEditableLore(entry.getLore()) : "";
        this.loreField.setText(this.syncedEditorLoreText);
        this.loreField.scrollToEndAnimated();
        this.bannedLoreField.setText(entry.getBannedLore());
        this.maxBuyField.setText(this.numberText(entry.getMaxPrice()));
        this.minBuyField.setText(String.valueOf(entry.getMinBuyCount()));
        this.sellPriceField.setText(this.numberText(entry.getSellPrice()));
        this.minSellField.setText(String.valueOf(entry.getMinSellCount()));
        this.unfocusEditorFields();
    }

    private void commitEditorFields(AutoMarket module, PriceEntry entry) {
        if (module == null || entry == null || !this.editMode) {
            return;
        }

        boolean changed = false;
        String nameText = this.nameField.getText();
        if (!java.util.Objects.equals(this.syncedEditorNameText, nameText)) {
            String name = ItemStackExtractor.fromEditableLegacy(nameText);
            if (!java.util.Objects.equals(entry.getName(), name)) {
                entry.setName(name);
                changed = true;
            }
            this.syncedEditorNameText = nameText;
        }

        String material = this.materialField.getText();
        if (!java.util.Objects.equals(entry.getMaterial(), material)) {
            entry.setMaterial(material);
            entry.setCachedIconStack(null);
            changed = true;
        }

        String loreText = this.loreField.getText();
        if (!java.util.Objects.equals(this.syncedEditorLoreText, loreText)) {
            String lore = this.normalizeLore(ItemStackExtractor.fromEditableLore(loreText));
            if (!java.util.Objects.equals(entry.getLore(), lore)) {
                entry.setLore(lore);
                changed = true;
            }
            this.syncedEditorLoreText = loreText;
        }

        String bannedLore = this.bannedLoreField.getText();
        if (!java.util.Objects.equals(entry.getBannedLore(), bannedLore)) {
            entry.setBannedLore(bannedLore);
            changed = true;
        }

        Double maxBuy = this.parseDouble(this.maxBuyField.getText());
        if (maxBuy != null && entry.getMaxPrice() != maxBuy) {
            entry.setMaxPrice(maxBuy);
            changed = true;
        }

        Integer minBuy = this.parseInt(this.minBuyField.getText());
        if (minBuy != null && entry.getMinBuyCount() != minBuy) {
            entry.setMinBuyCount(minBuy);
            changed = true;
        }

        Double sellPrice = this.parseDouble(this.sellPriceField.getText());
        if (sellPrice != null && entry.getSellPrice() != sellPrice) {
            entry.setSellPrice(sellPrice);
            changed = true;
        }

        Integer minSell = this.parseInt(this.minSellField.getText());
        if (minSell != null && entry.getMinSellCount() != minSell) {
            entry.setMinSellCount(minSell);
            changed = true;
        }

        if (changed) {
            module.itemRegistry.notifyChanged();
        }
    }

    private void syncSettingsFields(AutoMarket module) {
        if (module == null) {
            this.syncedSettingsModule = null;
            return;
        }
        boolean force = this.syncedSettingsModule != module;
        this.syncSettingField(this.marketCommandField, module.marketCommand, force);
        this.syncSettingField(this.customTitleField, module.customMarketTitle, force);
        this.syncSettingField(this.sellCommandField, module.sellCommand, force);
        this.syncSettingField(this.loreRegexField, module.loreRegex, force);
        this.syncSettingField(this.sellerRegexField, module.loreSellerRegex, force);
        this.syncSettingField(this.confirmItemField, module.confirmItemName, force);
        this.syncSettingField(this.sortingKeywordField, module.sortingKeyword, force);
        this.syncSettingField(this.passwordField, module.password, force);
        this.syncSettingField(this.loginCommandField, module.loginCommand, force);
        this.syncedSettingsModule = module;
    }

    private void syncSettingField(TextField field, StringSetting setting, boolean force) {
        String value = setting.getText() == null ? "" : setting.getText();
        if ((force || !field.isFocused()) && !java.util.Objects.equals(field.getText(), value)) {
            field.setText(value);
        }
    }

    private void commitSettingsFields(AutoMarket module) {
        if (module == null) {
            return;
        }
        this.commitSettingField(this.marketCommandField, module.marketCommand);
        this.commitSettingField(this.customTitleField, module.customMarketTitle);
        this.commitSettingField(this.sellCommandField, module.sellCommand);
        this.commitSettingField(this.loreRegexField, module.loreRegex);
        this.commitSettingField(this.sellerRegexField, module.loreSellerRegex);
        this.commitSettingField(this.confirmItemField, module.confirmItemName);
        this.commitSettingField(this.sortingKeywordField, module.sortingKeyword);
        this.commitSettingField(this.passwordField, module.password);
        this.commitSettingField(this.loginCommandField, module.loginCommand);
    }

    private void commitSettingField(TextField field, StringSetting setting) {
        if (!java.util.Objects.equals(setting.getText(), field.getText())) {
            setting.setText(field.getText());
        }
    }

    private String normalizeLore(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String part : text.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return String.join(";", parts);
    }

    private Double parseDouble(String text) {
        if (text == null || text.isBlank() || text.equals("-") || text.equals(".")) {
            return null;
        }
        try {
            return Double.parseDouble(text.replace(",", "."));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseInt(String text) {
        Double value = this.parseDouble(text);
        if (value == null) {
            return null;
        }
        return Math.max(0, value.intValue());
    }

    private String numberText(double value) {
        if (Math.floor(value) == value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private void unfocusEditorFields() {
        this.nameField.setFocused(false);
        this.materialField.setFocused(false);
        this.loreField.setFocused(false);
        this.bannedLoreField.setFocused(false);
        this.maxBuyField.setFocused(false);
        this.minBuyField.setFocused(false);
        this.sellPriceField.setFocused(false);
        this.minSellField.setFocused(false);
    }

    private void unfocusSettingsFields() {
        this.marketCommandField.setFocused(false);
        this.customTitleField.setFocused(false);
        this.sellCommandField.setFocused(false);
        this.loreRegexField.setFocused(false);
        this.sellerRegexField.setFocused(false);
        this.confirmItemField.setFocused(false);
        this.sortingKeywordField.setFocused(false);
        this.passwordField.setFocused(false);
        this.loginCommandField.setFocused(false);
    }

    private void unfocusAllTextFields() {
        this.marketSearch.setFocused(false);
        this.enchantSearch.setFocused(false);
        this.bannedSearch.setFocused(false);
        this.unfocusEditorFields();
        this.unfocusSettingsFields();
    }

    private List<PriceEntry> filteredEntries(AutoMarket module) {
        if (module == null) {
            return List.of();
        }
        String query = this.marketSearch.getText();
        List<PriceEntry> out = new ArrayList<>();
        // Snapshot the registry because scanning can update it during rendering.
        for (PriceEntry entry : new ArrayList<>(module.itemRegistry.getValue())) {
            if (dev.bkclient.systems.modules.modules.bkmods.automarket.MarketSearch.matches(
                    this.displayName(entry), entry.getMaterial(), query)) {
                out.add(entry);
            }
        }
        return out;
    }

    private List<String> filteredEnchants(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String enchant : ALL_ENCHANTS) {
            if (q.isBlank() || enchant.contains(q)) {
                out.add(enchant);
            }
        }
        return out;
    }

    private void toggleEntry(PriceEntry entry) {
        AutoMarket module = this.module();
        if (entry == null || module == null) {
            return;
        }
        if (entry.isEnabled()) {
            entry.setBuyEnabled(false);
            entry.setSellEnabled(false);
        } else {
            entry.setBuyEnabled(true);
        }
        module.itemRegistry.notifyChanged();
    }

    private void removeEntry(PriceEntry entry) {
        AutoMarket module = this.module();
        if (entry == null || module == null) {
            return;
        }
        module.itemRegistry.getValue().remove(entry);
        if (this.selectedEntry == entry) {
            this.selectedEntry = null;
            this.editMode = false;
            this.ensureSelected(module);
        }
        module.itemRegistry.notifyChanged();
    }

    private void toggleRequiredEnchant(AutoMarket module, String id) {
        if (module == null || this.selectedEntry == null) {
            return;
        }
        Map<String, Integer> enchants = this.selectedEntry.getEnchantments();
        if (enchants.containsKey(id)) {
            enchants.remove(id);
        } else {
            enchants.put(id, 1);
        }
        module.itemRegistry.notifyChanged();
    }

    private void toggleBannedEnchant(AutoMarket module, String id) {
        if (module == null || this.selectedEntry == null) {
            return;
        }
        List<String> banned = this.selectedEntry.getBannedEnchantsList();
        if (banned.contains(id)) {
            banned.remove(id);
        } else {
            banned.add(id);
        }
        module.itemRegistry.notifyChanged();
    }

    private void syncSlider(dev.bkclient.ui.menu.csgui.widget.CsSlider slider, SliderSetting setting) {
        slider.setValue(setting.getCurrentValue());
    }

    static void releaseSlider(dev.bkclient.ui.menu.csgui.widget.CsSlider slider, SliderSetting setting) {
        boolean wasDragging = slider.isDragging();
        slider.onRelease();
        if (wasDragging && setting != null) {
            setting.setCurrentValueAndSave((float) slider.getValue());
        }
    }

    private boolean handleSliderClick(double mouseX, double mouseY) {
        AutoMarket module = this.module();
        return module != null && (
                this.refreshSlider.onClick(mouseX, mouseY)
                        || this.confirmSlider.onClick(mouseX, mouseY)
                        || this.restartSlider.onClick(mouseX, mouseY)
                        || this.sellIntervalSlider.onClick(mouseX, mouseY)
                        || this.sellTaxSlider.onClick(mouseX, mouseY)
                        || this.reconnectDelaySlider.onClick(mouseX, mouseY)
                        || this.confirmSlotSlider.onClick(mouseX, mouseY)
                        || this.sortingSlotSlider.onClick(mouseX, mouseY)
                        || this.refreshSlotSlider.onClick(mouseX, mouseY)
                        || this.nextPageSlotSlider.onClick(mouseX, mouseY)
                        || (module.minestarMode.isEnabled() && this.minestarNextSlotSlider.onClick(mouseX, mouseY))
                        || (module.minestarMode.isEnabled() && this.minestarPrevSlotSlider.onClick(mouseX, mouseY))
                        // Margin slidery reagują TYLKO gdy ich override jest włączony (inaczej slider
                        // nie jest renderowany, a stare bounds łapałyby klik → "skakanie").
                        || this.minDaysSlider.onClick(mouseX, mouseY));
    }

    /** Czy slider Buy Margin jest aktywny (override włączony na rozwiniętym entry). */

    /** Czy slider Sell Margin jest aktywny (override włączony na rozwiniętym entry). */

    private boolean handleSliderDrag(double mouseX) {
        AutoMarket module = this.module();
        if (module == null) {
            return false;
        }
        boolean handled = false;
        handled |= this.dragSlider(this.refreshSlider, module.refreshRate, mouseX);
        handled |= this.dragSlider(this.confirmSlider, module.confirmDelay, mouseX);
        handled |= this.dragSlider(this.restartSlider, module.restartDelay, mouseX);
        handled |= this.dragSlider(this.sellIntervalSlider, module.sellInterval, mouseX);
        handled |= this.dragSlider(this.sellTaxSlider, module.sellTax, mouseX);
        handled |= this.dragSlider(this.reconnectDelaySlider, module.reconnectDelay, mouseX);
        handled |= this.dragSlider(this.confirmSlotSlider, module.confirmSlotSetting, mouseX);
        handled |= this.dragSlider(this.sortingSlotSlider, module.sortingSlotSetting, mouseX);
        handled |= this.dragSlider(this.refreshSlotSlider, module.refreshSlotSetting, mouseX);
        handled |= this.dragSlider(this.nextPageSlotSlider, module.nextPageSlotSetting, mouseX);
        if (module.minestarMode.isEnabled()) {
            handled |= this.dragSlider(this.minestarNextSlotSlider, module.minestarNextSlot, mouseX);
            handled |= this.dragSlider(this.minestarPrevSlotSlider, module.minestarPrevSlot, mouseX);
        }
        // Per-item margin slidery — piszą wprost do rozwiniętego entry (bez SliderSetting).
        // Zaokrąglamy do pełnego % (krok 1) — bez tego slider zwraca surowy float (np. 69.87).
        // setValue() wpycha zaokrągloną wartość z powrotem, żeby knob też skoczył do pełnego %.
        // Persist on release to avoid writing the profile for each drag event.
        
        // Min Days slider — pisze wprost do entry (0 = null = off). Zapis registry na release.
        if (this.minDaysSliderEntry != null && this.minDaysSlider.onDrag(mouseX)) {
            int v = Math.round((float) this.minDaysSlider.getValue());
            this.minDaysSlider.setValue(v);
            this.minDaysSliderEntry.setMinDaysLeft(v <= 0 ? null : v);
            handled = true;
        }
        return handled;
    }

    static boolean dragSlider(dev.bkclient.ui.menu.csgui.widget.CsSlider slider, SliderSetting setting, double mouseX) {
        if (!slider.onDrag(mouseX)) {
            return false;
        }
        // Keep the preview local while dragging. AutoMarket setters save both the
        // full client configuration and market profile; commit once in releaseSlider.
        return true;
    }

    private boolean handleSliderKey(int keyCode) {
        AutoMarket module = this.module();
        if (module == null) {
            return false;
        }
        if (keyCode == 256 && this.isAnySliderFocused()) {
            this.unfocusAllSliders();
            return true;
        }
        if (keyCode != 262 && keyCode != 263) {
            return false;
        }
        float dir = keyCode == 262 ? 1.0f : -1.0f;
        return this.nudgeSlider(this.refreshSlider, module.refreshRate, dir)
                || this.nudgeSlider(this.confirmSlider, module.confirmDelay, dir)
                || this.nudgeSlider(this.restartSlider, module.restartDelay, dir)
                || this.nudgeSlider(this.sellIntervalSlider, module.sellInterval, dir)
                || this.nudgeSlider(this.sellTaxSlider, module.sellTax, dir)
                || this.nudgeSlider(this.reconnectDelaySlider, module.reconnectDelay, dir)
                || this.nudgeSlider(this.confirmSlotSlider, module.confirmSlotSetting, dir)
                || this.nudgeSlider(this.sortingSlotSlider, module.sortingSlotSetting, dir)
                || this.nudgeSlider(this.refreshSlotSlider, module.refreshSlotSetting, dir)
                || this.nudgeSlider(this.nextPageSlotSlider, module.nextPageSlotSetting, dir)
                || (module.minestarMode.isEnabled() && this.nudgeSlider(this.minestarNextSlotSlider, module.minestarNextSlot, dir))
                || (module.minestarMode.isEnabled() && this.nudgeSlider(this.minestarPrevSlotSlider, module.minestarPrevSlot, dir))
                || this.nudgeMinDaysSlider(dir);
    }

    /** Strzałki ←/→ dla Min Days slidera. Krok 1, clamp [0,60], 0=off, zapis registry. */
    private boolean nudgeMinDaysSlider(float dir) {
        if (!this.minDaysSlider.isFocused() || this.minDaysSliderEntry == null) {
            return false;
        }
        int next = Math.round((float) this.minDaysSlider.getValue()) + (int) dir;
        next = Math.max(0, Math.min(60, next));
        this.minDaysSlider.setValue(next);
        this.minDaysSliderEntry.setMinDaysLeft(next <= 0 ? null : next);
        AutoMarket module = this.module();
        if (module != null) module.itemRegistry.notifyChanged();
        return true;
    }

    /**
     * Strzałki ←/→ dla per-item margin sliderów. Krok 1%, clamp do [min,max], zapis override TEGO
     * itemu (saveEntry). buyMargin=true → pisze buy, false → sell.
     */

    private boolean nudgeSlider(dev.bkclient.ui.menu.csgui.widget.CsSlider slider, SliderSetting setting, float dir) {
        if (!slider.isFocused() || setting == null) {
            return false;
        }
        float step = setting.getStep() > 0.0f ? setting.getStep() : 1.0f;
        float next = setting.getCurrentValue() + step * dir;
        setting.setCurrentValueAndSave(next);
        slider.setValue(setting.getCurrentValue());
        return true;
    }

    private boolean isAnySliderDragging() {
        return this.refreshSlider.isDragging() || this.confirmSlider.isDragging() || this.restartSlider.isDragging()
                || this.sellIntervalSlider.isDragging() || this.sellTaxSlider.isDragging()
                || this.reconnectDelaySlider.isDragging() || this.confirmSlotSlider.isDragging()
                || this.sortingSlotSlider.isDragging() || this.refreshSlotSlider.isDragging()
                || this.nextPageSlotSlider.isDragging() || this.minestarNextSlotSlider.isDragging()
                || this.minestarPrevSlotSlider.isDragging()
                || this.minDaysSlider.isDragging();
    }

    private void releaseSlidersWhenMouseIsUp() {
        if (!this.isAnySliderDragging() || mc == null || mc.getWindow() == null) return;
        long window = mc.getWindow().getHandle();
        if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(window,
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            this.handleRelease();
        }
    }

    private boolean isAnySliderFocused() {
        return this.refreshSlider.isFocused() || this.confirmSlider.isFocused() || this.restartSlider.isFocused()
                || this.sellIntervalSlider.isFocused() || this.sellTaxSlider.isFocused()
                || this.reconnectDelaySlider.isFocused() || this.confirmSlotSlider.isFocused()
                || this.sortingSlotSlider.isFocused() || this.refreshSlotSlider.isFocused()
                || this.nextPageSlotSlider.isFocused() || this.minestarNextSlotSlider.isFocused()
                || this.minestarPrevSlotSlider.isFocused()
                || this.minDaysSlider.isFocused();
    }

    private void unfocusAllSliders() {
        this.refreshSlider.unfocus();
        this.confirmSlider.unfocus();
        this.restartSlider.unfocus();
        this.sellIntervalSlider.unfocus();
        this.sellTaxSlider.unfocus();
        this.reconnectDelaySlider.unfocus();
        this.confirmSlotSlider.unfocus();
        this.sortingSlotSlider.unfocus();
        this.refreshSlotSlider.unfocus();
        this.nextPageSlotSlider.unfocus();
        this.minestarNextSlotSlider.unfocus();
        this.minestarPrevSlotSlider.unfocus();
        this.minDaysSlider.unfocus();
    }

    private void addHit(float x, float y, float w, float h, Runnable action) {
        this.actionHits.add(new ActionHit(x, y, w, h, action));
    }

    private boolean hover(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private String displayName(PriceEntry entry) {
        if (entry == null) {
            return "Unknown";
        }
        if (entry.getName() != null && !entry.getName().isEmpty()) {
            return entry.getName();
        }
        return entry.getMaterial() != null && !entry.getMaterial().isEmpty() ? entry.getMaterial() : "Unknown";
    }

    private static final java.util.Map<Character, Character> SMALL_FONT_MAP = new java.util.HashMap<>() {
        {
            put('ᴀ','a'); put('ʙ','b'); put('ᴄ','c'); put('ᴅ','d'); put('ᴇ','e'); put('ꜰ','f');
            put('ɢ','g'); put('ʜ','h'); put('ɪ','i'); put('ᴊ','j'); put('ᴋ','k'); put('ʟ','l');
            put('ᴍ','m'); put('ɴ','n'); put('ᴏ','o'); put('ᴘ','p'); put('ʀ','r'); put('ꜱ','s');
            put('ᴛ','t'); put('ᴜ','u'); put('ᴠ','v'); put('ᴡ','w'); put('ʏ','y'); put('ᴢ','z');
        }
    };

    /**
     * True dla itemów z datą wygaśnięcia (pety/kostiumy/zwierzątka) — tylko dla nich pokazujemy
     * "Min Days Left". Wykrywa po nazwie znormalizowanej (small-caps → litery): Anarchia "pet "/
     * "kostium ", Minestar "ZWIERZĄTKO ..."/"KOSTIUM ..." (z dekoracjami, więc contains).
     */
    private boolean isExpiringItem(PriceEntry entry) {
        if (entry == null || entry.getName() == null) return false;
        String n = this.cleanName(entry.getName()).toLowerCase().trim();
        return n.startsWith("pet ") || n.startsWith("kostium ")
                || n.contains("zwierzątko") || n.contains("zwierzatko") || n.contains("kostium");
    }

    /** Strip color codes (incl. hex §x...) and normalize small-caps unicode so the font can render the name. */
    private String cleanName(String s) {
        return ItemStackExtractor.toReadablePlain(s);
    }

    private String trim(String value, Font font, float maxW) {
        if (value == null) {
            return "";
        }
        String clean = cleanName(value);
        if (font.width(clean) <= maxW) {
            return clean;
        }
        while (clean.length() > 1 && font.width(clean + "...") > maxW) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean + "...";
    }

    private String price(double value) {
        if (value <= 0.0) {
            return "$0";
        }
        return "$" + Math.round(value);
    }

    private String limitText(PriceEntry entry) {
        if (entry == null) {
            return translate("No Limits Set");
        }
        String buy = entry.isBuyEnabled() ? translate("Buy") + ": " + entry.getMinBuyCount() : "";
        String sell = entry.isSellEnabled() ? translate("Sell") + ": " + entry.getMinSellCount() : "";
        if (!buy.isEmpty() && !sell.isEmpty()) {
            return buy + " | " + sell;
        }
        if (!buy.isEmpty()) {
            return buy;
        }
        if (!sell.isEmpty()) {
            return sell;
        }
        return translate("No Limits Set");
    }

    private String enchantText(PriceEntry entry) {
        if (entry == null || entry.getEnchantments().isEmpty()) {
            return translate("No Enchantments required");
        }
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Integer> e : entry.getEnchantments().entrySet()) {
            if (i++ > 0) {
                builder.append(", ");
            }
            builder.append(e.getKey()).append(" ").append(e.getValue());
            if (i == 2 && entry.getEnchantments().size() > 2) {
                builder.append(" +").append(entry.getEnchantments().size() - 2);
                break;
            }
        }
        return builder.toString();
    }

    private ItemStack iconStack(PriceEntry entry) {
        if (entry == null) {
            return Items.BARRIER.getDefaultStack();
        }
        try {
            ItemStack iconStack = entry.getCachedIconStack();
            if (iconStack != null && !iconStack.isEmpty()) {
                return iconStack;
            }

            String mat = entry.getMaterial();
            if (mat == null || mat.isEmpty()) {
                return Items.BARRIER.getDefaultStack();
            }
            if (!mat.contains(":")) {
                mat = "minecraft:" + mat;
            }

            iconStack = Registries.ITEM.get(Identifier.of(mat)).getDefaultStack().copy();
            if (iconStack.isEmpty()) {
                iconStack = Items.BARRIER.getDefaultStack();
            }
            if (entry.getCustomModelData() != null && !iconStack.isEmpty()) {
                iconStack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                        new CustomModelDataComponent(List.of((float) entry.getCustomModelData()),
                                List.of(), List.of(), List.of()));
            }
            if (entry.getDyeColor() != null && !iconStack.isEmpty()) {
                iconStack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(entry.getDyeColor(), false));
            }
            if (entry.getHeadTextureValue() != null && !iconStack.isEmpty()) {
                GameProfile gp = new GameProfile(java.util.UUID.randomUUID(), "");
                gp.getProperties().put("textures", new Property("textures", entry.getHeadTextureValue()));
                iconStack.set(DataComponentTypes.PROFILE, new ProfileComponent(gp));
            }
            entry.setCachedIconStack(iconStack);
            return iconStack;
        } catch (Exception ignored) {
            return Items.BARRIER.getDefaultStack();
        }
    }

    private record ActionHit(float x, float y, float w, float h, Runnable action) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        }
    }

    private record EntryHit(
            PriceEntry entry,
            float rowX, float rowY, float rowW, float rowH,
            float toggleX, float toggleY, float toggleW, float toggleH,
            float removeX, float removeY, float removeW, float removeH,
            float editX, float editY, float editW, float editH) {

        boolean containsRow(double mouseX, double mouseY) {
            return this.contains(mouseX, mouseY, rowX, rowY, rowW, rowH);
        }

        boolean containsToggle(double mouseX, double mouseY) {
            return this.contains(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH);
        }

        boolean containsRemove(double mouseX, double mouseY) {
            return this.contains(mouseX, mouseY, removeX, removeY, removeW, removeH);
        }

        boolean containsEdit(double mouseX, double mouseY) {
            return this.contains(mouseX, mouseY, editX, editY, editW, editH);
        }

        private boolean contains(double mouseX, double mouseY, float x, float y, float w, float h) {
            return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        }
    }
}
