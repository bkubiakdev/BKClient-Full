package dev.bkclient.larp.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import dev.bkclient.larp.gui.Hit;
import dev.bkclient.larp.gui.ModOption;
import dev.bkclient.larp.gui.Setting;

public class Category {
    public static final int WIDTH = 186;
    public static final int HEADER_HEIGHT = 20;
    private static final int FOOTER_HEIGHT = 3;
    public final String id;
    public final String name;
    public final List<ModOption> options = new ArrayList<ModOption>();
    public int x;
    public int y;
    public boolean expanded = true;

    public Category(String id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public Category with(ModOption option) {
        this.options.add(option);
        return this;
    }

    public int totalHeight() {
        int h = 20;
        if (this.expanded) {
            for (ModOption o : this.options) {
                h += o.totalHeight();
            }
            h += 3;
        }
        return h;
    }

    public void clampTo(int screenWidth, int screenHeight) {
        this.x = Math.max(0, Math.min(screenWidth - 186, this.x));
        int lowest = screenHeight - this.totalHeight();
        this.y = lowest < 0 ? Math.max(lowest, Math.min(0, this.y)) : Math.max(0, Math.min(lowest, this.y));
    }

    public void render(DrawContext ctx, TextRenderer tr, int mouseX, int mouseY, boolean dragged) {
        boolean headerHover = ModOption.inside(mouseX, mouseY, this.x, this.y, 186, 20);
        ctx.fill(this.x, this.y, this.x + 186, this.y + 20, headerHover || dragged ? -266394062 : -266986974);
        ctx.fill(this.x, this.y, this.x + 2, this.y + 20, -11760129);
        ctx.drawText(tr, this.name, this.x + 10, this.y + 6, -1643790, true);
        String arrow = this.expanded ? "v" : ">";
        ctx.drawText(tr, arrow, this.x + 186 - 12, this.y + 6, -8090209, false);
        int cy = this.y + 20;
        if (this.expanded) {
            for (ModOption o : this.options) {
                cy = o.render(ctx, tr, this.x, cy, 186, mouseX, mouseY);
            }
            ctx.fill(this.x, cy, this.x + 186, cy + 3, -266986974);
            cy += 3;
        }
        ctx.drawBorder(this.x, this.y, 186, cy - this.y, -13946302);
    }

    public Hit hit(double mouseX, double mouseY) {
        if (ModOption.inside(mouseX, mouseY, this.x, this.y, 186, 20)) {
            return new Hit(this, null, null, true);
        }
        if (!this.expanded) {
            return null;
        }
        int cy = this.y + 20;
        for (ModOption o : this.options) {
            if (ModOption.inside(mouseX, mouseY, this.x, cy, 186, 18)) {
                return new Hit(this, o, null, false);
            }
            cy += 18;
            if (!o.settingsOpen) continue;
            for (Setting s : o.settings) {
                if (ModOption.inside(mouseX, mouseY, this.x, cy, 186, s.getHeight())) {
                    return new Hit(this, o, s, false);
                }
                cy += s.getHeight();
            }
        }
        if (ModOption.inside(mouseX, mouseY, this.x, cy, 186, 3)) {
            return new Hit(this, null, null, false);
        }
        return null;
    }
}
