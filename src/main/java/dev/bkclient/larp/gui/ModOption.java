package dev.bkclient.larp.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import dev.bkclient.larp.gui.Setting;

public class ModOption {
    public static final int HEIGHT = 18;
    public final String id;
    public final String name;
    public final List<Setting> settings = new ArrayList<Setting>();
    public dev.bkclient.larp.feature.FakeItemOption item;
    public ModOption item(dev.bkclient.larp.feature.FakeItemOption value) { this.item = value; return this; }
    public boolean enabled;
    public boolean settingsOpen;
    private ToggleHandler toggleHandler;

    public ModOption(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public ModOption with(Setting setting) {
        this.settings.add(setting);
        return this;
    }

    public ModOption onToggle(ToggleHandler handler) {
        this.toggleHandler = handler;
        return this;
    }

    public void toggle() {
        boolean target;
        boolean bl = target = !this.enabled;
        if (this.toggleHandler == null || this.toggleHandler.onToggle(target)) {
            this.enabled = target;
        }
    }

    public int totalHeight() {
        int h = 18;
        if (this.settingsOpen) {
            for (Setting s : this.settings) {
                h += s.getHeight();
            }
        }
        return h;
    }

    public int render(DrawContext ctx, TextRenderer tr, int x, int y, int width, int mouseX, int mouseY) {
        boolean hovered = ModOption.inside(mouseX, mouseY, x, y, width, 18);
        ctx.fill(x, y, x + width, y + 18, hovered ? -534829518 : -535554015);
        ctx.drawText(tr, this.name, x + 10, y + 5, this.enabled ? -1643790 : -8090209, false);
        int pillW = 16;
        int pillH = 8;
        int px = x + width - pillW - 10;
        int py = y + (18 - pillH) / 2;
        ctx.fill(px, py, px + pillW, py + pillH, this.enabled ? -13934664 : -13288632);
        int knobX = this.enabled ? px + pillW - 7 : px + 1;
        ctx.fill(knobX, py + 1, knobX + 6, py + pillH - 1, this.enabled ? -11760129 : -6774350);
        if (!this.settings.isEmpty()) {
            String arrow = this.settingsOpen ? "v" : ">";
            ctx.drawText(tr, arrow, px - 11, y + 5, this.settingsOpen ? -11760129 : -8090209, false);
        }
        int cy = y + 18;
        if (this.settingsOpen) {
            for (Setting s : this.settings) {
                s.render(ctx, tr, x, cy, width, ModOption.inside(mouseX, mouseY, x, cy, width, s.getHeight()));
                cy += s.getHeight();
            }
        }
        return cy;
    }

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
    }

    @FunctionalInterface
    public static interface ToggleHandler {
        public boolean onToggle(boolean var1);
    }
}
