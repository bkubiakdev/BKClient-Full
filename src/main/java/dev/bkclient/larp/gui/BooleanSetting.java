package dev.bkclient.larp.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import dev.bkclient.larp.gui.Setting;

public class BooleanSetting
extends Setting {
    public boolean value;

    public BooleanSetting(String id, String name, boolean value) {
        super(id, name);
        this.value = value;
    }

    @Override
    public int getHeight() {
        return 13;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, int x, int y, int width, boolean hovered) {
        this.drawFrame(ctx, x, y, width, hovered);
        ctx.drawText(tr, this.name, x + 15, y + 3, this.value ? -1643790 : -8090209, false);
        int box = 7;
        int bx = x + width - box - 9;
        int by = y + (this.getHeight() - box) / 2;
        ctx.fill(bx, by, bx + box, by + box, this.value ? -11760129 : -13288632);
        ctx.drawBorder(bx, by, box, box, -13946302);
    }

    @Override
    public void click(double mouseX, int button) {
        this.value = !this.value;
        this.changed();
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(this.value);
    }

    @Override
    public void load(JsonElement el) {
        if (el != null && el.isJsonPrimitive()) {
            this.value = el.getAsBoolean();
        }
    }
}
