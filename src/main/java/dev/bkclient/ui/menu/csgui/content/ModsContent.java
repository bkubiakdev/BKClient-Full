/*
 * ModsContent — zawartość kategorii modułów (Mods / Visuals / Player / Misc) w CsGui.
 * Karty modułów w 3 kolumnach (masonry), pionowy scroll z clippingiem, routing klawiatury/myszy
 * do aktywnej karty (bind modułu / focus text-boxa). Lista modułów realnie z ModuleManagera.
 */
package dev.bkclient.ui.menu.csgui.content;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.ui.menu.csgui.content.mods.ModuleCard;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsColorPicker;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsOverlayHost;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsSelectRow;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.render.ScissorUtility;

public class ModsContent implements CsOverlayHost {

    private static final int COLUMNS = 3;
    private static final float COL_GAP = 8.0f;
    private static final float CARD_GAP = 6.0f;
    private static final float PAD_X = 8.0f;
    private static final float PAD_Y = 8.0f;

    private final Map<ModuleCategory, List<ModuleCard>> cardsByCategory = new EnumMap<>(ModuleCategory.class);
    private final List<ModuleCard> allCards = new ArrayList<>();
    private boolean built;
    private String moduleStateSignature = "";

    private final ScrollHandler scroll = new ScrollHandler();
    private ModuleCategory lastCategory;
    private ModuleCard capturingCard;
    private CsColorPicker colorPicker;
    private CsSelectRow activeDropdown;
    private CsSelectRow closingDropdown;   // dropdown w trakcie animacji zamykania
    private String searchQuery = "";       // filtr wyszukiwania (nazwa modułu)

    /** Ustawia zapytanie wyszukiwania; gdy się zmieni — restart staggera i scrolla. */
    public void setSearchQuery(String query) {
        this.refreshModuleCards();
        String q = query == null ? "" : query;
        if (!q.equals(this.searchQuery)) {
            this.searchQuery = q;
            this.scroll.reset();
            if (this.lastCategory != null) {
                List<ModuleCard> shown = this.hasSearch()
                        ? this.allFiltered()
                        : this.filtered(this.cardsFor(this.lastCategory));
                for (int i = 0; i < shown.size(); ++i) {
                    shown.get(i).triggerAppear(i * 18L);
                }
            }
        }
    }

    private List<ModuleCard> filtered(List<ModuleCard> source) {
        if (this.searchQuery.isEmpty()) {
            return source;
        }
        String q = this.searchQuery.toLowerCase();
        List<ModuleCard> out = new ArrayList<>();
        for (ModuleCard c : source) {
            if (c.getModule().getName().toLowerCase().contains(q)) {
                out.add(c);
            }
        }
        return out;
    }

    public boolean hasSearch() {
        return !this.searchQuery.isEmpty();
    }

    /** Wszystkie karty (ze wszystkich kategorii) pasujące do zapytania. */
    private List<ModuleCard> allFiltered() {
        this.refreshModuleCards();
        String q = this.searchQuery.toLowerCase();
        List<ModuleCard> out = new ArrayList<>();
        for (ModuleCard c : this.allCards) {
            if (c.getModule().getName().toLowerCase().contains(q)) {
                out.add(c);
            }
        }
        return out;
    }

    private void build() {
        this.build(this.currentModuleStateSignature());
    }

    private void build(String signature) {
        this.cardsByCategory.clear();
        this.allCards.clear();
        List<Module> modules = BKClientInit.getInstance().getModuleManager().getModules();
        for (Module m : modules) {
            if (m.isHidden()) {
                continue;
            }
            ModuleCategory cat = m.getCategory();
            ModuleCard card = new ModuleCard(m, this);
            this.cardsByCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(card);
            this.allCards.add(card);
        }
        this.moduleStateSignature = signature;
        this.built = true;
    }

    private String currentModuleStateSignature() {
        List<Module> modules = BKClientInit.getInstance().getModuleManager().getModules();
        StringBuilder signature = new StringBuilder(modules.size() * 32);
        for (Module m : modules) {
            signature.append(m.getName())
                    .append('|')
                    .append(m.getCategory().name())
                    .append('|')
                    .append(m.isHidden())
                    .append('\n');
        }
        return signature.toString();
    }

    private void refreshModuleCards() {
        String signature = this.currentModuleStateSignature();
        if (!this.built) {
            this.build(signature);
            return;
        }
        if (signature.equals(this.moduleStateSignature)) {
            return;
        }
        this.scroll.reset();
        this.lastCategory = null;
        this.capturingCard = null;
        this.colorPicker = null;
        this.activeDropdown = null;
        this.closingDropdown = null;
        this.build(signature);
    }

