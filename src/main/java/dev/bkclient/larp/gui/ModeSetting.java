package dev.bkclient.larp.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import dev.bkclient.larp.gui.Setting;

public class ModeSetting
extends Setting {
    public final List<String> modes;
    public int index;

    public ModeSetting(String id, String name, List<String> modes, int index) {
        super(id, name);
        this.modes = List.copyOf(modes);
        this.index = index;
    }

    public String getMode() {
        return this.modes.get(this.index);
    }

    @Override
    public int getHeight() {
        return 13;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int x, int y, int width, boolean hovered) {
        this.drawFrame(ctx, x, y, width, hovered);
        ctx.drawText(tr, this.name, x + 15, y + 3, -8090209, false);
        String mode = this.getMode();
        ctx.drawText(tr, mode, x + width - 9 - tr.getWidth(mode), y + 3, -11760129, false);
    }

    @Override
    public void click(double mouseX, int button) {
        this.index = button == 1 ? (this.index - 1 + this.modes.size()) % this.modes.size() : (this.index + 1) % this.modes.size();
        this.changed();
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(this.index);
    }

    @Override
    public void load(JsonElement el) {
        int i;
        if (el != null && el.isJsonPrimitive() && (i = el.getAsInt()) >= 0 && i < this.modes.size()) {
            this.index = i;
        }
    }
}
