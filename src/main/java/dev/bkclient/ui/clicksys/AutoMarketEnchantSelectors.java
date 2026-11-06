package dev.bkclient.ui.clicksys;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import dev.bkclient.ui.clicksys.port.gui.widget.FilterDropdown;
import dev.bkclient.ui.clicksys.port.gui.widget.ScissorStack;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.SearchableListSetting;
import dev.bkclient.utility.gui.ScrollHandler;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.lwjgl.opengl.GL11;

/** The same multiselect used by Xray, bound to one market entry. */
public final class AutoMarketEnchantSelectors {
    private PriceEntry entry;
    private SearchableListSetting required, banned;
    private FilterDropdown requiredRow, bannedRow;
    private final ScrollHandler scroll = new ScrollHandler();
    private boolean syncing, visible;
    private float x, y, width, height;

    public void beginFrame() { visible = false; }

    public static String enchantmentLabel(String id) {
        String key = "enchantment." + (id.contains(":") ? id.replace(':', '.') : "minecraft." + id);
        String label = dev.bkclient.systems.localization.Localizator.translate(key);
        return label.equals(key) ? id : label;
    }

    private void bind(PriceEntry next, String[] options, Runnable save) {
        if (entry == next) return;
        entry = next;
        scroll.reset();
        // Preserve custom IDs as well as levels from existing configurations.
        var ids = new LinkedHashSet<>(List.of(options));
        ids.addAll(entry.getEnchantments().keySet());
        ids.addAll(entry.getBannedEnchantsList());
        required = new SearchableListSetting("Required Enchants", "", new ArrayList<>(ids),
            new ArrayList<>(entry.getEnchantments().keySet()), true, 0, AutoMarketEnchantSelectors::enchantmentLabel, dev.bkclient.systems.localization.Localizator::getCurrentLanguage,
            id -> id.replace('_', ' '));
        banned = new SearchableListSetting("Banned Enchants", "", new ArrayList<>(ids),
            new ArrayList<>(entry.getBannedEnchantsList()), true, 0, AutoMarketEnchantSelectors::enchantmentLabel, dev.bkclient.systems.localization.Localizator::getCurrentLanguage,
            id -> id.replace('_', ' '));
        required.setOnChange(() -> {
            if (syncing) return;
            List<String> chosen = required.getList3();
            entry.getEnchantments().keySet().retainAll(chosen);
            for (String id : chosen) entry.getEnchantments().putIfAbsent(id, 1);
            save.run();
        });
        banned.setOnChange(() -> {
            if (syncing) return;
            entry.getBannedEnchantsList().clear();
            entry.getBannedEnchantsList().addAll(banned.getList3());
            save.run();
        });
        requiredRow = new FilterDropdown(required);
        bannedRow = new FilterDropdown(banned);
    }

    /** Returns full content height; bounded mode scrolls inside the summary panel. */
    public float render(UIContext context, PriceEntry entry, String[] options, Runnable save,
            float x, float contentY, float width, float clipY, float clipHeight, float alpha, boolean bounded) {
        bind(entry, options, save);
        syncing = true;
        try {
            required.addList(new ArrayList<>(entry.getEnchantments().keySet()));
            banned.addList(new ArrayList<>(entry.getBannedEnchantsList()));
        } finally { syncing = false; }
        requiredRow.setValue237(width * 2);
        bannedRow.setValue237(width * 2);
        float total = (requiredRow.getFloat2() + bannedRow.getFloat2()) / 2 + 6;
        this.x = x;
        this.y = clipY;
        this.width = width;
        this.height = Math.max(0, clipHeight);
        visible = height > 0;
        if (!visible) return total;
        scroll.setMax(bounded ? Math.min(0, height - total) : 0);
        scroll.update();
        float rowY = contentY - (bounded ? (float) scroll.getValue() : 0);
        context.draw();
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] box = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
        ScissorStack.onFloatFloatFloatFloat(width * 2, height * 2, clipY * 2, x * 2);
        try {
            for (var row : List.of(requiredRow, bannedRow)) {
                row.onFloatFloat2(rowY * 2, x * 2);
                row.setValue238(row.getFloat2());
                row.onFloatFloatFloatMatrix4f(alpha, context.getMouseY() * 2, context.getMouseX() * 2,
                    AutoMarketControls.matrix(context));
                TextShader.update2();
                rowY += row.getFloat2() / 2 + 6;
            }
        } finally {
            ScissorStack.update();
            if (scissor) RenderSystem.enableScissor(box[0], box[1], box[2], box[3]);
            else RenderSystem.disableScissor();
        }
        return total;
    }

    private boolean contains(double mx, double my) {
        return visible && mx >= x && mx < x + width && my >= y && my < y + height;
    }

    public boolean click(double mx, double my) {
        if (!contains(mx, my)) { unfocus(); return false; }
        if (requiredRow.isIntDoubleDouble(0, mx * 2, my * 2)) {
            bannedRow.update3();
            return true;
        }
        if (bannedRow.isIntDoubleDouble(0, mx * 2, my * 2)) {
            requiredRow.update3();
            return true;
        }
        unfocus();
        return false;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        return visible && (requiredRow.isIntIntInt2(scan, mods, key)
            || bannedRow.isIntIntInt2(scan, mods, key));
    }

    public boolean charTyped(char character, int mods) {
        return visible && (requiredRow.isIntChar(mods, character) || bannedRow.isIntChar(mods, character));
    }

    private void unfocus() {
        if (requiredRow != null) { requiredRow.update3(); bannedRow.update3(); }
    }

    public boolean scroll(double mx, double my, double amount, boolean bounded) {
        if (!contains(mx, my)) return false;
        if (requiredRow.isDoubleDoubleDouble(amount, mx * 2, my * 2)
                || bannedRow.isDoubleDoubleDouble(amount, mx * 2, my * 2)) return true;
        if (bounded) { scroll.scroll(amount); return true; }
        return false;
    }

    public boolean drag(double mx, double my) {
        return visible && (requiredRow.isDoubleDoubleIntDoubleDouble(my * 2, mx * 2, 0, 0, 0)
            || bannedRow.isDoubleDoubleIntDoubleDouble(my * 2, mx * 2, 0, 0, 0));
    }

    public void release() {
        if (requiredRow != null) {
            requiredRow.isDoubleDoubleInt(0, 0, 0);
            bannedRow.isDoubleDoubleInt(0, 0, 0);
        }
    }

    public void deactivate() {
        visible = false;
        unfocus();
        scroll.reset();
    }
}
