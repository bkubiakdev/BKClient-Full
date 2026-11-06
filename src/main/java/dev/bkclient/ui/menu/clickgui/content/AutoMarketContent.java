/*
 * AutoMarketContent - AutoMarket tab for CsGui.
 * The layout stays in the existing three-panel design, but the controls are bound
 * to the real Auto-Market registry/settings.
 */
package dev.bkclient.ui.menu.clickgui.content;

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
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.inventory.ItemStackExtractor;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.sounds.ClientSounds;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class AutoMarketContent implements IMinecraft {

    private static final float TOGGLE_WIDTH = 26.0F;
    private static final float TOGGLE_TRAVEL = 10.0F;
    private final java.util.Map<String, dev.bkclient.utility.animation.base.Animation> toggleAnimations = new java.util.HashMap<>();
    private final java.util.Map<String, dev.bkclient.utility.animation.base.Animation> togglePressAnimations = new java.util.HashMap<>();
    private final java.util.List<ToggleHit> toggleHits = new java.util.ArrayList<>();
    private ToggleHit pressedToggle;
    private Runnable pressedToggleAction;
    private double toggleStartX;
    private float toggleProgress;
    private boolean toggleMoved;
    private static final float PANEL_RADIUS = 8.0f;
    private static final float PANEL_GAP = 8.0f;
    private static final float PAD = 8.0f;
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

    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider refreshSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Refresh Rate", 10, 1000, 77,
                    v -> Math.round(v) + "ms");
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider confirmSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Confirm Delay", 0, 1000, 173,
                    v -> Math.round(v) + "ms");
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider restartSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Restart Delay", 0, 5000, 1500,
                    v -> Math.round(v) + "ms");
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider sellIntervalSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Sell Interval", 0.1, 5, 1,
                    v -> String.format(Locale.ROOT, "%.1fs", v));
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider sellTaxSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Sell Tax", 0, 25, 2,
                    v -> String.format(Locale.ROOT, "%.1f%%", v));
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider reconnectDelaySlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Reconnect Delay", 0, 30, 5,
                    v -> Math.round(v) + "s");
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider confirmSlotSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Confirm Slot", 0, 53, 10,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider sortingSlotSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Sorting Slot", 0, 53, 53,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider refreshSlotSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Refresh Slot", 0, 53, 51,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider nextPageSlotSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Next Page Slot", 0, 53, 53,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider minestarNextSlotSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("MineStar Next", 0, 53, 53,
                    v -> String.valueOf(Math.round(v)));
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider minestarPrevSlotSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("MineStar Prev", 0, 53, 52,
                    v -> String.valueOf(Math.round(v)));

    // Min Days Left slider — tylko dla itemów z datą wygaśnięcia (zwierzątka/kostiumy). 0 = off.
    private final dev.bkclient.ui.menu.clickgui.widget.CsSlider minDaysSlider =
            new dev.bkclient.ui.menu.clickgui.widget.CsSlider("Min Days Left", 0, 60, 0,
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
    private boolean detachedEditor;
    private boolean nameRulesOpen, enchantRulesOpen, sellingRulesOpen, advancedOpen;
    private dev.bkclient.ui.menu.dropdown.components.settings.impl.SelectSettingComponent requiredSelector, forbiddenSelector;
    private PriceEntry selectorEntry;
    private final dev.bkclient.framework.base.CustomComponent selectorParent = new dev.bkclient.framework.base.CustomComponent() {
        @Override protected void renderComponent(UIContext context) {}
    };

    private dev.bkclient.ui.menu.dropdown.components.settings.impl.SelectSettingComponent makeSelector(PriceEntry entry, boolean banned) {
        var localSettings = new ArrayList<dev.bkclient.systems.setting.Setting>();
        var setting = new dev.bkclient.systems.setting.settings.SelectSetting(() -> localSettings,
            banned ? "Banned Enchants" : "Select Enchantments");
        for (String id : ALL_ENCHANTS) {
            var value = new dev.bkclient.systems.setting.settings.SelectSetting.Value(setting, id) {
                @Override public dev.bkclient.systems.setting.settings.SelectSetting.Value toggle() {
                    super.toggle();
                    if (banned) {
                        if (isSelected()) { if (!entry.getBannedEnchantsList().contains(id)) entry.getBannedEnchantsList().add(id); }
                        else entry.getBannedEnchantsList().remove(id);
                    } else {
                        if (isSelected()) entry.getEnchantments().putIfAbsent(id, 1);
                        else entry.getEnchantments().remove(id);
                    }
                    if (module() != null) module().itemRegistry.notifyChanged();
                    return this;
                }
            };
            if (banned ? entry.getBannedEnchantsList().contains(id) : entry.getEnchantments().containsKey(id)) value.select();
        }
        return new dev.bkclient.ui.menu.dropdown.components.settings.impl.SelectSettingComponent(setting, selectorParent)
            .maxVisibleRows(3).prefixCompletion();
    }

    private float renderSelectors(UIContext context, PriceEntry entry, float x, float y, float w) {
        if (selectorEntry != entry) {
            selectorEntry = entry;
            requiredSelector = makeSelector(entry, false);
            forbiddenSelector = makeSelector(entry, true);
        }
        for (var selector : List.of(requiredSelector, forbiddenSelector)) {
            selector.set(x - 9, y, w + 18, selector.getHeight());
            selector.render(context);
            y += selector.getHeight() + 8;
        }
        return y;
    }

    private String enchantQuery = "", bannedQuery = "";
    private float marketListX, marketListY, marketListW, marketListH;
    private float enchantListX, enchantListY, enchantListW, enchantListH;
    private float bannedListX, bannedListY, bannedListW, bannedListH;
    private float editPanelX, editPanelY, editPanelW, editPanelH;
    private float settingsPanelX, settingsPanelY, settingsPanelW, settingsPanelH;

    /** Detached market screen reuses the existing rule and settings editors. */
    public void renderSidebar(UIContext context, PriceEntry entry, boolean settings,
            float x, float y, float w, float h) {
        detachedEditor = true;
        releaseSlidersWhenMouseIsUp();
        entryHits.clear(); actionHits.clear(); toggleHits.clear();
        selectedEntry = entry;
        editMode = !settings && entry != null;
        if (settings) renderSettings(context, module(), x, y, w, h, 1);
        else if (entry != null) renderEditMode(context, module(), entry, x + 10, y + 6, w - 20, h - 12, 1);
    }

    public void finishEditing() {
        handleRelease();
        commitEditorFields(module(), selectedEntry);
        commitSettingsFields(module());
        unfocusAllTextFields();
        unfocusAllSliders();
        if (requiredSelector != null) { requiredSelector.clearFocus(); forbiddenSelector.clearFocus(); }
    }

    public void toggleItem(PriceEntry entry) { toggleEntry(entry); }

    public void renderItemCard(UIContext context, PriceEntry entry, float x, float y, float w, boolean expanded) {
        ItemStack icon = iconStack(entry);
        if (icon != null && !icon.isEmpty()) context.drawItem(icon, x + 10, y + 10, 1);
        Font font = Fonts.SEMIBOLD.getFont(8);
        context.drawText(font, trim(displayName(entry), font, w - 100), x + 32, y + 11, Colors.getTextColor());
        context.drawText(Fonts.REGULAR.getFont(6), translate("Item rule"), x + 32, y + 23, Colors.getTextColor().mulAlpha(.45f));
        miniToggle(context, x + w - 60, y + 18, entry.isEnabled(), 1);
        dev.bkclient.ui.components.LucideIcons.MORE.draw(context, x + w - 20, y + 12, 12, Colors.getTextColor().mulAlpha(.65f));
        if (expanded) {
            infoBlock(context, x + 12, y + 40, w - 24, "NAME RULE", entry.getName(), 1);
            pricesBlock(context, entry, x + 12, y + 65, w - 24, 1);
        }
    }

    public void render(UIContext context, float x, float y, float w, float h, float alpha) {
        this.releaseSlidersWhenMouseIsUp();
        AutoMarket module = this.module();
        this.entryHits.clear();
        this.actionHits.clear();
        this.toggleHits.clear();
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
                    Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
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
        BorderRadius cr = BorderRadius.all(5.0f);
        context.drawRoundedRect(x - 2.5f, y, w + 5.0f, h, cr,
                Colors.getBackgroundColor().withAlpha(155.0f * alpha));

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
                    Colors.getAccent().withAlpha(255.0f * 0.08f * alpha));
        }

        ItemStack icon = this.iconStack(entry);
        if (icon != null && !icon.isEmpty()) {
            context.drawItem(icon, x - 1.5f, centerY - 7.0f, 0.75f);
        }

        String name = this.trim(this.displayName(entry), font, w - 72.0f);
        context.drawText(font, name, x + 13.0f, centerY - font.height() / 2.0f,
                Colors.getTextColor().withAlpha(255.0f * 0.75f * alpha));

        float tx = x + w - 56.0f;
        this.miniToggle(context, tx, centerY, entry != null && entry.isEnabled(), alpha);
        context.drawIcon(Fonts.ICONS, 'L', x + w - 22.0f, centerY - 5.0f, 9.5f,
                Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
        context.drawIcon(Fonts.ICONS, '5', x + w - 10.0f, centerY - 5.7f, 10.5f,
                Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));

        this.entryHits.add(new EntryHit(entry, x - 3.0f, y - 1.0f, w + 6.0f, 12.0f,
                tx - 2.0f, centerY - 6.0f, TOGGLE_WIDTH + 4.0f, 12.0f,
                x + w - 24.0f, centerY - 7.0f, 12.0f, 12.0f,
                x + w - 11.0f, centerY - 7.0f, 12.0f, 12.0f));
    }

    private void pricesBlock(UIContext context, PriceEntry entry, float x, float y, float w, float alpha) {
        Font small = Fonts.SEMIBOLD.getFont(6.5f);
        Font lbl = Fonts.MEDIUM.getFont(7.0f);
        Font amt = Fonts.SEMIBOLD.getFont(6.5f);
        context.drawText(small, translate("PRICES"), x, y, Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));

        float rowY = y + 11.0f;
        float iconSz = 10.0f;
        float iconY = rowY + 1.0f;
        dev.bkclient.ui.components.LucideIcons.BUY.draw(context, x - 1, iconY - 1.5f, iconSz + 3,
                ColorRGBA.GREEN.withAlpha(255.0f * alpha));

        float bx = x + iconSz + 4.0f;
        context.drawText(lbl, translate("Buy"), bx, rowY, Colors.getTextColor().withAlpha(255.0f * 0.75f * alpha));
        context.drawText(amt, this.price(entry != null ? entry.getMaxPrice() : 0.0), bx, rowY + 7.0f,
                GREEN.withAlpha(255.0f * alpha));

        float sx = x + w * 0.52f;
        dev.bkclient.ui.components.LucideIcons.SELL.draw(context, sx - 1.5f, iconY - 1.5f, iconSz + 4,
                Colors.getAccent().withAlpha(255.0f * alpha));

        float sbx = sx + iconSz + 4.0f;
        context.drawText(lbl, translate("Sell"), sbx, rowY, Colors.getTextColor().withAlpha(255.0f * 0.75f * alpha));
        context.drawText(amt, this.price(entry != null ? entry.getSellPrice() : 0.0), sbx, rowY + 7.0f,
                Colors.getAccent().withAlpha(255.0f * alpha));
    }

    private void infoBlock(UIContext context, float x, float y, float w, String title, String value, float alpha) {
        Font small = Fonts.SEMIBOLD.getFont(6.0f);
        Font val = Fonts.SEMIBOLD.getFont(6.5f);
        context.drawText(small, translate(title), x, y, Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
        context.drawText(val, this.trim(value, val, w - 4.0f), x, y + 8.0f,
                Colors.getTextColor().withAlpha(255.0f * 0.5f * alpha));
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
                    Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
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

        this.sectionLabel(context, cx, cy, cw, "Select Enchantments", entry.getEnchantments().size() + " / 40", alpha);
        cy += 10.0f;
        this.searchField(context, this.enchantSearch, cx, cy, cw, alpha);
        cy += 14.0f;
        this.enchantListX = cx - 2.5f;
        this.enchantListY = cy;
        this.enchantListW = cw + 5.0f;
        this.enchantListH = 44.0f;
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
        this.bannedListH = Math.min(44.0f, Math.max(0.0f, y + h - PAD - cy));
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

        if (detachedEditor) {
            renderCompactEditor(context, module, entry, x, y, w, h, alpha);
            return;
        }
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
        if (detachedEditor) {
            cy = renderSelectors(context, entry, x, cy, w);
        } else {
        this.sectionLabel(context, x, cy, w, "Select Enchantments", entry.getEnchantments().size() + " / 40", alpha);
        cy += 10.0f;
        this.searchField(context, this.enchantSearch, x, cy, w, alpha);
        cy += detachedEditor ? 20.0f : 14.0f;
        this.enchantListX = x - 2.5f;
        this.enchantListY = cy;
        this.enchantListW = w + 5.0f;
        this.enchantListH = 44.0f;
        this.enchantList(context, module, entry, this.enchantSearch.getText(), false, this.enchantScroll,
                x - 2.5f, cy, w + 5.0f, this.enchantListH, alpha);
        cy += this.enchantListH + 7.0f;

        this.sectionLabel(context, x, cy, w, "Banned Enchants", entry.getBannedEnchantsList().size() + " / 40", alpha);
        cy += 10.0f;
        this.searchField(context, this.bannedSearch, x, cy, w, alpha);
        cy += detachedEditor ? 20.0f : 14.0f;
        this.bannedListX = x - 2.5f;
        this.bannedListY = cy;
        this.bannedListW = w + 5.0f;
        this.bannedListH = 44.0f;
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

    private float ruleSection(UIContext context, String label, String summary, boolean open,
            float x, float y, float w, float alpha, Runnable toggle) {
        boolean hovered = this.hover(context.getMouseX(), context.getMouseY(), x, y, w, 22);
        context.drawRoundedRect(x, y, w, 22, BorderRadius.all(5),
                (dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.current().dark ? ColorRGBA.BLACK : ColorRGBA.WHITE).mulAlpha((open || hovered ? .48F : .32F) * alpha));
        var icon = switch (label) {
            case "Name & lore" -> dev.bkclient.ui.components.LucideIcons.INFO;
            case "Enchantments" -> dev.bkclient.ui.components.LucideIcons.SPARKLES;
            case "Selling" -> dev.bkclient.ui.components.LucideIcons.SELL;
            default -> dev.bkclient.ui.components.LucideIcons.GAUGE;
        };
        icon.draw(context, x + 5, y + 6, 10, Colors.getTextColor().mulAlpha(.7F * alpha));
        context.drawText(Fonts.MEDIUM.getFont(7), translate(label), x + 20, y + 7, Colors.getTextColor().mulAlpha(alpha));
        context.drawRightText(Fonts.MEDIUM.getFont(6), translate(summary), x + w - 18, y + 8, Colors.getTextColor().mulAlpha(.55F * alpha));
        context.drawRightText(Fonts.MEDIUM.getFont(8), open ? "−" : "+", x + w - 6, y + 6, Colors.getTextColor().mulAlpha(.65F * alpha));
        addHit(x, y, w, 22, () -> {
            finishEditing();
            toggle.run();
        });
        return y + 28;
    }

    private void renderCompactEditor(UIContext context, AutoMarket module, PriceEntry entry,
            float x, float y, float w, float h, float alpha) {
        for (TextField field : List.of(nameField, materialField, loreField, bannedLoreField, minBuyField, minSellField)) field.set(-10000, -10000, 0, 0);
        minDaysSliderEntry = null;
        float origin = y + 3 - (float)editScroll.getValue();
        float cy = origin;
        ScissorUtility.push(context.getMatrices(), x - 3, y - 3, w + 6, h + 3);
        cy = editorInfo(context, entry, x, cy, w, alpha);
        cy = editorFieldPair(context, maxBuyField, "Max buy price", sellPriceField, "Sell price", x, cy, (w - 6)/2, alpha);
        this.settingsToggleRow(context, x, cy, w, "Buying", null, entry.isBuyEnabled(), alpha);
        this.addHit(x, cy, w, 17.0f, () -> {
            if (this.selectedEntry != null && module != null) {
                this.selectedEntry.setBuyEnabled(!this.selectedEntry.isBuyEnabled());
                module.itemRegistry.notifyChanged();
            }
        });
        cy += 20.0f;
        this.settingsToggleRow(context, x, cy, w, "Selling", null, entry.isSellEnabled(), alpha);
        this.addHit(x, cy, w, 17.0f, () -> {
            if (this.selectedEntry != null && module != null) {
                this.selectedEntry.setSellEnabled(!this.selectedEntry.isSellEnabled());
                module.itemRegistry.notifyChanged();
            }
        });
        cy += 23.0f;

        cy = ruleSection(context, "Name & lore", (entry.getLore() == null || entry.getLore().isBlank()) && entry.getBannedLore().isBlank() ? "Name only" : "Lore set",
            nameRulesOpen, x, cy, w, alpha, () -> nameRulesOpen = !nameRulesOpen);
        if (nameRulesOpen) {
            cy = editorField(context, nameField, "Name rule", x, cy, w, alpha);
            cy = editorField(context, loreField, "Required lore", x, cy, w, alpha);
            cy = editorField(context, bannedLoreField, "Banned lore (buying)", x, cy, w, alpha);
        }
        cy = ruleSection(context, "Enchantments", (entry.getEnchantments().size() + entry.getBannedEnchantsList().size()) + " " + translate("rules"),
            enchantRulesOpen, x, cy, w, alpha, () -> enchantRulesOpen = !enchantRulesOpen);
        if (enchantRulesOpen) cy = renderSelectors(context, entry, x, cy, w);
        cy = ruleSection(context, "Selling", entry.isSellEnabled() ? "Enabled" : "Disabled",
            sellingRulesOpen, x, cy, w, alpha, () -> sellingRulesOpen = !sellingRulesOpen);
        if (sellingRulesOpen) {
            cy = editorFieldPair(context, minBuyField, "Min buy count", minSellField, "Min sell count", x, cy, (w - 6)/2, alpha);
        this.settingsToggleRow(context, x, cy, w, "Price per item", null, entry.isUnitMode(), alpha);
        this.addHit(x, cy, w, 17.0f, () -> {
            if (this.selectedEntry != null && module != null) {
                this.selectedEntry.setUnitMode(!this.selectedEntry.isUnitMode());
                module.itemRegistry.notifyChanged();
            }
        });
        cy += 20.0f;
        }
        cy = ruleSection(context, "Advanced", "ID", advancedOpen, x, cy, w, alpha, () -> advancedOpen = !advancedOpen);
        if (advancedOpen) {
            cy = editorField(context, materialField, "Material ID", x, cy, w, alpha);
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
            this.settingsToggleRow(context, x, cy, w, "Ignore CMD", null,
                    entry.isIgnoreCustomModelData(), alpha);
            this.addHit(x, cy, w, 17.0f, () -> {
                if (this.selectedEntry != null && module != null) {
                    this.selectedEntry.setIgnoreCustomModelData(!this.selectedEntry.isIgnoreCustomModelData());
                    module.itemRegistry.notifyChanged();
                }
            });
            cy += 23.0f;
        }

        }
        ScissorUtility.pop();
        editScroll.setMax(Math.min(0, h - (cy - origin) - 6));
        editScroll.update();
        commitEditorFields(module, entry);
    }

    private float editorInfo(UIContext context, PriceEntry entry, float x, float y, float w, float alpha) {
        if (detachedEditor) {
            context.drawRoundedRect(x, y, w, 26, BorderRadius.all(5), Colors.getTextColor().mulAlpha(.035F * alpha));
            ItemStack item = this.iconStack(entry);
            if (item != null && !item.isEmpty()) context.drawItem(item, x + 5, y + 6, .85F);
            var titleFont = Fonts.SEMIBOLD.getFont(7);
            context.drawText(titleFont, this.trim(this.displayName(entry), titleFont, w - 30), x + 24, y + 9, Colors.getTextColor().mulAlpha(alpha));
            return y + 34;
        }
        ItemStack icon = this.iconStack(entry);
        if (icon != null && !icon.isEmpty()) {
            context.drawItem(icon, x - 1.5f, y - 2.0f, 0.75f);
        }
        Font nameFont = Fonts.SEMIBOLD.getFont(6.5f);
        context.drawText(nameFont, this.trim(this.displayName(entry), nameFont, w - 16.0f), x + 13.0f, y + 1.0f,
                Colors.getTextColor().withAlpha(255.0f * 0.75f * alpha));
        y += 14.0f;
        if (detachedEditor) return y + 8.0f;

        Font label = Fonts.MEDIUM.getFont(6.0f);
        Font value = Fonts.SEMIBOLD.getFont(6.2f);
        context.drawText(label, translate("Material"), x, y, Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
        context.drawText(value, this.trim(entry.getMaterial() != null ? entry.getMaterial() : "air", value, w * 0.44f),
                x, y + 7.0f, Colors.getTextColor().withAlpha(255.0f * 0.5f * alpha));
        context.drawText(label, translate("Lore"), x + w * 0.5f, y, Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
        String lore = entry.getLore() == null || entry.getLore().isBlank()
                ? "No lore"
                : ItemStackExtractor.toReadablePlain(entry.getLore());
        context.drawText(value, this.trim(lore, value, w * 0.45f), x + w * 0.5f, y + 7.0f,
                Colors.getTextColor().withAlpha(255.0f * 0.5f * alpha));
        y += 19.0f;
        this.divider(context, x, y, w, alpha);
        return y + 5.0f;
    }

    private float editorField(UIContext context, TextField field, String label, float x, float y, float w, float alpha) {
        Font labelFont = Fonts.MEDIUM.getFont(detachedEditor ? 7.0f : 6.0f);
        context.drawText(labelFont, translate(label), x + 1.0f, y, Colors.getTextColor().withAlpha(255.0f * 0.5f * alpha));
        y += detachedEditor ? 11.0f : 8.0f;
        this.textInput(context, field, x, y, w, alpha);
        return y + (detachedEditor ? 25.0f : 15.0f);
    }

    private float editorFieldPair(UIContext context, TextField left, String leftLabel, TextField right,
            String rightLabel, float x, float y, float colW, float alpha) {
        Font labelFont = Fonts.MEDIUM.getFont(detachedEditor ? 7.0f : 6.0f);
        context.drawText(labelFont, translate(leftLabel), x + 1.0f, y, Colors.getTextColor().withAlpha(255.0f * 0.5f * alpha));
        context.drawText(labelFont, translate(rightLabel), x + colW + 7.0f, y, Colors.getTextColor().withAlpha(255.0f * 0.5f * alpha));
        y += detachedEditor ? 11.0f : 8.0f;
        this.textInput(context, left, x, y, colW, alpha);
        this.textInput(context, right, x + colW + 6.0f, y, colW, alpha);
        return y + (detachedEditor ? 25.0f : 15.0f);
    }

    private void textInput(UIContext context, TextField field, float x, float y, float w, float alpha) {
        if (detachedEditor) {
            context.drawRoundedRect(x, y, w, 18, BorderRadius.all(4), Colors.getTextColor().mulAlpha((field.isFocused() ? .075F : .035F) * alpha));
            context.drawRoundedBorder(x, y, w, 18, .5F, BorderRadius.all(4), Colors.getTextColor().mulAlpha((field.isFocused() ? .28F : .10F) * alpha));
            field.setAlpha(alpha); field.setTextColor(Colors.getTextColor());
            field.setPreview(field == bannedLoreField ? "Otrucie; Spowolnienie" : field == loreField ? "Optional lore" : field == nameField ? "Item name" : field == materialField ? "minecraft:item" : (field == maxBuyField || field == minBuyField || field == sellPriceField || field == minSellField) ? "0" : "Enter text");
            field.set(x + 6, y, w - 12, 18); field.render(context);
            return;
        }
        float h = 12.0f;
        context.drawRoundedRect(x - 2.5f, y, w + 4.0f, h, BorderRadius.all(4.0f),
                Colors.getBackgroundColor().withAlpha(155.0f * alpha));
        context.drawRoundedBorder(x - 2.5f, y, w + 4.0f, h, 0.05f, BorderRadius.all(4.0f),
                Colors.getTextColor().withAlpha(14.0f * alpha));
        field.setAlpha(alpha);
        field.setTextColor(Colors.getTextColor());
        field.setPreview("Enter text");
        field.set(x - 0.5f, y, w - 2.0f, h);
        field.render(context);
    }

    private void enchantList(UIContext context, AutoMarket module, PriceEntry entry, String query, boolean banned,
            ScrollHandler scroll, float x, float y, float w, float h, float alpha) {
        context.drawRoundedRect(x, y, w, h, BorderRadius.all(6), Colors.getBackgroundColor().withAlpha(76.5f * alpha));

        List<String> shown = this.filteredEnchants(query);
        float rowH = 12.0f;
        float scrollY = (float) scroll.getValue();
        ScissorUtility.push(context.getMatrices(), x, y, w, h);
        Font ef = Fonts.REGULAR.getFont(7.0f);
        float ey = y + 4.0f;
        if (shown.isEmpty()) context.drawText(ef, translate("No matching entries"), x + 7, y + 12, Colors.getTextColor().mulAlpha(.4f * alpha));
        int first = Math.clamp((int)(scrollY / rowH), 0, Math.max(0, shown.size() - 3));
        for (int i = first; i < Math.min(shown.size(), first + 3); ++i) {
            String enchant = shown.get(i);
            float rowY = ey + (i - first) * rowH;
            if (rowY + rowH < y || rowY > y + h) {
                continue;
            }
            boolean selected = banned
                    ? entry.getBannedEnchantsList().contains(enchant)
                    : entry.getEnchantments().containsKey(enchant);
            boolean hovered = hover(context.getMouseX(), context.getMouseY(), x, rowY - 2, w, rowH);
            context.drawFadeoutText(ef, enchant, x + 7, rowY,
                    Colors.getTextColor().mulAlpha((selected || hovered ? 1 : .75f) * alpha), .8f, 1, w - 28);
            if (selected) dev.bkclient.ui.components.LucideIcons.CHECK.draw(context, x + w - 15, rowY - .5f, 9, Colors.getTextColor().mulAlpha(alpha));
            final String id = enchant;
            float hitTop = Math.max(y, rowY - 3.0f);
            float hitBottom = Math.min(y + h, rowY - 3.0f + rowH);
            this.addHit(x, hitTop, w, Math.max(0, hitBottom - hitTop), () -> {
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
                    Colors.getTextColor().withAlpha(255.0f * 0.06f * alpha));
            context.drawRoundedRect(sbX, thumbY, sbW, thumbH, BorderRadius.all(sbW / 2.0f),
                    Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
        }
    }

    private float settingsEditorField(UIContext context, TextField field, String label,
            float x, float y, float w, float alpha, float top, float bottom) {
        float fieldWidth = Math.max(35, w * .48F);
        float fieldX = x + w - fieldWidth - 5;
        field.set(fieldX + 4, y + 2, fieldWidth - 8, 13);
        field.setPreview(translate("Type..."));
        field.setAlpha(alpha);
        field.setTextColor(Colors.getTextColor());
        if (y + 17 >= top && y <= bottom || field.isFocused()) {
            dev.bkclient.ui.menu.clickgui.widget.MarketSettingRow.draw(context, label, x, y, w, fieldX - x - 21, alpha);
            context.drawRoundedRect(fieldX, y + 2, fieldWidth, 13, BorderRadius.all(3),
                    Colors.getTextColor().mulAlpha((field.isFocused() ? .075F : .035F) * alpha));
            context.drawRoundedBorder(fieldX, y + 2, fieldWidth, 13, .5F, BorderRadius.all(3),
                    Colors.getTextColor().mulAlpha((field.isFocused() ? .28F : .10F) * alpha));
            field.render(context);
        }
        return y + 19;
    }

    private void settingsToggleRow(UIContext context, float x, float y, float w, String name,
            String hint, boolean on, float alpha) {
        dev.bkclient.ui.menu.clickgui.widget.MarketSettingRow.draw(context, name, x, y, w, w - 38, alpha);
        var ink = Colors.getTextColor();
        var animation = this.toggleAnimations.computeIfAbsent("settings:" + name, key ->
                new dev.bkclient.utility.animation.base.Animation(180L, on ? 1 : 0, dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT));
        float progress = animation.update(on ? 1 : 0);
        context.drawRoundedRect(x + w - 15, y + 4, 9, 9, BorderRadius.all(2.5F), ink.mulAlpha((.12F + .88F * progress) * alpha));
        if (progress > .01F) dev.bkclient.ui.components.LucideIcons.CHECK.draw(context, x + w - 14, y + 5, 7,
                (dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.current().dark ? ColorRGBA.BLACK : ColorRGBA.WHITE).mulAlpha(progress * alpha));
        if (progress < .99F) dev.bkclient.ui.components.LucideIcons.CROSS.draw(context, x + w - 14, y + 5, 7, ink.mulAlpha(.45F * (1 - progress) * alpha));
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
        boolean showMineStarSlots = module != null && module.minestarMode.isEnabled();
        this.syncSettingsFields(module);
        float viewH = Math.max(0.0f, contentBottom - contentTop);
        this.settingsScroll.update();
        float scrollY = (float) this.settingsScroll.getValue();
        cy = contentTop - scrollY;

        ScissorUtility.push(context.getMatrices(), x, contentTop - 2.0f, w, viewH + 2.0f);

        boolean aggressive = module != null && module.aggressiveMode.isEnabled();
        this.settingsToggleRow(context, cx, cy, cw, "Anarchia", null, aggressive, alpha);
        this.addHit(cx, cy, cw, 17.0f, () -> {
            if (module != null) module.aggressiveMode.toggle();
        });
        cy += 19.0f;

        boolean rapy = module != null && module.rapyMode.isEnabled();
        this.settingsToggleRow(context, cx, cy, cw, "Rapy", null, rapy, alpha);
        this.addHit(cx, cy, cw, 17.0f, () -> {
            if (module != null) module.rapyMode.toggle();
        });
        cy += 19.0f;

        boolean minestar = module != null && module.minestarMode.isEnabled();
        this.settingsToggleRow(context, cx, cy, cw, "MineStar", null, minestar, alpha);
        this.addHit(cx, cy, cw, 17.0f, () -> {
            if (module != null) module.minestarMode.toggle();
        });
        cy += 19.0f;

        boolean autoReconnect = module != null && module.autoReconnect.isEnabled();
        this.settingsToggleRow(context, cx, cy, cw, "Auto Reconnect", null, autoReconnect, alpha);
        this.addHit(cx, cy, cw, 17.0f, () -> {
            if (module != null) module.autoReconnect.toggle();
        });
        cy += 19.0f;

        boolean autoJump = module != null && module.autoJump.isEnabled();
        this.settingsToggleRow(context, cx, cy, cw, "Auto Jump", null, autoJump, alpha);
        this.addHit(cx, cy, cw, 17.0f, () -> {
            if (module != null) module.autoJump.toggle();
        });
        cy += 19.0f;

        boolean autoRepair = module != null && module.autoRepair.isEnabled();
        this.settingsToggleRow(context, cx, cy, cw, "Auto Repair", null, autoRepair, alpha);
        this.addHit(cx, cy, cw, 17.0f, () -> {
            if (module != null) module.autoRepair.toggle();
        });
        cy += 19.0f;

        this.divider(context, cx, cy, cw, alpha);
        cy += 8.0f;

        Font lbl = Fonts.SEMIBOLD.getFont(7.0f);
        dev.bkclient.ui.components.LucideIcons.BUY.draw(context, cx + 3, cy - 1, 8, Colors.getTextColor().mulAlpha(.7F * alpha));
        context.drawText(lbl, translate("Price Mode"), cx + 16, cy, Colors.getTextColor().mulAlpha(.75F * alpha));
        cy += 9.0f;
        float pmH = 14.0f;
        float pmGap = 4.0f;
        float pmW = (cw - pmGap) / 2.0f;
        boolean unit = module == null || module.unitModeVal.isSelected();
        this.modeButton(context, cx, cy, pmW, pmH, "Unit", unit, alpha);
        this.modeButton(context, cx + pmW + pmGap, cy, pmW, pmH, "Sum", !unit, alpha);
        float priceModeY = cy;
        this.addHit(cx, priceModeY, pmW, pmH, () -> {
            if (module != null) module.unitModeGlobal.setValue(module.unitModeVal);
        });
        this.addHit(cx + pmW + pmGap, priceModeY, pmW, pmH, () -> {
            if (module != null) module.unitModeGlobal.setValue(module.sumModeVal);
        });
        cy += pmH + 8.0f;

        this.divider(context, cx, cy, cw, alpha);
        cy += 8.0f;

        if (module != null) {
            this.syncSlider(this.refreshSlider, module.refreshRate);
            this.refreshSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            this.syncSlider(this.confirmSlider, module.confirmDelay);
            this.confirmSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            this.syncSlider(this.restartSlider, module.restartDelay);
            this.restartSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            this.syncSlider(this.sellIntervalSlider, module.sellInterval);
            this.sellIntervalSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            this.syncSlider(this.sellTaxSlider, module.sellTax);
            this.sellTaxSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            this.syncSlider(this.reconnectDelaySlider, module.reconnectDelay);
            this.reconnectDelaySlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            this.syncSlider(this.confirmSlotSlider, module.confirmSlotSetting);
            this.confirmSlotSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            this.syncSlider(this.sortingSlotSlider, module.sortingSlotSetting);
            this.sortingSlotSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            this.syncSlider(this.refreshSlotSlider, module.refreshSlotSetting);
            this.refreshSlotSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            this.syncSlider(this.nextPageSlotSlider, module.nextPageSlotSetting);
            this.nextPageSlotSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
            cy += 19.0f;
            if (module.minestarMode.isEnabled()) {
                this.syncSlider(this.minestarNextSlotSlider, module.minestarNextSlot);
                this.minestarNextSlotSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
                cy += 19.0f;
                this.syncSlider(this.minestarPrevSlotSlider, module.minestarPrevSlot);
                this.minestarPrevSlotSlider.renderRow(context, cx, cy, cw, alpha, cy + 19.0f >= contentTop - 2.0f && cy <= contentBottom);
                cy += 19.0f;
            }

            this.divider(context, cx, cy, cw, alpha);
            cy += 8.0f;

            cy = this.settingsEditorField(context, this.marketCommandField, "Market Command", cx, cy, cw, alpha, contentTop - 2.0f, contentBottom);
            cy = this.settingsEditorField(context, this.customTitleField, "Custom Title", cx, cy, cw, alpha, contentTop - 2.0f, contentBottom);
            cy = this.settingsEditorField(context, this.sellCommandField, "Sell Command", cx, cy, cw, alpha, contentTop - 2.0f, contentBottom);
            cy = this.settingsEditorField(context, this.loreRegexField, "Lore Regex", cx, cy, cw, alpha, contentTop - 2.0f, contentBottom);
            cy = this.settingsEditorField(context, this.sellerRegexField, "Seller Regex", cx, cy, cw, alpha, contentTop - 2.0f, contentBottom);
            cy = this.settingsEditorField(context, this.confirmItemField, "Confirm Item", cx, cy, cw, alpha, contentTop - 2.0f, contentBottom);
            cy = this.settingsEditorField(context, this.sortingKeywordField, "Sorting Keyword", cx, cy, cw, alpha, contentTop - 2.0f, contentBottom);
            if (module.nonPremium.isSelected()) {
                cy = this.settingsEditorField(context, this.passwordField, "Login Password", cx, cy, cw, alpha, contentTop - 2.0f, contentBottom);
                cy = this.settingsEditorField(context, this.loginCommandField, "Login Command", cx, cy, cw, alpha, contentTop - 2.0f, contentBottom);
            }
            this.commitSettingsFields(module);
        }

        ScissorUtility.pop();
        this.settingsScroll.setMax(Math.min(0.0, viewH - (cy + scrollY - contentTop + 8.0f)));
    }

    private void divider(UIContext context, float x, float y, float w, float alpha) {
        context.drawRect(x, y, w, 1.0f, Colors.getSeparatorColor().mulAlpha(alpha));
    }

    private void modeButton(UIContext context, float x, float y, float w, float h, String text, boolean selected,
            float alpha) {
        ColorRGBA fill = selected
                ? Colors.getAccent().withAlpha(115.0f * alpha)
                : Colors.getBackgroundColor().withAlpha(85.0f * alpha);
        boolean v2 = dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.active();
        if (v2) {
            var palette = dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.current();
            context.drawRoundedRect(x, y, w, h, BorderRadius.all(5),
                (selected ? palette.selection : palette.field).mulAlpha(alpha));
        } else {
        context.drawLiquidGlass(x, y, w, h, 2.0f, 0.05f, BorderRadius.all(6.0f), fill);
        }
        Font font = Fonts.MEDIUM.getFont(6.0f);
        context.drawCenteredText(font, translate(text), x + w / 2.0f, y + (h - font.height()) / 2.0f,
                (selected && !v2 ? ColorRGBA.WHITE : Colors.getTextColor()).mulAlpha((selected ? 0.95f : 0.65f) * alpha));
    }

    private float header(UIContext context, float x, float y, float w, char icon, String title, float alpha,
            float iconSize, float fontSize) {
        float hy = y + PAD;
        Font font = Fonts.SEMIBOLD.getFont(fontSize);
        float lineH = Math.max(iconSize, font.height());
        context.drawIcon(Fonts.ICONS, icon, x + PAD, hy + (lineH - iconSize) / 2.0f, iconSize,
                Colors.getAccent().withAlpha(255.0f * alpha));
        context.drawText(font, translate(title), x + PAD + iconSize + 3.0f, hy + (lineH - font.height()) / 2.0f,
                Colors.getTextColor().withAlpha(255.0f * alpha));
        return hy + lineH + 6.0f;
    }

    private void searchField(UIContext context, TextField field, float x, float y, float w, float alpha) {
        if (field == enchantSearch || field == bannedSearch) {
            String query = field.getText();
            boolean banned = field == bannedSearch;
            if (!query.equals(banned ? bannedQuery : enchantQuery)) {
                (banned ? bannedScroll : enchantScroll).reset();
                if (banned) bannedQuery = query; else enchantQuery = query;
            }
            String suggestion = enchantSuggestion(query);
            field.setAppend(suggestion.isEmpty() ? Map.of() : Map.of(suggestion,
                new dev.bkclient.ui.components.textfield.FieldAction(() -> field.setText(suggestion), () -> field.setText(suggestion))));
        }
        if (field == enchantSearch || field == bannedSearch) {
            field.setAlpha(alpha);
            field.setPreview(translate("Search..."));
            dev.bkclient.ui.menu.dropdown.components.settings.impl.SelectSettingComponent.renderSearchControl(context, field, x, y, w);
            return;
        }
        float fh = detachedEditor ? 16.0f : 12.0f;
        context.drawRoundedRect(x - 2.5f, y, w + 4f, fh, BorderRadius.all(4.0f),
                Colors.getBackgroundColor().withAlpha(155.0f * alpha));
        context.drawRoundedBorder(x - 2.5f, y, w + 4f, fh, 0.05f, BorderRadius.all(4.0f),
                Colors.getTextColor().withAlpha(14.0f * alpha));
        field.setAlpha(alpha);
        field.setTextColor(Colors.getTextColor());
        field.setPreview(translate("Search..."));
        field.set(x - 0.5f, y, w - 10.0f, fh);
        field.render(context);
        context.drawIcon(Fonts.ICONS, 'v', x + w - 9.0f, y + (fh - 5.5f) / 2.0f - 1.25f, 7.5f,
                Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
    }

    private void toggleRow(UIContext context, float x, float y, float w, String name, String hint, boolean on,
            float alpha) {
        Font font = Fonts.SEMIBOLD.getFont(7.0f);
        float centerY = y + 5.0f;
        context.drawText(font, translate(name), x, centerY - font.height() / 2.0f,
                Colors.getTextColor().withAlpha(255.0f * 0.75f * alpha));
        if (hint != null) {
            Font hintFont = Fonts.MEDIUM.getFont(6.5f);
            float hx = x + font.width(translate(name)) + 3.0f;
            context.drawText(hintFont, translate(hint), hx, centerY - hintFont.height() / 2.0f,
                    Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
        }
        var animation = this.toggleAnimations.computeIfAbsent(name, key -> new dev.bkclient.utility.animation.base.Animation(300L, on ? 1.0F : 0.0F, dev.bkclient.utility.animation.base.Easing.BAKEK));
        animation.update(on ? 1.0F : 0.0F);
        boolean pressed = this.pressedToggle != null && this.pressedToggle.name.equals(name);
        var press = this.togglePressAnimations.computeIfAbsent(name, key -> new dev.bkclient.utility.animation.base.Animation(200L, 0.0F, dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT));
        press.update(pressed);
        if (!dev.bkclient.ui.components.SettingControls.cleanOptions()) this.toggleHits.add(new ToggleHit(name, x, y - 2.0F, w, on));
        dev.bkclient.ui.components.SettingControls.option(context, x + w - TOGGLE_WIDTH, centerY - 5.0F,
            pressed ? this.toggleProgress : animation.getValue(), press.getValue(), alpha);
    }

    private String marginText(double value) {
        return String.format(Locale.US, "%.0f%%", value);
    }

    private void miniToggle(UIContext context, float x, float centerY, boolean on, float alpha) {
        dev.bkclient.ui.components.SettingControls.option(context, x, centerY - 5.0F, on ? 1.0F : 0.0F, 0.0F, alpha);
    }

    private void sectionLabel(UIContext context, float x, float y, float w, String name, String right, float alpha) {
        Font font = Fonts.SEMIBOLD.getFont(7.0f);
        context.drawText(font, translate(name), x, y, Colors.getTextColor().withAlpha(255.0f * 0.5f * alpha));
        context.drawRightText(font, right, x + w, y, Colors.getTextColor().withAlpha(255.0f * 0.25f * alpha));
    }

    private void panel(UIContext context, float px, float py, float pw, float ph, float alpha) {
        context.drawClientRect(px, py, pw, ph, alpha, 0.0f, PANEL_RADIUS);
    }

    public boolean handleClick(double mouseX, double mouseY) {
        if (detachedEditor && enchantRulesOpen && requiredSelector != null) {
            boolean hit = requiredSelector.isHovered(mouseX, mouseY) || forbiddenSelector.isHovered(mouseX, mouseY);
            requiredSelector.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            forbiddenSelector.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            if (hit) { unfocusAllTextFields(); unfocusAllSliders(); return true; }
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
                return true;
            }
            if (hit.containsRemove(mouseX, mouseY)) {
                this.removeEntry(hit.entry);
                return true;
            }
            if (hit.containsEdit(mouseX, mouseY)) {
                this.selectedEntry = hit.entry;
                this.editMode = true;
                this.syncedEditorEntry = null;
                return true;
            }
            if (hit.containsRow(mouseX, mouseY)) {
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
                for (ToggleHit toggle : this.toggleHits) {
                    if (Math.abs(toggle.x - hit.x) < 0.01F && Math.abs(toggle.y - hit.y) < 0.01F
                        && Math.abs(toggle.width - hit.w) < 0.01F) {
                        this.pressedToggle = toggle;
                        this.pressedToggleAction = hit.action;
                        this.toggleStartX = mouseX;
                        this.toggleProgress = toggle.on ? 1.0F : 0.0F;
                        this.toggleMoved = false;
                        return true;
                    }
                }
                hit.action.run();
                return true;
            }
        }

        return false;
    }

    public void handleRelease() {
        if (requiredSelector != null) {
            requiredSelector.onMouseReleased(-1, -1, MouseButton.LEFT);
            forbiddenSelector.onMouseReleased(-1, -1, MouseButton.LEFT);
        }
        if (this.pressedToggle != null) {
            boolean target = this.toggleMoved ? this.toggleProgress > 0.5F : !this.pressedToggle.on;
            var animation = this.toggleAnimations.get(this.pressedToggle.name);
            if (animation != null) animation.setValue(this.toggleProgress);
            Runnable action = this.pressedToggleAction;
            boolean changed = target != this.pressedToggle.on;
            this.pressedToggle = null;
            this.pressedToggleAction = null;
            if (changed && action != null) {
                action.run();
                if (this.module() != null) this.playToggleSound(target);
            }
        }
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
        if (this.pressedToggle != null) {
            double distance = mouseX - this.toggleStartX;
            if (this.toggleMoved || Math.abs(distance) > 3.0) {
                this.toggleMoved = true;
                this.toggleProgress = (float) Math.clamp((this.pressedToggle.on ? 1.0 : 0.0)
                        + distance / TOGGLE_TRAVEL, 0.0, 1.0);
            }
            return true;
        }
        return this.handleSliderDrag(mouseX);
    }

    private String enchantSuggestion(String query) {
        if (query == null || query.isBlank()) return "";
        String prefix = query.toLowerCase(Locale.ROOT);
        for (String enchant : ALL_ENCHANTS) if (enchant.startsWith(prefix)) return enchant;
        return "";
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (enchantRulesOpen && requiredSelector != null) for (var selector : List.of(requiredSelector, forbiddenSelector)) {
            if (selector.isSearchFocused()) { selector.onKeyPressed(keyCode, scanCode, modifiers); return true; }
        }
        TextField completionField = enchantSearch.isFocused() ? enchantSearch : bannedSearch.isFocused() ? bannedSearch : null;
        if (completionField != null && (keyCode == 258 || keyCode == 257)) {
            String suggestion = enchantSuggestion(completionField.getText());
            if (!suggestion.isEmpty()) {
                completionField.setText(suggestion);
                if (keyCode == 257 && selectedEntry != null && module() != null) {
                    if (completionField == bannedSearch) {
                        if (!selectedEntry.getBannedEnchantsList().contains(suggestion)) toggleBannedEnchant(module(), suggestion);
                    } else if (!selectedEntry.getEnchantments().containsKey(suggestion)) toggleRequiredEnchant(module(), suggestion);
                    completionField.setText("");
                }
                completionField.setFocused(true);
                (completionField == bannedSearch ? bannedScroll : enchantScroll).reset();
                return true;
            }
        }
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
        if (enchantRulesOpen && requiredSelector != null) for (var selector : List.of(requiredSelector, forbiddenSelector)) {
            if (selector.isSearchFocused()) return selector.charTyped(chr, modifiers);
        }
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
        if (detachedEditor && enchantRulesOpen && requiredSelector != null && !isAnySliderDragging()) {
            for (var selector : List.of(requiredSelector, forbiddenSelector)) {
                if (selector.isScrollAreaHovered(mouseX, mouseY) && selector.canScroll(verticalAmount)) {
                    selector.onScroll(mouseX, mouseY, 0, verticalAmount); return true;
                }
            }
        }
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
        if (module == null || syncedSettingsModule != module) {
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
        this.playToggleSound(entry.isEnabled());
    }

    private void playToggleSound(boolean enabled) {
        Sounds sounds = BKClientInit.getInstance().getModuleManager().getModule(Sounds.class);
        if (sounds != null && sounds.isEnabled()) {
            ClientSounds.MODULE.play(sounds.getVolume().getCurrentValue(), enabled ? 1.1F : 1.0F);
        }
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

    private void syncSlider(dev.bkclient.ui.menu.clickgui.widget.CsSlider slider, SliderSetting setting) {
        slider.setValue(setting.getCurrentValue());
    }

private void releaseSlider(dev.bkclient.ui.menu.clickgui.widget.CsSlider slider, SliderSetting setting) {
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
                        || ((!detachedEditor || advancedOpen) && this.minDaysSliderEntry != null && this.minDaysSlider.onClick(mouseX, mouseY)));
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

    private boolean dragSlider(dev.bkclient.ui.menu.clickgui.widget.CsSlider slider, SliderSetting setting, double mouseX) {
        // Setting setters persist profiles synchronously. Commit only on release.
        return slider.onDrag(mouseX);
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

    private boolean nudgeSlider(dev.bkclient.ui.menu.clickgui.widget.CsSlider slider, SliderSetting setting, float dir) {
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

    private record ToggleHit(String name, float x, float y, float width, boolean on) {}

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