    private List<ModuleCard> cardsFor(ModuleCategory cat) {
        this.refreshModuleCards();
        if (cat == ModuleCategory.COMBAT) {
            List<ModuleCard> combined = new ArrayList<>(this.cardsByCategory.getOrDefault(ModuleCategory.COMBAT, List.of()));
            combined.addAll(this.cardsByCategory.getOrDefault(ModuleCategory.MOVEMENT, List.of()));
            return combined;
        }
        return this.cardsByCategory.getOrDefault(cat, List.of());
    }

    public void render(UIContext context, ModuleCategory category, float x, float y, float w, float h, float alpha) {
        this.refreshModuleCards();
        if (category != this.lastCategory) {
            this.scroll.reset();
            this.clearCaptureExcept(null);
            if (this.activeDropdown != null) {
                this.activeDropdown.setOpen(false);
                this.closingDropdown = this.activeDropdown;
                this.activeDropdown = null;
            }
            this.lastCategory = category;
            List<ModuleCard> shown = this.filtered(this.cardsFor(category));
            for (int i = 0; i < shown.size(); ++i) {
                shown.get(i).triggerAppear(i * 25L);
            }
        }

        List<ModuleCard> cards = this.hasSearch()
                ? this.allFiltered()
                : this.filtered(this.cardsFor(category));
        if (cards.isEmpty()) {
            return;
        }

        float colW = (w - PAD_X * 2.0f - COL_GAP * (COLUMNS - 1)) / COLUMNS;
        int[] cols = new int[cards.size()];
        for (int i = 0; i < cards.size(); ++i) {
            cols[i] = i % COLUMNS;
        }
        float[] colY = new float[COLUMNS];
        float scrollY = (float) this.scroll.getValue();
        float top = y + PAD_Y;

        ScissorUtility.push(context.getMatrices(), x, y, w, h);
        for (int i = 0; i < cards.size(); ++i) {
            ModuleCard card = cards.get(i);
            int col = cols[i];
            float cardX = x + PAD_X + col * (colW + COL_GAP);
            float cardY = top + colY[col] - scrollY;
            card.render(context, cardX, cardY, colW, alpha);
            colY[col] += card.getHeight() + CARD_GAP;
        }
        ScissorUtility.pop();

        float maxColH = 0.0f;
        for (float cy : colY) {
            maxColH = Math.max(maxColH, cy);
        }
        double max = Math.min(0.0, (h - PAD_Y * 2.0) - maxColH);
        this.scroll.setMax(max);
        this.scroll.update();

        if (false && this.colorPicker != null) {
            this.colorPicker.render(context, alpha);
            if (this.colorPicker.isDead()) {
                this.colorPicker = null;
            }
        }

        if (false && this.activeDropdown != null) {
            this.activeDropdown.renderDropdown(context, alpha);
        }
        // dropdown w trakcie zamykania — renderuj aż animacja się skończy
        if (false && this.closingDropdown != null && this.closingDropdown != this.activeDropdown) {
            this.closingDropdown.renderDropdown(context, alpha);
            if (!this.closingDropdown.isAnimating()) {
                this.closingDropdown = null;
            }
        }
    }

    public void renderOverlay(UIContext context, float alpha) {
        if (this.colorPicker != null) {
            this.colorPicker.render(context, alpha);
            if (this.colorPicker.isDead()) {
                this.colorPicker = null;
            }
        }

        if (this.activeDropdown != null) {
            this.activeDropdown.renderDropdown(context, alpha);
        }
        if (this.closingDropdown != null && this.closingDropdown != this.activeDropdown) {
            this.closingDropdown.renderDropdown(context, alpha);
            if (!this.closingDropdown.isAnimating()) {
                this.closingDropdown = null;
            }
        }
    }

    public boolean handleOverlayClick(double mouseX, double mouseY, MouseButton button) {
        this.refreshModuleCards();
        if (this.colorPicker != null && !this.colorPicker.isClosing()) {
            if (this.colorPicker.onClick(mouseX, mouseY, button)) {
                return true;
            }
            if (button == MouseButton.LEFT && !this.colorPicker.isHovered(mouseX, mouseY)) {
                this.colorPicker.close();
            }
        }

        if (this.activeDropdown != null) {
            if (this.activeDropdown.handleDropdownClick(mouseX, mouseY, button)) {
                return true;
            }
            if (button == MouseButton.LEFT && !this.activeDropdown.isFieldHovered(mouseX, mouseY)) {
                this.activeDropdown.setOpen(false);
                this.closingDropdown = this.activeDropdown;
                this.activeDropdown = null;
            }
        }
        return false;
    }

