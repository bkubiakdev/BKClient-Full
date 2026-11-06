package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import java.util.*;
import java.util.function.Consumer;

/** Global module/setting navigation, with ranked matches and keyboard selection. */
public final class GuiSearchOverlay {
    public record Result(ClickGuiModuleCard card, Setting setting, String title, String path, int rank) {}
    private final List<Result> results = new ArrayList<>();
    private record RowHit(int index, float top) {}
    private final List<RowHit> rowHits = new ArrayList<>();
    private String query;
    private int selected, first;
    private float x, y, width, height;
    private static final int ROWS = 8;
    private static final float RESULTS_TOP = 44;
    private final dev.bkclient.utility.animation.base.Animation opening = new dev.bkclient.utility.animation.base.Animation(140L, dev.bkclient.utility.animation.base.Easing.QUARTIC_OUT);
    private static final float ROW_HEIGHT = 24;

    public static String normalize(String text) {
        return text == null ? "" : java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace("ł", "l").replaceAll("[^\\p{L}\\p{N}]", "");
    }
    public static int rank(String title, String description, String query) {
        String name = normalize(title), q = normalize(query);
        if (q.isEmpty()) return -1;
        if (name.equals(q)) return 0;
        if (name.startsWith(q)) return 1;
        if (name.contains(q)) return 2;
        return normalize(description).contains(q) ? 3 : -1;
    }
    public void invalidate() { query = null; }
    private void refresh(TextField field, List<ClickGuiModuleCard> cards) {
        String current = field.getText();
        if (Objects.equals(query, current)) return;
        query = current; results.clear(); selected = first = 0;
        if (normalize(current).isEmpty()) return;
        for (var card : cards) {
            var module = card.getModule();
            int rank = rank(module.getName(), module.getDescription(), current);
            if (rank < 0 && normalize(module.getClass().getSimpleName()).contains(normalize(current))) rank = 2;
            if (rank >= 0) results.add(new Result(card, null, module.getName(), module.getCategory().name(), rank));
            for (var setting : module.getDisplaySettings()) {
                if (!setting.isVisible()) continue;
                String name = Localizator.translate(setting.getName());
                String description = Localizator.translate(setting.getDescription());
                rank = rank(name, description, current);
                if (rank >= 0) results.add(new Result(card, setting, name, module.getName(), rank));
            }
        }
        results.sort(Comparator.comparing((Result r) -> r.setting() != null).thenComparingInt(Result::rank)
            .thenComparing(Result::title, String.CASE_INSENSITIVE_ORDER).thenComparing(Result::path));
    }
    public void render(UIContext c, TextField field, List<ClickGuiModuleCard> cards,
                       float areaX, float areaY, float areaWidth, float areaHeight, float alpha) {
        if (!field.isFocused()) { opening.setValue(0); return; }
        refresh(field, cards);
        float visibility = opening.update(1);
        width = Math.min(370, areaWidth - 32);
        height = 96 + Math.max(2, Math.min(ROWS, results.size())) * ROW_HEIGHT;
        x = areaX + (areaWidth - width) / 2;
        y = areaY + (areaHeight - height) / 2;
        alpha *= visibility;
        var theme = dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.current();
        ColorRGBA ink = theme.text, accent = Colors.getAccent();
        c.drawShadow(x, y + 3, width, height, 18, BorderRadius.all(9), ColorRGBA.BLACK.mulAlpha(.25F * alpha));
        c.drawClientRect(x, y, width, height, alpha, 0, 7);
        // Search entry and results share the centered overlay.
        c.drawRoundedRect(x + 9, y + 9, width - 18, 30, BorderRadius.all(6), ink.mulAlpha(.045F * alpha));
        c.drawIcon(Fonts.ICONS, 'v', x + 19, y + 19, 10, ink.mulAlpha(.65F * alpha));
        field.set(x + 33, y + 12, width - 78, 24);
        field.setPreview("Search modules and settings...");
        field.setAlpha(alpha);
        field.setTextColor(ink.mulAlpha(alpha));
        field.render(c);
        c.drawRoundedRect(x + width - 38, y + 18, 22, 12, BorderRadius.all(3), ink.mulAlpha(.055F * alpha));
        c.drawCenteredText(Fonts.REGULAR.getFont(5.5F), "Esc", x + width - 27, y + 21.5F, ink.mulAlpha(.55F * alpha));
        if (results.isEmpty()) {
            c.drawCenteredText(Fonts.MEDIUM.getFont(8), normalize(query).isEmpty() ? "Search modules and settings" : "No results found", x + width / 2, y + 72, ink.mulAlpha(.8F * alpha));
            c.drawCenteredText(Fonts.REGULAR.getFont(6), normalize(query).isEmpty() ? "Start typing to find an option." : "Try a shorter name.", x + width / 2, y + 88, ink.mulAlpha(.4F * alpha));
        }
        rowHits.clear();
        float rowY = y + RESULTS_TOP;
        Boolean previousSettings = null;
        for (int i = first; i < Math.min(first + ROWS, results.size()); i++) {
            Result result = results.get(i);
            boolean isSetting = result.setting() != null;
            if (previousSettings == null || previousSettings != isSetting) {
                c.drawText(Fonts.MEDIUM.getFont(6.5F), isSetting ? "Settings" : "Modules", x + 13, rowY + 2, ink.mulAlpha(.4F * alpha));
                rowY += 14;
                previousSettings = isSetting;
            }
            rowHits.add(new RowHit(i, rowY));
            boolean hover = GuiUtility.isHovered(x + 8, rowY, width - 16, ROW_HEIGHT, c);
            boolean active = i == selected;
            if (active || hover) c.drawRoundedRect(x + 8, rowY, width - 16, ROW_HEIGHT - 2,
                BorderRadius.all(4), ink.mulAlpha((active ? .075F : .035F) * alpha));
            resultIcon(result).draw(c, x + 15, rowY + 6, 9, ink.mulAlpha(.55F * alpha));
            var titleFont = Fonts.MEDIUM.getFont(8);
            var tagFont = Fonts.MEDIUM.getFont(5);
            String tag = isSetting ? typeLabel(result.setting()) : "";
            float tagWidth = isSetting ? tagFont.width(tag) + 7 : 0;
            float titleWidth = width - 164 - tagWidth;
            c.drawFadeoutText(titleFont, result.title(), x + 30, rowY + 7,
                ink.mulAlpha(alpha), .8F, 1, titleWidth);
            if (isSetting) {
                float tagX = x + 36 + Math.min(titleFont.width(result.title()), titleWidth);
                float tagCenterY = rowY + 7 + titleFont.height() / 2;
                c.drawRoundedRect(tagX, tagCenterY - 4.5F, tagWidth, 9, BorderRadius.all(2.5F), accent.mulAlpha(.13F * alpha));
                dev.bkclient.ui.components.BadgeText.drawGlyphCentered(c, tagFont, tag,
                    tagX + tagWidth / 2, tagCenterY, accent.mulAlpha(alpha));
            }
            String categoryName = result.card().getModule().getCategory().name();
            for (var category : dev.bkclient.ui.menu.api.MenuCategory.values()) {
                if (category.getCategory() == result.card().getModule().getCategory()) { categoryName = category.getName(); break; }
            }
            String path = categoryName + (isSetting ? " / " + result.card().getModule().getName() : "");
            c.drawFadeoutText(Fonts.REGULAR.getFont(6), path, x + width - 119, rowY + 8,
                ink.mulAlpha(.4F * alpha), .8F, 1, 105);
            if (hover) dev.bkclient.utility.game.cursor.CursorUtility.set(dev.bkclient.utility.game.cursor.CursorType.HAND);
            rowY += ROW_HEIGHT;
        }
        c.drawRect(x + 14, y + height - 20, width - 28, .5F, ink.mulAlpha(.07F * alpha));
        c.drawText(Fonts.REGULAR.getFont(5.5F), "Up / Down   Navigate", x + 15, y + height - 12, ink.mulAlpha(.4F * alpha));
        c.drawRightText(Fonts.REGULAR.getFont(5.5F), "Enter   Open", x + width - 15, y + height - 12, ink.mulAlpha(.4F * alpha));
        if (results.size() > ROWS) {
            float track = ROWS * ROW_HEIGHT, thumb = Math.max(14, track * ROWS / results.size());
            c.drawRoundedRect(x + width - 4, y + RESULTS_TOP + (track - thumb) * first / (results.size() - ROWS),
                1.5F, thumb, BorderRadius.all(.75F), ink.mulAlpha(.25F * alpha));
        }
    }
    private String typeLabel(Setting setting) {
        if (setting instanceof dev.bkclient.systems.setting.settings.BooleanSetting) return "CHECKBOX";
        if (setting instanceof dev.bkclient.systems.setting.settings.ColorSetting) return "COLOR";
        if (setting instanceof dev.bkclient.systems.setting.settings.SliderSetting) return "SLIDER";
        if (setting instanceof dev.bkclient.systems.setting.settings.RangeSetting) return "RANGE";
        if (setting instanceof dev.bkclient.systems.setting.settings.ModeSetting) return "COMBOBOX";
        if (setting instanceof dev.bkclient.systems.setting.settings.SelectSetting) return "MULTISELECT";
        if (setting instanceof dev.bkclient.systems.setting.settings.BindSetting) return "KEYBIND";
        if (setting instanceof dev.bkclient.systems.setting.settings.ButtonSetting) return "ACTION";
        return "SETTING";
    }
    private LucideIcons resultIcon(Result result) {
        if (result.setting() != null) return icon(result.setting());
        for (var category : dev.bkclient.ui.menu.api.MenuCategory.values())
            if (category.getCategory() == result.card().getModule().getCategory()) return LucideIcons.category(category);
        return LucideIcons.MORE;
    }
    private LucideIcons icon(Setting setting) {
        if (setting instanceof dev.bkclient.systems.setting.settings.ColorSetting) return LucideIcons.PALETTE;
        if (setting instanceof dev.bkclient.systems.setting.settings.BooleanSetting) return LucideIcons.CHECK;
        if (setting instanceof dev.bkclient.systems.setting.settings.SliderSetting) return LucideIcons.RULER;
        return LucideIcons.MORE;
    }
    public boolean click(double mx, double my, MouseButton button, TextField field, Consumer<Result> open) {
        if (!GuiUtility.isHovered(x, y, width, height, mx, my)) return false;
        if (GuiUtility.isHovered(x + width - 41, y + 10, 32, 28, mx, my)) {
            if (button == MouseButton.LEFT) field.setFocused(false);
            return true;
        }
        if (my < y + 42) { field.onMouseClicked(mx, my, button); field.setFocused(true); return true; }
        if (button == MouseButton.LEFT) {
            for (RowHit hit : rowHits) {
                if (my >= hit.top() && my < hit.top() + ROW_HEIGHT && hit.index() < results.size()) {
                    open.accept(results.get(hit.index()));
                    break;
                }
            }
        }
        return true;
    }
    public boolean key(int code, TextField field, List<ClickGuiModuleCard> cards, Consumer<Result> open) {
        refresh(field, cards);
        if (code == 256) { field.setFocused(false); return true; }
        if (code == 257 || code == 335) { if (!results.isEmpty()) open.accept(results.get(selected)); return true; }
        if (code == 264 || code == 265) {
            selected = Math.clamp(selected + (code == 264 ? 1 : -1), 0, Math.max(0, results.size() - 1));
            if (selected < first) first = selected;
            if (selected >= first + ROWS) first = selected - ROWS + 1;
            return true;
        }
        return false;
    }
    public void scroll(double amount) {
        first = Math.clamp(first - (int)Math.signum(amount) * 2, 0, Math.max(0, results.size() - ROWS));
        selected = Math.clamp(selected, first, Math.min(first + ROWS - 1, Math.max(0, results.size() - 1)));
    }
}