    public boolean handleClick(ModuleCategory category, double mouseX, double mouseY, MouseButton button) {
        this.refreshModuleCards();
        if (this.handleOverlayClick(mouseX, mouseY, button)) {
            return true;
        }
        if (this.colorPicker != null && !this.colorPicker.isClosing()) {
            if (this.colorPicker.onClick(mouseX, mouseY, button)) {
                return true;
            }
            if (button == MouseButton.LEFT && !this.colorPicker.isHovered(mouseX, mouseY)) {
                this.colorPicker.close();
            }
        }

        if (this.activeDropdown != null) {
            if (this.activeDropdown.handleDropdownClick(mouseX, mouseY, button)) {
                return true;
            }
            // klik w pole tego samego selecta → toggle (obsłuży handleClick karty niżej);
            // klik gdziekolwiek indziej → zamknij dropdown
            if (button == MouseButton.LEFT && !this.activeDropdown.isFieldHovered(mouseX, mouseY)) {
                this.activeDropdown.setOpen(false);
                this.closingDropdown = this.activeDropdown;
                this.activeDropdown = null;
            }
        }

        // przechwyt myszy dla bindu modułu (nie-lewy przycisk gdy karta nasłuchuje)
        if (this.capturingCard != null && this.capturingCard.isBindingMode() && button != MouseButton.LEFT) {
            this.capturingCard.applyMouseBind(button);
            return true;
        }

        ModuleCard consumed = null;
        java.util.List<ModuleCard> clickCards = this.hasSearch()
                ? this.allFiltered()
                : this.filtered(this.cardsFor(category));
        for (ModuleCard card : clickCards) {
            if (card.handleClick(mouseX, mouseY, button)) {
                consumed = card;
                break;
            }
        }

        if (consumed != null) {
            this.clearCaptureExcept(consumed);
            this.capturingCard = consumed.isCapturing() ? consumed : null;
            return true;
        }
        this.clearCaptureExcept(null);
        this.capturingCard = null;
        return false;
    }

    private void clearCaptureExcept(ModuleCard keep) {
        for (ModuleCard card : this.allCards) {
            if (card != keep) {
                card.clearCapture();
            }
        }
    }

    public void handleRelease() {
        if (this.colorPicker != null) {
            this.colorPicker.onRelease();
        }
        for (ModuleCard card : this.allCards) {
            card.handleRelease();
        }
    }

    @Override
    public void openColorPicker(ColorSetting setting, float anchorX, float anchorY) {
        if (this.colorPicker != null && this.colorPicker.getSetting() == setting && !this.colorPicker.isClosing()) {
            this.colorPicker.close();
            return;
        }
        this.colorPicker = new CsColorPicker(setting, anchorX - 44.0f, anchorY + 8.0f);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.activeDropdown != null && this.activeDropdown.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.capturingCard != null) {
            boolean consumed = this.capturingCard.keyPressed(keyCode, scanCode, modifiers);
            if (!this.capturingCard.isCapturing()) {
                this.capturingCard = null;
            }
            return consumed;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (this.activeDropdown != null && this.activeDropdown.charTyped(chr)) {
            return true;
        }
        if (this.capturingCard != null) {
            return this.capturingCard.charTyped(chr);
        }
        return false;
    }

    public boolean isCapturing() {
        return this.capturingCard != null && this.capturingCard.isCapturing();
    }
    public void openDropdown(CsSelectRow row) {
        if (this.activeDropdown == row) {
            row.setOpen(false);
            this.closingDropdown = row;
            this.activeDropdown = null;
            return;
        }
        if (this.activeDropdown != null) {
            this.activeDropdown.setOpen(false);
            this.closingDropdown = this.activeDropdown;
        }
        row.setOpen(true);
        this.activeDropdown = row;
    }

    @Override
    public void closeDropdown(CsSelectRow row) {
        if (this.activeDropdown == row) {
            row.setOpen(false);
            this.closingDropdown = row;
            this.activeDropdown = null;
        }
    }
    public void onScroll(double mouseX, double mouseY, double amount) {
        // gdy dropdown otwarty — kółko przewija jego listę
        if (this.activeDropdown != null && this.activeDropdown.isOpen()) {
            this.activeDropdown.onScroll(amount);
            return;
        }
        this.scroll.scroll(amount);
    }

    public void resetScroll() {
        this.scroll.reset();
    }

}
